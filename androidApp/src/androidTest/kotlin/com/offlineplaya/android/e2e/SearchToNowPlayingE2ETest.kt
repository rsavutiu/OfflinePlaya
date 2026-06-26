package com.offlineplaya.android.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.core.app.ApplicationProvider
import com.offlineplaya.shared.presentation.ui.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Second E2E happy path (Phase 2): search → result → play. Same real graph as
 * [LibraryToNowPlayingE2ETest] (in-memory DB + [E2ELibrary] via fake scanners +
 * [FakeMusicPlayer]); drives the search field, plays a match, and asserts the
 * transition into Now Playing off the player state.
 *
 *   launch → open Search → type a query → result appears → tap it →
 *   Now Playing shows it and the player is playing.
 */
@OptIn(ExperimentalTestApi::class)
class SearchToNowPlayingE2ETest {

    @Test
    fun search_for_a_track_then_play_it() = runComposeUiTest {
        val handle = startE2EKoin(ApplicationProvider.getApplicationContext())
        try {
            launchSeededE2EApp(handle)

            onNodeWithTag(TestTags.Home.ROOT).assertIsDisplayed()

            // Open search from the header.
            onNodeWithTag(TestTags.Home.SEARCH).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(TestTags.Search.FIELD).fetchSemanticsNodes().isNotEmpty()
            }

            // Type a query and wait for the seeded match to surface (results
            // depend on the async seed having landed in the DB).
            onNodeWithTag(TestTags.Search.FIELD).performTextInput("Beta")
            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("Beta Ballad").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(TestTags.Search.RESULTS).assertIsDisplayed()

            // Play the match → routes to Now Playing.
            onNodeWithText("Beta Ballad").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(TestTags.NowPlaying.ROOT).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Beta Ballad").assertIsDisplayed()
            assertTrue(
                "playing a search result should start playback",
                handle.player.playbackState.value.isPlaying,
            )
            assertEquals(
                "the played search result should be current",
                "Beta Ballad",
                handle.player.playbackState.value.currentTrack?.title,
            )
        } finally {
            stopE2EKoin()
        }
    }
}
