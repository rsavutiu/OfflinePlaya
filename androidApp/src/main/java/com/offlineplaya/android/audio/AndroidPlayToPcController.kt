package com.offlineplaya.android.audio

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.offlineplaya.android.net.NsdReceiverDiscovery
import com.offlineplaya.android.net.StreamSender
import com.offlineplaya.shared.domain.stream.PcReceiver
import com.offlineplaya.shared.domain.stream.PlayToPcController
import com.offlineplaya.shared.domain.stream.PlayToPcError
import com.offlineplaya.shared.domain.stream.PlayToPcState
import com.offlineplaya.shared.util.AppLogger
import com.offlineplaya.stream.StreamFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The phone's "Play to PC" brain: LAN discovery + the TCP [StreamSender] + the
 * connection state the UI observes. A Koin singleton so the picker UI and the
 * playback service share one source of truth; [active] is the DSP-bypass gate
 * the service folds in alongside `bitPerfectActive`.
 *
 * The player itself is owned by `PlaybackService`, so it is handed in via
 * [attachPlayer] once it exists. While connected this mutes the local output
 * (`player.volume = 0`) so the PCM goes to the PC "instead of the speaker" —
 * the muted AudioTrack still consumes at real time, which paces the stream for
 * free — and captures the decoded PCM off [PcmTap], forwarding it to the sender.
 */
class AndroidPlayToPcController(
    context: Context,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) : PlayToPcController, PcmTap.Listener {

    private val discovery = NsdReceiverDiscovery(context, logger)
    private val sender = StreamSender(logger)
    private val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    private val main = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(PlayToPcState())
    override val state: StateFlow<PlayToPcState> = _state.asStateFlow()

    private val _active = MutableStateFlow(false)
    override val active: StateFlow<Boolean> = _active.asStateFlow()

    /** Set by the player's seek/pause events; consumed in [onPcm] so a Flush is
     *  emitted in-band, immediately before the first post-seek audio frame. */
    private val pendingFlush = AtomicBoolean(false)

    // Player wiring (null until attachPlayer). Touched only on the main thread.
    private var player: ExoPlayer? = null
    private var tap: PcmTap? = null
    private var savedVolume = 1f
    private val playerListener = object : Player.Listener {
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            // Explicit seek or skip — drop the receiver's buffer so it's prompt.
            // The flush is emitted in-band right before the first post-seek PCM
            // (see onPcm), so old-position audio can't trail in after it. Auto
            // track advance (gapless) must NOT flush.
            if (reason == Player.DISCONTINUITY_REASON_SEEK) pendingFlush.set(true)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Pause: no more PCM will flow, so a pending flush would wait until
            // resume and the PC would play out a bufferful. Flush now instead.
            if (!isPlaying && _active.value) scope.launch { sender.enqueueFlush() }
        }
    }

    // ---- UI-facing API (PlayToPcController) ----

    override fun startDiscovery() {
        _state.update { it.copy(discovering = true, error = null) }
        discovery.start { receivers -> _state.update { it.copy(receivers = receivers) } }
    }

    override fun stopDiscovery() {
        discovery.stop()
        _state.update { it.copy(discovering = false, receivers = emptyList()) }
    }

    override fun connect(receiver: PcReceiver) = connectTo(receiver)

    override fun connectManual(host: String, port: Int) =
        connectTo(PcReceiver(name = host, host = host, port = port))

    override fun disconnect() {
        sender.close()
        _active.value = false
        tap?.listener = null
        setStreamingOutput(false)
        _state.update { it.copy(connected = null, connecting = null) }
    }

    private fun connectTo(receiver: PcReceiver) {
        _state.update { it.copy(connecting = receiver, error = null) }
        scope.launch {
            val ok = sender.connect(receiver.host, receiver.port, deviceName) {
                // Socket died later — return to local output.
                _active.value = false
                tap?.listener = null
                setStreamingOutput(false)
                _state.update {
                    it.copy(connected = null, connecting = null, error = PlayToPcError.ConnectionLost)
                }
            }
            if (ok) {
                // Capture decoded PCM only now — before this, handleBuffer must
                // not allocate/copy on the playback thread for every user.
                tap?.listener = this@AndroidPlayToPcController
                _state.update { it.copy(connected = receiver, connecting = null) }
                _active.value = true
                setStreamingOutput(true)
                // Prime the receiver with the current format so it opens its
                // line before the first audio frame arrives.
                tap?.currentFormat?.let { sender.sendFormat(it) }
            } else {
                _state.update {
                    it.copy(
                        connecting = null,
                        error = PlayToPcError.ConnectFailed(receiver.host, receiver.port),
                    )
                }
            }
        }
    }

    // ---- Player wiring (called by PlaybackService) ----

    fun attachPlayer(player: ExoPlayer, tap: PcmTap) {
        this.player = player
        this.tap = tap
        player.addListener(playerListener)
        // Honour current state if the service restarted mid-stream.
        if (_active.value) {
            tap.listener = this
            setStreamingOutput(true)
        }
    }

    fun detachPlayer() {
        player?.removeListener(playerListener)
        tap?.listener = null
        player = null
        tap = null
    }

    // ---- PcmTap.Listener (ExoPlayer playback thread) ----

    override fun onPcm(pcm: ByteArray, format: StreamFormat) {
        if (!sender.isConnected) return
        if (pendingFlush.getAndSet(false)) sender.enqueueFlush()
        sender.sendFormat(format) // no-op unless the format changed
        sender.enqueuePcm(pcm)
    }

    /**
     * Routes audio to the PC "instead of the speaker" by muting the local
     * output, and keeps the CPU/WiFi awake so a screen-off album doesn't starve
     * the receiver's ring buffer.
     *
     * The mute is re-asserted shortly after enabling because a crossfade that
     * happens to be mid-fade at connect restores `mainPlayer.volume` once as it
     * aborts on bypass; the re-assert wins that narrow race (crossfade is then
     * suppressed and never touches volume again).
     */
    private fun setStreamingOutput(streaming: Boolean) {
        main.post {
            val p = player ?: return@post
            if (streaming) {
                if (p.volume > 0f) savedVolume = p.volume
                p.volume = 0f
                p.setWakeMode(C.WAKE_MODE_NETWORK)
                main.postDelayed({ if (_active.value) player?.volume = 0f }, MUTE_REASSERT_MS)
            } else {
                p.volume = savedVolume
                p.setWakeMode(C.WAKE_MODE_NONE)
            }
        }
    }

    private companion object {
        const val MUTE_REASSERT_MS = 300L
    }
}
