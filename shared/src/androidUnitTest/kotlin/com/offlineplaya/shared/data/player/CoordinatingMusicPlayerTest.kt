package com.offlineplaya.shared.data.player

import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.EngineState
import com.offlineplaya.shared.domain.player.PlaybackEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the playback single-source-of-truth coordinator. These exist to pin
 * the "art shows one track, text another, audio a third" desync that came from
 * racing, un-serialized queue commands and a side-channel "pending" track.
 *
 * The fake engine ([FakeEngine]) can hold its `apply*` calls open so a test can
 * force the adversarial interleavings — tap-after-tap, and a stale engine
 * callback arriving mid-apply — that a naive implementation gets wrong. The
 * coordinator runs on an eager [UnconfinedTestDispatcher] scope so commands are
 * applied as they arrive; [scopes] are cancelled in [tearDown] so the
 * never-ending actor/reconcile collectors don't trip runTest's leak check.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoordinatingMusicPlayerTest {

    private val scopes = mutableListOf<CoroutineScope>()

    @AfterTest
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    private fun TestScope.coordinator(engine: PlaybackEngine): CoordinatingMusicPlayer {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes.add(scope)
        return CoordinatingMusicPlayer(engine, scope)
    }

    // --- the invariant: one consistent track, everywhere ---

    @Test
    fun `tapping a track publishes it immediately, before the engine applies`() = runTest {
        val engine = FakeEngine(autoComplete = false)
        val player = coordinator(engine)

        player.setQueue(listOf(t(1), t(2), t(3)), startIndex = 2)

        // Engine apply is still held — yet the UI already shows the tapped track.
        // (This is what used to need the pendingNowPlayingTrack side channel.)
        val s = player.playbackState.value
        assertEquals(3L, s.currentTrack?.id)
        assertEquals(2, s.queueIndex)
        assertEquals(listOf(1L, 2L, 3L), s.queue.map { it.id })
        assertTrue(s.isPlaying)
        assertInvariant(s)

        engine.releaseAll()
        advanceUntilIdle()
        assertInvariant(player.playbackState.value)
        assertEquals(3L, player.playbackState.value.currentTrack?.id)
        assertEquals(listOf(1L, 2L, 3L), engine.appliedTracks.map { it.id })
    }

    @Test
    fun `a stale engine callback during an apply does not revert the shown track`() = runTest {
        val engine = FakeEngine(autoComplete = true)
        val player = coordinator(engine)

        // Queue A is playing its second track.
        player.setQueue(listOf(t(10), t(11)), startIndex = 1)
        advanceUntilIdle()
        assertEquals(11L, player.playbackState.value.currentTrack?.id)

        // User taps into queue B at its third track; hold B's apply open.
        engine.autoComplete = false
        player.setQueue(listOf(t(20), t(21), t(22)), startIndex = 2)
        // The engine is still on A (index 1). A naive reconcile would read
        // index 1 against the new queue B and show B[1] = id 21 — the classic
        // "wrong artwork" frame. The suppression-while-applying guard must keep
        // the optimistic B[2] = id 22.
        engine.emitChange()
        advanceUntilIdle()

        val s = player.playbackState.value
        assertEquals(22L, s.currentTrack?.id)
        assertEquals(2, s.queueIndex)
        assertInvariant(s)

        engine.releaseAll()
        advanceUntilIdle()
        assertEquals(22L, player.playbackState.value.currentTrack?.id)
        assertInvariant(player.playbackState.value)
    }

    // --- ordering: more commands must not fumble ---

    @Test
    fun `two rapid setQueue calls converge to the last queue in the engine`() = runTest {
        val engine = FakeEngine(autoComplete = true)
        val player = coordinator(engine)

        player.setQueue(listOf(t(1), t(2)), startIndex = 0)
        player.setQueue(listOf(t(8), t(9)), startIndex = 1)
        advanceUntilIdle()

        // The audio engine must actually be playing the LAST requested queue,
        // not whichever apply happened to finish last.
        assertEquals(listOf(8L, 9L), engine.appliedTracks.map { it.id })
        assertEquals(1, engine.index)

        val s = player.playbackState.value
        assertEquals(9L, s.currentTrack?.id)
        assertEquals(listOf(8L, 9L), s.queue.map { it.id })
        assertInvariant(s)
    }

    @Test
    fun `held setQueue applies cannot land out of order`() = runTest {
        // The adversarial timing the desync needs: hold both applies and try to
        // let the LATER one finish first. A serialized coordinator only ever has
        // one apply parked at a time, so the engine still ends on the last
        // queue; an un-serialized one would let queue A land after queue C and
        // leave the audio on the wrong (earlier) queue.
        val engine = FakeEngine(autoComplete = false)
        val player = coordinator(engine)

        player.setQueue(listOf(t(1), t(2)), startIndex = 0)
        player.setQueue(listOf(t(8), t(9)), startIndex = 0)

        var guard = 0
        while (engine.hasPendingGate() && guard++ < 10) {
            engine.releaseLast()
            advanceUntilIdle()
        }

        assertEquals(listOf(8L, 9L), engine.appliedTracks.map { it.id })
        assertEquals(8L, player.playbackState.value.currentTrack?.id)
        assertInvariant(player.playbackState.value)
    }

    @Test
    fun `skipToNext then selecting another track ends on the selected track`() = runTest {
        // The user's exact report: press next, then pick a different track.
        val engine = FakeEngine(autoComplete = true)
        val player = coordinator(engine)

        player.setQueue(listOf(t(1), t(2), t(3)), startIndex = 0)
        advanceUntilIdle()

        player.skipToNext()
        player.setQueue(listOf(t(50), t(51)), startIndex = 0)
        advanceUntilIdle()

        // next() applied to the OLD timeline, then the new queue replaced it —
        // because they were serialized in submission order. End state is the
        // picked track, and the engine plays exactly that queue.
        assertEquals(listOf(50L, 51L), engine.appliedTracks.map { it.id })
        assertEquals(0, engine.index)
        val s = player.playbackState.value
        assertEquals(50L, s.currentTrack?.id)
        assertInvariant(s)
    }

    @Test
    fun `every emission keeps currentTrack aligned with queueIndex across a command storm`() = runTest {
        val engine = FakeEngine(autoComplete = true)
        val player = coordinator(engine)
        val seen = mutableListOf<PlaybackState>()
        val collector = CoroutineScope(UnconfinedTestDispatcher(testScheduler)).also { scopes.add(it) }
        collector.launch { player.playbackState.collect { seen.add(it) } }

        player.setQueue(listOf(t(1), t(2), t(3)), startIndex = 0)
        player.skipToNext()
        player.setQueue(listOf(t(40), t(41), t(42), t(43)), startIndex = 3)
        player.seekToIndex(1)
        player.setQueue(listOf(t(7)), startIndex = 0)
        advanceUntilIdle()

        // No frame ever showed a track that wasn't the one at its own queueIndex.
        seen.forEach { assertInvariant(it) }
        assertEquals(7L, player.playbackState.value.currentTrack?.id)
        assertEquals(listOf(7L), engine.appliedTracks.map { it.id })
    }

    // --- cold-start restore ---

    @Test
    fun `restoreQueue publishes a paused queue at the saved position`() = runTest {
        val engine = FakeEngine(autoComplete = true)
        val player = coordinator(engine)

        player.restoreQueue(listOf(t(1), t(2), t(3)), startIndex = 1, positionMs = 42_000L)

        // Optimistic state is correct before the engine settles…
        val optimistic = player.playbackState.value
        assertEquals(2L, optimistic.currentTrack?.id)
        assertEquals(1, optimistic.queueIndex)
        assertEquals(42_000L, optimistic.positionMs)
        assertTrue(!optimistic.isPlaying, "restore must come back paused")
        assertInvariant(optimistic)

        advanceUntilIdle()
        // …and the engine really is paused at the saved spot.
        assertTrue(!engine.playWhenReady, "engine must not autoplay on restore")
        assertEquals(42_000L, engine.positionMs)
        val s = player.playbackState.value
        assertEquals(2L, s.currentTrack?.id)
        assertTrue(!s.isPlaying)
        assertInvariant(s)
    }

    @Test
    fun `restoreQueue is a no-op once anything was queued this session`() = runTest {
        val engine = FakeEngine(autoComplete = true)
        val player = coordinator(engine)

        player.setQueue(listOf(t(7)), startIndex = 0)
        advanceUntilIdle()

        player.restoreQueue(listOf(t(1), t(2)), startIndex = 0, positionMs = 0L)
        advanceUntilIdle()

        assertEquals(listOf(7L), engine.appliedTracks.map { it.id })
        assertEquals(7L, player.playbackState.value.currentTrack?.id)
        assertTrue(player.playbackState.value.isPlaying)
    }

    @Test
    fun `a user tap during a slow restore wins`() = runTest {
        val engine = FakeEngine(autoComplete = false)
        val player = coordinator(engine)

        player.restoreQueue(listOf(t(1), t(2)), startIndex = 0, positionMs = 9_000L)
        player.setQueue(listOf(t(50)), startIndex = 0)

        engine.releaseAll()
        advanceUntilIdle()

        // The superseded restore apply must be skipped entirely.
        assertEquals(listOf(50L), engine.appliedTracks.map { it.id })
        assertTrue(engine.playWhenReady, "user-initiated playback must not stay paused")
        assertEquals(50L, player.playbackState.value.currentTrack?.id)
        assertInvariant(player.playbackState.value)
    }

    // --- helpers ---

    /** The core consistency invariant the whole class defends. */
    private fun assertInvariant(s: PlaybackState) {
        if (s.queue.isEmpty()) {
            assertEquals(-1, s.queueIndex, "empty queue must have index -1")
            return
        }
        assertTrue(s.queueIndex in s.queue.indices, "queueIndex out of range: ${s.queueIndex}")
        assertEquals(
            s.queue[s.queueIndex].id,
            s.currentTrack?.id,
            "currentTrack must equal queue[queueIndex]",
        )
    }
}

/**
 * Fake [PlaybackEngine] whose `apply*` calls can be held open so tests can drive
 * adversarial timing. In [autoComplete] mode they apply immediately; otherwise
 * each parks on a gate until [releaseAll].
 */
private class FakeEngine(var autoComplete: Boolean) : PlaybackEngine {
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    override val changes = _changes

    var appliedTracks: List<Track> = emptyList()
        private set
    var index: Int = -1
        private set
    var playWhenReady = false
        private set
    var positionMs = 0L
        private set

    private val gates = ArrayDeque<CompletableDeferred<Unit>>()

    fun emitChange() {
        _changes.tryEmit(Unit)
    }

    fun releaseAll() {
        while (gates.isNotEmpty()) gates.removeFirst().complete(Unit)
    }

    fun hasPendingGate(): Boolean = gates.isNotEmpty()

    /** Release the most-recently parked apply first — the adversarial order. */
    fun releaseLast() {
        if (gates.isNotEmpty()) gates.removeLast().complete(Unit)
    }

    private suspend fun gate() {
        if (autoComplete) return
        val d = CompletableDeferred<Unit>()
        gates.addLast(d)
        d.await()
    }

    override fun readState(): EngineState = EngineState(
        index = index,
        itemCount = appliedTracks.size,
        playWhenReady = playWhenReady,
        isActive = appliedTracks.isNotEmpty(),
        positionMs = positionMs,
        durationMs = 0L,
        shuffleEnabled = false,
        repeatMode = RepeatMode.OFF,
        volume = 1f,
    )

    override suspend fun applyQueue(tracks: List<Track>, startIndex: Int) {
        gate()
        appliedTracks = tracks
        index = startIndex.coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
        playWhenReady = tracks.isNotEmpty()
        emitChange()
    }

    override suspend fun restoreQueue(tracks: List<Track>, startIndex: Int, positionMs: Long) {
        gate()
        appliedTracks = tracks
        index = startIndex.coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
        playWhenReady = false
        this.positionMs = positionMs
        emitChange()
    }

    override suspend fun jumpTo(index: Int) {
        gate()
        if (index in appliedTracks.indices) this.index = index
        emitChange()
    }

    override suspend fun next() {
        gate()
        if (index + 1 in appliedTracks.indices) index += 1
        emitChange()
    }

    override suspend fun previous() {
        gate()
        if (index - 1 in appliedTracks.indices) index -= 1
        emitChange()
    }

    override suspend fun append(track: Track): Int {
        gate()
        appliedTracks = appliedTracks + track
        emitChange()
        return appliedTracks.size
    }

    override suspend fun insertAfterCurrent(track: Track): Int {
        gate()
        val at = (index + 1).coerceIn(0, appliedTracks.size)
        appliedTracks = appliedTracks.toMutableList().also { it.add(at, track) }
        emitChange()
        return at
    }

    override suspend fun removeAt(index: Int) {
        gate()
        if (index in appliedTracks.indices) {
            appliedTracks = appliedTracks.toMutableList().also { it.removeAt(index) }
        }
        emitChange()
    }

    override suspend fun move(from: Int, to: Int) {
        gate()
        emitChange()
    }

    override suspend fun clear() {
        gate()
        appliedTracks = emptyList()
        index = -1
        playWhenReady = false
        emitChange()
    }

    override fun play() { playWhenReady = true; emitChange() }
    override fun pause() { playWhenReady = false; emitChange() }
    override fun stop() { playWhenReady = false; emitChange() }
    override fun seekTo(positionMs: Long) {}
    override fun setShuffleEnabled(enabled: Boolean) {}
    override fun setRepeatMode(mode: RepeatMode) {}
    override fun setVolume(volume: Float) {}
}

private fun t(id: Long): Track = Track(
    id = id,
    documentUri = "uri$id",
    treeUri = "tree",
    relativePath = "p/$id.mp3",
    fileName = "$id.mp3",
    title = "Track $id",
    artistName = "Artist",
    albumArtistName = null,
    albumName = "Album",
    genre = null,
    year = null,
    trackNumber = null,
    discNumber = null,
    durationMs = 200_000L,
    bitrate = null,
    sampleRate = null,
    channels = null,
    codec = null,
    artistId = null,
    albumId = null,
    folderId = null,
    scanStatus = ScanStatus.SCANNED,
)
