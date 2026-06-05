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
}
