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
import kotlinx.coroutines.flow.first

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
    }

    suspend operator fun invoke(
        onProgress: (EmbedReport.Running) -> Unit,
        shouldCancel: () -> Boolean = { false },
        treeUriFilter: String? = null,
    ): EmbedReport {
        logger.i(TAG, "Starting BurnMetadataUseCase walk")
        val all = tracks.observeAll().first()
        val scoped = if (treeUriFilter != null) {
            all.filter { it.treeUri == treeUriFilter }
        } else {
            // Skip synthetic MediaStore tracks as they are read-only SAF entries
            all.filterNot { it.treeUri == DeviceAudioScanner.ROOT_URI }
        }
        val total = scoped.size
        if (total == 0) return EmbedReport.Completed(0, 0, 0)

        // Group by (artist, album)
        val grouped = scoped
            .filterNot { it.artistName.equals(UNKNOWN_ARTIST, ignoreCase = true) }
            .filterNot { it.albumName.equals(UNKNOWN_ALBUM, ignoreCase = true) }
            .groupBy { (it.albumArtistName ?: it.artistName) to it.albumName }

        var processed = 0
        var successful = 0
        var failed = 0

        for ((key, group) in grouped) {
            if (shouldCancel()) {
                logger.i(TAG, "Pass cancelled by user")
                return EmbedReport.Completed(processed, successful, failed)
            }

            val (artist, album) = key

            // Check which tracks in this album need what
            val needingArt = group.filter {
                try {
                    !artWriter.hasEmbeddedArt(it.documentUri)
                } catch (e: Exception) {
                    false
                }
            }
            val needingGenre = group.filter { it.genre.isNullOrBlank() }

            if (needingArt.isEmpty() && needingGenre.isEmpty()) {
                processed += group.size
                onProgress(EmbedReport.Running(processed, total, successful, failed))
                continue
            }

            // --- FETCH STEP ---
            var artBytes: ByteArray? = null
            if (needingArt.isNotEmpty()) {
                // Try sidecar then remote
                artBytes = try {
                    folderArtSource.findInFolder(needingArt.first())
                } catch (e: Exception) {
                    null
                } ?: try {
                    artSource.resolve(artist, album)
                } catch (e: Exception) {
                    null
                }
            }

            var rawGenre: String? = null
            if (needingGenre.isNotEmpty()) {
                rawGenre = try {
                    genreSource.resolveGenre(artist, album)
                } catch (e: Exception) {
                    null
                }
            }

            // --- BURN STEP ---
            for (track in group) {
                if (shouldCancel()) break

                val needsArt = track in needingArt && artBytes != null
                val needsGenre = track in needingGenre && rawGenre != null

                if (!needsArt && !needsGenre) {
                    // This specific track was already fine or we found nothing for it
                    if (track in needingArt || track in needingGenre) {
                        // We needed something but found nothing -> "skipped"
                    } else {
                        // Already had it
                    }
                } else {
                    var trackSuccess = true

                    if (needsArt) {
                        val res = artWriter.write(track.documentUri, artBytes!!)
                        if (res.isFailure) trackSuccess = false
                    }

                    if (needsGenre && trackSuccess) {
                        val res = genreWriter.writeGenre(track.documentUri, rawGenre!!)
                        if (res.isSuccess) {
                            val canonical = GenreClassifier.classify(rawGenre)
                            tracks.setRawAndCanonicalGenre(track.id, rawGenre, canonical)
                        } else {
                            trackSuccess = false
                        }
                    }

                    if (trackSuccess) successful++ else failed++
                }

                processed++
                onProgress(EmbedReport.Running(processed, total, successful, failed))
            }
        }

        // Catch skipped tracks (Unknown artist/album)
        val skippedCount = total - processed
        processed += skippedCount
        onProgress(EmbedReport.Running(processed, total, successful, failed))

        val report = EmbedReport.Completed(processed, successful, failed)
        logger.i(TAG, "BurnMetadataUseCase finished: $report")
        return report
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
