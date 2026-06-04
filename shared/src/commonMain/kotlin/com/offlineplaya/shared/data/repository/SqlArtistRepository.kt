package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.data.mapper.toDomain
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class SqlArtistRepository(
    private val db: OfflinePlayaDatabase,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ArtistRepository {

    private companion object {
        const val TAG = "SqlArtistRepository"
    }

    private val queries get() = db.artistQueries

    override fun observeAll(): Flow<List<Artist>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher).map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: Long): Artist? = withContext(ioDispatcher) {
        queries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun findByName(name: String): Artist? = withContext(ioDispatcher) {
        queries.selectByName(name).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsert(name: String): Long = withContext(ioDispatcher) {
        logger.d(TAG, "Upserting artist: $name")
        queries.transactionWithResult {
            queries.insert(name)
            val id = queries.selectByName(name).executeAsOne().id
            logger.d(TAG, "Upserted artist '$name' with ID: $id")
            id
        }
    }

    override suspend fun refreshCounts(id: Long) = withContext(ioDispatcher) {
        queries.updateCounts(id)
    }

    override suspend fun updateImageUrl(id: Long, imageUrl: String?) = withContext(ioDispatcher) {
        queries.updateImageUrl(imageUrl, id)
    }

    override suspend fun deleteOrphans() = withContext(ioDispatcher) {
        queries.deleteOrphans()
    }

    override suspend fun deleteAll() = withContext(ioDispatcher) {
        queries.deleteAll()
    }
}
