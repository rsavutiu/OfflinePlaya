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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.SearchField
import com.offlineplaya.shared.presentation.ui.organisms.TrackDetailsSheet
import com.offlineplaya.shared.presentation.ui.organisms.TrackList
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Global search across track title / artist / album. The repository handles
 * the actual SQL match; the page just wires the query string + results into
 * the existing TrackList + TrackDetailsSheet pattern.
 */
@Composable
fun SearchPage(
    query: String,
    results: List<Track>,
    availablePlaylists: List<Playlist>,
    onQueryChange: (String) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track, Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { AppTopBar(title = "Search", onBack = onBack) },
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
                    label = "Title, artist, or album",
            )

            when {
                query.trim().length < 2 -> EmptyState(
                    title = "Search your library",
                    subtitle = "Type at least two characters to find a title, artist, or album.",
                )

                results.isEmpty() -> EmptyState(
                    title = "Nothing matches “${query.trim()}”",
                    subtitle = "Try a different spelling or fewer characters.",
                )

                else -> {
                    Text(
                        text = "${results.size} ${if (results.size == 1) "result" else "results"}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TrackList(
                        tracks = results,
                        onTrackClick = { selectedTrack = it },
                    )
                }
            }
        }
        }
    }

    selectedTrack?.let { track ->
        TrackDetailsSheet(
            track = track,
            availablePlaylists = availablePlaylists,
            onPlay = {
                val index = results.indexOf(track).coerceAtLeast(0)
                onPlayTracks(results, index)
                selectedTrack = null
            },
            onPlayNext = { onPlayNext(track); selectedTrack = null },
            onAddToQueue = { onAddToQueue(track); selectedTrack = null },
            onAddToPlaylist = { id -> onAddToPlaylist(track, id); selectedTrack = null },
            onDismiss = { selectedTrack = null },
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SearchPageInitialPreview() {
    PreviewTheme {
        SearchPage(
            query = "",
            results = emptyList(),
            availablePlaylists = emptyList(),
            onQueryChange = {},
            onPlayTracks = { _, _ -> },
            onPlayNext = {}, onAddToQueue = {}, onAddToPlaylist = { _, _ -> },
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
            results = listOf(
                sampleSearchTrack(1, "Once", "Pearl Jam"),
                sampleSearchTrack(2, "Even Flow", "Pearl Jam"),
            ),
            availablePlaylists = emptyList(),
            onQueryChange = {},
            onPlayTracks = { _, _ -> },
            onPlayNext = {}, onAddToQueue = {}, onAddToPlaylist = { _, _ -> },
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
            results = emptyList(),
            availablePlaylists = emptyList(),
            onQueryChange = {},
            onPlayTracks = { _, _ -> },
            onPlayNext = {}, onAddToQueue = {}, onAddToPlaylist = { _, _ -> },
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
