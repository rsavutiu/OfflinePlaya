package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.model.ExcludedFolder
import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.ExcludedFolderRepository
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
import com.offlineplaya.shared.domain.model.ScanStatus
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
    private val excludedFolders: ExcludedFolderRepository,
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

        /**
         * Synthetic album-artist for untagged compilations — a folder whose
         * tracks span more than one artist with no explicit albumArtist tag.
         * Standard music-library convention; keeps the album as one row.
         */
        const val VARIOUS_ARTISTS = "Various Artists"

        /**
         * Matches a trailing featuring credit so the primary artist can be
         * isolated. Anchored on an explicit feat./ft./featuring marker preceded
         * by whitespace or an opening bracket — never a bare "&"/"," — so band
         * names keep their commas and ampersands.
         */
        val FEAT_CREDIT = Regex(
            """[\s(\[]+(?:feat\.?|ft\.?|featuring)\s.*""",
            RegexOption.IGNORE_CASE,
        )
    }

    /**
     * Sync every managed tree root sequentially, accumulating the report,
     * then also pull in everything the platform's audio index knows about
     * via [DeviceAudioScanner]. The device-audio pass is what catches
     * music in Downloads / the internal-storage root / other locations
     * Android refuses to let users grant SAF tree access to.
     */
    suspend fun syncAll(force: Boolean = false): SyncReport {
        logger.i(TAG, "Starting syncAll (force=$force)")
        val roots = managedRoots.getAll()
        logger.d(TAG, "Found ${roots.size} managed roots")
        val safReport = roots.fold(SyncReport.Empty) { acc, root ->
            acc + syncOne(root.treeUri)
        }
        val deviceReport = syncDeviceAudio(force = force)
        // After every sync pass, top up canonical_genre on any rows still
        // missing it — covers pre-EQ-feature tracks that were scanned by an
        // older app version and never classified. Runs in chunks so a huge
        // legacy library doesn't block the sync's reported "Completed" state.
        backfillCanonicalGenre()
        // Drop albums/artists left with no tracks — e.g. the per-artist album
        // rows a compilation used to be split into, after a force-rescan
        // regroups its tracks under one "Various Artists" album.
        albums.deleteEmpty()
        artists.deleteOrphans()
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

            // User-excluded subtrees are invisible to the sync — folders
            // aren't materialized and files aren't inserted.
            val exclusions = excludedFolders.getAll()
            val keptFolders = scan.folders.filterNot { isExcluded(treeUri, it.relativePath, exclusions) }
            val keptFiles = scan.files.filterNot { isExcluded(treeUri, it.relativePath, exclusions) }
            if (keptFiles.size != scan.files.size) {
                logger.i(TAG, "Skipping ${scan.files.size - keptFiles.size} file(s) under excluded folders")
            }

            val folderIds = materializeFolders(keptFolders)
            val pendingTracks = insertPendingTracks(keptFiles, folderIds)
            // After inserting SAF tracks, promote them over any device-audio
            // row representing the same physical file. Otherwise a file picked
            // up by both scanners would show as a duplicate in the library.
            demoteDeviceDuplicates(pendingTracks)
            val (scanned, failed) = applyMetadataAndGrouping(pendingTracks)

            refreshAggregates(folderIds.values, pendingTracks.map { it.first })

            managedRoots.markScanned(treeUri)

            val report = SyncReport(
                foldersUpserted = folderIds.size,
                tracksDiscovered = keptFiles.size,
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
    suspend fun syncDeviceAudio(force: Boolean = false): SyncReport {
        logger.i(TAG, "Starting syncDeviceAudio (force=$force)")
        val allDeviceTracks = deviceAudio.scan()
        // Drop tracks under user-excluded folders before any other handling
        // so they don't count as discovered/failed in the report either.
        val exclusions = excludedFolders.getAll()
        val deviceTracks = allDeviceTracks.filterNot {
            isExcluded(DeviceAudioScanner.ROOT_URI, it.relativePath, exclusions)
        }
        if (deviceTracks.size != allDeviceTracks.size) {
            logger.i(
                TAG,
                "Skipping ${allDeviceTracks.size - deviceTracks.size} device track(s) under excluded folders",
            )
        }
        if (deviceTracks.isEmpty()) {
            logger.d(TAG, "No device tracks found")
            return SyncReport.Empty
        }
        logger.d(TAG, "Found ${deviceTracks.size} device tracks")

        val syntheticFolders = synthesizeFolders(deviceTracks)
        val folderIds = materializeFolders(syntheticFolders)

        var scanned = 0
        var skippedAsDuplicate = 0
        var unchanged = 0
        val touchedTrackIds = mutableListOf<Long>()
        val entries = mutableListOf<GroupEntry>()
        for (deviceTrack in deviceTracks) {
            try {
                // Fast path: a row for this exact MediaStore URI that's already
                // been fully scanned needs no work. syncDeviceAudio runs on
                // every app foreground (see the resync-on-resume trigger), so
                // without this short-circuit every resume would re-upsert every
                // artist + album and rewrite every track row — pure DB churn
                // (and the per-track log spam the user noticed) for a library
                // that hasn't changed. We still process PENDING rows (freshly
                // inserted, never classified) and ERROR rows (worth a retry).
                //
                // [force] overrides this — the "Rescan all" action reprocesses
                // everything so a logic change (e.g. compilation grouping) can
                // be applied to an already-scanned library without a wipe.
                val existing = tracks.findByDocumentUri(deviceTrack.sourceUri)
                if (!force && existing != null && existing.scanStatus == ScanStatus.SCANNED) {
                    unchanged++
                    continue
                }

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

                entries += GroupEntry(stored.id, parentPath, deviceTrack.metadata)
                touchedTrackIds += stored.id
                scanned++
            } catch (e: Exception) {
                logger.e(TAG, "Failed to process device track ${deviceTrack.sourceUri}", e)
            }
        }

        // Group after the loop so a compilation folder is seen as a whole.
        assignArtistsAndAlbums(entries)
        refreshAggregates(folderIds.values, touchedTrackIds)

        val report = SyncReport(
            foldersUpserted = folderIds.size,
            tracksDiscovered = deviceTracks.size,
            tracksScanned = scanned,
            // Neither SAF duplicates nor already-scanned unchanged rows are
            // failures — subtract both so a steady-state resync (everything
            // unchanged) reports zero failed instead of flagging the whole
            // library.
            tracksFailed = deviceTracks.size - scanned - skippedAsDuplicate - unchanged,
        )
        logger.i(
            TAG,
            "Finished syncDeviceAudio: $report " +
                "(skipped $skippedAsDuplicate SAF duplicates, $unchanged already scanned)",
        )
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

    /** True when [path] within [treeUri] falls under any user exclusion. */
    private fun isExcluded(
        treeUri: String,
        path: String,
        exclusions: List<ExcludedFolder>,
    ): Boolean = exclusions.any { it.covers(treeUri, path) }

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
                    Triple(trackId, file, metadataReader.read(file.documentUri))
                }
            }
        }.awaitAll()

        var failed = 0
        val entries = mutableListOf<GroupEntry>()
        for ((trackId, file, metadata) in results) {
            if (metadata == null) {
                tracks.markError(trackId)
                failed++
            } else {
                entries += GroupEntry(trackId, file.parentRelativePath(), metadata)
            }
        }
        // Artist/album assignment happens after all reads so compilations can
        // be detected across the whole folder (a single track can't know it's
        // part of a multi-artist album).
        assignArtistsAndAlbums(entries)
        entries.size to failed
    }

    /**
     * Assign Artist + Album foreign keys for a batch of freshly-read tracks,
     * collapsing multi-artist albums into a single "Various Artists"
     * compilation instead of splitting one album into one row per artist.
     *
     * Grouping key for albums is **(album name, parent folder)**: a
     * compilation living in one folder collapses to a single album even when
     * every track has a different artist, while two unrelated albums that
     * happen to share a name stay distinct as long as they're in different
     * folders. (Multi-disc albums split across sub-folders still merge at the
     * DB level, since album identity there is (name, artist).)
     *
     * Tracks with no album tag are handled individually — each keeps its own
     * artist and gets no album.
     */
    private suspend fun assignArtistsAndAlbums(entries: List<GroupEntry>) {
        val (withAlbum, withoutAlbum) = entries.partition { !it.metadata.album.isNullOrBlank() }

        for (e in withoutAlbum) {
            applyTrack(e, artistId = ownArtistId(e.metadata), albumId = null)
        }

        val groups = withAlbum.groupBy {
            AlbumGroupKey(it.metadata.album!!.trim().lowercase(), it.folderKey)
        }
        for ((_, group) in groups) {
            val albumName = group.first().metadata.album!!.trim()
            val artistId = resolveGroupArtistId(group)
            val year = group.firstNotNullOfOrNull { it.metadata.year }
            val albumId = albums.upsert(albumName, artistId, year)
            for (e in group) applyTrack(e, artistId, albumId)
        }
    }

    private suspend fun applyTrack(entry: GroupEntry, artistId: Long?, albumId: Long?) {
        // row may have disappeared between insert and update — skip silently
        val stored = tracks.findById(entry.trackId) ?: return
        tracks.updateMetadata(stored.applyMetadata(entry.metadata))
        tracks.updateForeignKeys(entry.trackId, artistId, albumId)
    }

    /**
     * The album-artist for a group:
     *  1. an explicit `albumArtist` tag (most common one in the group) wins —
     *     properly-tagged compilations already say "Various Artists" here;
     *  2. otherwise, if the group spans more than one distinct track artist,
     *     it's an untagged compilation → [VARIOUS_ARTISTS];
     *  3. otherwise the single track artist.
     *
     * Every track in the group (and the album) is filed under this artist, so
     * a compilation appears once in the Albums/Artists views. Each track still
     * shows its own artist *name* in track rows (preserved via
     * [com.offlineplaya.shared.domain.model.Track.applyMetadata]).
     */
    private suspend fun resolveGroupArtistId(group: List<GroupEntry>): Long? {
        val albumArtist = group
            .mapNotNull { it.metadata.albumArtist?.takeIf { a -> a.isNotBlank() } }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        if (albumArtist != null) return artists.upsert(albumArtist)

        // Normalize each track artist to its "primary" (strip a trailing
        // "feat./ft./featuring ..." credit) so a single-artist album with a few
        // guest tracks — e.g. a Phil Collins album with "Phil Collins feat.
        // Philip Bailey" — doesn't read as multiple artists.
        val primaries = group
            .mapNotNull { it.metadata.artist?.takeIf { a -> a.isNotBlank() } }
            .map { primaryArtist(it) }
            .filter { it.isNotBlank() }
        if (primaries.isEmpty()) return null

        val countsByKey = primaries.groupingBy { it.lowercase() }.eachCount()
        // primaries is non-empty (guarded above), so this is always present;
        // the elvis is defensive, not a reachable branch.
        val topKey = countsByKey.maxByOrNull { it.value }?.key ?: return null
        val topCount = countsByKey.getValue(topKey)

        // It's a compilation ONLY if no single artist owns the majority of the
        // tracks. One dominant artist (with occasional guests) keeps their name;
        // a genuinely mixed album (no majority) becomes Various Artists.
        return if (countsByKey.size == 1 || topCount * 2 > primaries.size) {
            // At least one primary matches topKey case-insensitively (topKey is
            // a lowercased primary), so the filtered list is non-empty; fall
            // back to topKey only to avoid a hard crash if that ever changes.
            val display = primaries.filter { it.equals(topKey, ignoreCase = true) }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: topKey
            artists.upsert(display)
        } else {
            artists.upsert(VARIOUS_ARTISTS)
        }
    }

    /**
     * The "primary" artist of a credit string: the part before a featuring
     * marker. "Phil Collins feat. Philip Bailey" / "Drake (ft. Future)" →
     * "Phil Collins" / "Drake". Deliberately only splits on explicit feat./ft./
     * featuring markers — never on "&" or "," — so band names like
     * "Earth, Wind & Fire" are left intact.
     */
    private fun primaryArtist(name: String): String =
        name.replace(FEAT_CREDIT, "").trim()

    /** A single track's own artist (no album-grouping context). */
    private suspend fun ownArtistId(metadata: AudioMetadata): Long? {
        val name = metadata.artist?.takeIf { it.isNotBlank() }
            ?: metadata.albumArtist?.takeIf { it.isNotBlank() }
            ?: return null
        return artists.upsert(name)
    }

    /** A track ready for artist/album assignment: id + parent folder + tags. */
    private class GroupEntry(
        val trackId: Long,
        val folderKey: String,
        val metadata: AudioMetadata,
    )

    private data class AlbumGroupKey(val albumNameLower: String, val folderKey: String)

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
