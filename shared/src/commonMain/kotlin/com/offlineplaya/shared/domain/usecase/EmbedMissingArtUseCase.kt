package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.image.AlbumArtWriter
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.repository.TrackRepository
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
    private val writer: AlbumArtWriter,
) {
    suspend operator fun invoke(
        onProgress: (EmbedReport.Running) -> Unit,
        shouldCancel: () -> Boolean = { false },
    ): EmbedReport {
        val all = tracks.observeAll().first()
        val total = all.size

        // Group by (artist, album) so each album hits MusicBrainz once.
        val grouped = all
            .filterNot { it.artistName.equals(UNKNOWN_ARTIST, ignoreCase = true) }
            .filterNot { it.albumName.equals(UNKNOWN_ALBUM, ignoreCase = true) }
            .groupBy { (it.albumArtistName ?: it.artistName) to it.albumName }

        var processed = 0
        var embedded = 0
        var failed = 0

        for ((key, group) in grouped) {
            if (shouldCancel()) {
                return EmbedReport.Completed(processed, embedded, failed)
            }
            val (artist, album) = key

            // Filter to tracks that actually need art.
            val needing = group.filter { !writer.hasEmbeddedArt(it.documentUri) }
            // Tracks that already had art still count as "processed" so the
            // progress numbers reflect the whole library walk.
            processed += group.size - needing.size
            onProgress(EmbedReport.Running(processed, total, embedded, failed))

            if (needing.isEmpty()) continue

            val artBytes = remoteSource.resolve(artist, album)
            if (artBytes == null) {
                // No cover available — these tracks stay arty-less.
                processed += needing.size
                onProgress(EmbedReport.Running(processed, total, embedded, failed))
                continue
            }

            for (track in needing) {
                if (shouldCancel()) {
                    return EmbedReport.Completed(processed, embedded, failed)
                }
                writer.write(track.documentUri, artBytes)
                    .fold(
                        onSuccess = { embedded++ },
                        onFailure = { failed++ },
                    )
                processed++
                onProgress(EmbedReport.Running(processed, total, embedded, failed))
            }
        }

        // Any tracks we filtered out at the top still count as "seen".
        val skipped = total - grouped.values.sumOf { it.size }
        processed += skipped
        onProgress(EmbedReport.Running(processed, total, embedded, failed))

        return EmbedReport.Completed(processed, embedded, failed)
    }
}

