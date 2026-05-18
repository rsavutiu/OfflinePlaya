package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.Album
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    fun observeAll(): Flow<List<Album>>
    fun observeByArtist(artistId: Long): Flow<List<Album>>
    suspend fun findById(id: Long): Album?
    suspend fun findByNameAndArtist(name: String, artistId: Long?): Album?
    suspend fun upsert(name: String, artistId: Long?, year: Int?): Long
    suspend fun refreshAggregates(id: Long)
    suspend fun deleteAll()
}
