package com.offlineplaya.android.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.LibraryArtistDetailPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Isolated-screen test for [LibraryArtistDetailPage]: the artist's albums grid
 * renders and tapping an album routes to [onAlbumClick] with its id.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryArtistDetailPageTest {

    @Test
    fun renders_albums_and_tap_routes_album_id() = runComposeUiTest {
        var clickedAlbumId = -1L
        setContent {
            PreviewTheme {
                LibraryArtistDetailPage(
                    artist = Artist(1, "Mother Love Bone", 2, 24),
                    albums = persistentListOf(
                        Album(10, "Apple Crate", artistId = 1, year = 1990, trackCount = 11, durationMs = 0),
                        Album(20, "Shine On", artistId = 1, year = 1989, trackCount = 5, durationMs = 0),
                    ),
                    onAlbumClick = { clickedAlbumId = it },
                    onPlayAlbum = {},
                    onPlayArtist = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.ArtistDetail.ROOT),
                )
            }
        }

        onNodeWithTag(TestTags.ArtistDetail.ROOT).assertIsDisplayed()
        onNodeWithText("Apple Crate").assertIsDisplayed()
        onNodeWithText("Shine On").assertIsDisplayed().performClick()
        assertEquals("album tap should route its id", 20L, clickedAlbumId)
    }

    @Test
    fun renders_artist_with_no_albums() = runComposeUiTest {
        setContent {
            PreviewTheme {
                LibraryArtistDetailPage(
                    artist = Artist(1, "Lonesome Act", 0, 0),
                    albums = persistentListOf(),
                    onAlbumClick = {},
                    onPlayAlbum = {},
                    onPlayArtist = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.ArtistDetail.ROOT),
                )
            }
        }
        onNodeWithTag(TestTags.ArtistDetail.ROOT).assertIsDisplayed()
    }
}
