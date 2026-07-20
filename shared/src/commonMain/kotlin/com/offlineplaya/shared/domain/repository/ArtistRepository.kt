package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.Artist
import kotlinx.coroutines.flow.Flow

interface ArtistRepository {
    fun observeAll(): Flow<List<Artist>>
    suspend fun findById(id: Long): Artist?
    suspend fun findByName(name: String): Artist?
    suspend fun upsert(name: String): Long
    suspend fun refreshCounts(id: Long)
    suspend fun updateImageUrl(id: Long, imageUrl: String?)

    /**
     * Recase the artist row to the majority casing among its tracks' raw
     * artist tags (ties prefer the uppercase-leaning variant). The row's
     * original casing is whatever file happened to be scanned first — an
     * arbitrary freeze this undoes. Returns the canonical name (renamed or
     * already-correct), or null when the artist has no matching tags.
     */
    suspend fun canonicalizeCasing(id: Long): String?

    /** Delete artists referenced by no track and no album (orphans after regrouping). */
    suspend fun deleteOrphans()

    suspend fun deleteAll()
}
