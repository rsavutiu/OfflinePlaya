package com.offlineplaya.shared.data.player

import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.EngineState
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.domain.player.PlaybackEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The single source of truth for "what is playing." Implements [MusicPlayer] on
 * top of a [PlaybackEngine], and is **main-thread (single-dispatcher) confined**:
 * every method is expected to be called from the same dispatcher the [scope]
 * runs on (the Android main looper in production, a test dispatcher in tests),
 * so its small mutable state needs no locks.
 *
 * It exists to kill a class of "the art shows one track, the text another, the
 * audio a third" desync bugs that came from three competing notions of the
 * current track:
 *
 *  1. **One published [PlaybackState].** The UI (artwork, headline, seek bar)
 *     reads only [playbackState] — never a side-channel "pending" track — so the
 *     three can't drift. The invariant `currentTrack == queue[queueIndex]` holds
 *     on every emission.
 *
 *  2. **Optimistic, synchronous publish.** Tapping a track publishes the new
 *     queue/track/index *immediately*, before the engine has finished applying
 *     it off-thread, so the Now Playing screen is correct on its first frame
 *     (this replaces the old `pendingNowPlayingTrack` shared-element hack).
 *
 *  3. **Serialized commands.** All timeline/index mutations run through a single
 *     FIFO actor ([commands]) so rapid tap-after-tap / skip-then-pick can't
 *     interleave and leave [queueTracks] mismatched against the engine timeline.
 *     A [generation] counter makes a superseded `setQueue` skip its (now stale)
 *     apply, and reconciliation from the engine is suppressed while an apply is
 *     in flight ([pendingApplies] > 0) so a late callback for an old queue can't
 *     clobber the optimistic state back to the wrong track.
 *
 * [queueTracks] is the list parallel to the engine timeline; index→[Track]
 * resolution goes through it rather than round-tripping the DB.
 */
class CoordinatingMusicPlayer(
    private val engine: PlaybackEngine,
    private val scope: CoroutineScope,
) : MusicPlayer {

    private val _playbackState = MutableStateFlow(PlaybackState.Empty)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    /** Parallel to the engine timeline; same indices. */
    private var queueTracks: List<Track> = emptyList()

    /** Bumped on every queue *replacement*; a stale apply (gen mismatch) is skipped. */
    private var generation = 0

    /**
     * Number of timeline/index applies currently in flight. While > 0 the
     * optimistic state is authoritative and engine reconciliation is suppressed,
     * so a callback from a just-superseded command can't revert the UI.
     */
    private var pendingApplies = 0

    /** Single-consumer FIFO queue of engine applies — guarantees command order. */
    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (command in commands) {
                runCatching { command() }
            }
        }
        scope.launch {
            engine.changes.collect { reconcile() }
        }
    }

    // --- transport ---

    override fun play() {
        if (_playbackState.value.currentTrack != null) {
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
        }
        engine.play()
    }

    override fun pause() {
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        engine.pause()
    }

    override fun stop() {
        engine.stop()
    }

    override fun seekTo(positionMs: Long) {
        _playbackState.value = _playbackState.value.copy(positionMs = positionMs.coerceAtLeast(0L))
        engine.seekTo(positionMs)
    }

    override fun skipToNext() {
        // No optimistic track change (wrap-around with repeat makes index+1
        // wrong); the post-apply reconcile shows the truth. Routed through the
        // actor so it stays ordered relative to a setQueue that follows it.
        enqueueApply { engine.next() }
    }

    override fun skipToPrevious() {
        enqueueApply { engine.previous() }
    }

    override fun seekToIndex(index: Int) {
        val tracks = queueTracks
        if (index !in tracks.indices) return
        val track = tracks[index]
        // Optimistic jump within the current queue.
        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            queueIndex = index,
            positionMs = 0L,
            isPlaying = true,
            durationMs = track.durationMs ?: 0L,
        )
        enqueueApply { engine.jumpTo(index) }
    }

    // --- queue management ---

    override fun setQueue(tracks: List<Track>, startIndex: Int) {
        val gen = ++generation
        val index = startIndex.coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
        queueTracks = tracks
        publishOptimisticQueue(tracks, index)
        enqueueApply {
            // Skip if a newer setQueue already superseded this one — avoids a
            // flash of the intermediate queue and a redundant timeline rebuild.
            if (gen == generation) engine.applyQueue(tracks, index)
        }
    }

    override fun restoreQueue(tracks: List<Track>, startIndex: Int, positionMs: Long) {
        // Restore must never win a race against anything the user (or a
        // resumption callback) already queued — it only fills a still-empty
        // session. generation > 0 means a setQueue/clearQueue happened first.
        if (queueTracks.isNotEmpty() || generation > 0 || tracks.isEmpty()) return
        val gen = ++generation
        val index = startIndex.coerceIn(0, tracks.size - 1)
        val track = tracks[index]
        val position = positionMs.coerceIn(0L, track.durationMs ?: Long.MAX_VALUE)
        queueTracks = tracks
        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            queue = tracks,
            queueIndex = index,
            isPlaying = false,
            positionMs = position,
            durationMs = track.durationMs ?: 0L,
        )
        enqueueApply {
            if (gen == generation) engine.restoreQueue(tracks, index, position)
        }
    }

    override fun addToQueue(track: Track) {
        queueTracks = queueTracks + track
        enqueueApply { engine.append(track) }
    }

    override fun addNext(track: Track) {
        val insertAt = (_playbackState.value.queueIndex + 1).coerceIn(0, queueTracks.size)
        queueTracks = queueTracks.toMutableList().also { it.add(insertAt, track) }
        enqueueApply { engine.insertAfterCurrent(track) }
    }

    override fun removeFromQueue(index: Int) {
        if (index !in queueTracks.indices) return
        queueTracks = queueTracks.toMutableList().also { it.removeAt(index) }
        enqueueApply { engine.removeAt(index) }
    }

    override fun moveInQueue(from: Int, to: Int) {
        if (from !in queueTracks.indices || to !in queueTracks.indices) return
        queueTracks = queueTracks.toMutableList().also { it.add(to, it.removeAt(from)) }
        enqueueApply { engine.move(from, to) }
    }

    override fun clearQueue() {
        generation++
        queueTracks = emptyList()
        _playbackState.value = PlaybackState.Empty
        enqueueApply { engine.clear() }
    }

    // --- mode ---

    override fun setShuffleEnabled(enabled: Boolean) {
        _playbackState.value = _playbackState.value.copy(shuffleEnabled = enabled)
        engine.setShuffleEnabled(enabled)
    }

    override fun setRepeatMode(mode: RepeatMode) {
        _playbackState.value = _playbackState.value.copy(repeatMode = mode)
        engine.setRepeatMode(mode)
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _playbackState.value = _playbackState.value.copy(volume = clamped)
        engine.setVolume(clamped)
    }

    // --- internals ---

    /** Enqueue an engine apply, counting it as in-flight so reconcile defers. */
    private fun enqueueApply(apply: suspend () -> Unit) {
        pendingApplies++
        commands.trySend {
            try {
                apply()
            } finally {
                pendingApplies--
                // When the last queued apply drains, fold in the engine's real
                // state in case the optimistic value diverged (e.g. clamped
                // index, repeat wrap on skip).
                if (pendingApplies == 0) reconcile()
            }
        }
    }

    private fun publishOptimisticQueue(tracks: List<Track>, index: Int) {
        val track = tracks.getOrNull(index)
        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            queue = tracks,
            queueIndex = if (tracks.isNotEmpty()) index else -1,
            isPlaying = tracks.isNotEmpty(),
            positionMs = 0L,
            durationMs = track?.durationMs ?: 0L,
        )
    }

    /**
     * Rebuild the published state from the engine's truth paired with
     * [queueTracks]. Suppressed while an apply is in flight so a callback from a
     * superseded command can't revert the optimistic state. Never resolves the
     * current track from an engine index that's out of range for the current
     * queue — that's exactly the stale-callback case that showed the wrong art.
     */
    private fun reconcile() {
        if (pendingApplies > 0) return
        val s: EngineState = engine.readState()
        val q = queueTracks
        val validIndex = q.isNotEmpty() && s.index in q.indices
        val track = if (validIndex) q[s.index] else _playbackState.value.currentTrack
        _playbackState.value = PlaybackState(
            currentTrack = track,
            isPlaying = s.playWhenReady && s.isActive,
            positionMs = s.positionMs.coerceAtLeast(0L),
            durationMs = s.durationMs.takeIf { it > 0 } ?: (track?.durationMs ?: 0L),
            shuffleEnabled = s.shuffleEnabled,
            repeatMode = s.repeatMode,
            queue = q,
            queueIndex = if (validIndex) s.index else -1,
            volume = s.volume,
        )
    }
}
