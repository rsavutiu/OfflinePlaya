package com.offlineplaya.shared.presentation.queue

import com.offlineplaya.shared.domain.model.PlaybackSnapshot
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.domain.repository.QueueRepository
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QueueStateRecorderTest {

    private val scopes = mutableListOf<CoroutineScope>()

    @AfterTest
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    private class Fixture(
        val player: FakePlayer,
        val repo: RecordingQueueRepository,
        var nowMs: Long,
    )

    private fun kotlinx.coroutines.test.TestScope.startRecorder(): Fixture {
        val player = FakePlayer()
        val repo = RecordingQueueRepository()
        val fixture = Fixture(player, repo, nowMs = 0L)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        scopes.add(scope)
        QueueStateRecorder(
            musicPlayer = player,
            queue = repo,
            scope = scope,
            logger = TestLogger(),
            now = { fixture.nowMs },
        ).start()
        return fixture
    }

    @Test
    fun `the startup empty state never clobbers the persisted queue`() = runTest {
        val f = startRecorder()
        // Initial PlaybackState.Empty was already collected on start.
        assertNull(f.repo.savedQueues.lastOrNull(), "startup empty must not be persisted")

        // A clear AFTER a real queue is a deliberate act and must persist.
        f.player.emit(state(listOf(t(1)), index = 0, playing = true))
        f.player.emit(PlaybackState.Empty)
        assertEquals(emptyList(), f.repo.savedQueues.last())
    }

    @Test
    fun `queue content changes are persisted with their ids in order`() = runTest {
        val f = startRecorder()
        f.player.emit(state(listOf(t(3), t(1), t(2)), index = 1, playing = true))
        assertEquals(listOf(3L, 1L, 2L), f.repo.savedQueues.last())

        // Same queue again (position tick) → no extra queue write.
        val writes = f.repo.savedQueues.size
        f.player.emit(state(listOf(t(3), t(1), t(2)), index = 1, playing = true, positionMs = 7_000L))
        assertEquals(writes, f.repo.savedQueues.size)
    }

    @Test
    fun `pause saves the position immediately`() = runTest {
        val f = startRecorder()
        f.player.emit(state(listOf(t(1)), index = 0, playing = true))

        f.nowMs = 1_000L // inside the periodic window — only the edge saves
        f.player.emit(state(listOf(t(1)), index = 0, playing = false, positionMs = 90_000L))

        assertEquals(90_000L, f.repo.savedSnapshots.last().positionMs)
    }

    @Test
    fun `position ticks are throttled while playing`() = runTest {
        val f = startRecorder()
        f.player.emit(state(listOf(t(1)), index = 0, playing = true))
        val initial = f.repo.savedSnapshots.size

        f.nowMs = 1_000L
        f.player.emit(state(listOf(t(1)), index = 0, playing = true, positionMs = 1_000L))
        f.nowMs = 2_000L
        f.player.emit(state(listOf(t(1)), index = 0, playing = true, positionMs = 2_000L))
        assertEquals(initial, f.repo.savedSnapshots.size, "sub-interval ticks must not write")

        f.nowMs = 6_000L
        f.player.emit(state(listOf(t(1)), index = 0, playing = true, positionMs = 6_000L))
        assertEquals(initial + 1, f.repo.savedSnapshots.size)
        assertEquals(6_000L, f.repo.savedSnapshots.last().positionMs)
    }

    @Test
    fun `track change and mode change save immediately`() = runTest {
        val f = startRecorder()
        val queue = listOf(t(1), t(2))
        f.player.emit(state(queue, index = 0, playing = true))

        f.nowMs = 1_000L
        f.player.emit(state(queue, index = 1, playing = true))
        assertEquals(1, f.repo.savedSnapshots.last().queueIndex)

        f.nowMs = 1_500L
        f.player.emit(state(queue, index = 1, playing = true, repeat = RepeatMode.ALL))
        assertEquals(RepeatMode.ALL, f.repo.savedSnapshots.last().repeatMode)
    }

    // --- fakes ---

    private class FakePlayer : MusicPlayer {
        private val _state = MutableStateFlow(PlaybackState.Empty)
        override val playbackState: StateFlow<PlaybackState> = _state
        fun emit(state: PlaybackState) {
            _state.value = state
        }

        override fun play() {}
        override fun pause() {}
        override fun stop() {}
        override fun seekTo(positionMs: Long) {}
        override fun skipToNext() {}
        override fun skipToPrevious() {}
        override fun seekToIndex(index: Int) {}
        override fun setQueue(tracks: List<Track>, startIndex: Int) {}
        override fun restoreQueue(tracks: List<Track>, startIndex: Int, positionMs: Long) {}
        override fun addToQueue(track: Track) {}
        override fun addNext(track: Track) {}
        override fun removeFromQueue(index: Int) {}
        override fun moveInQueue(from: Int, to: Int) {}
        override fun clearQueue() {}
        override fun setShuffleEnabled(enabled: Boolean) {}
        override fun setRepeatMode(mode: RepeatMode) {}
        override fun setVolume(volume: Float) {}
    }

    private class RecordingQueueRepository : QueueRepository {
        val savedQueues = mutableListOf<List<Long>>()
        val savedSnapshots = mutableListOf<PlaybackSnapshot>()

        override suspend fun replaceAll(trackIds: List<Long>) {
            savedQueues.add(trackIds)
        }

        override suspend fun savePlaybackSnapshot(snapshot: PlaybackSnapshot) {
            savedSnapshots.add(snapshot)
        }

        override fun observeQueue(): Flow<List<Track>> = emptyFlow()
        override suspend fun count(): Long = 0
        override suspend fun enqueue(trackId: Long, source: String) {}
        override suspend fun clear() {}
        override suspend fun loadQueue(): List<Track> = emptyList()
        override suspend fun loadPlaybackSnapshot(): PlaybackSnapshot? = null
    }
}

private fun state(
    queue: List<Track>,
    index: Int,
    playing: Boolean,
    positionMs: Long = 0L,
    repeat: RepeatMode = RepeatMode.OFF,
): PlaybackState = PlaybackState(
    currentTrack = queue.getOrNull(index),
    isPlaying = playing,
    positionMs = positionMs,
    durationMs = 0L,
    shuffleEnabled = false,
    repeatMode = repeat,
    queue = queue,
    queueIndex = if (queue.isEmpty()) -1 else index,
    volume = 1f,
)

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
