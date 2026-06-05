package com.offlineplaya.shared.domain.lyrics

/**
 * Pure, platform-agnostic parser for lyrics text. Handles the LRC format
 * (timestamped lines) and falls back to plain text when no timestamps are
 * present. No Android dependencies — this is the unit-tested core of the
 * lyrics feature.
 *
 * Supported LRC syntax:
 *  - Time tags `[mm:ss]`, `[mm:ss.xx]` (centiseconds), `[mm:ss.xxx]` (millis),
 *    and the `:`-separated fraction variant `[mm:ss:xx]`.
 *  - **Multiple time tags on one line** (`[00:10.00][00:25.00]chorus`) expand to
 *    one [LyricLine] each.
 *  - The `[offset:±ms]` metadata tag shifts every timestamp (positive = lyrics
 *    appear earlier, per common player convention, so it's subtracted).
 *  - Other ID/metadata tags (`[ar:]`, `[ti:]`, `[al:]`, `[by:]`, `[length:]`,
 *    `[re:]`, `[ve:]`, …) are ignored.
 *  - Blank lines, malformed lines, and stray text are tolerated.
 */
object LrcParser {

    // [mm:ss], [mm:ss.xx], [mm:ss.xxx], [mm:ss:xx]. Minutes/seconds 1+ digits
    // (tolerant of non-zero-padded hand-written files); fraction 1-3 digits.
    private val TIME_TAG = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val OFFSET_TAG = Regex("""\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)

    fun parse(raw: String): Lyrics {
        if (raw.isBlank()) return Lyrics.None

        val offsetMs = OFFSET_TAG.find(raw)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

        val timed = mutableListOf<LyricLine>()
        val plainLines = mutableListOf<String>()
        var sawAnyTimeTag = false

        for (rawLine in raw.lineSequence()) {
            val matches = TIME_TAG.findAll(rawLine).toList()
            if (matches.isEmpty()) {
                // No timestamp. Keep as plain-text candidate unless it's a pure
                // metadata/ID tag line like "[ar:Artist]".
                val trimmed = rawLine.trim()
                if (trimmed.isNotEmpty() && !isMetadataOnly(trimmed)) {
                    plainLines += trimmed
                }
                continue
            }

            sawAnyTimeTag = true
            // Text is whatever follows the last time tag on the line.
            val lastTag = matches.last()
            val text = rawLine.substring(lastTag.range.last + 1).trim()
            for (m in matches) {
                val timeMs = toMillis(m) - offsetMs
                timed += LyricLine(timeMs = timeMs.coerceAtLeast(0L), text = text)
            }
        }

        return when {
            sawAnyTimeTag && timed.isNotEmpty() ->
                Lyrics.Synced(timed.sortedBy { it.timeMs })
            plainLines.isNotEmpty() ->
                Lyrics.Plain(plainLines.joinToString("\n"))
            else -> Lyrics.None
        }
    }

    private fun toMillis(match: MatchResult): Long {
        val (_, minStr, secStr, fracStr) = match.groupValues
        val minutes = minStr.toLong()
        val seconds = secStr.toLong()
        val fracMs = when (fracStr.length) {
            0 -> 0L
            1 -> fracStr.toLong() * 100   // tenths
            2 -> fracStr.toLong() * 10    // centiseconds
            else -> fracStr.take(3).toLong() // milliseconds
        }
        return minutes * 60_000L + seconds * 1_000L + fracMs
    }

    /**
     * True for lines that are *only* a metadata/ID tag (`[ar:...]`, `[offset:...]`,
     * etc.) with no lyric text — so they don't leak into plain-text output.
     */
    private fun isMetadataOnly(line: String): Boolean {
        val m = Regex("""^\[[a-zA-Z]+:.*]$""").matchEntire(line)
        return m != null
    }
}
