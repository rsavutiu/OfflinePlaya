package com.offlineplaya.shared.domain.lyrics

import com.offlineplaya.shared.domain.model.Track

/**
 * Reads lyrics text embedded in the track's file tags (ID3 `USLT`, Vorbis
 * `LYRICS`, MP4 `©lyr`). Returns the raw text (usually unsynced) or `null` if
 * the file carries no lyrics tag. Implementations read on demand for a single
 * track — never the whole library.
 */
interface EmbeddedLyricsSource {
    suspend fun read(track: Track): String?
}

/**
 * Reads a lyrics sidecar file sitting next to the audio file — `<basename>.lrc`
 * (preferred, usually synced) or `<basename>.txt`. Returns the raw file text or
 * `null` when no sidecar is present (or the track has no SAF parent to list).
 */
interface SidecarLyricsSource {
    suspend fun read(track: Track): String?
}
