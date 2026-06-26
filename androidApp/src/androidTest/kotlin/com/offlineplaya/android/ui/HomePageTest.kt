package com.offlineplaya.android.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.HomePage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Isolated-screen test for [HomePage]: param-wired (no Koin). The recently-
 * played shelf renders when albums are supplied, and each browse-grid card
 * routes to its open-facet callback (the home → library navigation hop).
 */
@OptIn(ExperimentalTestApi::class)
class HomePageTest {

    @Test
    fun shows_stat_strip_recent_shelf_and_routes_browse_and_stats() = runComposeUiTest {
        var openedAlbums = 0
        var openedArtists = 0
        var openedStats = 0
        setContent {
            PreviewTheme {
                HomePage(
                    status = SyncStatus.Idle,
                    trackCount = 212,
                    folderCount = 22,
                    albumCount = 18,
                    artistCount = 12,
                    playlistCount = 4,
                    recentAlbums = persistentListOf(
                        Album(1, "Ten", 4, 1991, 11, 3_320_000),
                        Album(2, "A Moon Shaped Pool", 2, 2016, 11, 3_240_000),
                    ),
                    onOpenLibrary = {},
                    onOpenAllTracks = {},
                    onOpenAlbums = { openedAlbums++ },
                    onOpenArtists = { openedArtists++ },
                    onOpenPlaylists = {},
                    onOpenStats = { openedStats++ },
                    onOpenSearch = {},
                    onOpenSettings = {},
                )
            }
        }

        onNodeWithTag(TestTags.Home.ROOT).assertIsDisplayed()
        onNodeWithTag(TestTags.Home.STAT_STRIP).assertIsDisplayed()
        onNodeWithTag(TestTags.Home.RECENT_SHELF).assertIsDisplayed()

        onNodeWithTag(TestTags.Home.BROWSE_ALBUMS).assertIsDisplayed().performClick()
        assertEquals("albums card should route to onOpenAlbums", 1, openedAlbums)

        onNodeWithTag(TestTags.Home.BROWSE_ARTISTS).assertIsDisplayed().performClick()
        assertEquals("artists card should route to onOpenArtists", 1, openedArtists)

        // The total stat cell is the only route to the listening-stats page.
        onNodeWithTag(TestTags.Home.STAT_TOTAL).assertIsDisplayed().performClick()
        assertEquals("total stat cell should route to onOpenStats", 1, openedStats)
    }

    @Test
    fun omits_recent_shelf_when_no_recent_albums() = runComposeUiTest {
        setContent {
            PreviewTheme {
                HomePage(
                    status = SyncStatus.Idle,
                    trackCount = 0,
                    folderCount = 0,
                    albumCount = 0,
                    artistCount = 0,
                    playlistCount = 0,
                    recentAlbums = persistentListOf(),
                    onOpenLibrary = {},
                    onOpenAllTracks = {},
                    onOpenAlbums = {},
                    onOpenArtists = {},
                    onOpenPlaylists = {},
                    onOpenSearch = {},
                    onOpenSettings = {},
                )
            }
        }

        onNodeWithTag(TestTags.Home.ROOT).assertIsDisplayed()
        onNodeWithTag(TestTags.Home.RECENT_SHELF).assertDoesNotExist()
        onNodeWithTag(TestTags.Home.BROWSE_TRACKS).assertIsDisplayed()
    }
}
