package com.offlineplaya.shared.domain.lyrics

/**
 * Resolved lyrics for a track.
 *
 * - [Synced] — timestamped lines (from an `.lrc` sidecar or LRCLIB). Drives the
 *   auto-scrolling, line-highlighting view.
 * - [Plain] — unsynced text (embedded `USLT` / Vorbis `LYRICS`, or a `.txt`
 *   sidecar). Shown as a static scrollable block.
 * - [None] — nothing found anywhere.
 */
sealed interface Lyrics {
    data class Synced(val lines: List<LyricLine>) : Lyrics
    data class Plain(val text: String) : Lyrics
    data object None : Lyrics
}

/**
 * One timestamped lyric line. [timeMs] is the playback position at which the
 * line becomes the "current" line. [text] may be empty (an instrumental gap or
 * a deliberate blank line in the source).
 */
data class LyricLine(
    val timeMs: Long,
    val text: String,
)
