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
import com.offlineplaya.shared.domain.image.FolderArtSource
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.repository.SettingsRepository
import java.io.File
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
    private val folderSource: FolderArtSource,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        // 1. Check our own on-disk byte cache — survives process death and is
        //    keyed the same way as Coil's memory cache (per album slot) so
        //    every track on the album shares one file.
        cachedArtFile().let { f ->
            if (f.exists() && f.length() > 0) {
                runCatching { f.readBytes() }.getOrNull()?.let { return@withContext decode(it) }
            }
        }
        // 2. Extract from the audio file itself.
        readEmbedded()?.let { (bytes, result) ->
            persist(bytes)
            return@withContext result
        }
        // 3. Sidecar cover.jpg / folder.jpg sitting next to the track.
        folderSource.findInFolder(track)?.let { bytes ->
            decode(bytes)?.let { result ->
                persist(bytes)
                return@withContext result
            }
        }
        // 4. Hit the remote source if the user opted in.
        readRemoteIfEnabled()?.let { (bytes, result) ->
            persist(bytes)
            return@withContext result
        }
        null
    }

    private fun readEmbedded(): Pair<ByteArray, FetchResult>? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(track.documentUri))
            val bytes = retriever.embeddedPicture ?: return null
            decode(bytes)?.let { bytes to it }
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

    private suspend fun readRemoteIfEnabled(): Pair<ByteArray, FetchResult>? {
        val prefs = settings.getArtworkPreferences()
        if (!prefs.downloadRemoteArt) return null

        val artistKey = (track.albumArtistName ?: track.artistName).trim()
        val albumKey = track.albumName.trim()
        if (artistKey.isEmpty() || albumKey.isEmpty()) return null
        if (artistKey.equals(UNKNOWN_ARTIST, ignoreCase = true)) return null
        if (albumKey.equals(UNKNOWN_ALBUM, ignoreCase = true)) return null

        val bytes = remoteSource.resolve(artist = artistKey, album = albumKey) ?: return null
        return decode(bytes)?.let { bytes to it }
    }

    private fun decode(bytes: ByteArray): FetchResult? {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    /**
     * Cache file for [track]'s art. Slot routing is centralised in
     * [TrackArtCache] so the Coil fetcher, the FileProvider, and the
     * Media3 mapper all agree on where bytes live.
     */
    private fun cachedArtFile(): File = TrackArtCache.cacheFile(context, track)

    private fun persist(bytes: ByteArray) {
        val file = cachedArtFile()
        runCatching {
            // Atomic-ish write so a crash mid-write doesn't leave a half file.
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeBytes(bytes)
            tmp.renameTo(file)
        }
    }

    class Factory(
        private val context: Context,
        private val settings: SettingsRepository,
        private val remoteSource: RemoteArtSource,
        private val folderSource: FolderArtSource,
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
            folderSource = folderSource,
        )
    }
}

/**
 * Coil keyer that routes through [TrackArtCache] so the in-memory Coil
 * cache shares slot identity with the on-disk persistence layer. Without
 * this, two different albums whose tracks hashed to the same fallback
 * URI would never collide; we'd waste work re-decoding identical bytes.
 */
internal class TrackKeyer : Keyer<Track> {
    override fun key(data: Track, options: Options): String =
        TrackArtCache.cacheKey(data)
}
