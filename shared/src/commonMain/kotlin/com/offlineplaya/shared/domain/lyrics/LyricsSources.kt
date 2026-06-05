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

/**
 * Fetches lyrics for a track from a remote service (LRCLIB in the current
 * Android impl). Returns the raw text — synced LRC when available, plain
 * unsynced text as a fallback — or `null` when nothing was found or the
 * lookup was already recorded as a persistent miss.
 *
 * Implementations are expected to be polite (rate-limited / pooled) and to
 * persist negative results so a clean miss for one track doesn't translate
 * into a fresh request every time the user revisits Now Playing.
 */
interface RemoteLyricsSource {
    suspend fun resolve(track: Track): String?
}
