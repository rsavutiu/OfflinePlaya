package com.offlineplaya.android.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.ListeningStats
import com.offlineplaya.shared.domain.model.PlayedTrack
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.StatsPeriod
import com.offlineplaya.shared.domain.model.TopAlbumStat
import com.offlineplaya.shared.domain.model.TopArtistStat
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.ListeningStatsPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Isolated-screen test for [ListeningStatsPage]: headline totals + top lists
 * render with plays > 0, a top-artist tap routes to [onArtistClick], and the
 * zero-plays case falls back to the empty state (no totals).
 */
@OptIn(ExperimentalTestApi::class)
class ListeningStatsPageTest {

    @Test
    fun renders_totals_and_top_artist_tap_routes() = runComposeUiTest {
        var clickedArtistId = -1L
        setContent {
            PreviewTheme {
                ListeningStatsPage(
                    period = StatsPeriod.WEEK,
                    stats = ListeningStats(plays = 230, distinctTracks = 74, listenedMs = 13_980_000L),
                    topArtists = persistentListOf(
                        TopArtistStat(1, "Headline Artist", 58),
                        TopArtistStat(2, "Runner Up Artist", 31),
                    ),
                    topAlbums = persistentListOf(
                        TopAlbumStat(10, "Top Album", "Album Maker", 40),
                    ),
                    topTracks = persistentListOf(
                        PlayedTrack(track("Stat Track A"), 12),
                    ),
                    onPeriodChange = {},
                    onArtistClick = { clickedArtistId = it },
                    onAlbumClick = {},
                    onPlayTopTrack = {},
                    onTrackLongPress = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.Stats.ROOT),
                )
            }
        }

        onNodeWithTag(TestTags.Stats.ROOT).assertIsDisplayed()
        onNodeWithText("230").assertIsDisplayed() // total plays headline
        onNodeWithText("Headline Artist").assertIsDisplayed().performClick()
        assertEquals("top-artist tap should route its id", 1L, clickedArtistId)
    }

    @Test
    fun zero_plays_shows_empty_state_without_totals() = runComposeUiTest {
        setContent {
            PreviewTheme {
                ListeningStatsPage(
                    period = StatsPeriod.ALL,
                    stats = ListeningStats.Empty,
                    topArtists = persistentListOf(),
                    topAlbums = persistentListOf(),
                    topTracks = persistentListOf(),
                    onPeriodChange = {},
                    onArtistClick = {},
                    onAlbumClick = {},
                    onPlayTopTrack = {},
                    onTrackLongPress = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.Stats.ROOT),
                )
            }
        }
        onNodeWithTag(TestTags.Stats.ROOT).assertIsDisplayed()
        onNodeWithText("Headline Artist").assertDoesNotExist()
    }

    private fun track(title: String) = Track(
        id = title.hashCode().toLong(), documentUri = "u/$title", treeUri = "t",
        relativePath = "x/$title", fileName = "$title.mp3", title = title,
        artistName = "Track Artist", albumArtistName = null, albumName = "Album", genre = null,
        year = 1991, trackNumber = null, discNumber = null, durationMs = 224_000L, bitrate = null,
        sampleRate = null, channels = null, codec = null, artistId = 1L, albumId = 1L,
        folderId = 1L, scanStatus = ScanStatus.SCANNED,
    )
}
