package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.FolderRepository
import com.offlineplaya.shared.domain.repository.ManagedTreeRootRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.domain.scanner.AudioFolder
import com.offlineplaya.shared.domain.scanner.AudioMetadata
import com.offlineplaya.shared.domain.scanner.FolderScanner
import com.offlineplaya.shared.domain.scanner.MetadataReader
import com.offlineplaya.shared.domain.scanner.RawAudioFile

/**
 * Phase 2 orchestrator. For each managed tree root:
 *  1. Walk it with [FolderScanner].
 *  2. Upsert every folder (parents first) so we have stable folder ids.
 *  3. Insert a pending Track row per audio file, attached to its leaf folder.
 *  4. Read metadata for each pending track via [MetadataReader].
 *  5. Upsert Artist + Album rows derived from the metadata.
 *  6. Update the Track with metadata and FKs (artist_id, album_id).
 *  7. Refresh aggregate counts on Artist / Album / Folder.
 *  8. Mark the managed root as scanned.
 *
 * Pure domain logic — both [scanner] and [metadataReader] are interfaces, so
 * this whole pipeline is testable in `commonMain` against fakes.
 */
class LibrarySyncUseCase(
    private val managedRoots: ManagedTreeRootRepository,
    private val folders: FolderRepository,
    private val artists: ArtistRepository,
    private val albums: AlbumRepository,
    private val tracks: TrackRepository,
    private val scanner: FolderScanner,
    private val metadataReader: MetadataReader,
) {

    /** Sync every managed tree root sequentially, accumulating the report. */
    suspend fun syncAll(): SyncReport {
        val roots = managedRoots.getAll()
        return roots.fold(SyncReport.Empty) { acc, root ->
            acc + syncOne(root.treeUri)
        }
    }

    /** Sync a single tree root by URI. Returns a per-root report. */
    suspend fun syncOne(treeUri: String): SyncReport {
        val scan = scanner.scan(treeUri)

        val folderIds = materializeFolders(scan.folders)
        val pendingTracks = insertPendingTracks(scan.files, folderIds)
        val (scanned, failed) = applyMetadataAndGrouping(pendingTracks)

        refreshAggregates(folderIds.values, pendingTracks.map { it.first })

        managedRoots.markScanned(treeUri)

        return SyncReport(
            foldersUpserted = folderIds.size,
            tracksDiscovered = scan.files.size,
            tracksScanned = scanned,
            tracksFailed = failed,
        )
    }

    // --- private helpers ---

    /**
     * Upsert folders in parent-before-child order, returning a map from
     * relativePath → folder id.
     */
    private suspend fun materializeFolders(
        scanned: List<AudioFolder>,
    ): Map<String, Long> {
        val ids = mutableMapOf<String, Long>()
        for (folder in scanned) {
            val parentId = folder.parentRelativePath?.let { ids[it] }
            val id = folders.upsert(
                treeUri = folder.treeUri,
                relativePath = folder.relativePath,
                displayName = folder.displayName,
                parentId = parentId,
            )
            ids[folder.relativePath] = id
        }
        return ids
    }

    /**
     * Insert one pending Track row per file. Returns pairs of (track id, file)
     * for downstream metadata work.
     */
    private suspend fun insertPendingTracks(
        files: List<RawAudioFile>,
        folderIds: Map<String, Long>,
    ): List<Pair<Long, RawAudioFile>> {
        return files.map { file ->
            val folderId = folderIds[file.parentRelativePath()]
            val id = tracks.insertFile(
                documentUri = file.documentUri,
                treeUri = file.treeUri,
                relativePath = file.relativePath,
                fileName = file.fileName,
                fileSize = file.fileSize,
                lastModified = file.lastModified,
                folderId = folderId,
            )
            id to file
        }
    }

    /**
     * For each pending track, read metadata, upsert Artist+Album, and update
     * the Track. Returns (scannedCount, failedCount).
     */
    private suspend fun applyMetadataAndGrouping(
        pending: List<Pair<Long, RawAudioFile>>,
    ): Pair<Int, Int> {
        var scanned = 0
        var failed = 0

        for ((trackId, file) in pending) {
            val metadata = metadataReader.read(file.documentUri)
            if (metadata == null) {
                tracks.markError(trackId)
                failed++
                continue
            }

            val artistId = upsertArtist(metadata)
            val albumId = upsertAlbum(metadata, artistId)

            val stored = tracks.findById(trackId)
                ?: continue // row disappeared between insert and update — skip silently
            tracks.updateMetadata(stored.applyMetadata(metadata))
            tracks.updateForeignKeys(trackId, artistId, albumId)
            scanned++
        }

        return scanned to failed
    }

    /** Upsert Artist, preferring albumArtist over track-level artist. */
    private suspend fun upsertArtist(metadata: AudioMetadata): Long? {
        val name = metadata.albumArtist?.takeIf { it.isNotBlank() }
            ?: metadata.artist?.takeIf { it.isNotBlank() }
            ?: return null
        return artists.upsert(name)
    }

    private suspend fun upsertAlbum(metadata: AudioMetadata, artistId: Long?): Long? {
        val name = metadata.album?.takeIf { it.isNotBlank() } ?: return null
        return albums.upsert(name, artistId, metadata.year)
    }

    /** Refresh aggregate counts on every folder/artist/album that may have changed. */
    private suspend fun refreshAggregates(
        folderIds: Collection<Long>,
        trackIds: List<Long>,
    ) {
        folderIds.forEach { folders.refreshTrackCount(it) }

        val touchedArtists = mutableSetOf<Long>()
        val touchedAlbums = mutableSetOf<Long>()
        for (id in trackIds) {
            val track = tracks.findById(id) ?: continue
            track.artistId?.let(touchedArtists::add)
            track.albumId?.let(touchedAlbums::add)
        }
        touchedAlbums.forEach { albums.refreshAggregates(it) }
        touchedArtists.forEach { artists.refreshCounts(it) }
    }
}

/**
 * Derive the parent directory portion of a `relative_path`. Returns `""` for
 * files at the tree root.
 */
internal fun RawAudioFile.parentRelativePath(): String {
    val idx = relativePath.lastIndexOf('/')
    return if (idx < 0) "" else relativePath.substring(0, idx)
}

/**
 * Produce an updated Track with metadata fields populated from [metadata].
 * Domain-level fallbacks (file name when title is null, etc.) are applied
 * downstream in the SQL mapper; here we only forward whatever the reader gave us.
 */
internal fun com.offlineplaya.shared.domain.model.Track.applyMetadata(
    metadata: AudioMetadata,
): com.offlineplaya.shared.domain.model.Track = copy(
    title = metadata.title?.takeIf { it.isNotBlank() } ?: fileName,
    artistName = metadata.artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
    albumArtistName = metadata.albumArtist?.takeIf { it.isNotBlank() },
    albumName = metadata.album?.takeIf { it.isNotBlank() } ?: "Unknown Album",
    genre = metadata.genre?.takeIf { it.isNotBlank() },
    year = metadata.year,
    trackNumber = metadata.trackNumber,
    discNumber = metadata.discNumber,
    durationMs = metadata.durationMs,
    bitrate = metadata.bitrate,
    sampleRate = metadata.sampleRate,
    channels = metadata.channels,
    codec = metadata.codec,
)
