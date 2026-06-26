package com.offlineplaya.android.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.NowPlayingPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Isolated-screen test for [NowPlayingPage]: param-wired with a fake
 * [PlaybackState] (no Koin / no real player). Asserts the page reflects the
 * current track and that the play/pause control routes back to the callback;
 * the empty state renders when nothing is loaded.
 */
@OptIn(ExperimentalTestApi::class)
class NowPlayingPageTest {

    @Test
    fun reflects_the_current_track_and_routes_play_pause() = runComposeUiTest {
        var playPauseClicks = 0
        setContent {
            PreviewTheme {
                NowPlayingPage(
                    state = playingState(track("Once", "Pearl Jam", "Ten")),
                    onPlayPause = { playPauseClicks++ },
                    onPrevious = {}, onNext = {}, onSeek = {},
                    onShuffleToggle = {}, onRepeatChange = {},
                    onOpenQueue = {}, onOpenEqualizer = {}, onOpenLyrics = {},
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.NowPlaying.ROOT).assertIsDisplayed()
        onNodeWithText("Once").assertIsDisplayed()
        onNodeWithTag(TestTags.NowPlaying.EMPTY).assertDoesNotExist()

        onNodeWithTag(TestTags.NowPlaying.PLAY_PAUSE).assertIsDisplayed().performClick()
        assertEquals("play/pause tap should invoke onPlayPause once", 1, playPauseClicks)
    }

    @Test
    fun shows_the_empty_state_when_nothing_is_loaded() = runComposeUiTest {
        setContent {
            PreviewTheme {
                NowPlayingPage(
                    state = PlaybackState.Empty,
                    onPlayPause = {}, onPrevious = {}, onNext = {}, onSeek = {},
                    onShuffleToggle = {}, onRepeatChange = {},
                    onOpenQueue = {}, onOpenEqualizer = {}, onOpenLyrics = {},
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.NowPlaying.ROOT).assertIsDisplayed()
        onNodeWithTag(TestTags.NowPlaying.EMPTY).assertIsDisplayed()
        onNodeWithTag(TestTags.NowPlaying.PLAY_PAUSE).assertDoesNotExist()
    }
}

private fun playingState(current: Track) = PlaybackState(
    currentTrack = current,
    isPlaying = true,
    positionMs = 74_000L,
    durationMs = 224_000L,
    shuffleEnabled = false,
    repeatMode = RepeatMode.OFF,
    queue = persistentListOf(current),
    queueIndex = 0,
    volume = 1f,
)

private fun track(title: String, artist: String, album: String) = Track(
    id = 1L,
    documentUri = "u/1",
    treeUri = "t",
    relativePath = "$artist/$album/$title.flac",
    fileName = "$title.flac",
    title = title,
    artistName = artist,
    albumArtistName = null,
    albumName = album,
    genre = "Rock",
    year = 1991,
    trackNumber = 1,
    discNumber = 1,
    durationMs = 224_000L,
    bitrate = 1_000_000,
    sampleRate = 44_100,
    channels = 2,
    codec = "flac",
    artistId = 1L,
    albumId = 1L,
    folderId = 1L,
    scanStatus = ScanStatus.SCANNED,
)
