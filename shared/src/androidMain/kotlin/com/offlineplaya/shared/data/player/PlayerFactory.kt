package com.offlineplaya.shared.data.player

import android.content.ComponentName
import android.content.Context
import com.offlineplaya.shared.domain.player.MusicPlayer
import kotlinx.coroutines.CoroutineScope

/**
 * Public factory for the Android [MusicPlayer] implementation. The app module
 * supplies the [ComponentName] of its `MediaSessionService` (or
 * `MediaLibraryService`) subclass so the shared SDK stays decoupled from any
 * concrete service class.
 */
fun createAndroidMusicPlayer(
    context: Context,
    serviceComponent: ComponentName,
    scope: CoroutineScope,
): MusicPlayer = Media3MusicPlayer(
    context = context,
    sessionServiceComponent = serviceComponent,
    scope = scope,
)
