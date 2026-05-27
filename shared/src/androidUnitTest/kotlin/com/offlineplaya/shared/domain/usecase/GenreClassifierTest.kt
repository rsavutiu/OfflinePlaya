package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.model.CanonicalGenre
import kotlin.test.Test
import kotlin.test.assertEquals

class GenreClassifierTest {

    private fun assertClassifies(raw: String?, expected: CanonicalGenre) {
        assertEquals(expected, GenreClassifier.classify(raw), "input was: $raw")
    }

    @Test
    fun `null and blank fall through to DEFAULT`() {
        assertClassifies(null, CanonicalGenre.DEFAULT)
        assertClassifies("", CanonicalGenre.DEFAULT)
        assertClassifies("   ", CanonicalGenre.DEFAULT)
    }

    @Test
    fun `obvious matches work`() {
        assertClassifies("Rock", CanonicalGenre.ROCK)
        assertClassifies("rock", CanonicalGenre.ROCK)
        assertClassifies("ROCK", CanonicalGenre.ROCK)
        assertClassifies("Pop", CanonicalGenre.POP)
        assertClassifies("Jazz", CanonicalGenre.JAZZ)
        assertClassifies("Classical", CanonicalGenre.CLASSICAL)
        assertClassifies("Electronic", CanonicalGenre.ELECTRONIC)
        assertClassifies("Hip-Hop", CanonicalGenre.HIPHOP)
    }

    @Test
    fun `compound rock variants bucket as ROCK`() {
        assertClassifies("Alternative Rock", CanonicalGenre.ROCK)
        assertClassifies("Indie Rock", CanonicalGenre.ROCK)
        assertClassifies("Punk Rock", CanonicalGenre.ROCK)
        assertClassifies("Heavy Metal", CanonicalGenre.ROCK)
        assertClassifies("Death Metal", CanonicalGenre.ROCK)
        assertClassifies("Grunge", CanonicalGenre.ROCK)
    }

    @Test
    fun `electronic variants bucket correctly`() {
        assertClassifies("House", CanonicalGenre.ELECTRONIC)
        assertClassifies("Deep House", CanonicalGenre.ELECTRONIC)
        assertClassifies("Techno", CanonicalGenre.ELECTRONIC)
        assertClassifies("Trance", CanonicalGenre.ELECTRONIC)
        assertClassifies("Drum and Bass", CanonicalGenre.ELECTRONIC)
        assertClassifies("Dubstep", CanonicalGenre.ELECTRONIC)
        assertClassifies("Ambient", CanonicalGenre.ELECTRONIC)
        assertClassifies("Synthwave", CanonicalGenre.ELECTRONIC)
        assertClassifies("IDM", CanonicalGenre.ELECTRONIC)
    }

    @Test
    fun `hiphop variants bucket correctly`() {
        assertClassifies("Hip Hop", CanonicalGenre.HIPHOP)
        assertClassifies("Hip-Hop", CanonicalGenre.HIPHOP)
        assertClassifies("Rap", CanonicalGenre.HIPHOP)
        assertClassifies("Trap", CanonicalGenre.HIPHOP)
        assertClassifies("R&B", CanonicalGenre.HIPHOP)
        assertClassifies("Soul", CanonicalGenre.HIPHOP)
        assertClassifies("Funk", CanonicalGenre.HIPHOP)
    }

    @Test
    fun `pop variants bucket correctly`() {
        assertClassifies("K-Pop", CanonicalGenre.POP)
        assertClassifies("J-Pop", CanonicalGenre.POP)
        assertClassifies("Synth Pop", CanonicalGenre.POP)
        assertClassifies("Dance Pop", CanonicalGenre.POP)
    }

    @Test
    fun `classical variants bucket correctly`() {
        assertClassifies("Baroque", CanonicalGenre.CLASSICAL)
        assertClassifies("Opera", CanonicalGenre.CLASSICAL)
        assertClassifies("Chamber Music", CanonicalGenre.CLASSICAL)
        assertClassifies("Orchestra", CanonicalGenre.CLASSICAL)
        assertClassifies("Symphony No. 9", CanonicalGenre.CLASSICAL)
    }

    @Test
    fun `jazz family includes blues`() {
        assertClassifies("Jazz", CanonicalGenre.JAZZ)
        assertClassifies("Bebop", CanonicalGenre.JAZZ)
        assertClassifies("Swing", CanonicalGenre.JAZZ)
        assertClassifies("Blues", CanonicalGenre.JAZZ)
        assertClassifies("Delta Blues", CanonicalGenre.JAZZ)
    }

    @Test
    fun `numeric ID3v1 codes are expanded`() {
        assertClassifies("(17)", CanonicalGenre.ROCK)   // Rock
        assertClassifies("17", CanonicalGenre.ROCK)
        assertClassifies("(7)", CanonicalGenre.HIPHOP)  // Hip-Hop
        assertClassifies("(32)", CanonicalGenre.CLASSICAL)
        assertClassifies("(8)", CanonicalGenre.JAZZ)    // Jazz
        assertClassifies("(13)", CanonicalGenre.POP)
        assertClassifies("(52)", CanonicalGenre.ELECTRONIC) // Electronic
        // ID3v1 code with trailing text still resolves via the code.
        assertClassifies("(17)Rock", CanonicalGenre.ROCK)
    }

    @Test
    fun `out-of-range numeric codes fall through to keyword search`() {
        // 999 isn't a known ID3 code; "Rock" still matches via keyword pass.
        assertClassifies("(999)Rock", CanonicalGenre.ROCK)
        // Bare unknown number with no keyword → DEFAULT.
        assertClassifies("999", CanonicalGenre.DEFAULT)
    }

    @Test
    fun `multi-genre tags pick the first segment`() {
        assertClassifies("Rock; Pop", CanonicalGenre.ROCK)
        assertClassifies("Jazz / Fusion", CanonicalGenre.JAZZ)
        assertClassifies("Electronic, Dance", CanonicalGenre.ELECTRONIC)
        assertClassifies("Hip-Hop | Rap", CanonicalGenre.HIPHOP)
    }

    @Test
    fun `unknown tags fall to DEFAULT`() {
        assertClassifies("Other", CanonicalGenre.DEFAULT)
        assertClassifies("Unknown", CanonicalGenre.DEFAULT)
        assertClassifies("Soundtrack", CanonicalGenre.DEFAULT) // not in v1 buckets
        assertClassifies("Audiobook", CanonicalGenre.DEFAULT)
        assertClassifies("xyz nonsense", CanonicalGenre.DEFAULT)
    }

    @Test
    fun `whitespace and punctuation are tolerated`() {
        assertClassifies("  Rock  ", CanonicalGenre.ROCK)
        assertClassifies("\tjazz\n", CanonicalGenre.JAZZ)
    }
}
