package com.offlineplaya.shared.domain.player

import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * Multiplatform playback contract. The Android implementation
 * ([com.offlineplaya.shared.data.player.Media3MusicPlayer]) wraps a
 * [androidx.media3.session.MediaController] connected to a foreground
 * `PlaybackService`. Future iOS/Desktop targets supply their own actual.
 *
 * All methods are safe to call before the underlying player is connected —
 * pre-connection commands are dropped silently; state stays at
 * [PlaybackState.Empty] until the controller binds.
 */
interface MusicPlayer {

    val playbackState: StateFlow<PlaybackState>

    // --- transport ---
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun skipToNext()
    fun skipToPrevious()

    // --- queue management ---

    /** Replace the queue with [tracks] and start playback at [startIndex]. */
    fun setQueue(tracks: List<Track>, startIndex: Int = 0)

    /** Append [track] to the end of the current queue. */
    fun addToQueue(track: Track)

    /** Insert [track] immediately after the current track. */
    fun addNext(track: Track)

    fun removeFromQueue(index: Int)
    fun moveInQueue(from: Int, to: Int)
    fun clearQueue()

    // --- mode ---
    fun setShuffleEnabled(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)
    fun setVolume(volume: Float)
}
