package com.offlineplaya.shared.domain.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LrcParserTest {

    @Test
    fun `blank input is None`() {
        assertEquals(Lyrics.None, LrcParser.parse(""))
        assertEquals(Lyrics.None, LrcParser.parse("   \n  \n"))
    }

    @Test
    fun `simple synced lines parse with centisecond precision`() {
        val lrc = """
            [00:01.00]First line
            [00:03.50]Second line
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertTrue(result is Lyrics.Synced)
        result as Lyrics.Synced
        assertEquals(
            listOf(
                LyricLine(1_000L, "First line"),
                LyricLine(3_500L, "Second line"),
            ),
            result.lines,
        )
    }

    @Test
    fun `millisecond and tenths fractions both supported`() {
        val lrc = "[01:02.5]tenths\n[01:02.123]millis"
        val result = LrcParser.parse(lrc) as Lyrics.Synced
        // 1:02.5 = 62500ms ; 1:02.123 = 62123ms (sorted ascending)
        assertEquals(62_123L, result.lines[0].timeMs)
        assertEquals(62_500L, result.lines[1].timeMs)
    }

    @Test
    fun `multiple time tags on one line expand to one entry each`() {
        val lrc = "[00:10.00][00:25.00]chorus"
        val result = LrcParser.parse(lrc) as Lyrics.Synced
        assertEquals(
            listOf(
                LyricLine(10_000L, "chorus"),
                LyricLine(25_000L, "chorus"),
            ),
            result.lines,
        )
    }

    @Test
    fun `out of order timestamps are sorted ascending`() {
        val lrc = "[00:30.00]late\n[00:05.00]early"
        val result = LrcParser.parse(lrc) as Lyrics.Synced
        assertEquals("early", result.lines.first().text)
        assertEquals("late", result.lines.last().text)
    }

    @Test
    fun `metadata tags are ignored and do not break parsing`() {
        val lrc = """
            [ar:Some Artist]
            [ti:Some Title]
            [al:Some Album]
            [by:tagger]
            [00:01.00]actual line
        """.trimIndent()

        val result = LrcParser.parse(lrc) as Lyrics.Synced
        assertEquals(1, result.lines.size)
        assertEquals("actual line", result.lines.first().text)
    }

    @Test
    fun `offset tag shifts timestamps earlier`() {
        // +500ms offset → subtract 500ms from each timestamp.
        val lrc = "[offset:+500]\n[00:02.00]line"
        val result = LrcParser.parse(lrc) as Lyrics.Synced
        assertEquals(1_500L, result.lines.first().timeMs)
    }

    @Test
    fun `negative offset shifts timestamps later`() {
        val lrc = "[offset:-300]\n[00:02.00]line"
        val result = LrcParser.parse(lrc) as Lyrics.Synced
        assertEquals(2_300L, result.lines.first().timeMs)
    }

    @Test
    fun `offset never produces a negative timestamp`() {
        val lrc = "[offset:+5000]\n[00:02.00]line"
        val result = LrcParser.parse(lrc) as Lyrics.Synced
        assertEquals(0L, result.lines.first().timeMs)
    }

    @Test
    fun `text with no timestamps is treated as plain`() {
        val lrc = "Just some\nunsynced lyrics\nhere"
        val result = LrcParser.parse(lrc)
        assertEquals(Lyrics.Plain("Just some\nunsynced lyrics\nhere"), result)
    }

    @Test
    fun `plain fallback drops pure metadata lines`() {
        val lrc = "[ar:X]\nreal lyric line"
        assertEquals(Lyrics.Plain("real lyric line"), LrcParser.parse(lrc))
    }

    @Test
    fun `malformed and blank lines are tolerated`() {
        val lrc = """
            [00:01.00]good
            [garbage]

            [00:bad]also garbage but text kept
            [00:04.00]
        """.trimIndent()

        val result = LrcParser.parse(lrc) as Lyrics.Synced
        // Two valid timestamped lines; the empty-text one at 0:04 is preserved.
        assertEquals(2, result.lines.size)
        assertEquals("good", result.lines[0].text)
        assertEquals(4_000L, result.lines[1].timeMs)
        assertEquals("", result.lines[1].text)
    }

    @Test
    fun `only metadata with no lyrics is None`() {
        val lrc = "[ar:X]\n[ti:Y]\n[offset:+10]"
        assertEquals(Lyrics.None, LrcParser.parse(lrc))
    }
}
