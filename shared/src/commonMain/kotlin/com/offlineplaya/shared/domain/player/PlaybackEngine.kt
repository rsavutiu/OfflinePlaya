package com.offlineplaya.shared.domain.player

import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Narrow, platform-agnostic seam over the real playback controller (on Android,
 * a `MediaController` bound to the `PlaybackService`). It deliberately knows
 * nothing about [com.offlineplaya.shared.domain.model.PlaybackState], the
 * shared-element morph, or any UI concern — it only applies timeline/transport
 * commands and reports raw engine values.
 *
 * All the "which track is showing" coordination — the parallel track list, the
 * optimistic state on a new queue, and command serialization — lives in
 * [com.offlineplaya.shared.data.player.CoordinatingMusicPlayer] on top of this
 * interface, so that logic is unit-testable against a fake engine without any
 * Android framework. Keeping this surface thin is the whole point: the Android
 * actual stays a near-mechanical pass-through to `MediaController`.
 *
 * The queue-mutating operations are `suspend` and **must not return until the
 * command has been applied to the underlying timeline**, because the coordinator
 * serializes them to guarantee ordering (tap-after-tap can't interleave).
 */
interface PlaybackEngine {

    /**
     * Emits once whenever the underlying player reports any change (current
     * index, play/pause, timeline, position tick). The coordinator reconciles
     * its published state on every emission.
     */
    val changes: Flow<Unit>

    /** Latest raw engine values. Returns [EngineState.Empty] before ready. */
    fun readState(): EngineState

    /**
     * Replace the timeline with [tracks], start at [startIndex], begin playback.
     * Suspends until applied (awaiting controller readiness if necessary).
     */
    suspend fun applyQueue(tracks: List<Track>, startIndex: Int)

    /**
     * Like [applyQueue] but prepared **paused** at [positionMs] — the restore
     * path after process death. Suspends until applied.
     */
    suspend fun restoreQueue(tracks: List<Track>, startIndex: Int, positionMs: Long)

    /** Jump to [index] in the existing timeline and play. Suspends until applied. */
    suspend fun jumpTo(index: Int)

    /** Skip to the next / previous item. Suspends until applied. */
    suspend fun next()
    suspend fun previous()

    /** Append [track]; returns the new item count. Suspends until applied. */
    suspend fun append(track: Track): Int

    /** Insert [track] right after the current item; returns its index. Suspends. */
    suspend fun insertAfterCurrent(track: Track): Int

    /** Remove / move / clear timeline items. Suspend until applied. */
    suspend fun removeAt(index: Int)
    suspend fun move(from: Int, to: Int)
    suspend fun clear()

    // Transport / mode that don't change which track is current — fire-and-forget;
    // the resulting values come back through [changes] / [readState].
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setShuffleEnabled(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)
    fun setVolume(volume: Float)
}

/**
 * A raw snapshot of the engine's playback values, free of any track identity —
 * the coordinator pairs [index] with its own parallel track list to resolve the
 * current [Track]. [isActive] is true when the player is neither idle nor ended,
 * so `playWhenReady && isActive` is the steady "show the pause icon" intent that
 * survives buffering on a track skip.
 */
data class EngineState(
    val index: Int,
    val itemCount: Int,
    val playWhenReady: Boolean,
    val isActive: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode,
    val volume: Float,
) {
    companion object {
        val Empty = EngineState(
            index = -1,
            itemCount = 0,
            playWhenReady = false,
            isActive = false,
            positionMs = 0L,
            durationMs = 0L,
            shuffleEnabled = false,
            repeatMode = RepeatMode.OFF,
            volume = 1f,
        )
    }
}
