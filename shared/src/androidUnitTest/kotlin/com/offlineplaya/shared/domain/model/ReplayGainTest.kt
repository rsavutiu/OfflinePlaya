package com.offlineplaya.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReplayGainTest {

    @Test
    fun parsesGainStrings() {
        assertEquals(-6.48f, ReplayGainTags.parseGainDb("-6.48 dB"))
        assertEquals(3.2f, ReplayGainTags.parseGainDb("+3.2dB"))
        assertEquals(-1f, ReplayGainTags.parseGainDb(" -1.00 "))
        assertNull(ReplayGainTags.parseGainDb("abc"))
        assertNull(ReplayGainTags.parseGainDb(null))
        assertNull(ReplayGainTags.parseGainDb("NaN"))
    }

    @Test
    fun tagKeysAreCaseInsensitive() {
        val tags = ReplayGainTags.fromTags(
            listOf(
                "REPLAYGAIN_TRACK_GAIN" to "-7.00 dB",
                "replaygain_album_gain" to "-5.00 dB",
            ),
        )
        assertEquals(-7f, tags.trackGainDb)
        assertEquals(-5f, tags.albumGainDb)
    }

    @Test
    fun opusR128IsShiftedToReplayGainReference() {
        // -256 (Q7.8) = -1 dB relative to -23 LUFS → +4 dB vs the -18 LUFS reference.
        val tags = ReplayGainTags.fromTags(listOf("R128_TRACK_GAIN" to "-256"))
        assertEquals(4f, tags.trackGainDb)
    }

    @Test
    fun modeSelectsAndFallsBack() {
        val both = ReplayGainTags(trackGainDb = -6f, albumGainDb = -3f)
        assertEquals(-600, ReplayGain.gainMillibels(ReplayGainMode.TRACK, both))
        assertEquals(-300, ReplayGain.gainMillibels(ReplayGainMode.ALBUM, both))
        assertEquals(0, ReplayGain.gainMillibels(ReplayGainMode.OFF, both))

        val albumOnly = ReplayGainTags(albumGainDb = -3f)
        assertEquals(-300, ReplayGain.gainMillibels(ReplayGainMode.TRACK, albumOnly))
        val trackOnly = ReplayGainTags(trackGainDb = -6f)
        assertEquals(-600, ReplayGain.gainMillibels(ReplayGainMode.ALBUM, trackOnly))
    }

    @Test
    fun untaggedOrAbsurdMeansNoChange() {
        assertEquals(0, ReplayGain.gainMillibels(ReplayGainMode.TRACK, null))
        assertEquals(0, ReplayGain.gainMillibels(ReplayGainMode.TRACK, ReplayGainTags()))
        assertEquals(0, ReplayGain.gainMillibels(ReplayGainMode.TRACK, ReplayGainTags(trackGainDb = 90f)))
    }
}
