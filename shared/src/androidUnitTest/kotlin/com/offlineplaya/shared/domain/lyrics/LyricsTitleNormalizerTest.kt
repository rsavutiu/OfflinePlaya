package com.offlineplaya.shared.domain.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LyricsTitleNormalizerTest {

    private fun assertNormalized(expected: String, input: String) =
        assertEquals(expected, LyricsTitleNormalizer.normalize(input), "for input '$input'")

    @Test
    fun `strips year remaster bracket`() {
        assertNormalized("Hey Jude", "Hey Jude (2014 Remaster)")
        assertNormalized("Hey Jude", "Hey Jude [2014 Remaster]")
    }

    @Test
    fun `strips assorted remaster and edition tags`() {
        assertNormalized("Come Together", "Come Together (Remastered 2009)")
        assertNormalized("Come Together", "Come Together (Remastered)")
        assertNormalized("Bohemian Rhapsody", "Bohemian Rhapsody (Mono)")
        assertNormalized("Wish You Were Here", "Wish You Were Here (Deluxe Edition)")
        assertNormalized("Thriller", "Thriller (Single Version)")
        assertNormalized("Yesterday", "Yesterday (Anniversary Edition)")
    }

    @Test
    fun `strips dash suffix remaster`() {
        assertNormalized("Hey Jude", "Hey Jude - 2014 Remaster")
        assertNormalized("Let It Be", "Let It Be - Remastered 2009")
        assertNormalized("Money", "Money - 2011 Remastered Version")
    }

    @Test
    fun `strips stacked bracket tags`() {
        assertNormalized("Get Back", "Get Back (Mono) (2009 Remaster)")
        assertNormalized("Revolution", "Revolution (Single Version) (Remastered)")
    }

    @Test
    fun `stops at first non-noise bracket group`() {
        // (Reprise) is meaningful, so we peel the remaster tag but keep it.
        assertNormalized("The Trial (Reprise)", "The Trial (Reprise) (2011 Remaster)")
    }

    @Test
    fun `keeps meaningful bracket groups`() {
        assertNormalized("Ms. Jackson (feat. Killer Mike)", "Ms. Jackson (feat. Killer Mike)")
        assertNormalized("The Trial (Reprise)", "The Trial (Reprise)")
        assertNormalized("Karn Evil 9 (Part 1)", "Karn Evil 9 (Part 1)")
    }

    @Test
    fun `keeps meaningful dash content`() {
        assertNormalized("Marquee Moon", "Marquee Moon")
        assertNormalized("Panic - The Smiths Cover", "Panic - The Smiths Cover")
    }

    @Test
    fun `trims but does not empty a bare bracket title`() {
        assertNormalized("(Remastered)", "(Remastered)")
    }

    @Test
    fun `trims whitespace`() {
        assertNormalized("Hey Jude", "  Hey Jude  ")
    }

    // --- stripSpam ---

    @Test
    fun `stripSpam removes bare domain suffix`() {
        assertEquals("Fire Rides", LyricsTitleNormalizer.stripSpam("Fire Rides vk.com/xclusives_zone"))
        assertEquals("Maiden", LyricsTitleNormalizer.stripSpam("Maiden vk.com/xclusives_zone"))
    }

    @Test
    fun `stripSpam removes full urls`() {
        assertEquals("Song", LyricsTitleNormalizer.stripSpam("Song https://t.me/somechannel"))
        assertEquals("Song", LyricsTitleNormalizer.stripSpam("Song www.example.com/x"))
    }

    @Test
    fun `stripSpam leaves clean titles untouched`() {
        assertEquals("XXX 88 (feat. Diplo)", LyricsTitleNormalizer.stripSpam("XXX 88 (feat. Diplo)"))
        assertEquals("Don't Wanna Dance", LyricsTitleNormalizer.stripSpam("Don't Wanna Dance"))
    }

    @Test
    fun `stripSpam never empties`() {
        assertEquals("vk.com/x", LyricsTitleNormalizer.stripSpam("vk.com/x"))
    }

    // --- stripCredits ---

    @Test
    fun `stripCredits removes featuring credit`() {
        assertEquals("XXX 88", LyricsTitleNormalizer.stripCredits("XXX 88 (feat. Diplo)"))
        assertEquals("Ms. Jackson", LyricsTitleNormalizer.stripCredits("Ms. Jackson (feat. Killer Mike)"))
        assertEquals("Drake", LyricsTitleNormalizer.stripCredits("Drake ft. Future"))
    }

    @Test
    fun `stripCredits keeps band names with commas and ampersands`() {
        assertEquals("Earth, Wind & Fire", LyricsTitleNormalizer.stripCredits("Earth, Wind & Fire"))
    }

    // --- stripLeadingTrackNumber ---

    @Test
    fun `stripLeadingTrackNumber removes numeric prefixes with a separator`() {
        assertEquals("The Beautiful American", LyricsTitleNormalizer.stripLeadingTrackNumber("06 - The Beautiful American"))
        assertEquals("Fire Rides", LyricsTitleNormalizer.stripLeadingTrackNumber("01. Fire Rides"))
        assertEquals("Maiden", LyricsTitleNormalizer.stripLeadingTrackNumber("3) Maiden"))
    }

    @Test
    fun `stripLeadingTrackNumber leaves numeric titles alone`() {
        assertEquals("99 Luftballons", LyricsTitleNormalizer.stripLeadingTrackNumber("99 Luftballons"))
        assertEquals("7 rings", LyricsTitleNormalizer.stripLeadingTrackNumber("7 rings"))
        assertEquals("3.14", LyricsTitleNormalizer.stripLeadingTrackNumber("3.14"))
    }

    @Test
    fun `variants strips a leading track number in the cleaned pair`() {
        val v = LyricsTitleNormalizer.variants("The Great Reunion", "06 - The Beautiful American")
        assertEquals("The Great Reunion" to "06 - The Beautiful American", v.first())
        assertTrue(
            v.any { it.second == "The Beautiful American" },
            "expected a track-number-stripped title; got $v",
        )
    }

    // --- core ---

    @Test
    fun `core peels all trailing groups`() {
        assertEquals("Fire Rides", LyricsTitleNormalizer.core("Fire Rides (Night Version)"))
        assertEquals("Song", LyricsTitleNormalizer.core("Song (Live) (Remix)"))
        assertEquals("Song", LyricsTitleNormalizer.core("Song - 2011 Whatever"))
    }

    // --- variants (escalation) ---

    @Test
    fun `variants collapses a clean title to album-title and album-dropped`() {
        val v = LyricsTitleNormalizer.variants("Ten", "Once")
        assertEquals(listOf("Ten" to "Once", "" to "Once"), v)
    }

    @Test
    fun `variants escalates through spam strip for a vk suffix`() {
        val v = LyricsTitleNormalizer.variants(
            "No Mythologies To Follow (Deluxe Edition)",
            "Fire Rides vk.com/xclusives_zone",
        )
        // Raw first, then the reissue+spam-cleaned pair must appear.
        assertEquals(
            "No Mythologies To Follow (Deluxe Edition)" to "Fire Rides vk.com/xclusives_zone",
            v.first(),
        )
        assertTrue(
            v.contains("No Mythologies To Follow" to "Fire Rides"),
            "expected a cleaned (album, title) pair; got $v",
        )
        assertTrue(v.none { it.second.isEmpty() }, "no empty titles allowed: $v")
    }

    @Test
    fun `variants never emits duplicates`() {
        val v = LyricsTitleNormalizer.variants("Ten", "Once")
        assertEquals(v.size, v.toSet().size, "variants must be de-duplicated: $v")
    }
}
