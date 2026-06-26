package com.offlineplaya.android.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.LibraryArtistsPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

/**
 * Isolated-screen test for [LibraryArtistsPage]: param-wired with a fixed
 * artist list (no Koin). Artist names are data (not localized chrome), so they
 * are asserted by text; the page root is asserted by tag for navigation use.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryArtistsPageTest {

    @Test
    fun renders_the_given_artists() = runComposeUiTest {
        setContent {
            PreviewTheme {
                LibraryArtistsPage(
                    artists = persistentListOf(
                        Artist(1, "Aphex Twin", albumCount = 6, trackCount = 92),
                        Artist(2, "Pearl Jam", albumCount = 11, trackCount = 142),
                    ),
                    onArtistClick = {},
                    onPlayArtist = {},
                    onTabSelected = {},
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.Artists.ROOT).assertIsDisplayed()
        onNodeWithText("Aphex Twin").assertIsDisplayed()
        onNodeWithText("Pearl Jam").assertIsDisplayed()
    }

    @Test
    fun shows_no_artist_rows_when_the_library_is_empty() = runComposeUiTest {
        setContent {
            PreviewTheme {
                LibraryArtistsPage(
                    artists = persistentListOf(),
                    onArtistClick = {},
                    onPlayArtist = {},
                    onTabSelected = {},
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.Artists.ROOT).assertIsDisplayed()
        onNodeWithText("Pearl Jam").assertDoesNotExist()
    }
}
