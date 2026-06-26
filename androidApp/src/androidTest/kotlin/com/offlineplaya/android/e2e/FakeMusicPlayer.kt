package com.offlineplaya.android.e2e

import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.MusicPlayer
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory [MusicPlayer] for E2E tests. Models the queue + transport as plain
 * state mutations on a [MutableStateFlow] — no Media3, no service, no audio —
 * so the UI under test observes deterministic [PlaybackState] transitions and
 * the test can assert them directly off [playbackState].
 *
 * Deliberately synchronous: every call updates state on the calling thread, so
 * a tap that routes through here is reflected by the next Compose idle, with no
 * `Thread.sleep` and no real player to wait on.
 */
class FakeMusicPlayer : MusicPlayer {

    private val _state = MutableStateFlow(PlaybackState.Empty)
    override val playbackState: StateFlow<PlaybackState> = _state

    private fun trackAt(index: Int): Track? = _state.value.queue.getOrNull(index)

    private fun moveTo(index: Int, play: Boolean = _state.value.isPlaying) {
        val track = trackAt(index) ?: return
        _state.value = _state.value.copy(
            currentTrack = track,
            queueIndex = index,
            isPlaying = play,
            positionMs = 0L,
            durationMs = track.durationMs ?: 0L,
        )
    }

    override fun play() {
        if (_state.value.currentTrack != null) _state.value = _state.value.copy(isPlaying = true)
    }

    override fun pause() {
        _state.value = _state.value.copy(isPlaying = false)
    }

    override fun stop() {
        _state.value = PlaybackState.Empty
    }

    override fun seekTo(positionMs: Long) {
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    override fun skipToNext() {
        val next = _state.value.queueIndex + 1
        if (next in _state.value.queue.indices) moveTo(next)
    }

    override fun skipToPrevious() {
        val prev = _state.value.queueIndex - 1
        if (prev in _state.value.queue.indices) moveTo(prev)
    }

    override fun seekToIndex(index: Int) {
        if (index in _state.value.queue.indices) moveTo(index)
    }

    override fun setQueue(tracks: List<Track>, startIndex: Int) {
        _state.value = _state.value.copy(queue = tracks.toPersistentList())
        moveTo(startIndex.coerceIn(0, (tracks.size - 1).coerceAtLeast(0)), play = true)
    }

    override fun restoreQueue(tracks: List<Track>, startIndex: Int, positionMs: Long) {
        if (_state.value.queue.isNotEmpty()) return
        _state.value = _state.value.copy(queue = tracks.toPersistentList())
        moveTo(startIndex.coerceIn(0, (tracks.size - 1).coerceAtLeast(0)), play = false)
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    override fun addToQueue(track: Track) {
        _state.value = _state.value.copy(queue = (_state.value.queue + track).toPersistentList())
    }

    override fun addNext(track: Track) {
        val at = (_state.value.queueIndex + 1).coerceIn(0, _state.value.queue.size)
        val newQueue = _state.value.queue.toMutableList().apply { add(at, track) }
        _state.value = _state.value.copy(queue = newQueue.toPersistentList())
    }

    override fun removeFromQueue(index: Int) {
        if (index !in _state.value.queue.indices) return
        val newQueue = _state.value.queue.toMutableList().apply { removeAt(index) }
        _state.value = _state.value.copy(queue = newQueue.toPersistentList())
    }

    override fun moveInQueue(from: Int, to: Int) {
        val q = _state.value.queue
        if (from !in q.indices || to !in q.indices) return
        val newQueue = q.toMutableList().apply { add(to, removeAt(from)) }
        _state.value = _state.value.copy(queue = newQueue.toPersistentList())
    }

    override fun clearQueue() {
        _state.value = PlaybackState.Empty
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(shuffleEnabled = enabled)
    }

    override fun setRepeatMode(mode: RepeatMode) {
        _state.value = _state.value.copy(repeatMode = mode)
    }

    override fun setVolume(volume: Float) {
        _state.value = _state.value.copy(volume = volume)
    }
}
