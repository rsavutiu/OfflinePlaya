package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.FolderRepository
import com.offlineplaya.shared.domain.repository.ManagedTreeRootRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.domain.scanner.AudioFolder
import com.offlineplaya.shared.domain.scanner.AudioMetadata
import com.offlineplaya.shared.domain.scanner.DeviceAudioScanner
import com.offlineplaya.shared.domain.scanner.DeviceAudioTrack
import com.offlineplaya.shared.domain.scanner.FolderScanner
import com.offlineplaya.shared.domain.scanner.MetadataReader
import com.offlineplaya.shared.domain.scanner.RawAudioFile
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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
    private val deviceAudio: DeviceAudioScanner,
    private val logger: AppLogger,
) {
    private companion object {
        const val TAG = "LibrarySyncUseCase"
        /**
         * Cap on simultaneous metadata reads. `MediaMetadataRetriever` has
         * some internal global locking but 8 parallel reads measure ~6× faster
         * than serial on mid-range devices and avoid OOM on cheap ones.
         */
        const val METADATA_READ_CONCURRENCY = 8

        /**
         * Cap on rows pulled per backfill pass. Keeps memory bounded on
         * libraries with tens of thousands of pre-existing tracks; the loop
         * just repeats until the unclassified set is exhausted.
         */
        const val BACKFILL_CHUNK_SIZE = 500
    }

    /**
     * Sync every managed tree root sequentially, accumulating the report,
     * then also pull in everything the platform's audio index knows about
     * via [DeviceAudioScanner]. The device-audio pass is what catches
     * music in Downloads / the internal-storage root / other locations
     * Android refuses to let users grant SAF tree access to.
     */
    suspend fun syncAll(): SyncReport {
        logger.i(TAG, "Starting syncAll")
        val roots = managedRoots.getAll()
        logger.d(TAG, "Found ${roots.size} managed roots")
        val safReport = roots.fold(SyncReport.Empty) { acc, root ->
            acc + syncOne(root.treeUri)
        }
        val deviceReport = syncDeviceAudio()
        // After every sync pass, top up canonical_genre on any rows still
        // missing it — covers pre-EQ-feature tracks that were scanned by an
        // older app version and never classified. Runs in chunks so a huge
        // legacy library doesn't block the sync's reported "Completed" state.
        backfillCanonicalGenre()
        val totalReport = safReport + deviceReport
        logger.i(TAG, "Completed syncAll: $totalReport")
        return totalReport
    }

    /**
     * Re-classify legacy rows whose `canonical_genre` is null. Pulls in chunks
     * of [BACKFILL_CHUNK_SIZE] rows; loops until the table is exhausted. Pure
     * SQL work — no file I/O — so even thousands of rows finish in well under
     * a second on cold cache.
     */
    private suspend fun backfillCanonicalGenre() {
        var totalUpdated = 0
        while (true) {
            val chunk = tracks.selectMissingCanonicalGenre(BACKFILL_CHUNK_SIZE)
            if (chunk.isEmpty()) break
            for (row in chunk) {
                tracks.setCanonicalGenre(row.trackId, GenreClassifier.classify(row.rawGenre))
            }
            totalUpdated += chunk.size
            if (chunk.size < BACKFILL_CHUNK_SIZE) break
        }
        if (totalUpdated > 0) {
            logger.i(TAG, "Backfilled canonical_genre on $totalUpdated track(s)")
        }
    }

    /** Sync a single tree root by URI. Returns a per-root report. */
    suspend fun syncOne(treeUri: String): SyncReport {
        logger.i(TAG, "Syncing root: $treeUri")
        return try {
            val scan = scanner.scan(treeUri)
            logger.d(TAG, "Scanned $treeUri: ${scan.folders.size} folders, ${scan.files.size} files")

            val folderIds = materializeFolders(scan.folders)
            val pendingTracks = insertPendingTracks(scan.files, folderIds)
            // After inserting SAF tracks, promote them over any device-audio
            // row representing the same physical file. Otherwise a file picked
            // up by both scanners would show as a duplicate in the library.
            demoteDeviceDuplicates(pendingTracks)
            val (scanned, failed) = applyMetadataAndGrouping(pendingTracks)

            refreshAggregates(folderIds.values, pendingTracks.map { it.first })

            managedRoots.markScanned(treeUri)

            val report = SyncReport(
                foldersUpserted = folderIds.size,
                tracksDiscovered = scan.files.size,
                tracksScanned = scanned,
                tracksFailed = failed,
            )
            logger.i(TAG, "Finished syncing root $treeUri: $report")
            report
        } catch (e: Exception) {
            logger.e(TAG, "Failed to sync root $treeUri", e)
            SyncReport.Empty
        }
    }

    /**
     * Pull every audio file the platform already knows about (Android's
     * MediaStore, equivalents on iOS/Desktop) and merge it into the library
     * under the synthetic [DeviceAudioScanner.ROOT_URI] tree.
     *
     * Idempotent: track rows are keyed by `document_uri`, so re-running this
     * pass after a scan is a no-op insert plus a metadata refresh.
     *
     * Returns [SyncReport.Empty] when the scanner returns nothing — which
     * is the path taken on permission-denied and on platforms with no
     * device-audio implementation. We treat "no permission" identically to
     * "no audio found" so the sync never throws at the user.
     */
    suspend fun syncDeviceAudio(): SyncReport {
        logger.i(TAG, "Starting syncDeviceAudio")
        val deviceTracks = deviceAudio.scan()
        if (deviceTracks.isEmpty()) {
            logger.d(TAG, "No device tracks found")
            return SyncReport.Empty
        }
        logger.d(TAG, "Found ${deviceTracks.size} device tracks")

        val syntheticFolders = synthesizeFolders(deviceTracks)
        val folderIds = materializeFolders(syntheticFolders)

        var scanned = 0
        var skippedAsDuplicate = 0
        val touchedTrackIds = mutableListOf<Long>()
        for (deviceTrack in deviceTracks) {
            try {
                // Skip files already covered by a SAF-managed root. Identity
                // is the physical fingerprint (name + size + mtime) — same
                // bytes on disk, two different URIs.
                val safMatch = tracks.findByContentKeyExcludingTree(
                    fileName = deviceTrack.fileName,
                    fileSize = deviceTrack.fileSize,
                    lastModified = deviceTrack.lastModified,
                    excludeTreeUri = DeviceAudioScanner.ROOT_URI,
                )
                if (safMatch != null) {
                    skippedAsDuplicate++
                    continue
                }

                val parentPath = parentRelativePath(deviceTrack.relativePath)
                val folderId = folderIds[parentPath]

                // INSERT OR IGNORE on document_uri — re-scans are no-ops here.
                tracks.insertFile(
                    documentUri = deviceTrack.sourceUri,
                    treeUri = DeviceAudioScanner.ROOT_URI,
                    relativePath = deviceTrack.relativePath,
                    fileName = deviceTrack.fileName,
                    fileSize = deviceTrack.fileSize,
                    lastModified = deviceTrack.lastModified,
                    folderId = folderId,
                )
                // Look up by URI instead of trusting lastInsertId — that value
                // is unreliable when INSERT OR IGNORE collapses to a no-op.
                val stored = tracks.findByDocumentUri(deviceTrack.sourceUri) ?: continue

                val artistId = upsertArtist(deviceTrack.metadata)
                val albumId = upsertAlbum(deviceTrack.metadata, artistId)
                tracks.updateMetadata(stored.applyMetadata(deviceTrack.metadata))
                tracks.updateForeignKeys(stored.id, artistId, albumId)
                touchedTrackIds += stored.id
                scanned++
            } catch (e: Exception) {
                logger.e(TAG, "Failed to process device track ${deviceTrack.sourceUri}", e)
            }
        }

        refreshAggregates(folderIds.values, touchedTrackIds)

        val report = SyncReport(
            foldersUpserted = folderIds.size,
            tracksDiscovered = deviceTracks.size,
            tracksScanned = scanned,
            // Tracks skipped as SAF duplicates aren't failures — count them
            // separately in the log but don't surface as failed in the report.
            tracksFailed = deviceTracks.size - scanned - skippedAsDuplicate,
        )
        logger.i(TAG, "Finished syncDeviceAudio: $report (skipped $skippedAsDuplicate SAF duplicates)")
        return report
    }

    /**
     * After a SAF scan inserts its tracks, sweep the device-audio tree and
     * delete any row matching the same (fileName, fileSize, lastModified).
     * The SAF row wins because it has a real parent folder in the user's tree
     * — the device-audio synthetic root was just a fallback.
     */
    private suspend fun demoteDeviceDuplicates(pending: List<Pair<Long, RawAudioFile>>) {
        if (pending.isEmpty()) return
        var removed = 0
        for ((_, file) in pending) {
            val deviceIds = tracks.findIdsByContentKeyInTree(
                fileName = file.fileName,
                fileSize = file.fileSize,
                lastModified = file.lastModified,
                inTreeUri = DeviceAudioScanner.ROOT_URI,
            )
            for (id in deviceIds) {
                tracks.deleteById(id)
                removed++
            }
        }
        if (removed > 0) {
            logger.i(TAG, "Removed $removed device-audio duplicates after SAF scan")
        }
    }

    /**
     * Derive a list of [AudioFolder] entries for the synthetic device-audio
     * tree by walking each track's `relativePath`. Returns folders in
     * parent-before-child order so [materializeFolders] can resolve FKs.
     *
     * The root folder ("" / [DeviceAudioScanner.ROOT_DISPLAY_NAME]) is
     * always emitted first, even if every track happens to live under a
     * subdirectory, so the UI has a single anchor for the synthetic tree.
     */
    private fun synthesizeFolders(deviceTracks: List<DeviceAudioTrack>): List<AudioFolder> {
        // Set of every directory path that needs to exist.
        val paths = sortedSetOf("")
        for (track in deviceTracks) {
            val parent = parentRelativePath(track.relativePath)
            if (parent.isEmpty()) continue
            // Add the parent and every ancestor up to the root.
            var current = parent
            while (current.isNotEmpty()) {
                paths += current
                val idx = current.lastIndexOf('/')
                current = if (idx < 0) "" else current.substring(0, idx)
            }
        }
        // sortedSetOf("") sorts lexically, which gives us parent-before-child
        // because "/foo" sorts before "/foo/bar" — but the root "" comes first
        // either way.
        return paths.map { path ->
            val parent = if (path.isEmpty()) null else parentRelativePath(path)
            val display = if (path.isEmpty()) {
                DeviceAudioScanner.ROOT_DISPLAY_NAME
            } else {
                val idx = path.lastIndexOf('/')
                if (idx < 0) path else path.substring(idx + 1)
            }
            AudioFolder(
                treeUri = DeviceAudioScanner.ROOT_URI,
                relativePath = path,
                displayName = display,
                parentRelativePath = parent,
            )
        }
    }

    private fun parentRelativePath(path: String): String {
        val idx = path.lastIndexOf('/')
        return if (idx < 0) "" else path.substring(0, idx)
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
     *
     * The metadata read (file I/O via MediaMetadataRetriever) is the
     * expensive step and gets fanned out across [METADATA_READ_CONCURRENCY]
     * coroutines. The follow-up DB writes stay sequential because Artist /
     * Album upserts have read-then-insert semantics that race under
     * concurrent writers — keeping them serial is cheaper than serializing
     * inside the repo.
     */
    private suspend fun applyMetadataAndGrouping(
        pending: List<Pair<Long, RawAudioFile>>,
    ): Pair<Int, Int> = coroutineScope {
        val semaphore = Semaphore(METADATA_READ_CONCURRENCY)
        val results = pending.map { (trackId, file) ->
            async {
                semaphore.withPermit {
                    trackId to metadataReader.read(file.documentUri)
                }
            }
        }.awaitAll()

        var scanned = 0
        var failed = 0
        for ((trackId, metadata) in results) {
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
        scanned to failed
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
): com.offlineplaya.shared.domain.model.Track {
    val rawGenre = metadata.genre?.takeIf { it.isNotBlank() }
    return copy(
        title = metadata.title?.takeIf { it.isNotBlank() } ?: fileName,
        artistName = metadata.artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
        albumArtistName = metadata.albumArtist?.takeIf { it.isNotBlank() },
        albumName = metadata.album?.takeIf { it.isNotBlank() } ?: "Unknown Album",
        genre = rawGenre,
        canonicalGenre = GenreClassifier.classify(rawGenre),
        year = metadata.year,
        trackNumber = metadata.trackNumber,
        discNumber = metadata.discNumber,
        durationMs = metadata.durationMs,
        bitrate = metadata.bitrate,
        sampleRate = metadata.sampleRate,
        channels = metadata.channels,
        codec = metadata.codec,
    )
}
