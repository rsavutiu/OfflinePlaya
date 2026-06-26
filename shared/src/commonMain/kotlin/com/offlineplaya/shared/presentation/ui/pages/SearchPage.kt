package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.SearchField
import com.offlineplaya.shared.presentation.ui.organisms.TrackList
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.search_results_count
import offlineplaya.shared.generated.resources.search_empty_subtitle
import offlineplaya.shared.generated.resources.search_empty_title
import offlineplaya.shared.generated.resources.search_field_label
import offlineplaya.shared.generated.resources.search_no_results_subtitle
import offlineplaya.shared.generated.resources.search_no_results_title
import offlineplaya.shared.generated.resources.top_bar_search
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Global search across track title / artist / album. The repository handles
 * the actual SQL match; the page just wires the query string + results into
 * the shared TrackList. Tapping a result plays it; long-pressing raises the
 * global track-actions sheet via [onTrackLongPress].
 */
@Composable
fun SearchPage(
    query: String,
    results: PersistentList<Track>,
    onQueryChange: (String) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(TestTags.Search.ROOT),
        contentWindowInsets = WindowInsets(0),
        topBar = { AppTopBar(title = stringResource(Res.string.top_bar_search), onBack = onBack) },
    ) { padding ->
        ResponsiveContent(
            modifier = Modifier
                .padding(padding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                SearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    label = stringResource(Res.string.search_field_label),
                    modifier = Modifier.testTag(TestTags.Search.FIELD),
            )

            when {
                query.trim().length < 2 -> EmptyState(
                    title = stringResource(Res.string.search_empty_title),
                    subtitle = stringResource(Res.string.search_empty_subtitle),
                    modifier = Modifier.testTag(TestTags.Search.PROMPT),
                )

                results.isEmpty() -> EmptyState(
                    title = stringResource(Res.string.search_no_results_title, query.trim()),
                    subtitle = stringResource(Res.string.search_no_results_subtitle),
                    modifier = Modifier.testTag(TestTags.Search.NO_RESULTS),
                )

                else -> {
                    Text(
                        text = pluralStringResource(Res.plurals.search_results_count, results.size, results.size),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TrackList(
                        tracks = results,
                        onTrackClick = { onPlayTracks(results, results.indexOf(it).coerceAtLeast(0)) },
                        onTrackLongPress = onTrackLongPress,
                        modifier = Modifier.testTag(TestTags.Search.RESULTS),
                    )
                }
            }
        }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SearchPageInitialPreview() {
    PreviewTheme {
        SearchPage(
            query = "",
            results = persistentListOf(),
            onQueryChange = {},
            onPlayTracks = { _, _ -> },
            onTrackLongPress = {},
            onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SearchPageResultsPreview() {
    PreviewTheme {
        SearchPage(
            query = "Pearl",
            results = persistentListOf(
                sampleSearchTrack(1, "Once", "Pearl Jam"),
                sampleSearchTrack(2, "Even Flow", "Pearl Jam"),
            ),
            onQueryChange = {},
            onPlayTracks = { _, _ -> },
            onTrackLongPress = {},
            onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SearchPageNoMatchesPreview() {
    PreviewTheme(darkTheme = true) {
        SearchPage(
            query = "zzz",
            results = persistentListOf(),
            onQueryChange = {},
            onPlayTracks = { _, _ -> },
            onTrackLongPress = {},
            onBack = {},
        )
    }
}

private fun sampleSearchTrack(id: Long, title: String, artist: String) = Track(
    id = id,
    documentUri = "u/$id",
    treeUri = "t",
    relativePath = "$artist/Ten/$title.flac",
    fileName = "$title.flac",
    title = title,
    artistName = artist,
    albumArtistName = null,
    albumName = "Ten",
    genre = "Rock",
    year = 1991,
    trackNumber = id.toInt(),
    discNumber = 1,
    durationMs = 224_000L,
    bitrate = 1_000_000,
    sampleRate = 44_100,
    channels = 2,
    codec = "flac",
    artistId = 1L,
    albumId = 1L,
    folderId = 1L,
    scanStatus = ScanStatus.SCANNED,
)
