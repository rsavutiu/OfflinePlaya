package com.offlineplaya.android.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.SmartPlaylistKind
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.PlaylistsPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

/**
 * Isolated-screen test for [PlaylistsPage]: param-wired (no Koin). With the
 * smart-playlist callback supplied, all five [SmartPlaylistKind] rows render
 * above the user's own playlists (asserted by name — playlist names are data).
 */
@OptIn(ExperimentalTestApi::class)
class PlaylistsPageTest {

    @Test
    fun renders_five_smart_rows_and_the_user_playlists() = runComposeUiTest {
        setContent {
            PreviewTheme {
                PlaylistsPage(
                    playlists = persistentListOf(
                        Playlist(1, "Morning Run", 0, 0),
                        Playlist(2, "Focus", 0, 0),
                    ),
                    onPlaylistClick = {},
                    onCreate = {},
                    onBack = {},
                    onSmartPlaylistClick = {},
                )
            }
        }

        onNodeWithTag(TestTags.Playlists.ROOT).assertIsDisplayed()
        onAllNodesWithTag(TestTags.Playlists.SMART_ROW)
            .assertCountEquals(SmartPlaylistKind.entries.size)
        onNodeWithText("Morning Run").assertIsDisplayed()
        onNodeWithText("Focus").assertIsDisplayed()
    }

    @Test
    fun omits_smart_rows_when_no_smart_callback() = runComposeUiTest {
        setContent {
            PreviewTheme {
                PlaylistsPage(
                    playlists = persistentListOf(Playlist(1, "Morning Run", 0, 0)),
                    onPlaylistClick = {},
                    onCreate = {},
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.Playlists.ROOT).assertIsDisplayed()
        onAllNodesWithTag(TestTags.Playlists.SMART_ROW).assertCountEquals(0)
        onNodeWithText("Morning Run").assertIsDisplayed()
    }
}
