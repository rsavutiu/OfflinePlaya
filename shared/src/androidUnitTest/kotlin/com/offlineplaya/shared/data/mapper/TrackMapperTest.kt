package com.offlineplaya.shared.data.mapper

import com.offlineplaya.shared.domain.model.ScanStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.offlineplaya.shared.database.Track as TrackRow

class TrackMapperTest {

    @Test
    fun `mapper falls back to filename when title is null or blank`() {
        val row = trackRow(title = null, file_name = "song.mp3")
        assertEquals("song.mp3", row.toDomain().title)

        val blank = trackRow(title = "   ", file_name = "song.mp3")
        assertEquals("song.mp3", blank.toDomain().title)
    }

    @Test
    fun `mapper uses title when present and non-blank`() {
        val row = trackRow(title = "Real Title")
        assertEquals("Real Title", row.toDomain().title)
    }

    @Test
    fun `mapper falls back to Unknown Artist when artist_name is null or blank`() {
        assertEquals(UNKNOWN_ARTIST, trackRow(artist_name = null).toDomain().artistName)
        assertEquals(UNKNOWN_ARTIST, trackRow(artist_name = "  ").toDomain().artistName)
    }

    @Test
    fun `mapper falls back to Unknown Album when album_name is null or blank`() {
        assertEquals(UNKNOWN_ALBUM, trackRow(album_name = null).toDomain().albumName)
        assertEquals(UNKNOWN_ALBUM, trackRow(album_name = "  ").toDomain().albumName)
    }

    @Test
    fun `mapper preserves album_artist_name as nullable and trims blank to null`() {
        assertNull(trackRow(album_artist_name = null).toDomain().albumArtistName)
        assertNull(trackRow(album_artist_name = "   ").toDomain().albumArtistName)
        assertEquals("AA", trackRow(album_artist_name = "AA").toDomain().albumArtistName)
    }

    @Test
    fun `mapper converts Long numeric fields to Int domain fields`() {
        val row = trackRow(
            year = 1999L,
            track_number = 4L,
            disc_number = 1L,
            bitrate = 320_000L,
            sample_rate = 44_100L,
            channels = 2L,
        )
        val track = row.toDomain()
        assertEquals(1999, track.year)
        assertEquals(4, track.trackNumber)
        assertEquals(1, track.discNumber)
        assertEquals(320_000, track.bitrate)
        assertEquals(44_100, track.sampleRate)
        assertEquals(2, track.channels)
    }

    @Test
    fun `mapper preserves duration_ms as Long`() {
        assertEquals(184_000L, trackRow(duration_ms = 184_000L).toDomain().durationMs)
        assertNull(trackRow(duration_ms = null).toDomain().durationMs)
    }

    @Test
    fun `mapper translates scan_status string to ScanStatus enum`() {
        assertEquals(ScanStatus.PENDING, trackRow(scan_status = "pending").toDomain().scanStatus)
        assertEquals(ScanStatus.SCANNED, trackRow(scan_status = "scanned").toDomain().scanStatus)
        assertEquals(ScanStatus.ERROR, trackRow(scan_status = "error").toDomain().scanStatus)
    }

    @Test
    fun `mapper carries foreign keys through unchanged`() {
        val row = trackRow(artist_id = 7L, album_id = 42L, folder_id = 99L)
        val track = row.toDomain()
        assertEquals(7L, track.artistId)
        assertEquals(42L, track.albumId)
        assertEquals(99L, track.folderId)
    }

    @Suppress("LongParameterList")
    private fun trackRow(
        id: Long = 1L,
        document_uri: String = "content://uri/1",
        tree_uri: String = "content://tree",
        relative_path: String = "Artist/Album/song.mp3",
        file_name: String = "song.mp3",
        file_size: Long = 5_000_000L,
        last_modified: Long = 1_700_000_000L,
        title: String? = "Title",
        artist_name: String? = "Artist",
        album_artist_name: String? = null,
        album_name: String? = "Album",
        genre: String? = null,
        year: Long? = null,
        track_number: Long? = null,
        disc_number: Long? = null,
        duration_ms: Long? = null,
        bitrate: Long? = null,
        sample_rate: Long? = null,
        channels: Long? = null,
        codec: String? = null,
        artist_id: Long? = null,
        album_id: Long? = null,
        folder_id: Long? = null,
        scan_status: String = "pending",
        canonical_genre: String? = null,
        created_at: Long = 0L,
        updated_at: Long = 0L,
    ) = TrackRow(
        id = id,
        document_uri = document_uri,
        tree_uri = tree_uri,
        relative_path = relative_path,
        file_name = file_name,
        file_size = file_size,
        last_modified = last_modified,
        title = title,
        artist_name = artist_name,
        album_artist_name = album_artist_name,
        album_name = album_name,
        genre = genre,
        year = year,
        track_number = track_number,
        disc_number = disc_number,
        duration_ms = duration_ms,
        bitrate = bitrate,
        sample_rate = sample_rate,
        channels = channels,
        codec = codec,
        artist_id = artist_id,
        album_id = album_id,
        folder_id = folder_id,
        scan_status = scan_status,
        canonical_genre = canonical_genre,
        created_at = created_at,
        updated_at = updated_at,
    )
}
