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

    /** Delete artists referenced by no track and no album (orphans after regrouping). */
    suspend fun deleteOrphans()

    suspend fun deleteAll()
}
