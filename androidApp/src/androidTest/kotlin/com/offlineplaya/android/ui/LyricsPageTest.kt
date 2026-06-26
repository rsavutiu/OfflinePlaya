package com.offlineplaya.android.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.lyrics.LyricLine
import com.offlineplaya.shared.presentation.lyrics.LyricsUiState
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.LyricsPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import org.junit.Test

/**
 * Isolated-screen test for [LyricsPage]: param-wired with each [LyricsUiState]
 * (no Koin). Asserts the page renders the matching body — Loading / None /
 * Plain / Synced — keyed by the tags in [TestTags.Lyrics].
 */
@OptIn(ExperimentalTestApi::class)
class LyricsPageTest {

    @Test
    fun shows_the_spinner_while_loading() = lyricsCase(
        state = LyricsUiState.Loading,
        expected = TestTags.Lyrics.LOADING,
    )

    @Test
    fun shows_the_empty_state_when_no_lyrics_found() = lyricsCase(
        state = LyricsUiState.None,
        expected = TestTags.Lyrics.EMPTY,
    )

    @Test
    fun shows_plain_lyrics_when_untimed() = lyricsCase(
        state = LyricsUiState.Plain("no timestamps here\njust a block"),
        expected = TestTags.Lyrics.PLAIN,
    )

    @Test
    fun shows_synced_lyrics_when_timestamped() = lyricsCase(
        state = LyricsUiState.Synced(
            lines = listOf(
                LyricLine(0L, "Lights will guide you home"),
                LyricLine(4_000L, "And ignite your bones"),
            ),
            activeIndex = 0,
        ),
        expected = TestTags.Lyrics.SYNCED,
    )

    private fun lyricsCase(state: LyricsUiState, expected: String) = runComposeUiTest {
        setContent {
            PreviewTheme {
                LyricsPage(
                    state = state,
                    trackTitle = "Fix You",
                    onSeekToLine = {},
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.Lyrics.ROOT).assertIsDisplayed()
        onNodeWithTag(expected).assertIsDisplayed()
    }
}
