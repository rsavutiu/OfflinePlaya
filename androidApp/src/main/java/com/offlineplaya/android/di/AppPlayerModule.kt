package com.offlineplaya.android.di

import android.content.ComponentName
import com.offlineplaya.android.audio.AndroidPlayToPcController
import com.offlineplaya.android.service.PlaybackService
import com.offlineplaya.shared.data.player.createAndroidMusicPlayer
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.domain.stream.PlayToPcController
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * App-module-only DI bindings. Keeps the [PlaybackService] reference out of
 * the shared module so that module can stay free of references to any
 * concrete Android service class.
 */
val appPlayerModule: Module = module {
    single<MusicPlayer> {
        createAndroidMusicPlayer(
            context = androidContext(),
            serviceComponent = ComponentName(androidContext(), PlaybackService::class.java),
            scope = get(),
        )
    }

    // Play-to-PC: one controller shared by the picker UI (as PlayToPcController)
    // and the playback service (as the concrete type, which also owns the
    // player-side wiring via attachPlayer).
    single {
        AndroidPlayToPcController(
            context = androidContext(),
            scope = get(),
            logger = get(),
        )
    }
    single<PlayToPcController> { get<AndroidPlayToPcController>() }
}
