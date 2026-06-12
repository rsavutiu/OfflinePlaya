package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.offlineplaya.shared.data.mapper.UNKNOWN_ALBUM
import com.offlineplaya.shared.data.mapper.UNKNOWN_ARTIST
import com.offlineplaya.shared.data.mapper.toDomain
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.database.SelectTopPlayed
import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.domain.model.ListeningStats
import com.offlineplaya.shared.domain.model.PlayedTrack
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.TopAlbumStat
import com.offlineplaya.shared.domain.model.TopArtistStat
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

    override fun observeTopPlayed(
        artistId: Long?,
        sinceMs: Long,
        limit: Int,
    ): Flow<List<PlayedTrack>> =
        queries.selectTopPlayed(artistId, sinceMs, limit.toLong())
            .asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toPlayedTrack() } }

    override fun observeStats(sinceMs: Long): Flow<ListeningStats> =
        queries.selectStatsTotals(sinceMs)
            .asFlow().mapToOne(ioDispatcher)
            .map { row ->
                ListeningStats(
                    plays = row.plays,
                    distinctTracks = row.distinct_tracks,
                    listenedMs = row.listened_ms ?: 0L,
                )
            }

    override fun observeTopArtists(sinceMs: Long, limit: Int): Flow<List<TopArtistStat>> =
        queries.selectTopArtists(sinceMs, limit.toLong())
            .asFlow().mapToList(ioDispatcher)
            .map { rows ->
                rows.mapNotNull { row ->
                    TopArtistStat(
                        artistId = row.artist_id ?: return@mapNotNull null,
                        name = row.artist_name?.takeIf { it.isNotBlank() } ?: UNKNOWN_ARTIST,
                        plays = row.plays,
                    )
                }
            }

    override fun observeTopAlbums(sinceMs: Long, limit: Int): Flow<List<TopAlbumStat>> =
        queries.selectTopAlbums(sinceMs, limit.toLong())
            .asFlow().mapToList(ioDispatcher)
            .map { rows ->
                rows.mapNotNull { row ->
                    TopAlbumStat(
                        albumId = row.album_id ?: return@mapNotNull null,
                        name = row.album_name?.takeIf { it.isNotBlank() } ?: UNKNOWN_ALBUM,
                        artistName = row.artist_name?.takeIf { it.isNotBlank() },
                        plays = row.plays,
                    )
                }
            }
}

/**
 * The `Track.*, play_count` projection mirrors [toDomain]'s defaults so a
 * top-played row renders identically to the same track anywhere else.
 */
private fun SelectTopPlayed.toPlayedTrack(): PlayedTrack = PlayedTrack(
    track = Track(
        id = id,
        documentUri = document_uri,
        treeUri = tree_uri,
        relativePath = relative_path,
        fileName = file_name,
        title = title?.takeIf { it.isNotBlank() } ?: file_name,
        artistName = artist_name?.takeIf { it.isNotBlank() } ?: UNKNOWN_ARTIST,
        albumArtistName = album_artist_name?.takeIf { it.isNotBlank() },
        albumName = album_name?.takeIf { it.isNotBlank() } ?: UNKNOWN_ALBUM,
        genre = genre?.takeIf { it.isNotBlank() },
        year = year?.toInt(),
        trackNumber = track_number?.toInt(),
        discNumber = disc_number?.toInt(),
        durationMs = duration_ms,
        bitrate = bitrate?.toInt(),
        sampleRate = sample_rate?.toInt(),
        channels = channels?.toInt(),
        codec = codec?.takeIf { it.isNotBlank() },
        artistId = artist_id,
        albumId = album_id,
        folderId = folder_id,
        scanStatus = ScanStatus.fromDbValue(scan_status),
        canonicalGenre = canonical_genre?.let { CanonicalGenre.fromDbValue(it) },
    ),
    playCount = play_count,
)
