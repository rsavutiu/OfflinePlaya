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
import com.offlineplaya.shared.domain.model.SmartPlaylistKind
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.SmartPlaylistPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Isolated-screen test for [SmartPlaylistPage]: the live track list renders and
 * a row tap plays the list from that index (the playlist *is* the queue).
 */
@OptIn(ExperimentalTestApi::class)
class SmartPlaylistPageTest {

    @Test
    fun renders_tracks_and_tap_plays_from_index() = runComposeUiTest {
        var playedFrom = -1
        setContent {
            PreviewTheme {
                SmartPlaylistPage(
                    kind = SmartPlaylistKind.MOST_PLAYED,
                    tracks = persistentListOf(
                        track(1, "Smart Song One"),
                        track(2, "Smart Song Two"),
                    ),
                    onPlayTracks = { _, index -> playedFrom = index },
                    onTrackLongPress = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.SmartPlaylist.ROOT),
                )
            }
        }

        onNodeWithTag(TestTags.SmartPlaylist.ROOT).assertIsDisplayed()
        onNodeWithText("Smart Song One").assertIsDisplayed()
        onNodeWithText("Smart Song Two").assertIsDisplayed().performClick()
        assertEquals("row tap should play from that track's index", 1, playedFrom)
    }

    @Test
    fun renders_empty_smart_playlist() = runComposeUiTest {
        setContent {
            PreviewTheme {
                SmartPlaylistPage(
                    kind = SmartPlaylistKind.NEVER_PLAYED,
                    tracks = persistentListOf(),
                    onPlayTracks = { _, _ -> },
                    onTrackLongPress = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.SmartPlaylist.ROOT),
                )
            }
        }
        onNodeWithTag(TestTags.SmartPlaylist.ROOT).assertIsDisplayed()
    }

    private fun track(id: Long, title: String) = Track(
        id = id, documentUri = "u/$id", treeUri = "t", relativePath = "x/$title.mp3",
        fileName = "$title.mp3", title = title, artistName = "Artist $id", albumArtistName = null,
        albumName = "Album", genre = "Rock", year = 1975, trackNumber = id.toInt(), discNumber = 1,
        durationMs = 354_000L, bitrate = null, sampleRate = null, channels = null, codec = null,
        artistId = id, albumId = id, folderId = id, scanStatus = ScanStatus.SCANNED,
    )
}
