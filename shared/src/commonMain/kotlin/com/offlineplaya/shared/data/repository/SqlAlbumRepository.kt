package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.data.mapper.toDomain
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class SqlAlbumRepository(
    private val db: OfflinePlayaDatabase,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AlbumRepository {

    private companion object {
        const val TAG = "SqlAlbumRepository"
    }

    private val queries get() = db.albumQueries

    override fun observeAll(): Flow<List<Album>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher).map { rows -> rows.map { it.toDomain() } }

    override fun observeByArtist(artistId: Long): Flow<List<Album>> =
        queries.selectByArtist(artistId).asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: Long): Album? = withContext(ioDispatcher) {
        queries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun findByNameAndArtist(name: String, artistId: Long?): Album? =
        withContext(ioDispatcher) {
            queries.selectByNameAndArtist(name, artistId).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun upsert(name: String, artistId: Long?, year: Int?): Long =
        withContext(ioDispatcher) {
            logger.d(TAG, "Upserting album: $name (artistId: $artistId)")
            queries.transactionWithResult {
                queries.insert(name, artistId, year?.toLong())
                val id = queries.selectByNameAndArtist(name, artistId).executeAsOne().id
                logger.d(TAG, "Upserted album '$name' with ID: $id")
                id
            }
        }

    override suspend fun refreshAggregates(id: Long) = withContext(ioDispatcher) {
        queries.updateAggregates(id)
    }

    override suspend fun deleteEmpty() = withContext(ioDispatcher) {
        queries.deleteEmpty()
    }

    override suspend fun deleteAll() = withContext(ioDispatcher) {
        queries.deleteAll()
    }
}
