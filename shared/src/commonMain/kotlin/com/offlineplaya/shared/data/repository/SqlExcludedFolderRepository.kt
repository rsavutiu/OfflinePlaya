package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.ExcludedFolder
import com.offlineplaya.shared.domain.repository.ExcludedFolderRepository
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class SqlExcludedFolderRepository(
    private val db: OfflinePlayaDatabase,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ExcludedFolderRepository {

    private companion object {
        const val TAG = "SqlExcludedFolderRepository"
    }

    private val queries get() = db.excludedFolderQueries

    override fun observeAll(): Flow<List<ExcludedFolder>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAll(): List<ExcludedFolder> = withContext(ioDispatcher) {
        queries.selectAll().executeAsList().map { it.toDomain() }
    }

    override suspend fun add(treeUri: String, relativePath: String, displayName: String) =
        withContext(ioDispatcher) {
            logger.i(TAG, "Excluding folder '$relativePath' in $treeUri")
            queries.insert(treeUri, relativePath, displayName)
        }

    override suspend fun remove(id: Long) = withContext(ioDispatcher) {
        logger.i(TAG, "Removing exclusion $id")
        queries.deleteById(id)
    }

    private fun com.offlineplaya.shared.database.ExcludedFolder.toDomain() =
        ExcludedFolder(
            id = id,
            treeUri = tree_uri,
            relativePath = relative_path,
            displayName = display_name,
        )
}
