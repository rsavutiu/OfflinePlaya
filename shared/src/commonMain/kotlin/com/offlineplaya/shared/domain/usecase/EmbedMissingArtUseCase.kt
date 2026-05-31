package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.image.AlbumArtWriter
import com.offlineplaya.shared.domain.image.FolderArtSource
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.flow.first

private const val UNKNOWN_ARTIST = "Unknown Artist"
private const val UNKNOWN_ALBUM = "Unknown Album"

/**
 * Walks every scanned track, finds the ones without embedded album art,
 * looks up a cover for their album via [RemoteArtSource], and writes the
 * cover back via [AlbumArtWriter].
 *
 * Strategy:
 *  - Tracks are grouped by `(artistName, albumName)` so we hit MusicBrainz
 *    at most once per album, not once per track.
 *  - Tracks tagged with "Unknown …" placeholders are skipped — we have no
 *    confident album identity to query.
 *  - Per-track write failures increment a counter; they don't abort the
 *    whole pass.
 *
 * Callers receive incremental progress via [onProgress] and a final report
 * via [onComplete]. They can also pass a [shouldCancel] callback to stop
 * mid-flight (e.g. user navigated away).
 */
class EmbedMissingArtUseCase(
    private val tracks: TrackRepository,
    private val remoteSource: RemoteArtSource,
    private val folderSource: FolderArtSource,
    private val writer: AlbumArtWriter,
    private val logger: AppLogger,
) {
    private companion object {
        const val TAG = "EmbedMissingArtUseCase"
    }

    suspend operator fun invoke(
        onProgress: (EmbedReport.Running) -> Unit,
        shouldCancel: () -> Boolean = { false },
        treeUriFilter: String? = null,
    ): EmbedReport {
        logger.i(
            TAG, "Starting EmbedMissingArtUseCase" +
                (treeUriFilter?.let { " scoped to $it" } ?: " (whole library)"))
        val all = tracks.observeAll().first()
        val scoped = if (treeUriFilter != null) {
            all.filter { it.treeUri == treeUriFilter }
        } else {
            all
        }
        val total = scoped.size
        logger.d(TAG, "Found $total tracks in scope (of ${all.size} total)")

        // Group by (artist, album) so each album hits MusicBrainz once.
        val grouped = scoped
            .filterNot { it.artistName.equals(UNKNOWN_ARTIST, ignoreCase = true) }
            .filterNot { it.albumName.equals(UNKNOWN_ALBUM, ignoreCase = true) }
            .groupBy { (it.albumArtistName ?: it.artistName) to it.albumName }

        logger.d(TAG, "Grouped into ${grouped.size} candidate albums")

        var processed = 0
        var embedded = 0
        var failed = 0

        for ((key, group) in grouped) {
            if (shouldCancel()) {
                logger.i(TAG, "Sync cancelled by caller")
                return EmbedReport.Completed(processed, embedded, failed)
            }
            val (artist, album) = key

            // Filter to tracks that actually need art.
            val needing = group.filter { 
                try {
                    !writer.hasEmbeddedArt(it.documentUri)
                } catch (e: Exception) {
                    logger.e(TAG, "Error checking embedded art for ${it.documentUri}", e)
                    false // Skip if we can't even check
                }
            }
            
            // Tracks that already had art still count as "processed" so the
            // progress numbers reflect the whole library walk.
            processed += group.size - needing.size
            onProgress(EmbedReport.Running(processed, total, embedded, failed))

            if (needing.isEmpty()) {
                logger.d(TAG, "Album '$album' by '$artist' already has art for all ${group.size} tracks")
                continue
            }

            logger.i(TAG, "Fetching art for album '$album' by '$artist' (${needing.size} tracks need it)")
            // Sidecar (cover.jpg / folder.jpg) wins over remote — it's local,
            // free, and usually higher quality than what MusicBrainz/CAA has.
            // One track per group is enough; siblings share the folder.
            val sidecarBytes = try {
                folderSource.findInFolder(needing.first())
            } catch (e: Exception) {
                logger.e(TAG, "Error reading sidecar art for '$album' by '$artist'", e)
                null
            }
            val artBytes = sidecarBytes ?: try {
                remoteSource.resolve(artist, album)
            } catch (e: Exception) {
                logger.e(TAG, "Error fetching art for '$album' by '$artist'", e)
                null
            }
            if (sidecarBytes != null) {
                logger.d(
                    TAG,
                    "Using sidecar art for '$album' by '$artist' (${sidecarBytes.size} bytes)"
                )
            }

            if (artBytes == null) {
                logger.d(TAG, "No art found for album '$album' by '$artist'")
                // No cover available — these tracks stay arty-less.
                processed += needing.size
                onProgress(EmbedReport.Running(processed, total, embedded, failed))
                continue
            }

            logger.d(TAG, "Found art for '$album' by '$artist' (${artBytes.size} bytes). Writing to ${needing.size} tracks.")

            for (track in needing) {
                if (shouldCancel()) {
                    logger.i(TAG, "Sync cancelled by caller during writing")
                    return EmbedReport.Completed(processed, embedded, failed)
                }
                writer.write(track.documentUri, artBytes)
                    .fold(
                        onSuccess = { 
                            embedded++
                            logger.d(TAG, "Successfully embedded art into ${track.documentUri}")
                        },
                        onFailure = { error -> 
                            failed++
                            logger.e(TAG, "Failed to embed art into ${track.documentUri}", error)
                        },
                    )
                processed++
                onProgress(EmbedReport.Running(processed, total, embedded, failed))
            }
        }

        // Any tracks we filtered out at the top still count as "seen".
        val skipped = total - grouped.values.sumOf { it.size }
        processed += skipped
        onProgress(EmbedReport.Running(processed, total, embedded, failed))

        val report = EmbedReport.Completed(processed, embedded, failed)
        logger.i(TAG, "EmbedMissingArtUseCase completed: $report")
        return report
    }
}

