package com.offlineplaya.shared.data.image

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import com.offlineplaya.shared.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coil fetcher that pulls embedded album art out of an audio file using
 * Android's [MediaMetadataRetriever]. Returns `null` when no art is present —
 * Coil then shows the AsyncImage's error/fallback content.
 *
 * Registered via [TrackArtImageLoader] in the app module.
 */
internal class TrackArtFetcher(
    private val context: Context,
    private val track: Track,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(track.documentUri))
            val bytes = retriever.embeddedPicture ?: return@withContext null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext null
            ImageFetchResult(
                image = bitmap.asImage(),
                isSampled = false,
                dataSource = DataSource.DISK,
            )
        } catch (_: Throwable) {
            null
        } finally {
            // Retriever release errors aren't actionable here — swallow them so we
            // never crash a fetch on cleanup.
            try {
                retriever.release()
            } catch (_: Throwable) {
                // ignore
            }
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Track> {
        override fun create(
            data: Track,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = TrackArtFetcher(context, data)
    }
}

/** Identifies cached art by the track's SAF document URI — stable across recompositions. */
internal class TrackKeyer : Keyer<Track> {
    override fun key(data: Track, options: Options): String = "track-art:${data.documentUri}"
}
