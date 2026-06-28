package com.offlineplaya.shared.presentation.review

import com.offlineplaya.shared.data.repository.SqlSettingsRepository
import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.domain.review.ReviewPrompter
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewPromptCoordinatorTest {

    /** Records every launch; [accept] models whether an Activity was present. */
    private class FakePrompter(var accept: Boolean = true) : ReviewPrompter {
        var launches = 0
        override suspend fun requestReview(): Boolean {
            launches++
            return accept
        }
    }

    private class Harness(
        val coordinator: ReviewPromptCoordinator,
        val prompter: FakePrompter,
        val settings: SettingsRepository,
        val clock: MutableClock,
    )

    private class MutableClock(var nowMs: Long = 0L) {
        fun read(): Long = nowMs
    }

    private fun harness(
        promptAccepts: Boolean = true,
        cooldownMs: Long = ReviewPromptCoordinator.DEFAULT_RESYNC_COOLDOWN_MILLIS,
    ): Harness {
        val settings = SqlSettingsRepository(createInMemoryDatabase(), Dispatchers.Unconfined)
        val prompter = FakePrompter(accept = promptAccepts)
        val clock = MutableClock()
        val coordinator = ReviewPromptCoordinator(
            settings = settings,
            prompter = prompter,
            logger = TestLogger(),
            now = clock::read,
            resyncCooldownMillis = cooldownMs,
        )
        return Harness(coordinator, prompter, settings, clock)
    }

    private suspend fun ReviewPromptCoordinator.play(times: Int) {
        repeat(times) { onTrackPlayed() }
    }

    @Test
    fun `does not prompt before the first milestone`() = runTest {
        val h = harness()
        h.coordinator.play(9)
        h.coordinator.maybePromptForReview()
        assertEquals(0, h.prompter.launches)
    }

    @Test
    fun `prompts once at the 10 play milestone`() = runTest {
        val h = harness()
        h.coordinator.play(10)
        h.coordinator.maybePromptForReview()
        h.coordinator.maybePromptForReview() // checkpoint fires again — must not re-prompt
        assertEquals(1, h.prompter.launches)
        assertEquals(10, h.settings.getReviewPromptState().highestMilestoneFired)
    }

    @Test
    fun `escalates through 10, 100 and 1000 then stops`() = runTest {
        val h = harness()
        h.coordinator.play(10); h.coordinator.maybePromptForReview()
        h.coordinator.play(90); h.coordinator.maybePromptForReview()   // reach 100
        h.coordinator.play(900); h.coordinator.maybePromptForReview()  // reach 1000
        h.coordinator.play(5000); h.coordinator.maybePromptForReview() // nothing left
        assertEquals(3, h.prompter.launches)
        assertEquals(1000, h.settings.getReviewPromptState().highestMilestoneFired)
    }

    @Test
    fun `milestone reached while backgrounded is retried, not burned`() = runTest {
        // accept=false models "no resumed Activity": the flow can't launch.
        val h = harness(promptAccepts = false)
        h.coordinator.play(10)
        h.coordinator.maybePromptForReview() // attempt, declined
        assertEquals(0, h.settings.getReviewPromptState().highestMilestoneFired)

        h.prompter.accept = true             // back in foreground
        h.coordinator.maybePromptForReview() // retry succeeds
        assertEquals(2, h.prompter.launches)
        assertEquals(10, h.settings.getReviewPromptState().highestMilestoneFired)
    }

    @Test
    fun `post-resync nudge is cooldown gated and only after first milestone`() = runTest {
        val cooldown = 1_000L
        val h = harness(cooldownMs = cooldown)

        // Brand-new user (no milestone yet) is never nudged by a resume/resync.
        h.coordinator.maybePromptForReview()
        assertEquals(0, h.prompter.launches)

        // Cross the first milestone (1 launch), which also sets lastPromptAt = now.
        h.coordinator.play(10)
        h.coordinator.maybePromptForReview()
        assertEquals(1, h.prompter.launches)

        // Within the cooldown: a further resume does not nudge.
        h.clock.nowMs += cooldown - 1
        h.coordinator.maybePromptForReview()
        assertEquals(1, h.prompter.launches)

        // After the cooldown elapses: one more nudge fires.
        h.clock.nowMs += 2
        h.coordinator.maybePromptForReview()
        assertEquals(2, h.prompter.launches)
    }
}
