package com.offlineplaya.android.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.tag.TagEditorStatus
import com.offlineplaya.shared.presentation.tag.TagEditorUiState
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.TagEditorPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import org.junit.Test

/**
 * Isolated-screen test for [TagEditorPage]: the editing state surfaces the
 * loaded track's file name (the form is present), and the not-found state (no
 * track) renders without it.
 */
@OptIn(ExperimentalTestApi::class)
class TagEditorPageTest {

    @Test
    fun editing_state_shows_track_file_name() = runComposeUiTest {
        setContent {
            PreviewTheme {
                TagEditorPage(
                    state = TagEditorUiState(
                        track = track("Editing Target.mp3"),
                        status = TagEditorStatus.Editing,
                    ),
                    onSave = { _, _ -> },
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.TagEditor.ROOT),
                )
            }
        }
        onNodeWithTag(TestTags.TagEditor.ROOT).assertIsDisplayed()
        onNodeWithText("Editing Target.mp3").assertIsDisplayed()
    }

    @Test
    fun not_found_state_has_no_file_name() = runComposeUiTest {
        setContent {
            PreviewTheme {
                TagEditorPage(
                    state = TagEditorUiState(track = null, status = TagEditorStatus.NotFound),
                    onSave = { _, _ -> },
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.TagEditor.ROOT),
                )
            }
        }
        onNodeWithTag(TestTags.TagEditor.ROOT).assertIsDisplayed()
        onNodeWithText("Editing Target.mp3").assertDoesNotExist()
    }

    private fun track(fileName: String) = Track(
        id = 1, documentUri = "u", treeUri = "t", relativePath = "x/$fileName",
        fileName = fileName, title = "Some Title", artistName = "Some Artist",
        albumArtistName = "Some Artist", albumName = "Some Album", genre = "Rock", year = 1975,
        trackNumber = 11, discNumber = 1, durationMs = 354_000L, bitrate = 320_000,
        sampleRate = 44_100, channels = 2, codec = "mp3", artistId = 1L, albumId = 1L,
        folderId = 1L, scanStatus = ScanStatus.SCANNED,
    )
}
