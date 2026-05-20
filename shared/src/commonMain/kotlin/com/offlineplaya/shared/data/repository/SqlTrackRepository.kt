package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.data.mapper.toDomain
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.TrackRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class SqlTrackRepository(
    private val db: OfflinePlayaDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TrackRepository {

    private val queries get() = db.trackQueries

    override fun observeAll(): Flow<List<Track>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher).map { rows -> rows.map { it.toDomain() } }

    override fun observeByAlbum(albumId: Long): Flow<List<Track>> =
        queries.selectByAlbum(albumId).asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeByArtist(artistId: Long): Flow<List<Track>> =
        queries.selectByArtist(artistId).asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeByFolder(folderId: Long): Flow<List<Track>> =
        queries.selectByFolder(folderId).asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun count(): Long = withContext(ioDispatcher) {
        queries.countAll().executeAsOne()
    }

    override suspend fun countByStatus(status: ScanStatus): Long = withContext(ioDispatcher) {
        queries.countByStatus(status.dbValue).executeAsOne()
    }

    override suspend fun findById(id: Long): Track? = withContext(ioDispatcher) {
        queries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun findByDocumentUri(uri: String): Track? = withContext(ioDispatcher) {
        queries.selectByDocumentUri(uri).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun findFirstByAlbum(albumId: Long): Track? = withContext(ioDispatcher) {
        // selectByAlbum already sorts by disc + track + title, so the first
        // row is the one users would think of as "track 1".
        queries.selectByAlbum(albumId).executeAsList().firstOrNull()?.toDomain()
    }

    override suspend fun findPending(limit: Int): List<Track> = withContext(ioDispatcher) {
        queries.selectPending(limit.toLong()).executeAsList().map { it.toDomain() }
    }

    override suspend fun search(query: String, limit: Int): List<Track> = withContext(ioDispatcher) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val pattern = "%$trimmed%"
        queries.search(pattern, pattern, pattern, limit.toLong())
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun insertFile(
        documentUri: String,
        treeUri: String,
        relativePath: String,
        fileName: String,
        fileSize: Long,
        lastModified: Long,
        folderId: Long?,
    ): Long = withContext(ioDispatcher) {
        queries.transactionWithResult {
            queries.insertFile(documentUri, treeUri, relativePath, fileName, fileSize, lastModified, folderId)
            queries.lastInsertId().executeAsOne()
        }
    }

    override suspend fun updateMetadata(track: Track) = withContext(ioDispatcher) {
        queries.updateMetadata(
            title = track.title,
            artist_name = track.artistName,
            album_artist_name = track.albumArtistName,
            album_name = track.albumName,
            genre = track.genre,
            year = track.year?.toLong(),
            track_number = track.trackNumber?.toLong(),
            disc_number = track.discNumber?.toLong(),
            duration_ms = track.durationMs,
            bitrate = track.bitrate?.toLong(),
            sample_rate = track.sampleRate?.toLong(),
            channels = track.channels?.toLong(),
            codec = track.codec,
            id = track.id,
        )
    }

    override suspend fun updateForeignKeys(id: Long, artistId: Long?, albumId: Long?) =
        withContext(ioDispatcher) {
            queries.updateForeignKeys(artist_id = artistId, album_id = albumId, id = id)
        }

    override suspend fun markError(id: Long) = withContext(ioDispatcher) {
        queries.markError(id)
    }

    override suspend fun deleteAll() = withContext(ioDispatcher) {
        queries.deleteAll()
    }
}
