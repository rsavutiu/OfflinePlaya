package com.offlineplaya.shared.data.repository

import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlPlayHistoryRepositoryTest {

    private class Fixture {
        val db = createInMemoryDatabase()
        val tracks = SqlTrackRepository(db, TestLogger(), Dispatchers.Unconfined)
        val history = SqlPlayHistoryRepository(db, TestLogger(), Dispatchers.Unconfined)

        suspend fun seedTrack(name: String): Long {
            val id = tracks.insertFile(
                documentUri = "content://t/$name", treeUri = "content://tree",
                relativePath = name, fileName = name,
                fileSize = 1L, lastModified = 1L, folderId = null,
            )
            // Promote to scanned so the smart-list queries (which filter on
            // scan_status) see the row.
            val base = tracks.findById(id)!!
            tracks.updateMetadata(base.copy(title = name))
            return id
        }
    }

    @Test
    fun `most played orders by play count`() = runTest {
        val f = Fixture()
        val a = f.seedTrack("a.mp3")
        val b = f.seedTrack("b.mp3")
        repeat(3) { f.history.recordPlay(a, 1_000L + it) }
        f.history.recordPlay(b, 2_000L)

        val most = f.history.observeMostPlayed(10).first()
        assertEquals(listOf(a, b), most.map { it.id })
        assertEquals(3L, f.history.countForTrack(a))
    }

    @Test
    fun `recently played orders by last play, not count`() = runTest {
        val f = Fixture()
        val a = f.seedTrack("a.mp3")
        val b = f.seedTrack("b.mp3")
        repeat(5) { f.history.recordPlay(a, 1_000L + it) }
        f.history.recordPlay(b, 99_000L) // fewer plays but most recent

        val recent = f.history.observeRecentlyPlayed(10).first()
        assertEquals(listOf(b, a), recent.map { it.id })
    }

    @Test
    fun `never played excludes anything with history`() = runTest {
        val f = Fixture()
        val a = f.seedTrack("a.mp3")
        val b = f.seedTrack("b.mp3")
        f.history.recordPlay(a, 1_000L)

        val never = f.history.observeNeverPlayed(10).first()
        assertEquals(listOf(b), never.map { it.id })
    }

    @Test
    fun `forgotten favorites needs enough plays and silence since cutoff`() = runTest {
        val f = Fixture()
        val favorite = f.seedTrack("fav.mp3")     // 3 old plays → qualifies
        val casual = f.seedTrack("casual.mp3")    // 1 old play → not a favorite
        val current = f.seedTrack("current.mp3")  // favorite but played recently
        repeat(3) { f.history.recordPlay(favorite, 1_000L + it) }
        f.history.recordPlay(casual, 1_000L)
        repeat(3) { f.history.recordPlay(current, 1_000L + it) }
        f.history.recordPlay(current, 50_000L) // within the window

        val forgotten = f.history
            .observeForgottenFavorites(minPlays = 3, cutoffMs = 10_000L, limit = 10)
            .first()
        assertEquals(listOf(favorite), forgotten.map { it.id })
    }
}
