package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    fun observeAll(): Flow<List<Track>>
    fun observeByAlbum(albumId: Long): Flow<List<Track>>
    fun observeByArtist(artistId: Long): Flow<List<Track>>
    fun observeByFolder(folderId: Long): Flow<List<Track>>

    suspend fun count(): Long
    suspend fun countByStatus(status: ScanStatus): Long
    suspend fun findById(id: Long): Track?
    suspend fun findByDocumentUri(uri: String): Track?
    suspend fun findPending(limit: Int): List<Track>

    suspend fun insertFile(
        documentUri: String,
        treeUri: String,
        relativePath: String,
        fileName: String,
        fileSize: Long,
        lastModified: Long,
        folderId: Long?,
    ): Long

    suspend fun updateMetadata(track: Track)
    suspend fun updateForeignKeys(id: Long, artistId: Long?, albumId: Long?)
    suspend fun markError(id: Long)
    suspend fun deleteAll()
}
