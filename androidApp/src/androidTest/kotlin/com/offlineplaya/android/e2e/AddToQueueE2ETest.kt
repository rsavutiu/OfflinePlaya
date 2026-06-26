package com.offlineplaya.android.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.core.app.ApplicationProvider
import com.offlineplaya.shared.presentation.navigation.AppDestination
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.ui.TestTags
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Third E2E happy path (Phase 2): add-to-queue → QueuePage shows it. Same real
 * graph + fakes as the other E2E tests. Long-presses two tracks, adds each to
 * the queue via the details sheet, and asserts both land in the queue and
 * render on the Queue page.
 *
 *   launch → All Tracks → long-press → Add to queue (×2) → Queue shows both.
 *
 * Note: tap-to-play replaces the whole queue (the list is the play context), so
 * this path uses the long-press "Add to queue" action instead. With nothing
 * playing there is no in-UI route to the Queue page (it hangs off Now Playing),
 * so the final hop is pushed on the navigator the test already holds — the
 * asserted behaviour (add-to-queue, then the page renders the queue) stays
 * fully UI-driven.
 */
@OptIn(ExperimentalTestApi::class)
class AddToQueueE2ETest {

    @Test
    fun add_two_tracks_to_the_queue_then_view_them() = runComposeUiTest {
        val handle = startE2EKoin(ApplicationProvider.getApplicationContext())
        try {
            launchSeededE2EApp(handle)

            onNodeWithTag(TestTags.Home.ROOT).assertIsDisplayed()
            onNodeWithTag(TestTags.Home.BROWSE_TRACKS).performClick()
            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("Alpha Anthem").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(TestTags.Flat.ROOT).assertIsDisplayed()

            addToQueueViaLongPress("Alpha Anthem")
            assertEquals("first add should queue one track", 1, handle.player.playbackState.value.queue.size)

            addToQueueViaLongPress("Gamma Groove")
            assertEquals("second add should queue two tracks", 2, handle.player.playbackState.value.queue.size)

            // No current track ⇒ no in-UI route to Queue; hop via the navigator.
            handle.koin.get<AppNavigator>().push(AppDestination.Queue)
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(TestTags.Queue.ROOT).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Alpha Anthem").assertIsDisplayed()
            onNodeWithText("Gamma Groove").assertIsDisplayed()
        } finally {
            stopE2EKoin()
        }
    }

    /** Long-press a track row, then tap "Add to queue" in the details sheet. */
    private fun androidx.compose.ui.test.ComposeUiTest.addToQueueViaLongPress(title: String) {
        onNodeWithText(title).performTouchInput { longClick() }
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag(TestTags.TrackActions.ADD_TO_QUEUE).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag(TestTags.TrackActions.ADD_TO_QUEUE).performClick()
        // Sheet dismisses on tap; wait until it's gone before the next long-press.
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag(TestTags.TrackActions.ADD_TO_QUEUE).fetchSemanticsNodes().isEmpty()
        }
    }
}
