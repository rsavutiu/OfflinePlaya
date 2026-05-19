package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.ManagedTreeRoot
import kotlinx.coroutines.flow.Flow

interface ManagedTreeRootRepository {
    fun observeAll(): Flow<List<ManagedTreeRoot>>

    /** One-shot read of all managed roots — used by sync jobs that don't want a Flow. */
    suspend fun getAll(): List<ManagedTreeRoot>

    suspend fun findByUri(treeUri: String): ManagedTreeRoot?
    suspend fun add(treeUri: String, displayName: String)
    suspend fun markScanned(treeUri: String)
    suspend fun remove(treeUri: String)
}
