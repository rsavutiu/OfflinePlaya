package com.offlineplaya.android.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.QueuePage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Isolated-screen test for [QueuePage]: the queue renders its tracks and a row
 * tap routes to [onJumpTo] with that index.
 */
@OptIn(ExperimentalTestApi::class)
class QueuePageTest {

    @Test
    fun renders_queue_and_jump_routes_index() = runComposeUiTest {
        var jumpedTo = -1
        setContent {
            PreviewTheme {
                QueuePage(
                    state = PlaybackState(
                        currentTrack = track(2, "Queue Beta"),
                        isPlaying = true,
                        positionMs = 0L,
                        durationMs = 224_000L,
                        shuffleEnabled = false,
                        repeatMode = RepeatMode.OFF,
                        queue = persistentListOf(
                            track(1, "Queue Alpha"),
                            track(2, "Queue Beta"),
                            track(3, "Queue Gamma"),
                        ),
                        queueIndex = 1,
                        volume = 1f,
                    ),
                    onJumpTo = { jumpedTo = it },
                    onRemove = {},
                    onClearQueue = {},
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.Queue.ROOT).assertIsDisplayed()
        onNodeWithText("Queue Alpha").assertIsDisplayed()
        onNodeWithText("Queue Gamma").assertIsDisplayed().performClick()
        assertEquals("tapping a queue row should jump to its index", 2, jumpedTo)
    }

    @Test
    fun renders_empty_queue() = runComposeUiTest {
        setContent {
            PreviewTheme {
                QueuePage(
                    state = PlaybackState.Empty,
                    onJumpTo = {},
                    onRemove = {},
                    onClearQueue = {},
                    onBack = {},
                )
            }
        }
        onNodeWithTag(TestTags.Queue.ROOT).assertIsDisplayed()
    }

    private fun track(id: Long, title: String) = Track(
        id = id, documentUri = "u/$id", treeUri = "t", relativePath = "$title.flac",
        fileName = "$title.flac", title = title, artistName = "Artist $id",
        albumArtistName = null, albumName = "Album", genre = null, year = 1991,
        trackNumber = id.toInt(), discNumber = 1, durationMs = 224_000L, bitrate = null,
        sampleRate = null, channels = null, codec = null, artistId = id, albumId = 1L,
        folderId = 1L, scanStatus = ScanStatus.SCANNED,
    )
}
