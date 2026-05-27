package com.offlineplaya.shared.presentation.auto

import com.offlineplaya.shared.presentation.auto.MediaIdRouter.Kind
import com.offlineplaya.shared.presentation.auto.MediaIdRouter.ParentContext
import com.offlineplaya.shared.presentation.auto.MediaIdRouter.ParsedId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MediaIdRouterTest {

    @Test
    fun `top-level node ids round-trip`() {
        val parsed = MediaIdRouter.parse(MediaIdRouter.ROOT)
        assertIs<ParsedId.Node>(parsed)
        assertEquals(MediaIdRouter.ROOT, parsed.name)

        listOf(
            MediaIdRouter.NODE_RECENTS,
            MediaIdRouter.NODE_ALBUMS,
            MediaIdRouter.NODE_ARTISTS,
            MediaIdRouter.NODE_PLAYLISTS,
        ).forEach { node ->
            val p = MediaIdRouter.parse(node)
            assertIs<ParsedId.Node>(p, "expected Node for $node")
            assertEquals(node, p.name)
        }
    }

    @Test
    fun `album artist playlist ids round-trip without parent`() {
        val albumId = MediaIdRouter.albumId(42L)
        val parsedAlbum = MediaIdRouter.parse(albumId)
        assertIs<ParsedId.Entity>(parsedAlbum)
        assertEquals(Kind.ALBUM, parsedAlbum.kind)
        assertEquals(42L, parsedAlbum.id)
        assertNull(parsedAlbum.parent)

        val parsedArtist = MediaIdRouter.parse(MediaIdRouter.artistId(7L))
        assertIs<ParsedId.Entity>(parsedArtist)
        assertEquals(Kind.ARTIST, parsedArtist.kind)
        assertEquals(7L, parsedArtist.id)

        val parsedPl = MediaIdRouter.parse(MediaIdRouter.playlistId(13L))
        assertIs<ParsedId.Entity>(parsedPl)
        assertEquals(Kind.PLAYLIST, parsedPl.kind)
        assertEquals(13L, parsedPl.id)
    }

    @Test
    fun `track id with album parent round-trips`() {
        val tid = MediaIdRouter.trackId(100L, ParentContext.Album(42L))
        val parsed = MediaIdRouter.parse(tid)
        assertIs<ParsedId.Entity>(parsed)
        assertEquals(Kind.TRACK, parsed.kind)
        assertEquals(100L, parsed.id)
        assertEquals(ParentContext.Album(42L), parsed.parent)
    }

    @Test
    fun `track id with artist parent round-trips`() {
        val tid = MediaIdRouter.trackId(101L, ParentContext.Artist(7L))
        val parsed = MediaIdRouter.parse(tid)
        assertIs<ParsedId.Entity>(parsed)
        assertEquals(ParentContext.Artist(7L), parsed.parent)
    }

    @Test
    fun `track id with playlist parent round-trips`() {
        val tid = MediaIdRouter.trackId(102L, ParentContext.Playlist(13L))
        val parsed = MediaIdRouter.parse(tid)
        assertIs<ParsedId.Entity>(parsed)
        assertEquals(ParentContext.Playlist(13L), parsed.parent)
    }

    @Test
    fun `track id without parent round-trips`() {
        val tid = MediaIdRouter.trackId(200L, parent = null)
        val parsed = MediaIdRouter.parse(tid)
        assertIs<ParsedId.Entity>(parsed)
        assertEquals(Kind.TRACK, parsed.kind)
        assertEquals(200L, parsed.id)
        assertNull(parsed.parent)
    }

    @Test
    fun `malformed ids return null`() {
        assertNull(MediaIdRouter.parse(""))
        assertNull(MediaIdRouter.parse("/"))
        assertNull(MediaIdRouter.parse("album/"))
        assertNull(MediaIdRouter.parse("/42"))
        assertNull(MediaIdRouter.parse("album/notanumber"))
        assertNull(MediaIdRouter.parse("madeUpKind/42"))
    }

    @Test
    fun `bare unknown segment returns null`() {
        // "junk" isn't a top node and has no slash → null, not a crash.
        assertNull(MediaIdRouter.parse("junk"))
    }

    @Test
    fun `track with unparseable parent yields null parent on the entity`() {
        // Manually craft a malformed "from" — parser should drop it rather
        // than fail the whole id parse.
        val parsed = MediaIdRouter.parse("track/5?from=bogus")
        assertIs<ParsedId.Entity>(parsed)
        assertEquals(Kind.TRACK, parsed.kind)
        assertEquals(5L, parsed.id)
        assertNull(parsed.parent)
    }
}
