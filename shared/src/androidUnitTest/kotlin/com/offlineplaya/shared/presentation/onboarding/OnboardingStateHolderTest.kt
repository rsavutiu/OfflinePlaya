package com.offlineplaya.shared.presentation.onboarding

import com.offlineplaya.shared.data.repository.SqlSettingsRepository
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
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
 * wizard flash on cold start. Once resolved it reflects the persisted flag, and
 * [OnboardingStateHolder.complete] flips it to `true`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingStateHolderTest {

    @Test
    fun `resolves to false on a fresh install then flips true after complete`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        try {
            val settings = SqlSettingsRepository(createInMemoryDatabase(), dispatcher)
            val holder = OnboardingStateHolder(settings, scope)

            // Fresh store → resolves to "not completed" (false), never stuck null.
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
            val settings = SqlSettingsRepository(createInMemoryDatabase(), dispatcher)
            settings.setOnboardingCompleted(true)

            val holder = OnboardingStateHolder(settings, scope)
            runCurrent()
            assertEquals(true, holder.completed.value)
        } finally {
            scope.cancel()
        }
    }
}
