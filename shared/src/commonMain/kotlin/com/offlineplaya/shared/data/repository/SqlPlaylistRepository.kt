package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.data.mapper.toDomain
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.PlaylistRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class SqlPlaylistRepository(
    private val db: OfflinePlayaDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PlaylistRepository {

    private val playlistQueries get() = db.playlistQueries
    private val playlistTrackQueries get() = db.playlistTrackQueries

    override fun observeAll(): Flow<List<Playlist>> =
        playlistQueries.selectAll().asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeTracks(playlistId: Long): Flow<List<Track>> =
        playlistTrackQueries.selectByPlaylist(playlistId, ::mapPlaylistTrackRow)
            .asFlow()
            .mapToList(ioDispatcher)

    override suspend fun findById(id: Long): Playlist? = withContext(ioDispatcher) {
        playlistQueries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun create(name: String): Long = withContext(ioDispatcher) {
        playlistQueries.transactionWithResult {
            playlistQueries.insert(name)
            playlistQueries.lastInsertId().executeAsOne()
        }
    }

    override suspend fun rename(id: Long, name: String) = withContext(ioDispatcher) {
        playlistQueries.rename(name, id)
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        playlistQueries.deleteById(id)
    }

    override suspend fun addTrack(playlistId: Long, trackId: Long) = withContext(ioDispatcher) {
        playlistTrackQueries.transaction {
            val nextPosition = (playlistTrackQueries.maxPosition(playlistId).executeAsOne().maxPosition ?: -1L) + 1
            playlistTrackQueries.insert(playlistId, trackId, nextPosition)
        }
    }

    override suspend fun removeEntry(playlistTrackId: Long) = withContext(ioDispatcher) {
        playlistTrackQueries.deleteById(playlistTrackId)
    }

    override suspend fun reorder(playlistTrackId: Long, newPosition: Int) = withContext(ioDispatcher) {
        playlistTrackQueries.updatePosition(newPosition.toLong(), playlistTrackId)
    }

    @Suppress("LongParameterList", "UNUSED_PARAMETER")
    private fun mapPlaylistTrackRow(
        playlistTrackId: Long,
        position: Long,
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
