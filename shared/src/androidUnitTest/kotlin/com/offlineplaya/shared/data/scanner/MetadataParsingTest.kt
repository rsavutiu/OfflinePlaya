package com.offlineplaya.shared.data.scanner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetadataParsingTest {

    @Test
    fun `parseTrackNumber handles bare integers`() {
        assertEquals(1, MetadataParsing.parseTrackNumber("1"))
        assertEquals(12, MetadataParsing.parseTrackNumber("12"))
    }

    @Test
    fun `parseTrackNumber handles ID3 slash form`() {
        assertEquals(3, MetadataParsing.parseTrackNumber("3/12"))
        assertEquals(1, MetadataParsing.parseTrackNumber("01/10"))
    }

    @Test
    fun `parseTrackNumber strips leading zeros`() {
        assertEquals(3, MetadataParsing.parseTrackNumber("03"))
        assertEquals(7, MetadataParsing.parseTrackNumber("007"))
    }

    @Test
    fun `parseTrackNumber returns null for blank or unparseable input`() {
        assertNull(MetadataParsing.parseTrackNumber(null))
        assertNull(MetadataParsing.parseTrackNumber(""))
        assertNull(MetadataParsing.parseTrackNumber("   "))
        assertNull(MetadataParsing.parseTrackNumber("foo"))
        assertNull(MetadataParsing.parseTrackNumber("0"), "zero is not a valid track number")
        assertNull(MetadataParsing.parseTrackNumber("-1"))
    }

    @Test
    fun `parseYear handles bare 4-digit years`() {
        assertEquals(1991, MetadataParsing.parseYear("1991"))
        assertEquals(2024, MetadataParsing.parseYear("2024"))
    }

    @Test
    fun `parseYear extracts year from ISO-like dates`() {
        assertEquals(1991, MetadataParsing.parseYear("1991-05-20"))
        assertEquals(2010, MetadataParsing.parseYear("2010-01-01T00:00:00Z"))
    }

    @Test
    fun `parseYear extracts the first 19xx-20xx substring`() {
        assertEquals(1985, MetadataParsing.parseYear("recorded in 1985"))
        // Earliest 19xx wins because it appears first in the string.
        assertEquals(1969, MetadataParsing.parseYear("1969 reissued 2009"))
    }

    @Test
    fun `parseYear rejects implausible years and blanks`() {
        assertNull(MetadataParsing.parseYear(null))
        assertNull(MetadataParsing.parseYear(""))
        assertNull(MetadataParsing.parseYear("nope"))
        assertNull(MetadataParsing.parseYear("1899"), "outside 19xx-20xx range")
        assertNull(MetadataParsing.parseYear("2100"), "outside 19xx-20xx range")
    }
}
