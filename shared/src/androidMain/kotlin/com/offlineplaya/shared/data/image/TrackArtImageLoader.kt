package com.offlineplaya.shared.data.image

import android.content.Context
import co.touchlab.kermit.Logger
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.SettingsRepository

/**
 * Installs the Coil [ImageLoader] that knows how to resolve track album art
 * through the embedded → remote → placeholder chain. Called from
 * `Application.onCreate`.
 */
fun installTrackArtImageLoader(
    context: Context,
    settings: SettingsRepository,
    artistsRepo: ArtistRepository,
    remoteSource: RemoteArtSource,
) {
    val log = Logger.withTag("ImageLoaderSetup")
    log.d { "installTrackArtImageLoader() called" }
    val appContext = context.applicationContext
    val httpClient = okhttp3.OkHttpClient()
    SingletonImageLoader.setSafe { ctx ->
        log.d { "Building ImageLoader with disk cache at ${appContext.cacheDir}/coil_cache" }
        ImageLoader.Builder(ctx)
            .diskCache {
                DiskCache.Builder()
                    .directory(appContext.cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { httpClient }))
                add(TrackKeyer())
                add(
                    TrackArtFetcher.Factory(
                        context = appContext,
                        settings = settings,
                        remoteSource = remoteSource,
                    ),
                )
                add(ArtistKeyer())
                add(
                    ArtistArtFetcher.Factory(
                        settings = settings,
                        artistsRepo = artistsRepo,
                        remoteSource = remoteSource,
                        httpClient = httpClient,
                    )
                )
            }
            .build()
    }
}
