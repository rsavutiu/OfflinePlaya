package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.offlineplaya.shared.data.mapper.toDomain
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.TrackAggregateRef
import com.offlineplaya.shared.domain.repository.TrackGenreRow
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class SqlTrackRepository(
    private val db: OfflinePlayaDatabase,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TrackRepository {

    private companion object {
        const val TAG = "SqlTrackRepository"
    }

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

    override fun observeCount(): Flow<Long> =
        queries.countAll().asFlow().mapToOne(ioDispatcher)

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
        logger.d(TAG, "Inserting file: $documentUri (tree: $treeUri, path: $relativePath)")
        queries.transactionWithResult {
            queries.insertFile(documentUri, treeUri, relativePath, fileName, fileSize, lastModified, folderId)
            val id = queries.selectIdByDocumentUri(documentUri).executeAsOne()
            logger.d(TAG, "Inserted/Found file $documentUri with ID: $id")
            id
        }
    }

    override suspend fun updateMetadata(track: Track) = withContext(ioDispatcher) {
        logger.d(TAG, "Updating metadata for track ID ${track.id}: ${track.title}")
        queries.updateMetadata(
            title = track.title,
            artist_name = track.artistName,
            album_artist_name = track.albumArtistName,
            album_name = track.albumName,
            genre = track.genre,
            canonical_genre = track.canonicalGenre?.name,
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

    override suspend fun selectMissingCanonicalGenre(limit: Int): List<TrackGenreRow> =
        withContext(ioDispatcher) {
            queries.selectMissingCanonicalGenre(limit.toLong()).executeAsList().map { row ->
                TrackGenreRow(row.id, row.genre)
            }
        }

    override suspend fun setCanonicalGenre(id: Long, genre: CanonicalGenre) =
        withContext(ioDispatcher) {
            queries.setCanonicalGenre(genre.name, id)
        }

    override suspend fun setRawAndCanonicalGenre(
        id: Long,
        rawGenre: String,
        canonical: CanonicalGenre,
    ) = withContext(ioDispatcher) {
        queries.setRawAndCanonicalGenre(genre = rawGenre, canonical_genre = canonical.name, id = id)
    }

    override suspend fun updateForeignKeys(id: Long, artistId: Long?, albumId: Long?) =
        withContext(ioDispatcher) {
            queries.updateForeignKeys(artist_id = artistId, album_id = albumId, id = id)
        }

    override suspend fun markError(id: Long) = withContext(ioDispatcher) {
        queries.markError(id)
    }

    override suspend fun findByContentKeyExcludingTree(
        fileName: String,
        fileSize: Long,
        lastModified: Long,
        excludeTreeUri: String,
    ): Track? = withContext(ioDispatcher) {
        queries.selectByContentKeyExcludingTree(fileName, fileSize, lastModified, excludeTreeUri)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun findIdsByContentKeyInTree(
        fileName: String,
        fileSize: Long,
        lastModified: Long,
        inTreeUri: String,
    ): List<Long> = withContext(ioDispatcher) {
        queries.selectIdsByContentKeyInTree(fileName, fileSize, lastModified, inTreeUri)
            .executeAsList()
    }

    override suspend fun deleteById(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }

    override suspend fun selectAggregatesByTreeUri(treeUri: String): List<TrackAggregateRef> =
        withContext(ioDispatcher) {
            queries.selectIdsByTreeUri(treeUri).executeAsList().map { row ->
                TrackAggregateRef(row.id, row.artist_id, row.album_id)
            }
        }

    override suspend fun deleteByTreeUri(treeUri: String) = withContext(ioDispatcher) {
        queries.deleteByTreeUri(treeUri)
    }

    override suspend fun deleteAll() = withContext(ioDispatcher) {
        queries.deleteAll()
    }
}
