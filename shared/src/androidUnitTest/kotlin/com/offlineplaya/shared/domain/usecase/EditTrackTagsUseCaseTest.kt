package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.data.repository.SqlAlbumRepository
import com.offlineplaya.shared.data.repository.SqlArtistRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.model.TrackTagEdits
import com.offlineplaya.shared.domain.tag.TagWriteStats
import com.offlineplaya.shared.domain.tag.TrackTagWriter
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditTrackTagsUseCaseTest {

    private class FakeWriter(
        var succeed: Boolean = true,
        var stats: TagWriteStats = TagWriteStats(),
    ) : TrackTagWriter {
        val calls = mutableListOf<Pair<String, TrackTagEdits>>()
        override suspend fun write(documentUri: String, edits: TrackTagEdits): Result<TagWriteStats> {
            calls += documentUri to edits
            return if (succeed) Result.success(stats) else Result.failure(IllegalStateException("not writable"))
        }
    }

    private class Fixture {
        val db = createInMemoryDatabase()
        val tracks = SqlTrackRepository(db, TestLogger())
        val artists = SqlArtistRepository(db, TestLogger())
        val albums = SqlAlbumRepository(db, TestLogger())
        val writer = FakeWriter()
        val useCase = EditTrackTagsUseCase(tracks, artists, albums, writer, TestLogger())

        /** Seed one scanned track grouped under "Old Artist" / "Old Album". */
        suspend fun seedTrack(): Track {
            val id = tracks.insertFile(
                documentUri = "content://t/1", treeUri = "content://tree",
                relativePath = "a/b.mp3", fileName = "b.mp3",
                fileSize = 100L, lastModified = 1L, folderId = null,
            )
            val aid = artists.upsert("Old Artist")
            val alid = albums.upsert("Old Album", aid, 2000)
            val base = tracks.findById(id)!!
            tracks.updateMetadata(
                base.copy(title = "Song", artistName = "Old Artist", albumName = "Old Album", year = 2000),
            )
            tracks.updateForeignKeys(id, aid, alid)
            artists.refreshCounts(aid)
            albums.refreshAggregates(alid)
            return tracks.findById(id)!!
        }
    }

    @Test
    fun `editing artist regroups track and removes the orphaned old artist`() = runTest {
        val f = Fixture()
        val track = f.seedTrack()

        val edits = TrackTagEdits.from(track).copy(artist = "New Artist")
        val result = f.useCase.edit(track, edits)

        assertTrue(result.isSuccess)
        // File write happened with the new value.
        assertEquals(1, f.writer.calls.size)
        assertEquals("New Artist", f.writer.calls.single().second.artist)

        // Track row updated + relinked.
        val after = f.tracks.findById(track.id)!!
        assertEquals("New Artist", after.artistName)

        // New artist exists; old artist orphaned and cleaned up.
        assertNotNull(f.artists.findByName("New Artist"))
        assertNull(f.artists.findByName("Old Artist"), "old artist should be deleted as an orphan")
    }

    @Test
    fun `album-artist drives grouping when present`() = runTest {
        val f = Fixture()
        val track = f.seedTrack()

        val edits = TrackTagEdits.from(track).copy(artist = "Performer", albumArtist = "The Band")
        f.useCase.edit(track, edits).getOrThrow()

        val after = f.tracks.findById(track.id)!!
        assertEquals("Performer", after.artistName)
        assertEquals("The Band", after.albumArtistName)
        // Artists list groups by album-artist.
        assertNotNull(f.artists.findByName("The Band"))
    }

    @Test
    fun `successful edit refreshes the stored content fingerprint`() = runTest {
        // Rewriting tags changes the file's size/mtime on disk. If the row
        // kept the stale fingerprint, the next device-audio resync would
        // fail the content-key match and re-insert the file as a duplicate.
        val f = Fixture()
        f.writer.stats = TagWriteStats(fileSize = 12_345L, lastModified = 9_000_000L)
        val track = f.seedTrack()

        f.useCase.edit(track, TrackTagEdits.from(track).copy(artist = "New Artist")).getOrThrow()

        val byNewKey = f.tracks.findByContentKeyExcludingTree(
            fileName = "b.mp3",
            fileSize = 12_345L,
            lastModified = 9_000_000L,
            excludeTreeUri = "device://audio",
        )
        assertNotNull(byNewKey, "row must be findable by the post-edit fingerprint")
        assertEquals(track.id, byNewKey.id)
    }

    @Test
    fun `failed file write leaves the database untouched`() = runTest {
        val f = Fixture()
        f.writer.succeed = false
        val track = f.seedTrack()

        val result = f.useCase.edit(track, TrackTagEdits.from(track).copy(artist = "New Artist"))

        assertTrue(result.isFailure)
        val after = f.tracks.findById(track.id)!!
        assertEquals("Old Artist", after.artistName, "row must not change when the write fails")
        assertNotNull(f.artists.findByName("Old Artist"))
        assertNull(f.artists.findByName("New Artist"))
    }
}
