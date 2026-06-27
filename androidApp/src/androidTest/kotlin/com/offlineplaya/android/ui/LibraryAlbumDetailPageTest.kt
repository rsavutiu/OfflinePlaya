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
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.LibraryAlbumDetailPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Isolated-screen test for [LibraryAlbumDetailPage]: the track listing renders
 * and tapping a row plays the album from that track's index.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryAlbumDetailPageTest {

    @Test
    fun renders_tracks_and_tap_plays_from_index() = runComposeUiTest {
        var playedFrom = -1
        var playedCount = 0
        setContent {
            PreviewTheme {
                LibraryAlbumDetailPage(
                    album = Album(1, "Tenacity", 1, 1991, 3, 672_000),
                    artistName = "Stone Gossard",
                    representativeTrack = null,
                    tracks = persistentListOf(
                        track(1, 1, "Album Song One"),
                        track(2, 2, "Album Song Two"),
                        track(3, 3, "Album Song Three"),
                    ),
                    onPlayTracks = { tracks, index -> playedCount = tracks.size; playedFrom = index },
                    onTrackLongPress = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.AlbumDetail.ROOT),
                )
            }
        }

        onNodeWithTag(TestTags.AlbumDetail.ROOT).assertIsDisplayed()
        onNodeWithText("Album Song One").assertIsDisplayed()
        onNodeWithText("Album Song Three").assertIsDisplayed().performClick()
        assertEquals("row tap should play the full album list", 3, playedCount)
        assertEquals("row tap should start from that track's index", 2, playedFrom)
    }

    @Test
    fun renders_empty_album() = runComposeUiTest {
        setContent {
            PreviewTheme {
                LibraryAlbumDetailPage(
                    album = Album(1, "Empty Album", 1, null, 0, 0),
                    artistName = "Nobody",
                    representativeTrack = null,
                    tracks = persistentListOf(),
                    onPlayTracks = { _, _ -> },
                    onTrackLongPress = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.AlbumDetail.ROOT),
                )
            }
        }
        onNodeWithTag(TestTags.AlbumDetail.ROOT).assertIsDisplayed()
    }

    private fun track(id: Long, n: Int, title: String) = Track(
        id = id, documentUri = "u/$id", treeUri = "t", relativePath = "x/$title.flac",
        fileName = "$title.flac", title = title, artistName = "Stone Gossard",
        albumArtistName = null, albumName = "Tenacity", genre = "Rock", year = 1991,
        trackNumber = n, discNumber = 1, durationMs = 224_000L, bitrate = null,
        sampleRate = null, channels = null, codec = null, artistId = 1L, albumId = 1L,
        folderId = 1L, scanStatus = ScanStatus.SCANNED,
    )
}
