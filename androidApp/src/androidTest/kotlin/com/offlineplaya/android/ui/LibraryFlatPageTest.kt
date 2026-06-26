package com.offlineplaya.android.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.LibraryFlatPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

/**
 * Isolated-screen test for [LibraryFlatPage]: param-wired with a fixed track
 * list (no Koin). Track titles are data (not localized chrome), so they are
 * asserted by text; the page root is asserted by tag for navigation use.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryFlatPageTest {

    @Test
    fun renders_the_given_tracks() = runComposeUiTest {
        setContent {
            PreviewTheme {
                LibraryFlatPage(
                    tracks = persistentListOf(
                        flatTrack(1, "Aphex Twin", "Alberto Balsalm"),
                        flatTrack(2, "Pearl Jam", "Once"),
                    ),
                    onPlayTracks = { _, _ -> },
                    onTrackLongPress = {},
                    onTabSelected = {},
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.Flat.ROOT).assertIsDisplayed()
        onNodeWithText("Alberto Balsalm").assertIsDisplayed()
        onNodeWithText("Once").assertIsDisplayed()
    }

    @Test
    fun shows_no_track_rows_when_the_library_is_empty() = runComposeUiTest {
        setContent {
            PreviewTheme {
                LibraryFlatPage(
                    tracks = persistentListOf(),
                    onPlayTracks = { _, _ -> },
                    onTrackLongPress = {},
                    onTabSelected = {},
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.Flat.ROOT).assertIsDisplayed()
        onNodeWithText("Once").assertDoesNotExist()
    }
}

private fun flatTrack(id: Long, artist: String, title: String) = Track(
    id = id,
    documentUri = "u/$id",
    treeUri = "t",
    relativePath = "$artist/$title.flac",
    fileName = "$title.flac",
    title = title,
    artistName = artist,
    albumArtistName = null,
    albumName = "Sample Album",
    genre = null,
    year = 1995,
    trackNumber = id.toInt(),
    discNumber = 1,
    durationMs = 240_000L,
    bitrate = 1_000_000,
    sampleRate = 44_100,
    channels = 2,
    codec = "flac",
    artistId = id,
    albumId = id,
    folderId = id,
    scanStatus = ScanStatus.SCANNED,
)
