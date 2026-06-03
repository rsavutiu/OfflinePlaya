package com.offlineplaya.shared.presentation.playlist

import com.offlineplaya.shared.data.repository.SqlPlaylistRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the create-then-add composition behind the long-press
 * "Add to new playlist" flow. The repo primitives have their own tests;
 * this guards that [PlaylistStateHolder.createAndAddTrack] wires them
 * together (and honours the blank-name guard) on the supplied scope.
 */
class PlaylistStateHolderTest {

    private data class Fixture(
        val playlists: SqlPlaylistRepository,
        val tracks: SqlTrackRepository,
        val holder: PlaylistStateHolder,
    )

    private fun fixture(): Fixture {
        val db = createInMemoryDatabase()
        val playlists = SqlPlaylistRepository(db, Dispatchers.Unconfined)
        return Fixture(
            playlists = playlists,
            tracks = SqlTrackRepository(db, TestLogger(), Dispatchers.Unconfined),
            holder = PlaylistStateHolder(playlists, CoroutineScope(Dispatchers.Unconfined)),
        )
    }

    @Test
    fun `createAndAddTrack creates the playlist and adds the track`() = runTest {
        val f = fixture()
        val trackId = f.tracks.insertFile("u1", "t", "p", "1.mp3", 0, 0, null)

        f.holder.createAndAddTrack("Roadtrip", trackId)

        val all = f.playlists.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("Roadtrip", all.first().name)
        val contained = f.playlists.observeTracks(all.first().id).first().map { it.documentUri }
        assertEquals(listOf("u1"), contained)
    }

    @Test
    fun `createAndAddTrack ignores blank names`() = runTest {
        val f = fixture()
        val trackId = f.tracks.insertFile("u1", "t", "p", "1.mp3", 0, 0, null)

        f.holder.createAndAddTrack("   ", trackId)

        assertEquals(0, f.playlists.observeAll().first().size)
    }
}
