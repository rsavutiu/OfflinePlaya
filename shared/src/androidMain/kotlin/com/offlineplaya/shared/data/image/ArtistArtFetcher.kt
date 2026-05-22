package com.offlineplaya.shared.data.image

import co.touchlab.kermit.Logger
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import android.graphics.BitmapFactory

internal class ArtistArtFetcher(
    private val artist: Artist,
    private val settings: SettingsRepository,
    private val artistsRepo: ArtistRepository,
    private val remoteSource: RemoteArtSource,
    private val httpClient: OkHttpClient,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        log.d { "fetch() called for artist='${artist.name}' id=${artist.id} imageUrl=${artist.imageUrl}" }
        var url = artist.imageUrl

        if (url == null) {
            val prefs = settings.getArtworkPreferences()
            if (!prefs.downloadRemoteArt) {
                log.d { "  downloadRemoteArt=false, returning null" }
                return@withContext null
            }

            log.d { "  imageUrl is null, resolving from remote..." }
            url = remoteSource.resolveArtistImage(artist.name)
            if (url == null) {
                log.d { "  remote returned null for '${artist.name}'" }
                return@withContext null
            }
            log.d { "  remote resolved URL: $url" }
            artistsRepo.updateImageUrl(artist.id, url)
            log.d { "  persisted URL to DB for artist id=${artist.id}" }
        }

        downloadAndDecode(url)
    }

    private fun downloadAndDecode(url: String): FetchResult? {
        log.d { "downloadAndDecode() url=$url" }
        val request = Request.Builder().url(url).build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                log.d { "  HTTP ${response.code} for $url" }
                if (!response.isSuccessful) {
                    log.w { "  download failed: HTTP ${response.code}" }
                    return null
                }
                val bytes = response.body?.bytes()
                if (bytes == null) {
                    log.w { "  response body is null" }
                    return null
                }
                log.d { "  downloaded ${bytes.size} bytes" }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap == null) {
                    log.w { "  BitmapFactory.decodeByteArray returned null (not a valid image?)" }
                    return null
                }
                log.d { "  decoded bitmap ${bitmap.width}x${bitmap.height}, returning ImageFetchResult" }
                ImageFetchResult(
                    image = bitmap.asImage(),
                    isSampled = false,
                    dataSource = DataSource.NETWORK,
                )
            }
        } catch (e: Exception) {
            log.e(e) { "  download/decode crashed for $url" }
            null
        }
    }

    class Factory(
        private val settings: SettingsRepository,
        private val artistsRepo: ArtistRepository,
        private val remoteSource: RemoteArtSource,
        private val httpClient: OkHttpClient,
    ) : Fetcher.Factory<Artist> {
        override fun create(
            data: Artist,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = ArtistArtFetcher(
            artist = data,
            settings = settings,
            artistsRepo = artistsRepo,
            remoteSource = remoteSource,
            httpClient = httpClient,
        )
    }

    private companion object {
        val log = Logger.withTag("ArtistArtFetcher")
    }
}

internal class ArtistKeyer : Keyer<Artist> {
    private val log = Logger.withTag("ArtistKeyer")

    override fun key(data: Artist, options: Options): String {
        val url = data.imageUrl
        val key = if (url != null) "artist-art:${data.id}:${url.hashCode()}" else "artist-art:${data.id}"
        log.v { "key() artist='${data.name}' imageUrl=${url != null} -> $key" }
        return key
    }
}
