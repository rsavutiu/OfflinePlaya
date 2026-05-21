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
     * Resolve album art bytes for the given album.
     *
     * @param artist primary artist.
     * @param album album name.
     * @return JPEG/PNG bytes for the front cover, or `null` when no match.
     */
    suspend fun resolve(artist: String, album: String): ByteArray?

    /**
     * Resolve an artist image URL.
     *
     * @param artist artist name.
     * @return URL string for the artist's portrait, or `null` when no match.
     */
    suspend fun resolveArtistImage(artist: String): String?
}
