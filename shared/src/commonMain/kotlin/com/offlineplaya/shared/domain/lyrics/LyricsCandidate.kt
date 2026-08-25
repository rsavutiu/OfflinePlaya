package com.offlineplaya.shared.domain.lyrics

/**
 * One selectable lyrics match returned by a [RemoteLyricsSource] search, for
 * the "pick from matches" flow. Unlike the automatic [RemoteLyricsSource.resolve]
 * path — which silently rejects matches whose duration is too far off — a
 * candidate list surfaces everything so the user can choose, including matches
 * the auto-matcher would have skipped. [durationSec] is shown so the user can
 * eyeball the closest recording.
 *
 * [rawText] is the LRC/plain text that gets persisted (and re-parsed) when the
 * candidate is chosen; candidates with no usable text (instrumental rows, blank
 * bodies) are never emitted, so [rawText] is always non-blank.
 */
data class LyricsCandidate(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val durationSec: Int,
    val synced: Boolean,
    val rawText: String,
)
