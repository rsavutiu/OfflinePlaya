package com.offlineplaya.shared.data.repository

import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SqlPlaylistRepositoryTest {

    private data class Fixture(
        val db: OfflinePlayaDatabase,
        val playlists: SqlPlaylistRepository,
        val tracks: SqlTrackRepository,
    )

    private fun fixture(): Fixture {
        val db = createInMemoryDatabase()
        return Fixture(
            db = db,
            playlists = SqlPlaylistRepository(db, Dispatchers.Unconfined),
            tracks = SqlTrackRepository(db, Dispatchers.Unconfined),
        )
    }

    @Test
    fun `create returns a new id and the playlist is observable`() = runTest {
        val f = fixture()
        val id = f.playlists.create("Morning Run")
        val found = f.playlists.findById(id)
        assertNotNull(found)
        assertEquals("Morning Run", found.name)
        assertEquals(1, f.playlists.observeAll().first().size)
    }

    @Test
    fun `rename updates the stored name`() = runTest {
        val f = fixture()
        val id = f.playlists.create("Old Name")
        f.playlists.rename(id, "New Name")
        assertEquals("New Name", f.playlists.findById(id)?.name)
    }

    @Test
    fun `delete removes the playlist and cascades to its tracks`() = runTest {
        val f = fixture()
        val playlistId = f.playlists.create("Doomed")
        val trackId = f.tracks.insertFile("u", "t", "p", "song.mp3", 0, 0, null)
        f.playlists.addTrack(playlistId, trackId)
        assertEquals(1, f.playlists.observeTracks(playlistId).first().size)

        f.playlists.delete(playlistId)

        assertEquals(0, f.playlists.observeAll().first().size)
        assertEquals(0, f.playlists.observeTracks(playlistId).first().size)
    }

    @Test
    fun `addTrack appends in insertion order`() = runTest {
        val f = fixture()
        val playlistId = f.playlists.create("Mix")
        val t1 = f.tracks.insertFile("u1", "t", "p", "1.mp3", 0, 0, null)
        val t2 = f.tracks.insertFile("u2", "t", "p", "2.mp3", 0, 0, null)
        val t3 = f.tracks.insertFile("u3", "t", "p", "3.mp3", 0, 0, null)

        f.playlists.addTrack(playlistId, t1)
        f.playlists.addTrack(playlistId, t2)
        f.playlists.addTrack(playlistId, t3)

        val ordered = f.playlists.observeTracks(playlistId).first().map { it.documentUri }
        assertEquals(listOf("u1", "u2", "u3"), ordered)
    }
}
