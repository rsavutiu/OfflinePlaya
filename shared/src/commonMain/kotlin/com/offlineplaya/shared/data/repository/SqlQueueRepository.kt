package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.QueueRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class SqlQueueRepository(
    private val db: OfflinePlayaDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : QueueRepository {

    private val queries get() = db.queueItemQueries

    override fun observeQueue(): Flow<List<Track>> =
        queries.selectAll(::mapQueueRow).asFlow().mapToList(ioDispatcher)

    override suspend fun count(): Long = withContext(ioDispatcher) {
        queries.count().executeAsOne()
    }

    override suspend fun enqueue(trackId: Long, source: String) = withContext(ioDispatcher) {
        queries.transaction {
            val nextPosition = (queries.maxPosition().executeAsOne().maxPosition ?: -1L) + 1
            queries.insert(trackId, nextPosition, source)
        }
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        queries.clear()
    }

    @Suppress("LongParameterList", "UNUSED_PARAMETER")
    private fun mapQueueRow(
        queueItemId: Long,
        position: Long,
        source: String,
        trackId: Long,
        documentUri: String,
        treeUri: String,
        relativePath: String,
        fileName: String,
        fileSize: Long,
        lastModified: Long,
        title: String?,
        artistName: String?,
        albumArtistName: String?,
        albumName: String?,
        genre: String?,
        year: Long?,
        trackNumber: Long?,
        discNumber: Long?,
        durationMs: Long?,
        bitrate: Long?,
        sampleRate: Long?,
        channels: Long?,
        codec: String?,
        artistId: Long?,
        albumId: Long?,
        folderId: Long?,
        scanStatus: String,
        canonicalGenre: String?,
        createdAt: Long,
        updatedAt: Long,
    ): Track = Track(
        id = trackId,
        documentUri = documentUri,
        treeUri = treeUri,
        relativePath = relativePath,
        fileName = fileName,
        title = title?.takeIf { it.isNotBlank() } ?: fileName,
        artistName = artistName?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
        albumArtistName = albumArtistName?.takeIf { it.isNotBlank() },
        albumName = albumName?.takeIf { it.isNotBlank() } ?: "Unknown Album",
        genre = genre?.takeIf { it.isNotBlank() },
        year = year?.toInt(),
        trackNumber = trackNumber?.toInt(),
        discNumber = discNumber?.toInt(),
        durationMs = durationMs,
        bitrate = bitrate?.toInt(),
        sampleRate = sampleRate?.toInt(),
        channels = channels?.toInt(),
        codec = codec?.takeIf { it.isNotBlank() },
        artistId = artistId,
        albumId = albumId,
        folderId = folderId,
        scanStatus = ScanStatus.fromDbValue(scanStatus),
        canonicalGenre = canonicalGenre?.let { CanonicalGenre.fromDbValue(it) },
    )
}
