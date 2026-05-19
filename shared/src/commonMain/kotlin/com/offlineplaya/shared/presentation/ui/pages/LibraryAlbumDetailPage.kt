package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.organisms.TrackDetailsSheet
import com.offlineplaya.shared.presentation.ui.organisms.TrackList
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun LibraryAlbumDetailPage(
    albumTitle: String,
    tracks: List<Track>,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = albumTitle, onBack = onBack) },
    ) { padding ->
        TrackList(
            tracks = tracks,
            onTrackClick = { selectedTrack = it },
            modifier = Modifier.padding(padding),
        )
    }

    selectedTrack?.let { track ->
        TrackDetailsSheet(
            track = track,
            onPlay = {
                val index = tracks.indexOf(track).coerceAtLeast(0)
                onPlayTracks(tracks, index)
                selectedTrack = null
            },
            onDismiss = { selectedTrack = null },
        )
    }
}

@Preview
@Composable
private fun LibraryAlbumDetailPagePreview() {
    PreviewTheme {
        LibraryAlbumDetailPage(
            albumTitle = "Ten",
            tracks = listOf(
                sampleTrack(1, 1, "Once"),
                sampleTrack(2, 2, "Even Flow"),
                sampleTrack(3, 3, "Alive"),
            ),
            onPlayTracks = { _, _ -> },
            onBack = {},
        )
    }
}

private fun sampleTrack(id: Long, n: Int, title: String) = Track(
    id = id,
    documentUri = "u/$id",
    treeUri = "tree",
    relativePath = "Pearl Jam/Ten/$title.flac",
    fileName = "$title.flac",
    title = title,
    artistName = "Pearl Jam",
    albumArtistName = null,
    albumName = "Ten",
    genre = "Rock",
    year = 1991,
    trackNumber = n,
    discNumber = 1,
    durationMs = 224_000L,
    bitrate = 1_000_000,
    sampleRate = 44_100,
    channels = 2,
    codec = "flac",
    artistId = 1L,
    albumId = 1L,
    folderId = 1L,
    scanStatus = com.offlineplaya.shared.domain.model.ScanStatus.SCANNED,
)
