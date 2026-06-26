package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.genre.GenreTagWriter
import com.offlineplaya.shared.domain.genre.RemoteGenreSource
import com.offlineplaya.shared.domain.image.AlbumArtWriter
import com.offlineplaya.shared.domain.image.FolderArtSource
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.domain.scanner.DeviceAudioScanner
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

private const val UNKNOWN_ARTIST = "Unknown Artist"
private const val UNKNOWN_ALBUM = "Unknown Album"

/**
 * Unified "burn" pass that handles both Album Art and Genre tags in one walk.
 *
 * Efficient because:
 *  1. Groups by (Artist, Album) so MusicBrainz is hit at most once per album.
 *  2. MusicBrainz Art and Genre lookups share the same MBID result.
 *  3. Skips tracks that already have both fields populated.
 *  4. Per-track write failures are counted but don't stop the whole pass.
 */
class BurnMetadataUseCase(
    private val tracks: TrackRepository,
    private val artSource: RemoteArtSource,
    private val folderArtSource: FolderArtSource,
    private val artWriter: AlbumArtWriter,
    private val genreSource: RemoteGenreSource,
    private val genreWriter: GenreTagWriter,
    private val logger: AppLogger,
) {
    private companion object {
        const val TAG = "BurnMetadataUseCase"

        // Albums processed in parallel. Each album does its own fetch
        // (rate-limited by [MusicBrainzArtSource]) + per-track writes.
        // 4 is a balance between throughput (multi-core file IO) and
        // resource caps (SAF file descriptors, OkHttp dispatcher slots).
        const val ALBUM_CONCURRENCY = 4
    }

    suspend operator fun invoke(
        onProgress: (EmbedReport.Running) -> Unit,
        shouldCancel: () -> Boolean = { false },
        treeUriFilter: String? = null,
    ): EmbedReport = coroutineScope {
        logger.i(TAG, "Starting BurnMetadataUseCase walk")
        val all = tracks.observeAll().first()
        val scoped = if (treeUriFilter != null) {
            all.filter { it.treeUri == treeUriFilter }
        } else {
            // Skip synthetic MediaStore tracks as they are read-only SAF entries
            all.filterNot { it.treeUri == DeviceAudioScanner.ROOT_URI }
        }
        val total = scoped.size
        if (total == 0) return@coroutineScope EmbedReport.Completed(0, 0, 0)

        // Group by (artist, album). Tracks with unknown artist/album are
        // excluded from the burn (no metadata target) but still count toward
        // `total` so the final processed == total accounting holds.
        val grouped = scoped
            .filterNot { it.artistName.equals(UNKNOWN_ARTIST, ignoreCase = true) }
            .filterNot { it.albumName.equals(UNKNOWN_ALBUM, ignoreCase = true) }
            .groupBy { (it.albumArtistName ?: it.artistName) to it.albumName }

        // Shared counters across album coroutines. Mutex contention is
        // minimal at ALBUM_CONCURRENCY=4 — each lock holds for nanoseconds.
        val counters = ProgressCounters()
        val semaphore = Semaphore(ALBUM_CONCURRENCY)

        val deferreds = grouped.map { (key, group) ->
            async {
                if (shouldCancel()) return@async
                semaphore.withPermit {
                    if (shouldCancel()) return@withPermit
                    processAlbum(
                        artist = key.first,
                        album = key.second,
                        group = group,
                        counters = counters,
                        total = total,
                        onProgress = onProgress,
                        shouldCancel = shouldCancel,
                    )
                }
            }
        }
        deferreds.awaitAll()

        // Roll any tracks that were filtered out (Unknown artist/album)
        // into the final processed count so the UI's progress bar lands
        // exactly at total.
        val (processed, successful, failed) = counters.snapshot()
        val finalProcessed = total
        if (finalProcessed != processed) {
            onProgress(EmbedReport.Running(finalProcessed, total, successful, failed))
        }

        val report = EmbedReport.Completed(finalProcessed, successful, failed)
        logger.i(TAG, "BurnMetadataUseCase finished: $report")
        report
    }

    /**
     * One album's worth of work: figure out what's missing, fetch the
     * art + genre payload once, then walk the tracks writing the data.
     * Per-track writes stay sequential — they hit the SAF file
     * descriptor for the same album folder, and the win there isn't worth
     * the second layer of concurrency control.
     */
    private suspend fun processAlbum(
        artist: String,
        album: String,
        group: List<Track>,
        counters: ProgressCounters,
        total: Int,
        onProgress: (EmbedReport.Running) -> Unit,
        shouldCancel: () -> Boolean,
    ) = coroutineScope {
        val needingArt = group.filter {
            runCatching { !artWriter.hasEmbeddedArt(it.documentUri) }.getOrDefault(false)
        }
        val needingGenre = group.filter { it.genre.isNullOrBlank() }

        if (needingArt.isEmpty() && needingGenre.isEmpty()) {
            counters.addProcessed(group.size)
            counters.emit(total, onProgress)
            return@coroutineScope
        }

        // Fetch art + genre in parallel. They share the MusicBrainz
        // rate-limit mutex inside MusicBrainzArtSource, so the two
        // requests won't actually race the upstream — but the local
        // sidecar lookup + Deezer attempt for art happens while the
        // genre request waits its turn at the MB gate, recovering most
        // of the latency.
        val artBytesDeferred = async {
            if (needingArt.isEmpty()) return@async null
            runCatching { folderArtSource.findInFolder(needingArt.first()) }.getOrNull()
                ?: runCatching { artSource.resolve(artist, album) }.getOrNull()
        }
        val rawGenreDeferred = async {
            if (needingGenre.isEmpty()) return@async null
            runCatching { genreSource.resolveGenre(artist, album) }.getOrNull()
        }

        val artBytes = artBytesDeferred.await()
        val rawGenre = rawGenreDeferred.await()

        for (track in group) {
            if (shouldCancel()) break

            val needsArt = track in needingArt && artBytes != null
            val needsGenre = track in needingGenre && rawGenre != null

            if (!needsArt && !needsGenre) {
                counters.addProcessed(1)
                counters.emit(total, onProgress)
                continue
            }

            var trackSuccess = true

            if (needsArt) {
                val res = artWriter.write(track.documentUri, artBytes!!)
                if (res.isFailure) trackSuccess = false
            }

            // Genre is independent of art — a different writer and a different
            // tag field — so don't gate it on the art write succeeding. Either
            // can fail on its own; trackSuccess just records that both worked.
            if (needsGenre) {
                val res = genreWriter.writeGenre(track.documentUri, rawGenre!!)
                if (res.isSuccess) {
                    val canonical = GenreClassifier.classify(rawGenre)
                    tracks.setRawAndCanonicalGenre(track.id, rawGenre, canonical)
                } else {
                    trackSuccess = false
                }
            }

            if (trackSuccess) counters.addSuccess() else counters.addFailure()
            counters.emit(total, onProgress)
        }
    }

    /**
     * Mutex-guarded counters shared across the album coroutines. Reads
     * via [snapshot] take a consistent triple under the same lock so the
     * progress callback can't observe a torn state mid-update.
     */
    private class ProgressCounters {
        private val lock = Mutex()
        private var processed = 0
        private var successful = 0
        private var failed = 0

        suspend fun addProcessed(n: Int) = lock.withLock { processed += n }
        suspend fun addSuccess() = lock.withLock { processed += 1; successful += 1 }
        suspend fun addFailure() = lock.withLock { processed += 1; failed += 1 }
        suspend fun snapshot(): Triple<Int, Int, Int> =
            lock.withLock { Triple(processed, successful, failed) }

        suspend fun emit(total: Int, onProgress: (EmbedReport.Running) -> Unit) {
            val (p, s, f) = snapshot()
            onProgress(EmbedReport.Running(p, total, s, f))
        }
    }

    /**
     * Single-album entry point used by the opportunistic EQ-Auto hook.
     * Tags every track in [tracksOnAlbum] with the genre [genreSource]
     * returns for ([artist], [album]). Returns null when MB has nothing —
     * the caller leaves the EQ on the flat fallback.
     */
    suspend fun tagAlbumOpportunistically(
        artist: String,
        album: String,
        tracksOnAlbum: List<Track>,
    ): com.offlineplaya.shared.domain.model.CanonicalGenre? {
        if (artist.equals(UNKNOWN_ARTIST, ignoreCase = true)) return null
        if (album.equals(UNKNOWN_ALBUM, ignoreCase = true)) return null
        val needing = tracksOnAlbum.filter { it.genre.isNullOrBlank() }
        if (needing.isEmpty()) return null

        val raw = runCatching { genreSource.resolveGenre(artist, album) }
            .onFailure {
                logger.w(
                    TAG,
                    "Genre lookup failed for '$album' by '$artist': ${it.message}"
                )
            }
            .getOrNull() ?: return null
        val canonical = GenreClassifier.classify(raw)

        for (track in needing) {
            genreWriter.writeGenre(track.documentUri, raw)
                .onSuccess {
                    tracks.setRawAndCanonicalGenre(track.id, raw, canonical)
                    logger.d(
                        TAG,
                        "Opportunistically tagged ${track.documentUri} → $raw / $canonical"
                    )
                }
                .onFailure {
                    logger.w(
                        TAG,
                        "Opportunistic tag write failed for ${track.documentUri}: ${it.message}"
                    )
                }
        }
        return canonical
    }
}
