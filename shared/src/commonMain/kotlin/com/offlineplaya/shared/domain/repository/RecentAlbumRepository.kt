package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.Album
import kotlinx.coroutines.flow.Flow

/**
 * Persisted "recently used" album list backing the home page's cover-fan
 * shelf. Empty on first launch — fills up as the user plays tracks.
 *
 * Order: most-recently-used first. Re-recording an existing album moves
 * it to the top. The home grid's cover fans show the first few entries
 * and never change during a background scan, since this list only
 * mutates on a user play action.
 */
interface RecentAlbumRepository {

    /**
     * Mark [albumId] as just-used. Use this from the playback path — any
     * caller that's about to start playing tracks from a specific album
     * should record it here. Null-safe at the callers; this function
     * assumes a real album_id (foreign-key constraint enforced).
     *
     * @param now wall-clock time of the use, in milliseconds since
     * epoch. Caller-provided so unit tests can pin specific orderings.
     */
    suspend fun recordUse(albumId: Long, now: Long)

    /**
     * Hot stream of the [limit] most-recently-used albums, newest first.
     * Joins through to Album rows so callers receive domain models
     * directly.
     */
    fun observeRecent(limit: Int): Flow<List<Album>>

    /** Wipe the recency history. Exposed for a future "clear" Settings action. */
    suspend fun clearAll()
}
