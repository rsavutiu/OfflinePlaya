package com.offlineplaya.shared.domain.image

/**
 * Looks up album art for an (artist, album) pair over the network. Returns
 * `null` when no confident match is found — callers fall back to a
 * placeholder. Lives in `commonMain` so future iOS/Desktop can provide their
 * own `actual` (or just stub it out).
 *
 * Implementations should:
 *  - Respect upstream rate limits (MusicBrainz allows ~1 req/sec).
 *  - Cache match results in-memory by `albumKey` to avoid hammering the API
 *    on repeated lookups within the session.
 *  - Send a polite, identifying User-Agent.
 */
interface RemoteArtSource {

    /**
     * Resolve album art bytes for the given album. May perform multiple
     * network round-trips (MusicBrainz release search + Cover Art Archive
     * fetch).
     *
     * @param artist primary artist (album artist if available, else track
     *   artist). Caller should NOT pass "Unknown Artist".
     * @param album album name. Caller should NOT pass "Unknown Album".
     * @return JPEG/PNG bytes for the front cover, or `null` when no match.
     */
    suspend fun resolve(artist: String, album: String): ByteArray?
}
