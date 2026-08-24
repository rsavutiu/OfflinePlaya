package com.offlineplaya.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.offlineplaya.android.audio.BluetoothAutoplayController
import com.offlineplaya.android.di.appPlayerModule
import com.offlineplaya.android.sync.AutoRescanController
import com.offlineplaya.shared.data.image.installTrackArtImageLoader
import com.offlineplaya.shared.data.review.CurrentActivityHolder
import com.offlineplaya.shared.data.scheduling.WorkManagerTaskRunner
import com.offlineplaya.shared.di.androidModule
import com.offlineplaya.shared.di.initKoin
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.domain.usecase.RestorePersistedQueueUseCase
import com.offlineplaya.shared.presentation.history.PlayHistoryRecorder
import com.offlineplaya.shared.presentation.queue.QueueStateRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level

class OfflinePlayaApp : Application() {

    // Explicitly owned so the Application — not the ContentResolver's internal
    // observer list — holds the controller's lifetime. It self-tears-down on app
    // scope completion (see AutoRescanController.stop); this reference just makes
    // that ownership explicit instead of relying on implicit reachability.
    private var autoRescanController: AutoRescanController? = null
    private var bluetoothAutoplayController: BluetoothAutoplayController? = null

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
        // Everything else touches the database (via Koin singletons) or the
        // network (remote-art source). Push it onto the application-scoped
        // background scope so Application.onCreate returns quickly and the
        // first frame isn't waiting on SQLite open + a full library scan.
        val koin = GlobalContext.get()
        // Register synchronously (not on appScope) so the holder is watching
        // before MainActivity resumes — the in-app review prompt needs the
        // resumed Activity. The holder is lightweight (no DB/network), so this
        // doesn't pull the slow singletons onto the main thread.
        registerActivityLifecycleCallbacks(koin.get<CurrentActivityHolder>())
        // Register the Coil singleton loader synchronously, before any Activity
        // can compose an AsyncImage. Coil 3.1's setSafe throws if the singleton
        // was already created by a get(); doing this inside appScope.launch (as
        // it used to be) raced the first frame and crashed on cold start. The
        // provider lambdas defer every DB/network Koin lookup until Coil builds
        // the loader on its first image load, so no SQLite opens on the main
        // thread here.
        installTrackArtImageLoader(
            context = this@OfflinePlayaApp,
            settings = { koin.get<SettingsRepository>() },
            artistsRepo = { koin.get<com.offlineplaya.shared.domain.repository.ArtistRepository>() },
            remoteSource = { koin.get<RemoteArtSource>() },
            folderSource = { koin.get<com.offlineplaya.shared.domain.image.FolderArtSource>() },
        )
        val appScope = koin.get<CoroutineScope>()
        appScope.launch {
            // Last session's queue comes back paused before the queue
            // recorder starts watching, so the recorder's first sight of the
            // player is the restored queue, never a clobbering empty state.
            koin.get<RestorePersistedQueueUseCase>().invoke()
            koin.get<QueueStateRecorder>().start()
            // Listening-history recorder: one observer for the app's
            // lifetime, appending a PlayHistory row per played track.
            koin.get<PlayHistoryRecorder>().start()
            // Resume-on-Bluetooth-connect (opt-in). App-lifetime like the
            // recorders: must outlive any single Activity, and the queue
            // restore above guarantees a paused queue exists to resume.
            bluetoothAutoplayController = BluetoothAutoplayController(
                context = this@OfflinePlayaApp,
                player = koin.get(),
                preferences = koin.get<com.offlineplaya.shared.presentation.settings.PlaybackTuningStateHolder>().preferences,
                logger = koin.get(),
            ).also { it.start() }
            // After the cold-start scan, keep watching: MediaStore changes
            // (torrents, downloads, sync clients) and app foreground events
            // each trigger a debounced reconcile. See [AutoRescanController].
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                autoRescanController = AutoRescanController(
                    context = this@OfflinePlayaApp,
                    coordinator = koin.get(),
                    scope = appScope,
                ).also { it.start() }
            }
        }
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
