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
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val UNKNOWN_ARTIST = "Unknown Artist"
private const val UNKNOWN_ALBUM = "Unknown Album"

/**
 * Coil fetcher that resolves album art for a [Track] through a chain of
 * sources:
 *
 *  1. Embedded art via [MediaMetadataRetriever.embeddedPicture].
 *  2. Remote lookup via [RemoteArtSource] — only if
 *     `ArtworkPreferences.downloadRemoteArt` is enabled AND the track has
 *     real artist + album metadata (skip "Unknown …" placeholders).
 *  3. `null` — Coil shows the placeholder.
 *
 * Cache keying ([TrackKeyer]) is by `(artistId,albumId)` when both are
 * present so all tracks on the same album share one cache slot.
 */
internal class TrackArtFetcher(
    private val context: Context,
    private val track: Track,
    private val settings: SettingsRepository,
    private val remoteSource: RemoteArtSource,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        readEmbedded()?.let { return@withContext it }
        readRemoteIfEnabled()
    }

    private fun readEmbedded(): FetchResult? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(track.documentUri))
            val bytes = retriever.embeddedPicture ?: return null
            decode(bytes)
        } catch (_: Throwable) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
                // ignore release errors
            }
        }
    }

    private suspend fun readRemoteIfEnabled(): FetchResult? {
        val prefs = settings.getArtworkPreferences()
        if (!prefs.downloadRemoteArt) return null

        val artistKey = (track.albumArtistName ?: track.artistName).trim()
        val albumKey = track.albumName.trim()
        if (artistKey.isEmpty() || albumKey.isEmpty()) return null
        if (artistKey.equals(UNKNOWN_ARTIST, ignoreCase = true)) return null
        if (albumKey.equals(UNKNOWN_ALBUM, ignoreCase = true)) return null

        val bytes = remoteSource.resolve(artist = artistKey, album = albumKey) ?: return null
        return decode(bytes)
    }

    private fun decode(bytes: ByteArray): FetchResult? {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(
        private val context: Context,
        private val settings: SettingsRepository,
        private val remoteSource: RemoteArtSource,
    ) : Fetcher.Factory<Track> {
        override fun create(
            data: Track,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = TrackArtFetcher(
            context = context,
            track = data,
            settings = settings,
            remoteSource = remoteSource,
        )
    }
}

/**
 * Cache key for [Track] art. When both artistId and albumId are present we
 * key by `(artistId,albumId)` so every track in the same album shares one
 * cached cover. Falls back to per-track keying when ids are missing
 * (pending-scan tracks etc.).
 */
internal class TrackKeyer : Keyer<Track> {
    override fun key(data: Track, options: Options): String {
        val artistId = data.artistId
        val albumId = data.albumId
        return if (artistId != null && albumId != null) {
            "album-art:$artistId:$albumId"
        } else {
            "track-art:${data.documentUri}"
        }
    }
}
