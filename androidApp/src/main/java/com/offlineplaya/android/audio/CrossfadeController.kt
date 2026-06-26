package com.offlineplaya.android.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.offlineplaya.shared.domain.model.PlaybackPreferences
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * True overlapping crossfade as an **overlay** on the existing single
 * [ExoPlayer], not a replacement for it.
 *
 * The [mainPlayer] stays the MediaSession's one and only player — it keeps
 * owning the queue, timeline, notification, Android Auto browse and the EQ
 * audio session exactly as before, so crossfade-OFF behaviour is byte-for-byte
 * the old behaviour (native gapless preserved). All this controller adds is a
 * throwaway [secondaryPlayer] used purely to keep the *outgoing* track audible
 * for the overlap:
 *
 * ```
 *  approaching end of track N (remaining ≤ crossfade):
 *    secondary  ← load track N, play its tail, fade DOWN
 *    mainPlayer → seekToNext()  (jump to N+1 early), fade UP from 0
 *  both audible at once  ⇒  real crossfade
 * ```
 *
 * The secondary shares [audioSessionId] with the main player so the EQ applies
 * to both halves of the overlap. It deliberately does **not** request audio
 * focus — the main player already holds it.
 *
 * Lifecycle mirrors [AppEqualizerController]: built by `PlaybackService` after
 * the ExoPlayer exists, released with the service. Every player touch happens
 * on the main looper (the player's application thread).
 */
class CrossfadeController(
    private val context: Context,
    private val mainPlayer: ExoPlayer,
    private val audioSessionId: Int,
    private val preferences: StateFlow<PlaybackPreferences>,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {

    private val mainDispatcher =
        Handler(Looper.getMainLooper()).asCoroutineDispatcher("CrossfadeMain")

    private var secondaryPlayer: ExoPlayer? = null

    /** Latest preferences; written from the collector, read on the main loop. */
    @Volatile
    private var prefs: PlaybackPreferences = PlaybackPreferences.Default

    // phase and targetVolume are NOT volatile on purpose: every reader/writer
    // (tick, arm, trigger, the fade loop, finishFade, abort, the player listener,
    // release) runs on the main looper. tickJob and fadeJob are both launched on
    // mainDispatcher, so they interleave cooperatively at suspension points but
    // never run in parallel — single-thread confinement, no data race. Only
    // [prefs] crosses threads (collector → tick), which is why only it is @Volatile.
    private enum class Phase { IDLE, ARMED, FADING }
    private var phase = Phase.IDLE

    /** User-facing volume captured before we start muting, restored after. */
    private var targetVolume = 1f

    private var tickJob: Job? = null
    private var fadeJob: Job? = null
    private var prefsJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // The seekToNext() we issue in trigger() is the ONE transition we
            // expect (reason SEEK, already in FADING) — let it pass. Anything
            // else (user skip, an un-crossfaded natural advance, queue edit)
            // invalidates the overlap, so bail out cleanly.
            if (phase == Phase.FADING && reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) return
            if (phase != Phase.IDLE) abort()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            // Pause mid-overlap: stop the secondary so the outgoing tail doesn't
            // keep playing into silence while the main player is paused.
            if (!playWhenReady && phase != Phase.IDLE) abort()
        }

        override fun onPlayerError(error: PlaybackException) {
            if (phase != Phase.IDLE) abort()
        }
    }

    fun start() {
        mainPlayer.addListener(listener)
        prefsJob = scope.launch {
            preferences.collect { updated ->
                prefs = updated
                if (!updated.crossfadeEnabled) withContext(mainDispatcher) { abort() }
            }
        }
        tickJob = scope.launch(mainDispatcher) {
            while (true) {
                tick()
                delay(TICK_MS)
            }
        }
    }

    /** One scheduler step. Runs on the main loop. */
    private fun tick() {
        val current = prefs
        if (!current.crossfadeEnabled) return
        if (phase == Phase.FADING) return
        // Repeat-one must keep replaying the same track; seekToNext() would jump
        // away from it, so crossfade is suppressed (correctness, not just perf).
        if (mainPlayer.repeatMode == Player.REPEAT_MODE_ONE) return
        if (!mainPlayer.isPlaying) return
        if (!mainPlayer.hasNextMediaItem()) return
        if (mainPlayer.currentMediaItem == null) return

        val duration = mainPlayer.duration
        if (duration <= 0L || duration == C.TIME_UNSET) return
        val remaining = duration - mainPlayer.currentPosition
        if (remaining <= 0L) return

        val xfadeMs = current.crossfadeDurationMs
        when (phase) {
            Phase.IDLE -> when {
                remaining <= xfadeMs + PREPARE_LEAD_MS && remaining > xfadeMs -> arm()
                // Missed the pre-prepare window (just-enabled, or a track shorter
                // than the lead): arm and fire together. The secondary buffers a
                // beat late, but only on a tail that's fading out anyway.
                remaining <= xfadeMs && remaining > MIN_TRIGGER_MS -> {
                    arm()
                    if (phase == Phase.ARMED) trigger()
                }
            }
            Phase.ARMED -> if (remaining <= xfadeMs) trigger()
            Phase.FADING -> Unit
        }
    }

    /** Pre-prepare the secondary on the outgoing track, paused and silent. */
    private fun arm() {
        val outgoing = mainPlayer.currentMediaItem ?: return
        val secondary = ensureSecondary()
        // Anticipate roughly where the main player will be at trigger time so the
        // tail picks up near the splice instead of replaying the lead window.
        val anticipated = (mainPlayer.currentPosition + PREPARE_LEAD_MS)
            .coerceAtMost((mainPlayer.duration - 50L).coerceAtLeast(0L))
        targetVolume = mainPlayer.volume.takeIf { it > 0f } ?: 1f
        secondary.volume = 0f
        secondary.setMediaItem(outgoing)
        secondary.prepare()
        secondary.seekTo(anticipated)
        secondary.playWhenReady = false
        phase = Phase.ARMED
    }

    /** Start the overlap: secondary plays the tail, main jumps ahead, fade. */
    private fun trigger() {
        val secondary = secondaryPlayer ?: run { phase = Phase.IDLE; return }
        // Set FADING before seekToNext() so the resulting transition callback
        // recognises itself as ours and doesn't abort.
        phase = Phase.FADING
        secondary.volume = targetVolume
        secondary.play()
        mainPlayer.volume = 0f
        mainPlayer.seekToNextMediaItem()
        startFade()
    }

    private fun startFade() {
        fadeJob?.cancel()
        fadeJob = scope.launch(mainDispatcher) {
            val xfadeMs = prefs.crossfadeDurationMs.coerceAtLeast(1L)
            val started = SystemClock.elapsedRealtime()
            while (true) {
                val t = ((SystemClock.elapsedRealtime() - started).toFloat() / xfadeMs)
                    .coerceIn(0f, 1f)
                mainPlayer.volume = targetVolume * t
                secondaryPlayer?.volume = targetVolume * (1f - t)
                if (t >= 1f) break
                delay(FADE_STEP_MS)
            }
            finishFade()
        }
    }

    private fun finishFade() {
        mainPlayer.volume = targetVolume
        secondaryPlayer?.let {
            it.pause()
            it.stop()
            it.clearMediaItems()
            it.volume = 0f
        }
        phase = Phase.IDLE
    }

    /** Tear down an in-flight (or armed) overlap and restore the main volume. */
    private fun abort() {
        fadeJob?.cancel()
        fadeJob = null
        secondaryPlayer?.let {
            it.pause()
            it.stop()
            it.clearMediaItems()
            it.volume = 0f
        }
        if (phase != Phase.IDLE) mainPlayer.volume = targetVolume
        phase = Phase.IDLE
    }

    private fun ensureSecondary(): ExoPlayer {
        secondaryPlayer?.let { return it }
        val player = ExoPlayer.Builder(context)
            // No audio-focus handling: the main player owns focus. handleAudioFocus
            // = false keeps this from fighting it for the brief overlap.
            .setAudioAttributes(MUSIC_ATTRIBUTES, /* handleAudioFocus = */ false)
            .build()
            .apply {
                // Share the EQ session so the fading-out tail is EQ'd identically
                // to the main stream. OEM-dependent; if a device dislikes two
                // tracks on one session the only downside is an un-EQ'd tail.
                setAudioSessionId(audioSessionId)
                volume = 0f
            }
        secondaryPlayer = player
        return player
    }

    fun release() {
        tickJob?.cancel()
        prefsJob?.cancel()
        fadeJob?.cancel()
        mainPlayer.removeListener(listener)
        secondaryPlayer?.release()
        secondaryPlayer = null
        phase = Phase.IDLE
    }

    private companion object {
        /** Scheduler cadence — fine enough to catch the trigger boundary. */
        const val TICK_MS = 50L

        /** How early to pre-prepare the secondary before the fade starts. */
        const val PREPARE_LEAD_MS = 1000L

        /** Volume-ramp step; ~25 fps is smooth and cheap. */
        const val FADE_STEP_MS = 40L

        /** Below this remaining time, don't bother starting an overlap. */
        const val MIN_TRIGGER_MS = 200L

        val MUSIC_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }
}
