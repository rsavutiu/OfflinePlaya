package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.ExcludedFolder
import kotlinx.coroutines.flow.Flow

interface ExcludedFolderRepository {
    fun observeAll(): Flow<List<ExcludedFolder>>
    suspend fun getAll(): List<ExcludedFolder>
    suspend fun add(treeUri: String, relativePath: String, displayName: String)
    suspend fun remove(id: Long)
}
