package com.offlineplaya.shared.data.image

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.repository.SettingsRepository

/**
 * Installs the Coil [ImageLoader] that knows how to resolve track album art
 * through the embedded → remote → placeholder chain. Called from
 * `Application.onCreate`.
 */
fun installTrackArtImageLoader(
    context: Context,
    settings: SettingsRepository,
    remoteSource: RemoteArtSource,
) {
    val appContext = context.applicationContext
    SingletonImageLoader.setSafe { ctx ->
        ImageLoader.Builder(ctx)
            .components {
                add(TrackKeyer())
                add(
                    TrackArtFetcher.Factory(
                        context = appContext,
                        settings = settings,
                        remoteSource = remoteSource,
                    ),
                )
            }
            .build()
    }
}
