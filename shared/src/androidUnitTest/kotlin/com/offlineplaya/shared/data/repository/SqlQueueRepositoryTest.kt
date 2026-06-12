package com.offlineplaya.shared.data.repository

import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.PlaybackSnapshot
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlQueueRepositoryTest {

    private fun setup(): Pair<SqlQueueRepository, SqlTrackRepository> {
        val db: OfflinePlayaDatabase = createInMemoryDatabase()
        return SqlQueueRepository(db, Dispatchers.Unconfined) to
                SqlTrackRepository(db, TestLogger(), Dispatchers.Unconfined)
    }

    @Test
    fun `enqueue preserves insertion order via position`() = runTest {
        val (queue, tracks) = setup()
        val t1 = tracks.insertFile("u1", "t", "p", "1.mp3", 0, 0, null)
        val t2 = tracks.insertFile("u2", "t", "p", "2.mp3", 0, 0, null)
        val t3 = tracks.insertFile("u3", "t", "p", "3.mp3", 0, 0, null)
        queue.enqueue(t1)
        queue.enqueue(t2)
        queue.enqueue(t3)

        assertEquals(listOf("u1", "u2", "u3"), queue.observeQueue().first().map { it.documentUri })
        assertEquals(3L, queue.count())
    }

    @Test
    fun `clear empties the queue without affecting tracks`() = runTest {
        val (queue, tracks) = setup()
        val t1 = tracks.insertFile("u1", "t", "p", "1.mp3", 0, 0, null)
        queue.enqueue(t1)
        assertEquals(1L, queue.count())

        queue.clear()

        assertEquals(0L, queue.count())
        assertEquals(1L, tracks.count(), "tracks survive a queue clear")
    }

    @Test
    fun `replaceAll swaps the persisted queue atomically, keeping order`() = runTest {
        val (queue, tracks) = setup()
        val t1 = tracks.insertFile("u1", "t", "p", "1.mp3", 0, 0, null)
        val t2 = tracks.insertFile("u2", "t", "p", "2.mp3", 0, 0, null)
        val t3 = tracks.insertFile("u3", "t", "p", "3.mp3", 0, 0, null)
        queue.enqueue(t1)

        queue.replaceAll(listOf(t3, t1, t2))

        assertEquals(listOf("u3", "u1", "u2"), queue.loadQueue().map { it.documentUri })

        queue.replaceAll(emptyList())
        assertEquals(0L, queue.count())
    }

    @Test
    fun `loadQueue drops tracks that left the library`() = runTest {
        val (queue, tracks) = setup()
        val t1 = tracks.insertFile("u1", "t", "p", "1.mp3", 0, 0, null)
        val t2 = tracks.insertFile("u2", "t", "p", "2.mp3", 0, 0, null)
        queue.replaceAll(listOf(t1, t2))

        tracks.deleteById(t1)

        assertEquals(listOf("u2"), queue.loadQueue().map { it.documentUri })
    }

    @Test
    fun `playback snapshot round-trips`() = runTest {
        val (queue, _) = setup()
        assertNull(queue.loadPlaybackSnapshot(), "no snapshot before the first save")

        val saved = PlaybackSnapshot(
            queueIndex = 4,
            positionMs = 123_456L,
            shuffleEnabled = true,
            repeatMode = RepeatMode.ALL,
        )
        queue.savePlaybackSnapshot(saved)

        assertEquals(saved, queue.loadPlaybackSnapshot())
    }
}
