package com.offlineplaya.android.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.LibraryFolderDetailPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Isolated-screen test for [LibraryFolderDetailPage]: subfolders + tracks render
 * and tapping a subfolder routes to [onFolderClick].
 */
@OptIn(ExperimentalTestApi::class)
class LibraryFolderDetailPageTest {

    @Test
    fun renders_subfolders_and_tracks_and_tap_subfolder_routes() = runComposeUiTest {
        var clickedFolderId = -1L
        setContent {
            PreviewTheme {
                LibraryFolderDetailPage(
                    folderName = "Live Sets",
                    subfolders = persistentListOf(
                        Folder(10, "t", "Live Sets/Summer", "Summer Tour", 1L, 11),
                        Folder(11, "t", "Live Sets/Winter", "Winter Tour", 1L, 12),
                    ),
                    tracks = persistentListOf(
                        track(1, "Loose Track One"),
                    ),
                    onFolderClick = { clickedFolderId = it },
                    onPlayTracks = { _, _ -> },
                    onTrackLongPress = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.FolderDetail.ROOT),
                )
            }
        }

        onNodeWithTag(TestTags.FolderDetail.ROOT).assertIsDisplayed()
        onNodeWithText("Loose Track One").assertIsDisplayed()
        onNodeWithText("Summer Tour").assertIsDisplayed()
        onNodeWithText("Winter Tour").assertIsDisplayed().performClick()
        assertEquals("subfolder tap should route its id", 11L, clickedFolderId)
    }

    @Test
    fun renders_empty_folder() = runComposeUiTest {
        setContent {
            PreviewTheme {
                LibraryFolderDetailPage(
                    folderName = "Empty Folder",
                    subfolders = persistentListOf(),
                    tracks = persistentListOf(),
                    onFolderClick = {},
                    onPlayTracks = { _, _ -> },
                    onTrackLongPress = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.FolderDetail.ROOT),
                )
            }
        }
        onNodeWithTag(TestTags.FolderDetail.ROOT).assertIsDisplayed()
    }

    private fun track(id: Long, title: String) = Track(
        id = id, documentUri = "u/$id", treeUri = "t", relativePath = "Live Sets/$title.flac",
        fileName = "$title.flac", title = title, artistName = "Artist", albumArtistName = null,
        albumName = "Album", genre = null, year = 1991, trackNumber = id.toInt(), discNumber = 1,
        durationMs = 224_000L, bitrate = null, sampleRate = null, channels = null, codec = null,
        artistId = 1L, albumId = 1L, folderId = 1L, scanStatus = ScanStatus.SCANNED,
    )
}
