package com.offlineplaya.android

import android.app.Application
import com.offlineplaya.android.di.appPlayerModule
import com.offlineplaya.shared.data.image.installTrackArtImageLoader
import com.offlineplaya.shared.di.androidModule
import com.offlineplaya.shared.di.initKoin
import com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level

class OfflinePlayaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        installTrackArtImageLoader(this)
        initKoin(
            appDeclaration = {
                androidLogger(Level.INFO)
                androidContext(this@OfflinePlayaApp)
            },
            platformModules = listOf(androidModule, appPlayerModule),
        )
        // Auto-rescan once per process start, after Koin is up. No-op when no
        // managed roots are registered yet (fresh install).
        GlobalContext.get().get<LibrarySyncCoordinator>().resyncAllIfHasRoots()
    }
}
