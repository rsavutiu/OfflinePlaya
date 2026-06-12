package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.data.repository.SqlQueueRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.domain.model.PlaybackSnapshot
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RestorePersistedQueueUseCaseTest {

    private class Fixture {
        val db = createInMemoryDatabase()
        val tracks = SqlTrackRepository(db, TestLogger(), Dispatchers.Unconfined)
        val queue = SqlQueueRepository(db, Dispatchers.Unconfined)
        val player = RecordingPlayer()
        val useCase = RestorePersistedQueueUseCase(queue, player, TestLogger())
    }

    @Test
    fun `restores the saved queue paused at the saved index and position`() = runTest {
        val f = Fixture()
        val t1 = f.tracks.insertFile("u1", "t", "p", "1.mp3", 0, 0, null)
        val t2 = f.tracks.insertFile("u2", "t", "p", "2.mp3", 0, 0, null)
        f.queue.replaceAll(listOf(t1, t2))
        f.queue.savePlaybackSnapshot(
            PlaybackSnapshot(queueIndex = 1, positionMs = 30_000L, shuffleEnabled = true, repeatMode = RepeatMode.ALL),
        )

        f.useCase()

        assertEquals(listOf("u1", "u2"), f.player.restoredTracks?.map { it.documentUri })
        assertEquals(1, f.player.restoredIndex)
        assertEquals(30_000L, f.player.restoredPositionMs)
        assertEquals(true, f.player.shuffle)
        assertEquals(RepeatMode.ALL, f.player.repeat)
    }

    @Test
    fun `does nothing when no queue was persisted`() = runTest {
        val f = Fixture()
        f.useCase()
        assertNull(f.player.restoredTracks)
    }

    @Test
    fun `clamps an out-of-range saved index`() = runTest {
        val f = Fixture()
        val t1 = f.tracks.insertFile("u1", "t", "p", "1.mp3", 0, 0, null)
        f.queue.replaceAll(listOf(t1))
        // Index 5 can become stale if tracks left the library since the save.
        f.queue.savePlaybackSnapshot(
            PlaybackSnapshot(queueIndex = 5, positionMs = 0L, shuffleEnabled = false, repeatMode = RepeatMode.OFF),
        )

        f.useCase()

        assertEquals(0, f.player.restoredIndex)
    }

    @Test
    fun `does not touch a player that already has a queue`() = runTest {
        val f = Fixture()
        val t1 = f.tracks.insertFile("u1", "t", "p", "1.mp3", 0, 0, null)
        f.queue.replaceAll(listOf(t1))
        f.player.state.value = f.player.state.value.copy(
            queue = listOf(track(99L)),
            queueIndex = 0,
            currentTrack = track(99L),
        )

        f.useCase()

        assertNull(f.player.restoredTracks)
        assertTrue(f.player.shuffle == null, "modes must not be reset either")
    }

    private class RecordingPlayer : MusicPlayer {
        val state = MutableStateFlow(PlaybackState.Empty)
        override val playbackState: StateFlow<PlaybackState> = state

        var restoredTracks: List<Track>? = null
        var restoredIndex: Int? = null
        var restoredPositionMs: Long? = null
        var shuffle: Boolean? = null
        var repeat: RepeatMode? = null

        override fun restoreQueue(tracks: List<Track>, startIndex: Int, positionMs: Long) {
            restoredTracks = tracks
            restoredIndex = startIndex
            restoredPositionMs = positionMs
        }

        override fun setShuffleEnabled(enabled: Boolean) {
            shuffle = enabled
        }

        override fun setRepeatMode(mode: RepeatMode) {
            repeat = mode
        }

        override fun play() {}
        override fun pause() {}
        override fun stop() {}
        override fun seekTo(positionMs: Long) {}
        override fun skipToNext() {}
        override fun skipToPrevious() {}
        override fun seekToIndex(index: Int) {}
        override fun setQueue(tracks: List<Track>, startIndex: Int) {}
        override fun addToQueue(track: Track) {}
        override fun addNext(track: Track) {}
        override fun removeFromQueue(index: Int) {}
        override fun moveInQueue(from: Int, to: Int) {}
        override fun clearQueue() {}
        override fun setVolume(volume: Float) {}
    }
}

private fun track(id: Long): Track = Track(
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
    scanStatus = com.offlineplaya.shared.domain.model.ScanStatus.SCANNED,
)
