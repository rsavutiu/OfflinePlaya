package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.organisms.TrackDetailsSheet
import com.offlineplaya.shared.presentation.ui.organisms.TrackList
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Single-playlist detail: header has Play-all + Delete actions, body is the
 * track list. Tapping a track opens the details sheet (Play sets the queue
 * to the entire playlist from that track).
 */
@Composable
fun PlaylistDetailPage(
    playlistName: String,
    tracks: List<Track>,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            AppTopBar(
                title = playlistName,
                onBack = onBack,
                actions = {
                    if (tracks.isNotEmpty()) {
                        IconButton(onClick = { onPlayTracks(tracks, 0) }) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play all")
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete playlist")
                    }
                },
            )
        },
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
private fun PlaylistDetailPagePopulatedPreview() {
    PreviewTheme {
        PlaylistDetailPage(
            playlistName = "Morning Run",
            tracks = listOf(
                samplePlaylistTrack(1, "Once", "Pearl Jam"),
                samplePlaylistTrack(2, "Cherub Rock", "Smashing Pumpkins"),
            ),
            onPlayTracks = { _, _ -> },
            onDelete = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun PlaylistDetailPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        PlaylistDetailPage(
            playlistName = "Empty",
            tracks = emptyList(),
            onPlayTracks = { _, _ -> },
            onDelete = {},
            onBack = {},
        )
    }
}

private fun samplePlaylistTrack(id: Long, title: String, artist: String) = Track(
    id = id,
    documentUri = "u/$id",
    treeUri = "t",
    relativePath = "$artist/$title.flac",
    fileName = "$title.flac",
    title = title,
    artistName = artist,
    albumArtistName = null,
    albumName = "Sample",
    genre = null,
    year = 1991,
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
