package com.offlineplaya.android.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.SearchPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

/**
 * First isolated-screen test of the instrumentation plan: drives [SearchPage]
 * with fake state (param-wired — no Koin) and asserts its three states via the
 * tags in [TestTags.Search].
 *
 * Lives in androidApp/androidTest for now; the body uses only portable APIs
 * (runComposeUiTest + tag finders + the commonMain page/theme) so it can move
 * to shared/commonTest unchanged once that path is set up.
 */
@OptIn(ExperimentalTestApi::class)
class SearchPageTest {

    @Test
    fun prompt_is_shown_when_the_query_is_too_short() = runComposeUiTest {
        setContent {
            PreviewTheme { searchPage(query = "", results = persistentListOf()) }
        }

        onNodeWithTag(TestTags.Search.FIELD).assertIsDisplayed()
        onNodeWithTag(TestTags.Search.PROMPT).assertIsDisplayed()
        onNodeWithTag(TestTags.Search.RESULTS).assertDoesNotExist()
        onNodeWithTag(TestTags.Search.NO_RESULTS).assertDoesNotExist()
    }

    @Test
    fun results_are_shown_for_a_matching_query() = runComposeUiTest {
        setContent {
            PreviewTheme {
                searchPage(
                    query = "pearl",
                    results = persistentListOf(track(1, "Once"), track(2, "Even Flow")),
                )
            }
        }

        onNodeWithTag(TestTags.Search.RESULTS).assertIsDisplayed()
        onNodeWithTag(TestTags.Search.PROMPT).assertDoesNotExist()
        onNodeWithTag(TestTags.Search.NO_RESULTS).assertDoesNotExist()
    }

    @Test
    fun no_results_state_for_a_query_that_matches_nothing() = runComposeUiTest {
        setContent {
            PreviewTheme { searchPage(query = "zzz", results = persistentListOf()) }
        }

        onNodeWithTag(TestTags.Search.NO_RESULTS).assertIsDisplayed()
        onNodeWithTag(TestTags.Search.RESULTS).assertDoesNotExist()
        onNodeWithTag(TestTags.Search.PROMPT).assertDoesNotExist()
    }
}

@androidx.compose.runtime.Composable
private fun searchPage(query: String, results: kotlinx.collections.immutable.PersistentList<Track>) {
    SearchPage(
        query = query,
        results = results,
        onQueryChange = {},
        onPlayTracks = { _, _ -> },
        onTrackLongPress = {},
        onBack = {},
    )
}

private fun track(id: Long, title: String) = Track(
    id = id,
    documentUri = "u/$id",
    treeUri = "t",
    relativePath = "Pearl Jam/Ten/$title.flac",
    fileName = "$title.flac",
    title = title,
    artistName = "Pearl Jam",
    albumArtistName = null,
    albumName = "Ten",
    genre = "Rock",
    year = 1991,
    trackNumber = id.toInt(),
    discNumber = 1,
    durationMs = 224_000L,
    bitrate = null,
    sampleRate = null,
    channels = null,
    codec = "flac",
    artistId = 1L,
    albumId = 1L,
    folderId = 1L,
    scanStatus = ScanStatus.SCANNED,
)
