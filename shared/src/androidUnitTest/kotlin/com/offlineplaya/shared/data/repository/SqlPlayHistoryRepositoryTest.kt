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
        val artists = SqlArtistRepository(db, TestLogger(), Dispatchers.Unconfined)
        val albums = SqlAlbumRepository(db, TestLogger(), Dispatchers.Unconfined)
        val history = SqlPlayHistoryRepository(db, TestLogger(), Dispatchers.Unconfined)

        suspend fun seedTrack(name: String, artistId: Long? = null, albumId: Long? = null): Long {
            val id = tracks.insertFile(
                documentUri = "content://t/$name", treeUri = "content://tree",
                relativePath = name, fileName = name,
                fileSize = 1L, lastModified = 1L, folderId = null,
            )
            // Promote to scanned so the smart-list queries (which filter on
            // scan_status) see the row.
            val base = tracks.findById(id)!!
            tracks.updateMetadata(base.copy(title = name))
            if (artistId != null || albumId != null) {
                tracks.updateForeignKeys(id, artistId, albumId)
            }
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

    @Test
    fun `top played scopes to an artist and carries play counts`() = runTest {
        val f = Fixture()
        val pearlJam = f.artists.upsert("Pearl Jam")
        val other = f.artists.upsert("Someone Else")
        val alive = f.seedTrack("alive.mp3", artistId = pearlJam)
        val black = f.seedTrack("black.mp3", artistId = pearlJam)
        val noise = f.seedTrack("noise.mp3", artistId = other)
        repeat(2) { f.history.recordPlay(alive, 1_000L + it) }
        repeat(5) { f.history.recordPlay(black, 1_000L + it) }
        repeat(9) { f.history.recordPlay(noise, 1_000L + it) }

        val top = f.history.observeTopPlayed(artistId = pearlJam, sinceMs = 0L, limit = 10).first()

        assertEquals(listOf(black, alive), top.map { it.track.id }, "other artists excluded")
        assertEquals(listOf(5L, 2L), top.map { it.playCount })
    }

    @Test
    fun `top played without an artist covers the library and honours since`() = runTest {
        val f = Fixture()
        val old = f.seedTrack("old.mp3")
        val fresh = f.seedTrack("fresh.mp3")
        repeat(9) { f.history.recordPlay(old, 1_000L + it) }   // before the cutoff
        repeat(2) { f.history.recordPlay(fresh, 50_000L + it) }

        val top = f.history.observeTopPlayed(artistId = null, sinceMs = 10_000L, limit = 10).first()

        assertEquals(listOf(fresh), top.map { it.track.id })
        assertEquals(listOf(2L), top.map { it.playCount })
    }

    @Test
    fun `stats totals count plays, distinct tracks, and listened time`() = runTest {
        val f = Fixture()
        val a = f.seedTrack("a.mp3")
        val b = f.seedTrack("b.mp3")
        // Give the tracks durations so listened time has something to sum.
        f.tracks.updateMetadata(f.tracks.findById(a)!!.copy(durationMs = 60_000L))
        f.tracks.updateMetadata(f.tracks.findById(b)!!.copy(durationMs = 30_000L))
        repeat(2) { f.history.recordPlay(a, 1_000L + it) }
        f.history.recordPlay(b, 2_000L)

        val stats = f.history.observeStats(sinceMs = 0L).first()

        assertEquals(3L, stats.plays)
        assertEquals(2L, stats.distinctTracks)
        assertEquals(150_000L, stats.listenedMs, "2×60s + 1×30s")

        val empty = f.history.observeStats(sinceMs = 99_000L).first()
        assertEquals(0L, empty.plays)
        assertEquals(0L, empty.listenedMs)
    }

    @Test
    fun `top artists and albums group plays by id`() = runTest {
        val f = Fixture()
        val pearlJam = f.artists.upsert("Pearl Jam")
        val other = f.artists.upsert("Someone Else")
        val ten = f.albums.upsert("Ten", pearlJam, 1991)
        val vs = f.albums.upsert("Vs.", pearlJam, 1993)
        val alive = f.seedTrack("alive.mp3", artistId = pearlJam, albumId = ten)
        val black = f.seedTrack("black.mp3", artistId = pearlJam, albumId = ten)
        val go = f.seedTrack("go.mp3", artistId = other, albumId = vs)
        repeat(2) { f.history.recordPlay(alive, 1_000L + it) }
        repeat(2) { f.history.recordPlay(black, 1_000L + it) }
        f.history.recordPlay(go, 2_000L)

        val topArtists = f.history.observeTopArtists(sinceMs = 0L, limit = 10).first()
        assertEquals(listOf(pearlJam, other), topArtists.map { it.artistId })
        assertEquals(listOf(4L, 1L), topArtists.map { it.plays })

        val topAlbums = f.history.observeTopAlbums(sinceMs = 0L, limit = 10).first()
        assertEquals(listOf(ten, vs), topAlbums.map { it.albumId })
        assertEquals(listOf(4L, 1L), topAlbums.map { it.plays })
    }
}
