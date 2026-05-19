package com.offlineplaya.shared.data.scanner

/**
 * Pure parsers for tag values that arrive as strings. Decouples the format
 * quirks from any Android API, so we can unit-test them in `commonTest`.
 */
internal object MetadataParsing {

    /**
     * Parse a track-number tag. ID3v2 commonly stores `"3/12"`, FLAC stores
     * just `"3"`, some files have leading zeroes (`"03"`). Returns `null` for
     * blank / unparseable input.
     */
    fun parseTrackNumber(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val numStr = raw.substringBefore('/').trim()
        return numStr.toIntOrNull()?.takeIf { it > 0 }
    }

    /**
     * Parse a year tag. Accepts a bare 4-digit year, an ISO-like `"1991-05-20"`,
     * or any string containing a `19xx`/`20xx` substring. Returns `null` if no
     * 4-digit year is found.
     */
    private val YEAR_REGEX = Regex("\\b(19|20)\\d{2}\\b")

    fun parseYear(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val match = YEAR_REGEX.find(raw) ?: return null
        return match.value.toIntOrNull()
    }
}
