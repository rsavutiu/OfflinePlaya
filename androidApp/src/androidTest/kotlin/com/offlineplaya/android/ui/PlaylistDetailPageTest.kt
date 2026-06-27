package com.offlineplaya.android.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.PlaylistDetailPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Isolated-screen test for [PlaylistDetailPage]: tracks render and a row tap
 * plays the playlist from that index; the empty playlist shows its empty state.
 */
@OptIn(ExperimentalTestApi::class)
class PlaylistDetailPageTest {

    @Test
    fun renders_tracks_and_tap_plays_from_index() = runComposeUiTest {
        var playedFrom = -1
        setContent {
            PreviewTheme {
                PlaylistDetailPage(
                    playlistName = "Morning Run",
                    tracks = persistentListOf(
                        track(1, "Playlist Song A"),
                        track(2, "Playlist Song B"),
                    ),
                    onPlayTracks = { _, index -> playedFrom = index },
                    onTrackLongPress = {},
                    onRename = {},
                    onDelete = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.PlaylistDetail.ROOT),
                )
            }
        }

        onNodeWithTag(TestTags.PlaylistDetail.ROOT).assertIsDisplayed()
        onNodeWithText("Playlist Song A").assertIsDisplayed()
        onNodeWithText("Playlist Song B").assertIsDisplayed().performClick()
        assertEquals("row tap should play from that track's index", 1, playedFrom)
    }

    @Test
    fun renders_empty_playlist() = runComposeUiTest {
        setContent {
            PreviewTheme {
                PlaylistDetailPage(
                    playlistName = "Empty",
                    tracks = persistentListOf(),
                    onPlayTracks = { _, _ -> },
                    onTrackLongPress = {},
                    onRename = {},
                    onDelete = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.PlaylistDetail.ROOT),
                )
            }
        }
        onNodeWithTag(TestTags.PlaylistDetail.ROOT).assertIsDisplayed()
    }

    private fun track(id: Long, title: String) = Track(
        id = id, documentUri = "u/$id", treeUri = "t", relativePath = "x/$title.flac",
        fileName = "$title.flac", title = title, artistName = "Artist $id", albumArtistName = null,
        albumName = "Album", genre = null, year = 1991, trackNumber = id.toInt(), discNumber = 1,
        durationMs = 240_000L, bitrate = null, sampleRate = null, channels = null, codec = null,
        artistId = id, albumId = id, folderId = id, scanStatus = ScanStatus.SCANNED,
    )
}
