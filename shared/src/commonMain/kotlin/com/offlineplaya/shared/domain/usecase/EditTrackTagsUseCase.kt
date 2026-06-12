package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.model.TrackTagEdits
import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.domain.tag.TrackTagWriter
import com.offlineplaya.shared.util.AppLogger

/**
 * Applies user tag edits to a single track: writes the tags into the file,
 * then brings the database in line — updates the denormalized Track row,
 * re-resolves the Artist/Album it belongs to, refreshes aggregate counts on
 * both the old and new groups, and drops any rows orphaned by the move.
 *
 * Mirrors the grouping rule the scanner uses ([com.offlineplaya.shared.domain.usecase.LibrarySyncUseCase]):
 * the Artists list is keyed by **album-artist** when present, else track artist.
 * Editing one track of a compilation therefore (intentionally) splits it out
 * under its own artist — the user is taking explicit control.
 *
 * If the file write fails (e.g. a non-writable MediaStore URI) the database is
 * left untouched and the failure is returned.
 */
class EditTrackTagsUseCase(
    private val tracks: TrackRepository,
    private val artists: ArtistRepository,
    private val albums: AlbumRepository,
    private val tagWriter: TrackTagWriter,
    private val logger: AppLogger,
) {

    suspend fun edit(track: Track, edits: TrackTagEdits): Result<Unit> {
        val write = tagWriter.write(track.documentUri, edits)
        val stats = write.getOrElse {
            logger.w(TAG, "Tag write failed for ${track.documentUri}; DB left unchanged")
            return Result.failure(it)
        }

        // The rewrite changed the file's size/mtime on disk. Refresh the
        // stored fingerprint or the SAF-vs-device content-key dedup stops
        // matching and the next resync re-inserts this file as a duplicate.
        val newSize = stats.fileSize
        val newModified = stats.lastModified
        if (newSize != null && newModified != null) {
            tracks.updateContentStats(track.id, newSize, newModified)
        }

        // Denormalized row, applying the same fallbacks the scanner uses.
        val newArtistName = edits.artist?.takeIf { it.isNotBlank() } ?: UNKNOWN_ARTIST
        val newAlbumName = edits.album?.takeIf { it.isNotBlank() } ?: UNKNOWN_ALBUM
        val newAlbumArtist = edits.albumArtist?.takeIf { it.isNotBlank() }
        val newGenre = edits.genre?.takeIf { it.isNotBlank() }

        val updated = track.copy(
            title = edits.title?.takeIf { it.isNotBlank() } ?: track.fileName,
            artistName = newArtistName,
            albumArtistName = newAlbumArtist,
            albumName = newAlbumName,
            genre = newGenre,
            canonicalGenre = newGenre?.let { GenreClassifier.classify(it) },
            year = edits.year,
            trackNumber = edits.trackNumber,
            discNumber = edits.discNumber,
        )
        tracks.updateMetadata(updated)

        // Regroup: Artists list keys on album-artist when present, else artist.
        val oldArtistId = track.artistId
        val oldAlbumId = track.albumId
        val groupingArtist = newAlbumArtist ?: newArtistName
        val newArtistId = artists.upsert(groupingArtist)
        val newAlbumId = albums.upsert(newAlbumName, newArtistId, edits.year)
        tracks.updateForeignKeys(track.id, newArtistId, newAlbumId)

        // Refresh both ends, then clean up rows left empty by the move.
        artists.refreshCounts(newArtistId)
        albums.refreshAggregates(newAlbumId)
        if (oldArtistId != null && oldArtistId != newArtistId) artists.refreshCounts(oldArtistId)
        if (oldAlbumId != null && oldAlbumId != newAlbumId) albums.refreshAggregates(oldAlbumId)
        albums.deleteEmpty()
        artists.deleteOrphans()

        logger.i(TAG, "Applied tag edits to track ${track.id} → artist=$newArtistId album=$newAlbumId")
        return Result.success(Unit)
    }

    private companion object {
        const val TAG = "EditTrackTagsUseCase"
        const val UNKNOWN_ARTIST = "Unknown Artist"
        const val UNKNOWN_ALBUM = "Unknown Album"
    }
}
