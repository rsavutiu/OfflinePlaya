package com.offlineplaya.shared.presentation.auto

import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrowseTreeBuilderTest {

    private val builder = BrowseTreeBuilder(
        artistNameByAlbum = { id -> if (id == 1L) "Radiohead" else null },
    )

    private fun album(id: Long, name: String, artistId: Long?) =
        Album(id = id, name = name, artistId = artistId, year = null, trackCount = 0, durationMs = 0L)

    private fun artist(id: Long, name: String, albumCount: Int) =
        Artist(id = id, name = name, albumCount = albumCount, trackCount = 0, imageUrl = null)

    private fun playlist(id: Long, name: String) =
        Playlist(id = id, name = name, createdAt = 0L, updatedAt = 0L)

    private fun track(id: Long, title: String, artistName: String = "Artist") = Track(
        id = id, documentUri = "uri", treeUri = "tree", relativePath = "p", fileName = "f",
        title = title, artistName = artistName, albumArtistName = null, albumName = "Album",
        genre = null, year = null, trackNumber = null, discNumber = null, durationMs = null,
        bitrate = null, sampleRate = null, channels = null, codec = null,
        artistId = null, albumId = null, folderId = null, scanStatus = ScanStatus.SCANNED,
        canonicalGenre = null,
    )

    @Test
    fun `rootChildren returns the four fixed nodes in stable order`() {
        val root = builder.rootChildren()
        assertEquals(4, root.size)
        assertEquals(MediaIdRouter.NODE_RECENTS, root[0].mediaId)
        assertEquals(MediaIdRouter.NODE_ALBUMS, root[1].mediaId)
        assertEquals(MediaIdRouter.NODE_ARTISTS, root[2].mediaId)
        assertEquals(MediaIdRouter.NODE_PLAYLISTS, root[3].mediaId)
        // All four are browsable; none playable.
        assertTrue(root.all { it.isBrowsable && !it.isPlayable })
    }

    @Test
    fun `albumsAsBrowsable maps fields and uses artistNameByAlbum for subtitle`() {
        val entries = builder.albumsAsBrowsable(
            listOf(
                album(10, "In Rainbows", artistId = 1L),
                album(11, "Untitled", artistId = null),
            ),
        )
        assertEquals(2, entries.size)
        val first = entries[0]
        assertEquals(MediaIdRouter.albumId(10), first.mediaId)
        assertEquals("In Rainbows", first.title)
        assertEquals("Radiohead", first.subtitle)
        assertTrue(first.isBrowsable)
        // artistId null → subtitle null, no crash.
        assertNull(entries[1].subtitle)
    }

    @Test
    fun `artistsAsBrowsable uses pluralisation for album count subtitle`() {
        val entries = builder.artistsAsBrowsable(
            listOf(
                artist(1, "Single", albumCount = 1),
                artist(2, "Many", albumCount = 7),
                artist(3, "None", albumCount = 0),
            ),
        )
        assertEquals("1 album", entries[0].subtitle)
        assertEquals("7 albums", entries[1].subtitle)
        // Zero collapses to null instead of "0 albums".
        assertNull(entries[2].subtitle)
    }

    @Test
    fun `playlistsAsBrowsable surfaces name and routes to the playlist media id`() {
        val entries = builder.playlistsAsBrowsable(
            listOf(playlist(5, "Roadtrip"), playlist(6, "Coding")),
        )
        assertEquals(MediaIdRouter.playlistId(5), entries[0].mediaId)
        assertEquals("Roadtrip", entries[0].title)
        assertTrue(entries.all { it.isBrowsable })
    }

    @Test
    fun `tracksAsPlayable bakes the parent context into each media id`() {
        val tracks = listOf(track(100, "T1"), track(101, "T2"))
        val parent = MediaIdRouter.ParentContext.Album(42L)
        val entries = builder.tracksAsPlayable(tracks, parent)

        assertEquals(2, entries.size)
        entries.forEachIndexed { i, e ->
            assertTrue(e.isPlayable)
            assertEquals(tracks[i].id, e.trackId)
            // Round-trip the embedded parent context.
            val parsed = MediaIdRouter.parse(e.mediaId) as MediaIdRouter.ParsedId.Entity
            assertEquals(MediaIdRouter.Kind.TRACK, parsed.kind)
            assertEquals(tracks[i].id, parsed.id)
            assertEquals(parent, parsed.parent)
        }
    }

    @Test
    fun `albumsAsBrowsable propagates artwork uri from the lambda`() {
        val withArt = BrowseTreeBuilder(
            albumArtUri = { album -> "content://art/album/${album.id}" },
        )
        val entries = withArt.albumsAsBrowsable(listOf(album(99, "X", artistId = 1L)))
        assertEquals("content://art/album/99", entries.single().artworkUri)
    }

    @Test
    fun `tracksAsPlayable propagates artwork uri from the lambda`() {
        val withArt = BrowseTreeBuilder(
            trackArtUri = { track -> "content://art/track/${track.id}" },
        )
        val entries = withArt.tracksAsPlayable(
            listOf(track(7, "Song")),
            MediaIdRouter.ParentContext.Album(1L),
        )
        assertEquals("content://art/track/7", entries.single().artworkUri)
    }

    @Test
    fun `artwork uri stays null when the lambda returns null`() {
        // Default builder has both lambdas returning null.
        val entries = builder.albumsAsBrowsable(listOf(album(1, "A", 1L)))
        assertNull(entries.single().artworkUri)
    }

    @Test
    fun `top-level cap is enforced`() {
        val many = (1..(BrowseTreeBuilder.MAX_TOP_LEVEL_RESULTS + 5)).map {
            album(it.toLong(), "Album $it", artistId = null)
        }
        val entries = builder.albumsAsBrowsable(many)
        assertEquals(BrowseTreeBuilder.MAX_TOP_LEVEL_RESULTS, entries.size)
    }

    @Test
    fun `per-node track cap is enforced`() {
        val many = (1..(BrowseTreeBuilder.MAX_TRACKS_PER_NODE + 10)).map { track(it.toLong(), "T$it") }
        val entries = builder.tracksAsPlayable(many, MediaIdRouter.ParentContext.Album(1L))
        assertEquals(BrowseTreeBuilder.MAX_TRACKS_PER_NODE, entries.size)
    }
}
