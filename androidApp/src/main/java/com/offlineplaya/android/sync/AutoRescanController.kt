package com.offlineplaya.android.sync

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Triggers a library re-scan when the device's audio inventory changes or
 * the user brings the app back to the foreground.
 *
 * Two signals:
 *  1. A [ContentObserver] on `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` —
 *     fires when *anything* (the system, a torrent client, a sync app, a file
 *     manager) adds or removes audio. Covers Downloads/ and any other location
 *     MediaStore indexes, regardless of whether the user added a SAF root for it.
 *  2. App foreground (`ProcessLifecycleOwner.ON_START`) — a torrent that
 *     finished while the app was backgrounded won't necessarily fire a
 *     MediaStore event we can hear (the broadcast may have happened before we
 *     registered, or to a folder MediaStore doesn't index). Returning to the
 *     app should always reconcile the library.
 *
 * Both signals are debounced — a torrent client dropping fifty files in a
 * second produces a burst of observer callbacks; we want one scan, not fifty.
 * The coordinator's own idle-guard ([LibrarySyncCoordinator.resyncIfIdle])
 * provides a second line of defence for the case where the debounce fires
 * while a scan is still wrapping up.
 *
 * The cold-start scan in [com.offlineplaya.android.OfflinePlayaApp] handles
 * the first launch of the process; this controller covers everything after.
 */
class AutoRescanController(
    private val context: Context,
    private val coordinator: LibrarySyncCoordinator,
    private val scope: CoroutineScope,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var observer: ContentObserver? = null
    private var pendingScan: Job? = null
    private var registeredAtMillis: Long = 0L

    /**
     * Wire up both triggers. Idempotent — calling twice from
     * `Application.onCreate` only registers once.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun start() {
        if (observer != null) return
        val resolver = context.contentResolver
        val obs = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                schedule()
            }
        }
        // notifyForDescendants = true so changes to a specific track URI
        // (content://media/external/audio/media/12345) also reach us — the
        // observer otherwise only sees notifications targeting the root URI.
        resolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            /* notifyForDescendants = */ true,
            obs,
        )
        observer = obs
        registeredAtMillis = System.currentTimeMillis()

        // [start] is called from the application-scoped background coroutine
        // (Dispatchers.Default) so we don't block the first frame on DB init.
        // Lifecycle.addObserver requires the main thread — post it.
        handler.post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    // Skip the very first ON_START — the cold-start scan from
                    // OfflinePlayaApp.onCreate is already running and a foreground
                    // rescan moments later is just churn. After that, every return
                    // to the app does a quiet reconcile.
                    if (System.currentTimeMillis() - registeredAtMillis < FOREGROUND_GRACE_MILLIS) return
                    schedule()
                }
            })
        }
    }

    /**
     * Debounce window: a burst of file changes within this window collapses
     * into a single scan. Set wide enough that a torrent client unpacking an
     * album's worth of files lands in one batch, narrow enough that the user
     * doesn't sit watching a stale library for long after the last file lands.
     */
    private fun schedule() {
        pendingScan?.cancel()
        pendingScan = scope.launch {
            delay(DEBOUNCE_MILLIS)
            coordinator.resyncIfIdle()
        }
    }

    companion object {
        private const val DEBOUNCE_MILLIS = 2_000L
        private const val FOREGROUND_GRACE_MILLIS = 5_000L
    }
}
