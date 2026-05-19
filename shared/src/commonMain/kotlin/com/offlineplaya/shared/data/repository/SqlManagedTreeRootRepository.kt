package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.data.mapper.toDomain
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.ManagedTreeRoot
import com.offlineplaya.shared.domain.repository.ManagedTreeRootRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class SqlManagedTreeRootRepository(
    private val db: OfflinePlayaDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ManagedTreeRootRepository {

    private val queries get() = db.managedTreeRootQueries

    override fun observeAll(): Flow<List<ManagedTreeRoot>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAll(): List<ManagedTreeRoot> = withContext(ioDispatcher) {
        queries.selectAll().executeAsList().map { it.toDomain() }
    }

    override suspend fun findByUri(treeUri: String): ManagedTreeRoot? = withContext(ioDispatcher) {
        queries.selectByUri(treeUri).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun add(treeUri: String, displayName: String) = withContext(ioDispatcher) {
        queries.insert(treeUri, displayName)
    }

    override suspend fun markScanned(treeUri: String) = withContext(ioDispatcher) {
        queries.markScanned(treeUri)
    }

    override suspend fun remove(treeUri: String) = withContext(ioDispatcher) {
        queries.deleteByUri(treeUri)
    }
}
