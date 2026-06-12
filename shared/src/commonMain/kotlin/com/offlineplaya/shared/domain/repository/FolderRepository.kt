package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun observeRoots(): Flow<List<Folder>>
    fun observeChildren(parentId: Long): Flow<List<Folder>>
    suspend fun findById(id: Long): Folder?
    suspend fun findByPath(treeUri: String, relativePath: String): Folder?
    suspend fun upsert(treeUri: String, relativePath: String, displayName: String, parentId: Long?): Long
    suspend fun refreshTrackCount(id: Long)
    suspend fun deleteByTreeUri(treeUri: String)

    /** Drop a folder subtree (the folder + every descendant). */
    suspend fun deleteByPathPrefix(treeUri: String, pathPrefix: String)

    suspend fun deleteAll()
}
