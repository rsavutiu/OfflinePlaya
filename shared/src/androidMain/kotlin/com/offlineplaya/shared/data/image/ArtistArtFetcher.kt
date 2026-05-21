package com.offlineplaya.shared.data.image

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

/**
 * Coil fetcher for artist images.
 * 
 * 1. If artist has an [Artist.imageUrl], fetches the bytes.
 * 2. If not, and remote download is enabled, looks up a new URL.
 */
internal class ArtistArtFetcher(
    private val artist: Artist,
    private val settings: SettingsRepository,
    private val artistsRepo: ArtistRepository,
    private val remoteSource: RemoteArtSource,
    private val httpClient: OkHttpClient,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        var url = artist.imageUrl
        
        if (url == null) {
            val prefs = settings.getArtworkPreferences()
            if (!prefs.downloadRemoteArt) return@withContext null
            
            url = remoteSource.resolveArtistImage(artist.name) ?: return@withContext null
            // Persist for next time
            artistsRepo.updateImageUrl(artist.id, url)
        }

        downloadAndDecode(url)
    }

    private fun downloadAndDecode(url: String): FetchResult? {
        val request = Request.Builder().url(url).build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            ImageFetchResult(
                image = bitmap.asImage(),
                isSampled = false,
                dataSource = DataSource.NETWORK,
            )
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
}

internal class ArtistKeyer : Keyer<Artist> {
    override fun key(data: Artist, options: Options): String {
        return "artist-art:${data.id}"
    }
}
