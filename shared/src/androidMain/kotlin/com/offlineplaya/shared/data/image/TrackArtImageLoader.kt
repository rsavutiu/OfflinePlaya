package com.offlineplaya.shared.data.image

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader

/**
 * Public installer for the Coil [ImageLoader] that knows how to render track
 * album art. The app module calls this from `Application.onCreate` so that
 * any `AsyncImage(model = track, …)` anywhere in the app routes through the
 * custom [TrackArtFetcher].
 */
fun installTrackArtImageLoader(context: Context) {
    SingletonImageLoader.setSafe { ctx ->
        ImageLoader.Builder(ctx)
            .components {
                add(TrackKeyer())
                add(TrackArtFetcher.Factory(context.applicationContext))
            }
            .build()
    }
}
