package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.model.BuiltInPresets
import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.domain.model.EqMode
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals

class PickPresetForTrackTest {

    private fun track(canonical: CanonicalGenre? = null) = Track(
        id = 1L,
        documentUri = "uri",
        treeUri = "tree",
        relativePath = "song.mp3",
        fileName = "song.mp3",
        title = "Song",
        artistName = "Artist",
        albumArtistName = null,
        albumName = "Album",
        genre = null,
        year = null,
        trackNumber = null,
        discNumber = null,
        durationMs = null,
        bitrate = null,
        sampleRate = null,
        channels = null,
        codec = null,
        artistId = null,
        albumId = null,
        folderId = null,
        scanStatus = ScanStatus.SCANNED,
        canonicalGenre = canonical,
    )

    @Test
    fun `OFF always returns FLAT regardless of track or manual preset`() {
        val prefs = EqPreferences(
            mode = EqMode.OFF,
            manualPresetName = "Rock",
            manualGains = listOf(500, 500, 500, 500, 500),
        )
        assertEquals(BuiltInPresets.FLAT, PickPresetForTrack.pick(track(CanonicalGenre.ROCK), prefs))
    }

    @Test
    fun `MANUAL returns the named preset when no overrides`() {
        val prefs = EqPreferences(EqMode.MANUAL, "Rock", emptyList())
        assertEquals(BuiltInPresets.ROCK, PickPresetForTrack.pick(track(), prefs))
    }

    @Test
    fun `MANUAL with overrides keeps the name but uses override gains`() {
        val overrides = listOf(100, 200, 300, 400, 500)
        val prefs = EqPreferences(EqMode.MANUAL, "Rock", overrides)
        val result = PickPresetForTrack.pick(track(), prefs)
        assertEquals("Rock", result.name)
        assertEquals(overrides, result.gainsMillibels)
    }

    @Test
    fun `MANUAL with unknown preset name falls back to FLAT`() {
        val prefs = EqPreferences(EqMode.MANUAL, "Made-up", emptyList())
        assertEquals(BuiltInPresets.FLAT, PickPresetForTrack.pick(track(), prefs))
    }

    @Test
    fun `AUTO maps canonical genre to preset`() {
        val prefs = EqPreferences(EqMode.AUTO, "Rock", emptyList())
        assertEquals(BuiltInPresets.JAZZ, PickPresetForTrack.pick(track(CanonicalGenre.JAZZ), prefs))
        assertEquals(BuiltInPresets.HIPHOP, PickPresetForTrack.pick(track(CanonicalGenre.HIPHOP), prefs))
        assertEquals(BuiltInPresets.CLASSICAL, PickPresetForTrack.pick(track(CanonicalGenre.CLASSICAL), prefs))
    }

    @Test
    fun `AUTO falls back to FLAT when track has no canonical_genre`() {
        val prefs = EqPreferences(EqMode.AUTO, "Rock", emptyList())
        assertEquals(BuiltInPresets.FLAT, PickPresetForTrack.pick(track(canonical = null), prefs))
    }

    @Test
    fun `AUTO with null track is DEFAULT preset`() {
        val prefs = EqPreferences(EqMode.AUTO, "Rock", emptyList())
        assertEquals(BuiltInPresets.FLAT, PickPresetForTrack.pick(null, prefs))
    }
}
