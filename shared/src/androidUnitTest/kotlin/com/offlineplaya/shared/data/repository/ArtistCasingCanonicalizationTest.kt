package com.offlineplaya.shared.data.repository

import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The artist row freezes whichever tag casing was scanned first
 * (INSERT OR IGNORE against UNIQUE NOCASE). [ArtistRepository.canonicalizeCasing]
 * plus [TrackRepository.normalizeArtistNameCasing] undo that freeze: majority
 * vote of the raw tags wins, ties prefer the uppercase-leaning variant, and
 * every NOCASE-equal tag is pulled onto the winner.
 */
class ArtistCasingCanonicalizationTest {

    private val db = createInMemoryDatabase()
    private val artists = SqlArtistRepository(db, TestLogger(), Dispatchers.Unconfined)
    private val tracks = SqlTrackRepository(db, TestLogger(), Dispatchers.Unconfined)

    private suspend fun seedTrack(n: Int, artistId: Long, artistTag: String): Long {
        val id = tracks.insertFile(
            documentUri = "doc://$n",
            treeUri = "tree://t",
            relativePath = "x/$n.mp3",
            fileName = "$n.mp3",
            fileSize = 1L,
            lastModified = 1L,
            folderId = null,
        )
        val track = tracks.findById(id)!!
        tracks.updateMetadata(track.copy(artistName = artistTag, albumArtistName = artistTag))
        tracks.updateForeignKeys(id, artistId = artistId, albumId = null)
        return id
    }

    private suspend fun artistTagsById(artistId: Long): List<String> =
        (1..COUNT).mapNotNull { tracks.findById(it.toLong())?.artistName }

    @Test
    fun `majority tag casing wins and recases row plus tracks`() = runTest {
        // Wrong casing scanned first -> row frozen as "Charli Xcx".
        val id = artists.upsert("Charli Xcx")
        seedTrack(1, id, "Charli Xcx")
        seedTrack(2, id, "Charli XCX")
        seedTrack(3, id, "Charli XCX")

        val canonical = artists.canonicalizeCasing(id)
        assertEquals("Charli XCX", canonical)
        tracks.normalizeArtistNameCasing(id, canonical!!)

        assertEquals("Charli XCX", artists.findById(id)?.name)
        assertEquals(List(3) { "Charli XCX" }, artistTagsById(id))
    }

    @Test
    fun `tie prefers the uppercase-leaning variant`() = runTest {
        val id = artists.upsert("Abba")
        seedTrack(1, id, "Abba")
        seedTrack(2, id, "ABBA")

        assertEquals("ABBA", artists.canonicalizeCasing(id))
    }

    @Test
    fun `compound featuring tags are left untouched`() = runTest {
        val id = artists.upsert("Charli XCX")
        seedTrack(1, id, "Charli Xcx")
        seedTrack(2, id, "Charli Xcx & Lorde")
        seedTrack(3, id, "Charli XCX")
        seedTrack(4, id, "Charli XCX")

        val canonical = artists.canonicalizeCasing(id)!!
        tracks.normalizeArtistNameCasing(id, canonical)

        assertEquals(
            listOf("Charli XCX", "Charli Xcx & Lorde", "Charli XCX", "Charli XCX"),
            (1..4).map { tracks.findById(it.toLong())!!.artistName },
        )
    }

    @Test
    fun `album-artist tags recase by name across the table`() = runTest {
        val id = artists.upsert("Charli Xcx")
        seedTrack(1, id, "Charli XCX")
        // Track under a DIFFERENT artist id but album-artist tagged Charli.
        val other = artists.upsert("Lorde")
        seedTrack(2, other, "Lorde")
        val t2 = tracks.findById(2)!!
        tracks.updateMetadata(t2.copy(albumArtistName = "Charli Xcx"))

        val canonical = artists.canonicalizeCasing(id)!!
        tracks.normalizeAlbumArtistNameCasing(canonical)

        assertEquals("Charli XCX", tracks.findById(1)?.albumArtistName)
        assertEquals("Charli XCX", tracks.findById(2)?.albumArtistName)
    }

    @Test
    fun `artist with no matching tags returns null and keeps its name`() = runTest {
        val id = artists.upsert("Various Artists")
        // Only compound / unrelated tags under this id (the compilation case).
        seedTrack(1, id, "Billy Idol")

        assertNull(artists.canonicalizeCasing(id))
        assertEquals("Various Artists", artists.findById(id)?.name)
    }

    private companion object {
        const val COUNT = 3
    }
}
