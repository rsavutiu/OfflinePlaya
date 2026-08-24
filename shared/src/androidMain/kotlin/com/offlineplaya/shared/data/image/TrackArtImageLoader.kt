package com.offlineplaya.shared.data.image

import android.content.Context
import co.touchlab.kermit.Logger
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.offlineplaya.shared.domain.image.FolderArtSource
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.SettingsRepository

/**
 * Installs the Coil [ImageLoader] that knows how to resolve track album art
 * through the embedded → remote → placeholder chain.
 *
 * **Must be called synchronously from `Application.onCreate`, before any Coil
 * API usage.** Coil 3.1's `setSafe` throws if the singleton loader was already
 * created by a `get()` (e.g. the first `AsyncImage` composing). Registration is
 * cheap and touches no DB; the dependencies are resolved lazily through the
 * provider lambdas, which Coil only invokes when it first builds the loader (on
 * the background thread that services the first image load) — so no SQLite
 * singleton is opened on the main thread at registration time.
 */
fun installTrackArtImageLoader(
    context: Context,
    settings: () -> SettingsRepository,
    artistsRepo: () -> ArtistRepository,
    remoteSource: () -> RemoteArtSource,
    folderSource: () -> FolderArtSource,
) {
    val log = Logger.withTag("ImageLoaderSetup")
    log.d { "installTrackArtImageLoader() called" }
    val appContext = context.applicationContext
    SingletonImageLoader.setSafe { ctx ->
        log.d { "Building ImageLoader with disk cache at ${appContext.cacheDir}/coil_cache" }
        // 50 MB HTTP cache so artist images (downloaded over OkHttp) survive
        // process restarts. Without this, every cold start re-downloads them.
        // Built lazily here (not at registration) so the factory stays cheap.
        val httpCache = okhttp3.Cache(
            directory = appContext.cacheDir.resolve("http_cache"),
            maxSize = 50L * 1024 * 1024,
        )
        val httpClient = okhttp3.OkHttpClient.Builder()
            .cache(httpCache)
            .build()
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
                        settings = settings(),
                        remoteSource = remoteSource(),
                        folderSource = folderSource(),
                    ),
                )
                add(ArtistKeyer())
                add(
                    ArtistArtFetcher.Factory(
                        settings = settings(),
                        artistsRepo = artistsRepo(),
                        remoteSource = remoteSource(),
                        httpClient = httpClient,
                    )
                )
            }
            .build()
    }
}
