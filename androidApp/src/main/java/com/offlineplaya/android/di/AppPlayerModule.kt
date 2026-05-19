package com.offlineplaya.android.di

import android.content.ComponentName
import com.offlineplaya.android.service.PlaybackService
import com.offlineplaya.shared.data.player.createAndroidMusicPlayer
import com.offlineplaya.shared.domain.player.MusicPlayer
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
}
