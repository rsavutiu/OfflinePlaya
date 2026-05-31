package com.offlineplaya.shared.domain.genre

/**
 * Looks up a genre tag for an (artist, album) pair over the network. Returns
 * `null` when no confident match is found — callers leave the track unlabeled
 * and EQ Auto falls back to the flat preset.
 *
 * Implementations should mirror [com.offlineplaya.shared.domain.image.RemoteArtSource]
 * for politeness:
 *  - Respect upstream rate limits (MusicBrainz allows ~1 req/sec).
 *  - Cache match results in-memory by `(artist, album)` so a single lookup
 *    covers every track on the album within the session.
 *  - Send a polite, identifying User-Agent.
 *
 * The unit of work is intentionally **per album**, not per track. A
 * 12-track album costs one MusicBrainz round-trip, and the use case fans
 * the result out across the album's tracks in-process.
 */
interface RemoteGenreSource {

    /**
     * Resolve a freeform genre string (e.g. "Electronic", "Indie Rock") for
     * the given album. Callers feed the result through
     * [com.offlineplaya.shared.domain.usecase.GenreClassifier] to bucket it
     * into a [com.offlineplaya.shared.domain.model.CanonicalGenre].
     */
    suspend fun resolveGenre(artist: String, album: String): String?
}
