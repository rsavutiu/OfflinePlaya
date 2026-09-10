package com.offlineplaya.shared.presentation.review

import com.offlineplaya.shared.data.repository.SqlSettingsRepository
import com.offlineplaya.shared.domain.model.ReviewPromptState
import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.domain.share.Sharer
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class ShareNudgeCoordinatorTest {

    private class FakeSharer : Sharer {
        var shares = 0
        override fun share(text: String): Boolean {
            shares++
            return true
        }
    }

    private class MutableClock(var nowMs: Long = 0L) {
        fun read(): Long = nowMs
    }

    private val week = ShareNudgeCoordinator.DEFAULT_MIN_AGE_MILLIS
    private val month = ShareNudgeCoordinator.DEFAULT_RESHOW_COOLDOWN_MILLIS

    private class Harness(
        val coordinator: ShareNudgeCoordinator,
        val sharer: FakeSharer,
        val settings: SettingsRepository,
        val clock: MutableClock,
    )

    private fun harness(startAtMs: Long = 1_000_000L): Harness {
        val settings = SqlSettingsRepository(createInMemoryDatabase(), Dispatchers.Unconfined)
        val sharer = FakeSharer()
        val clock = MutableClock(startAtMs)
        val coordinator = ShareNudgeCoordinator(
            settings = settings,
            sharer = sharer,
            logger = TestLogger(),
            now = clock::read,
        )
        return Harness(coordinator, sharer, settings, clock)
    }

    /** Pretend the user has played [n] tracks (bumps the shared counter). */
    private suspend fun SettingsRepository.playsCounted(n: Int) {
        setReviewPromptState(getReviewPromptState().copy(playsCounted = n))
    }

    @Test
    fun `does not show before a week of use`() = runTest {
        val h = harness()
        h.settings.playsCounted(5)
        // First call seeds firstLaunch = now; the week hasn't passed.
        assertFalse(h.coordinator.maybePromptForShare())
        h.clock.nowMs += week - 1
        assertFalse(h.coordinator.maybePromptForShare())
        assertFalse(h.coordinator.visible.value)
    }

    @Test
    fun `does not show when the user has never played anything`() = runTest {
        val h = harness()
        h.coordinator.maybePromptForShare() // seed firstLaunch
        h.clock.nowMs += week + 1
        assertFalse(h.coordinator.maybePromptForShare())
    }

    @Test
    fun `shows after a week with some listening, once`() = runTest {
        val h = harness()
        h.settings.playsCounted(3)
        h.coordinator.maybePromptForShare() // seed firstLaunch
        h.clock.nowMs += week + 1
        assertTrue(h.coordinator.maybePromptForShare())
        assertTrue(h.coordinator.visible.value)
        // A second checkpoint at the same instant must not re-fire.
        assertTrue(h.coordinator.maybePromptForShare()) // returns true because already visible
        h.coordinator.onDismiss()
        assertFalse(h.coordinator.maybePromptForShare()) // cooldown now in effect
    }

    @Test
    fun `dismissed dialog reappears a month later`() = runTest {
        val h = harness()
        h.settings.playsCounted(3)
        h.coordinator.maybePromptForShare()
        h.clock.nowMs += week + 1
        assertTrue(h.coordinator.maybePromptForShare())
        h.coordinator.onDismiss()

        // Just before the month elapses: still quiet.
        h.clock.nowMs += month - 1
        assertFalse(h.coordinator.maybePromptForShare())
        // After the month: it comes back on its own.
        h.clock.nowMs += 2
        assertTrue(h.coordinator.maybePromptForShare())
    }

    @Test
    fun `tapping share stops it forever`() = runTest {
        val h = harness()
        h.settings.playsCounted(3)
        h.coordinator.maybePromptForShare()
        h.clock.nowMs += week + 1
        assertTrue(h.coordinator.maybePromptForShare())
        h.coordinator.onShareClicked("hey try this: https://example")
        assertEquals(1, h.sharer.shares)
        assertTrue(h.settings.getReviewPromptState().shareNudgeActioned)

        // Even a month later it never shows again.
        h.clock.nowMs += month * 3
        assertFalse(h.coordinator.maybePromptForShare())
    }

    @Test
    fun `holds off right after another nudge fired (deconflict)`() = runTest {
        val h = harness()
        h.settings.playsCounted(3)
        h.coordinator.maybePromptForShare() // seed firstLaunch
        h.clock.nowMs += week + 1
        // Simulate the native review firing "now": it stamps the shared clock.
        h.settings.setReviewPromptState(
            h.settings.getReviewPromptState().copy(lastPromptAtMillis = h.clock.nowMs),
        )
        // Within the cross-nudge gap the share dialog stays out of the way.
        assertFalse(h.coordinator.maybePromptForShare())
        // Once the gap passes it may show.
        h.clock.nowMs += ShareNudgeCoordinator.DEFAULT_CROSS_NUDGE_GAP_MILLIS
        assertTrue(h.coordinator.maybePromptForShare())
    }

    @Test
    fun `existing user starts a fresh week, not a retroactive one`() = runTest {
        // A user upgrading in: they already have plays but firstLaunch is unset.
        val h = harness()
        h.settings.setReviewPromptState(
            ReviewPromptState(playsCounted = 500, firstLaunchAtMillis = 0L),
        )
        // First checkpoint seeds firstLaunch = now and must NOT show immediately.
        assertFalse(h.coordinator.maybePromptForShare())
        assertEquals(h.clock.nowMs, h.settings.getReviewPromptState().firstLaunchAtMillis)
        h.clock.nowMs += week + 1
        assertTrue(h.coordinator.maybePromptForShare())
    }
}
