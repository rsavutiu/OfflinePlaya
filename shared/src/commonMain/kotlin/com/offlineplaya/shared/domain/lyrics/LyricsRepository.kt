package com.offlineplaya.shared.domain.lyrics

import com.offlineplaya.shared.domain.model.Track

/**
 * Resolves lyrics for a track through the source chain (positive cache →
 * embedded tags → `.lrc` sidecar → remote) and persists what it finds. The
 * Android implementation is `SqlLyricsRepository`.
 */
interface LyricsRepository {

    /** Best available lyrics for [track], or [Lyrics.None] if nothing is found. */
    suspend fun lyricsFor(track: Track): Lyrics

    /**
     * Remote lyrics matches the user can choose between (the "pick from matches"
     * flow) — best first, empty when remote lookup is off or nothing is found.
     * Does not touch the cache.
     */
    suspend fun candidatesFor(track: Track): List<LyricsCandidate>

    /**
     * Persist [candidate] as the chosen lyrics for [track] — it becomes the
     * cached, authoritative result returned by [lyricsFor] from now on,
     * overriding any earlier auto-resolved lyrics. Returns the parsed [Lyrics].
     */
    suspend fun selectCandidate(track: Track, candidate: LyricsCandidate): Lyrics
}
