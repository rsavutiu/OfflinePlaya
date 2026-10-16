package com.offlineplaya.shared.presentation.review

import com.offlineplaya.shared.data.repository.SqlSettingsRepository
import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.domain.stream.PcReceiver
import com.offlineplaya.shared.domain.stream.PlayToPcController
import com.offlineplaya.shared.domain.stream.PlayToPcState
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import com.offlineplaya.shared.util.TestLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class PlayToPcNudgeCoordinatorTest {

    private class FakePlayToPcController(active: Boolean = false) : PlayToPcController {
        val activeFlow = MutableStateFlow(active)
        override val state = MutableStateFlow(PlayToPcState())
        override val active: StateFlow<Boolean> = activeFlow
        override fun startDiscovery() {}
        override fun stopDiscovery() {}
        override fun connect(receiver: PcReceiver) {}
        override fun connectManual(host: String, port: Int) {}
        override fun disconnect() {}
    }

    private class MutableClock(var nowMs: Long = 0L) {
        fun read(): Long = nowMs
    }

    private val month = PlayToPcNudgeCoordinator.DEFAULT_RESHOW_COOLDOWN_MILLIS
    private val gap = PlayToPcNudgeCoordinator.DEFAULT_CROSS_NUDGE_GAP_MILLIS

    private class Harness(
        val coordinator: PlayToPcNudgeCoordinator,
        val controller: FakePlayToPcController,
        val settings: SettingsRepository,
        val clock: MutableClock,
    )

    private fun harness(active: Boolean = false, startAtMs: Long = 1_000_000L): Harness {
        val settings = SqlSettingsRepository(createInMemoryDatabase(), Dispatchers.Unconfined)
        val controller = FakePlayToPcController(active)
        val clock = MutableClock(startAtMs)
        val coordinator = PlayToPcNudgeCoordinator(
            settings = settings,
            playToPc = controller,
            logger = TestLogger(),
            now = clock::read,
        )
        return Harness(coordinator, controller, settings, clock)
    }

    private suspend fun SettingsRepository.playsCounted(n: Int) {
        setReviewPromptState(getReviewPromptState().copy(playsCounted = n))
    }

    @Test
    fun `does not show below the play threshold`() = runTest {
        val h = harness()
        h.settings.playsCounted(1)
        assertFalse(h.coordinator.maybePrompt())
        assertFalse(h.coordinator.visible.value)
    }

    @Test
    fun `shows after a couple of plays, once`() = runTest {
        val h = harness()
        h.settings.playsCounted(2)
        assertTrue(h.coordinator.maybePrompt())
        assertTrue(h.coordinator.visible.value)
        // Already visible → returns true without re-firing.
        assertTrue(h.coordinator.maybePrompt())
        h.coordinator.onDismiss()
        assertFalse(h.coordinator.maybePrompt()) // monthly cooldown now in effect
    }

    @Test
    fun `never shows while already streaming to a PC`() = runTest {
        val h = harness(active = true)
        h.settings.playsCounted(10)
        assertFalse(h.coordinator.maybePrompt())
    }

    @Test
    fun `dismissed dialog reappears a month later`() = runTest {
        val h = harness()
        h.settings.playsCounted(3)
        assertTrue(h.coordinator.maybePrompt())
        h.coordinator.onDismiss()
        h.clock.nowMs += month - 1
        assertFalse(h.coordinator.maybePrompt())
        h.clock.nowMs += 2
        assertTrue(h.coordinator.maybePrompt())
    }

    @Test
    fun `engaging with the feature stops it forever`() = runTest {
        val h = harness()
        h.settings.playsCounted(3)
        assertTrue(h.coordinator.maybePrompt())
        h.coordinator.markActioned()
        assertTrue(h.settings.getReviewPromptState().playToPcNudgeActioned)
        h.clock.nowMs += month * 3
        assertFalse(h.coordinator.maybePrompt())
    }

    @Test
    fun `holds off right after another nudge fired (deconflict)`() = runTest {
        val h = harness()
        h.settings.playsCounted(3)
        h.settings.setReviewPromptState(
            h.settings.getReviewPromptState().copy(lastPromptAtMillis = h.clock.nowMs),
        )
        assertFalse(h.coordinator.maybePrompt())
        h.clock.nowMs += gap
        assertTrue(h.coordinator.maybePrompt())
    }

    @Test
    fun `stops after the show cap even if always dismissed`() = runTest {
        val h = harness()
        h.settings.playsCounted(3)
        // Show 1
        assertTrue(h.coordinator.maybePrompt())
        h.coordinator.onDismiss()
        h.clock.nowMs += month + 1
        // Show 2
        assertTrue(h.coordinator.maybePrompt())
        h.coordinator.onDismiss()
        h.clock.nowMs += month + 1
        // Cap (2) reached → never again.
        assertFalse(h.coordinator.maybePrompt())
    }
}
