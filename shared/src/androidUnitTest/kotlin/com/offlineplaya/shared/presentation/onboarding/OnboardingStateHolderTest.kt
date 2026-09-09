package com.offlineplaya.shared.presentation.onboarding

import com.offlineplaya.shared.data.repository.SqlSettingsRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the first-run gate ([OnboardingStateHolder.completed]).
 *
 * The tri-state matters: `null` means "not resolved yet" and the host must
 * render neither the wizard nor the app, so an existing user never sees the
 * wizard flash on cold start. Once resolved it reflects the persisted flag,
 * [OnboardingStateHolder.complete] flips it to `true`, and the upgrade
 * migration seeds `true` for a user who already has an indexed library.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingStateHolderTest {

    private fun tracksRepo(db: OfflinePlayaDatabase, d: CoroutineDispatcher) =
        SqlTrackRepository(db, TestLogger(), d)

    @Test
    fun `resolves to false on a fresh empty install then flips true after complete`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        try {
            val db = createInMemoryDatabase()
            val settings = SqlSettingsRepository(db, dispatcher)
            val holder = OnboardingStateHolder(settings, tracksRepo(db, dispatcher), scope)

            // Fresh, empty store → resolves to "not completed", never stuck null.
            runCurrent()
            assertEquals(false, holder.completed.value)

            holder.complete()
            runCurrent()
            assertEquals(true, holder.completed.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `reflects an already-completed flag from a returning user`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        try {
            val db = createInMemoryDatabase()
            val settings = SqlSettingsRepository(db, dispatcher)
            settings.setOnboardingCompleted(true)

            val holder = OnboardingStateHolder(settings, tracksRepo(db, dispatcher), scope)
            runCurrent()
            assertEquals(true, holder.completed.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `migrates an existing library (unset flag, tracks present) to completed`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        try {
            val db = createInMemoryDatabase()
            val settings = SqlSettingsRepository(db, dispatcher)
            val tracks = tracksRepo(db, dispatcher)
            // Existing user: a library exists but the (new) flag was never set.
            tracks.insertFile(
                documentUri = "u/1",
                treeUri = "t",
                relativePath = "Song.mp3",
                fileName = "Song.mp3",
                fileSize = 1_000L,
                lastModified = 0L,
                folderId = null,
            )
            assertEquals(false, settings.isOnboardingCompleted())

            val holder = OnboardingStateHolder(settings, tracks, scope)
            runCurrent()

            // Migrated: resolves to completed AND persists so it sticks.
            assertEquals(true, holder.completed.value)
            assertEquals(true, settings.isOnboardingCompleted())
        } finally {
            scope.cancel()
        }
    }
}
