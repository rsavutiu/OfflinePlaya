package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.repository.RecentAlbumRepository
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight-backed [RecentAlbumRepository] over the `RecentAlbum` table
 * (see `RecentAlbum.sq`). The write path is a single upsert; the read
 * path is a join with `Album` that the query handles for us, so the
 * mapper here is trivial.
 */
internal class SqlRecentAlbumRepository(
    private val db: OfflinePlayaDatabase,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : RecentAlbumRepository {

    private companion object {
        const val TAG = "SqlRecentAlbumRepository"
    }

    private val queries get() = db.recentAlbumQueries

    override suspend fun recordUse(albumId: Long, now: Long) = withContext(ioDispatcher) {
        logger.d(TAG, "recordUse(albumId=$albumId, now=$now)")
        queries.recordUse(albumId, now)
    }

    override fun observeRecent(limit: Int): Flow<List<Album>> =
        queries.selectRecentAlbums(limit.toLong())
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows ->
                rows.map { row ->
                    Album(
                        id = row.id,
                        name = row.name,
                        artistId = row.artist_id,
                        year = row.year?.toInt(),
                        trackCount = row.track_count.toInt(),
                        durationMs = row.duration_ms,
                    )
                }
            }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        logger.d(TAG, "clearAll()")
        queries.clearAll()
    }
}
