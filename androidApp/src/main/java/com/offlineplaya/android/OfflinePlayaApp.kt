package com.offlineplaya.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.offlineplaya.android.di.appPlayerModule
import com.offlineplaya.shared.data.image.installTrackArtImageLoader
import com.offlineplaya.shared.data.scheduling.WorkManagerTaskRunner
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
        createBackgroundChannel()
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
        // Auto-rescan once per process start, after Koin is up. syncAll() now
        // covers both SAF managed roots AND the platform's MediaStore index,
        // so this is meaningful even on a fresh install — the device-audio
        // pass picks up everything in Download/ etc. without the user having
        // to add a folder first.
        koin.get<LibrarySyncCoordinator>().resyncAll()
    }

    /**
     * Notification channel for the foreground worker that backs
     * [com.offlineplaya.shared.domain.scheduling.BackgroundTaskRunner].
     * Created once per process, idempotent.
     */
    private fun createBackgroundChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            WorkManagerTaskRunner.CHANNEL_ID,
            getString(R.string.background_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.background_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }
}
