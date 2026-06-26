package com.offlineplaya.android.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.core.app.ApplicationProvider
import com.offlineplaya.shared.presentation.ui.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * First full E2E happy path (Phase 2). Boots the *real* DI graph — real DB
 * (in-memory), repositories, state holders, sync coordinator and `App()` — with
 * only the platform leaves faked ([FakeMusicPlayer] + fake scanners over
 * [E2ELibrary]). Drives the UI like a user and asserts state transitions, not
 * audio:
 *
 *   launch → seed visible in All Tracks → tap track → Now Playing shows it →
 *   pause is reflected in the player state.
 *
 * Runs on the proven `runComposeUiTest` harness (the AndroidX activity rule
 * can't see this app's JetBrains-Compose composition — see the Phase-0 notes),
 * wiring `App()` exactly as `MainActivity.AndroidApp` does, minus the Android
 * host plumbing (SAF picker, permission prompt, system bars).
 */
@OptIn(ExperimentalTestApi::class)
class LibraryToNowPlayingE2ETest {

    @Test
    fun play_a_seeded_track_then_pause() = runComposeUiTest {
        val handle = startE2EKoin(ApplicationProvider.getApplicationContext())
        try {
            // Bring the host up on a quiet main thread, then seed.
            launchSeededE2EApp(handle)

            // Home is the landing screen.
            onNodeWithTag(TestTags.Home.ROOT).assertIsDisplayed()

            // Open All Tracks and wait for the seeded library to populate.
            onNodeWithTag(TestTags.Home.BROWSE_TRACKS).performClick()
            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("Alpha Anthem").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(TestTags.Flat.ROOT).assertIsDisplayed()

            // Tap the track → it plays and routes to Now Playing.
            onNodeWithText("Alpha Anthem").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(TestTags.NowPlaying.ROOT).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Alpha Anthem").assertIsDisplayed()
            assertTrue(
                "tapping a track should start playback in the player",
                handle.player.playbackState.value.isPlaying,
            )
            assertEquals(
                "the tapped track should be current",
                "Alpha Anthem",
                handle.player.playbackState.value.currentTrack?.title,
            )

            // Pause from Now Playing → reflected in player state.
            onNodeWithTag(TestTags.NowPlaying.PLAY_PAUSE).performClick()
            waitUntil(timeoutMillis = 5_000) { !handle.player.playbackState.value.isPlaying }
            assertFalse(
                "play/pause should pause the player",
                handle.player.playbackState.value.isPlaying,
            )
        } finally {
            stopE2EKoin()
        }
    }
}
