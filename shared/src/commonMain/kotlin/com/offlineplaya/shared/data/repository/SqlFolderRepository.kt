package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.data.mapper.toDomain
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.repository.FolderRepository
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class SqlFolderRepository(
    private val db: OfflinePlayaDatabase,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FolderRepository {

    private companion object {
        const val TAG = "SqlFolderRepository"
    }

    private val queries get() = db.folderQueries

    override fun observeRoots(): Flow<List<Folder>> =
        queries.selectRoots().asFlow().mapToList(ioDispatcher).map { rows -> rows.map { it.toDomain() } }

    override fun observeChildren(parentId: Long): Flow<List<Folder>> =
        queries.selectChildren(parentId).asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: Long): Folder? = withContext(ioDispatcher) {
        queries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun findByPath(treeUri: String, relativePath: String): Folder? =
        withContext(ioDispatcher) {
            queries.selectByPath(treeUri, relativePath).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun upsert(
        treeUri: String,
        relativePath: String,
        displayName: String,
        parentId: Long?,
    ): Long = withContext(ioDispatcher) {
        logger.d(TAG, "Upserting folder: $relativePath in $treeUri")
        queries.transactionWithResult {
            queries.insert(treeUri, relativePath, displayName, parentId)
            val id = queries.selectByPath(treeUri, relativePath).executeAsOne().id
            logger.d(TAG, "Upserted folder '$relativePath' with ID: $id")
            id
        }
    }

    override suspend fun refreshTrackCount(id: Long) = withContext(ioDispatcher) {
        queries.updateTrackCount(id)
    }

    override suspend fun deleteByTreeUri(treeUri: String) = withContext(ioDispatcher) {
        queries.deleteByTreeUri(treeUri)
    }

    override suspend fun deleteAll() = withContext(ioDispatcher) {
        queries.deleteAll()
    }
}
