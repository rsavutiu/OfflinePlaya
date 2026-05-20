package com.offlineplaya.android

import android.app.Application
import com.offlineplaya.android.di.appPlayerModule
import com.offlineplaya.shared.data.image.installTrackArtImageLoader
import com.offlineplaya.shared.di.androidModule
import com.offlineplaya.shared.di.initKoin
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level

class OfflinePlayaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            appDeclaration = {
                androidLogger(Level.INFO)
                androidContext(this@OfflinePlayaApp)
            },
            platformModules = listOf(androidModule, appPlayerModule),
        )
        // Coil ImageLoader is installed after Koin so it can pick up the
        // settings + remote-source singletons from the graph.
        val koin = GlobalContext.get()
        installTrackArtImageLoader(
            context = this,
            settings = koin.get<SettingsRepository>(),
            remoteSource = koin.get<RemoteArtSource>(),
        )
        // Auto-rescan once per process start, after Koin is up. No-op when no
        // managed roots are registered yet (fresh install).
        koin.get<LibrarySyncCoordinator>().resyncAllIfHasRoots()
    }
}
