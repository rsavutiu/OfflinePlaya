package com.offlineplaya.android.e2e

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.offlineplaya.android.di.appPlayerModule
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.di.androidModule
import com.offlineplaya.shared.di.sharedModule
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.domain.scanner.DeviceAudioScanner
import com.offlineplaya.shared.domain.scanner.FolderScanner
import com.offlineplaya.shared.domain.scanner.MetadataReader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

/**
 * Boots a full Koin graph for E2E tests — the real `sharedModule` +
 * `androidModule` + `appPlayerModule` (so every state holder the host resolves
 * is the production one) with only the platform edges replaced by fakes:
 *
 * - **Database** → an in-memory `AndroidSqliteDriver` (no on-disk file, fresh
 *   per test), so seeding and assertions never touch the real library DB.
 * - **MusicPlayer** → [FakeMusicPlayer], so transport is deterministic state
 *   and no `PlaybackService` / Media3 / audio is involved.
 * - **Scanners** → [FakeE2EDeviceAudioScanner] / folder / metadata, so a
 *   `resyncAll()` populates the library from [E2ELibrary] with no SAF/MediaStore.
 *
 * The bare instrumentation `TestApplication` starts no Koin, so the E2E test
 * owns this graph's lifetime: call [startE2EKoin] before launching the host
 * Activity and [stopE2EKoin] after. The returned [FakeMusicPlayer] is the same
 * instance the UI drives, so the test can assert transport off its state.
 */
class E2EKoinHandle(val koin: Koin, val player: FakeMusicPlayer)

fun startE2EKoin(context: Context): E2EKoinHandle {
    val player = FakeMusicPlayer()
    val overrides = module {
        single { inMemoryDatabase(androidContext()) }
        single<MusicPlayer> { player }
        single<DeviceAudioScanner> { FakeE2EDeviceAudioScanner() }
        single<FolderScanner> { FakeE2EFolderScanner() }
        single<MetadataReader> { FakeE2EMetadataReader() }
    }
    val app = startKoin {
        // allowOverride so the override module's definitions win over the
        // production bindings they shadow.
        allowOverride(true)
        androidLogger(Level.ERROR)
        androidContext(context)
        modules(sharedModule, androidModule, appPlayerModule, overrides)
    }
    return E2EKoinHandle(app.koin, player)
}

fun stopE2EKoin() = stopKoin()

private fun inMemoryDatabase(context: Context): OfflinePlayaDatabase {
    // name = null → AndroidSqliteDriver opens an in-memory database that lives
    // only as long as the driver, matching the prod schema (the real driver
    // doesn't set extra PRAGMAs either).
    val driver = AndroidSqliteDriver(
        schema = OfflinePlayaDatabase.Schema,
        context = context,
        name = null,
    )
    return OfflinePlayaDatabase(driver)
}
