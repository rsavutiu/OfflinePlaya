package com.offlineplaya.shared.data.player

import android.content.ComponentName
import android.content.Context
import com.offlineplaya.shared.domain.player.MusicPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

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
): MusicPlayer = CoordinatingMusicPlayer(
    engine = Media3PlaybackEngine(
        context = context,
        sessionServiceComponent = serviceComponent,
        scope = scope,
    ),
    // The coordinator's small mutable state is touched both by transport calls
    // (from the UI / main thread) and by its own actor + reconcile coroutines;
    // confining those coroutines to the main thread keeps it race-free without
    // locks. Heavy work (MediaItem mapping) still runs off-main inside the
    // engine's suspend applies, so this doesn't put load on the main thread.
    scope = CoroutineScope(scope.coroutineContext + Dispatchers.Main.immediate),
)
