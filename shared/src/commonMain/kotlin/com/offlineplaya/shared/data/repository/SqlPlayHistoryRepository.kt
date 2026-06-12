package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.data.mapper.toDomain
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.PlayHistoryRepository
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class SqlPlayHistoryRepository(
    private val db: OfflinePlayaDatabase,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PlayHistoryRepository {

    private companion object {
        const val TAG = "SqlPlayHistoryRepository"
    }

    private val queries get() = db.playHistoryQueries

    override suspend fun recordPlay(trackId: Long, playedAtMs: Long) =
        withContext(ioDispatcher) {
            logger.d(TAG, "Recording play of track $trackId")
            queries.recordPlay(trackId, playedAtMs)
        }

    override suspend fun countForTrack(trackId: Long): Long = withContext(ioDispatcher) {
        queries.countForTrack(trackId).executeAsOne()
    }

    override fun observeMostPlayed(limit: Int): Flow<List<Track>> =
        queries.selectMostPlayed(limit.toLong()).asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<Track>> =
        queries.selectRecentlyPlayed(limit.toLong()).asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeNeverPlayed(limit: Int): Flow<List<Track>> =
        queries.selectNeverPlayed(limit.toLong()).asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeForgottenFavorites(
        minPlays: Int,
        cutoffMs: Long,
        limit: Int,
    ): Flow<List<Track>> =
        queries.selectForgottenFavorites(minPlays.toLong(), cutoffMs, limit.toLong())
            .asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }
}
