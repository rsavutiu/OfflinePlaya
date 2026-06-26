package com.offlineplaya.android

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Instrumentation runner that swaps the production [OfflinePlayaApp] for a bare
 * [Application] during tests.
 *
 * Why: instrumented tests run inside the app process, so the declared
 * `android:name=".OfflinePlayaApp"` would otherwise boot the full app on every
 * test — Koin, the PlaybackService/MediaSession, library sync, the image loader
 * — which floods the main thread and starves the Compose test host activity
 * (the composition never registers → "No compose hierarchies found"). Isolated
 * screen tests build pages from fakes and need none of that.
 *
 * A Koin-override Application (in-memory DB + fake scanners) for full E2E tests
 * is a separate, later step; this bare host covers the isolated-screen layer.
 */
class HarnessTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, TestApplication::class.java.name, context)
}

/** Bare host Application — no Koin, no services, no background work. */
class TestApplication : Application()
