package com.offlineplaya.android.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.offlineplaya.shared.domain.audio.DacOutputManager
import com.offlineplaya.shared.domain.audio.OutputMode
import com.offlineplaya.shared.domain.audio.PcmEncoding
import com.offlineplaya.shared.domain.audio.PcmFormat
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Connects the [player] to the [DacOutputManager]:
 *
 *  - every time the playing track changes, its own format (rate, bit depth,
 *    channels — read from the extractor's [Format]) goes to
 *    [DacOutputManager.prepareFor], which requests or drops the bit-perfect mixer;
 *  - while bit-perfect is active the player is pinned to the DAC with
 *    [ExoPlayer.setPreferredAudioDevice], which also makes the AudioTrack
 *    re-route onto the newly opened bit-perfect output;
 *  - [bitPerfectActive] tells the EQ, preamp, volume-ramp and crossfade
 *    controllers to step out of the signal path.
 *
 * Built by PlaybackService before the other audio controllers (they take
 * [bitPerfectActive]) and released with the service. Player calls happen on the
 * main looper, the player's application thread.
 */
class DacPlaybackController(
    context: Context,
    private val player: ExoPlayer,
    private val manager: DacOutputManager,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mainDispatcher =
        Handler(Looper.getMainLooper()).asCoroutineDispatcher("DacPlaybackMain")

    /** `true` while the DAC gets an untouched stream; DSP controllers must bypass. */
    val bitPerfectActive: StateFlow<Boolean> = manager.state
        .map { !it.softwareDspAllowed }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    private var routingJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onTracksChanged(tracks: Tracks) = prepareFrom(tracks)
    }

    fun start() {
        manager.start()
        player.addListener(listener)
        prepareFrom(player.currentTracks)
        routingJob = scope.launch(mainDispatcher) {
            manager.state
                .map { state ->
                    val mode = state.mode
                    if (mode is OutputMode.BitPerfect) state.device?.id else null
                }
                .distinctUntilChanged()
                .collect { deviceId -> route(deviceId) }
        }
    }

    private fun prepareFrom(tracks: Tracks) {
        val format = tracks.selectedAudioFormat() ?: return
        if (format.sampleRate <= 0 || format.channelCount <= 0) return
        val source = PcmFormat(
            sampleRate = format.sampleRate,
            encoding = format.pcmEncoding.toSourceEncoding(),
            channelCount = format.channelCount,
        )
        val mode = manager.prepareFor(source)
        logger.d(TAG, "Track ${source.sampleRate} Hz / ${source.encoding} → $mode")
    }

    /** Pins the player to the DAC while bit-perfect, back to default routing otherwise. */
    private fun route(deviceId: Int?) {
        val device: AudioDeviceInfo? = deviceId?.let { id ->
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.id == id }
        }
        runCatching { player.setPreferredAudioDevice(device) }
            .onFailure { logger.w(TAG, "setPreferredAudioDevice failed: ${it.message}") }
    }

    fun release() {
        routingJob?.cancel()
        routingJob = null
        player.removeListener(listener)
        manager.releasePlayback()
        manager.stop()
    }

    private companion object {
        const val TAG = "DacPlayback"

        fun Tracks.selectedAudioFormat(): Format? {
            for (group in groups) {
                if (group.type != C.TRACK_TYPE_AUDIO) continue
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) return group.getTrackFormat(i)
                }
            }
            return null
        }

        /**
         * The file's own sample depth. FLAC and WAV extractors fill in
         * [Format.pcmEncoding]; lossy codecs leave it unset and decode to 16-bit.
         */
        fun Int.toSourceEncoding(): PcmEncoding = when (this) {
            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_24BIT_BIG_ENDIAN -> PcmEncoding.PCM_24
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_32BIT_BIG_ENDIAN -> PcmEncoding.PCM_32
            C.ENCODING_PCM_FLOAT -> PcmEncoding.PCM_FLOAT
            else -> PcmEncoding.PCM_16
        }
    }
}
