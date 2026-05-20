package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.PlaylistNameDialog
import com.offlineplaya.shared.presentation.ui.organisms.TrackDetailsSheet
import com.offlineplaya.shared.presentation.ui.organisms.TrackList
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Single-playlist detail: top bar has Play-all + Rename + Delete actions,
 * body is the track list. Tapping a track opens the details sheet (Play
 * sets the queue to the entire playlist from that track).
 */
@Composable
fun PlaylistDetailPage(
    playlistName: String,
    tracks: List<Track>,
    availablePlaylists: List<Playlist>,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track, Long) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename playlist")
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
            contentPadding = padding,
        )
    }

    selectedTrack?.let { track ->
        TrackDetailsSheet(
            track = track,
            availablePlaylists = availablePlaylists,
            onPlay = {
                val index = tracks.indexOf(track).coerceAtLeast(0)
                onPlayTracks(tracks, index)
                selectedTrack = null
            },
            onPlayNext = { onPlayNext(track); selectedTrack = null },
            onAddToQueue = { onAddToQueue(track); selectedTrack = null },
            onAddToPlaylist = { id -> onAddToPlaylist(track, id); selectedTrack = null },
            onDismiss = { selectedTrack = null },
        )
    }

    if (showRenameDialog) {
        PlaylistNameDialog(
            title = "Rename playlist",
            confirmLabel = "Rename",
            initialName = playlistName,
            onConfirm = onRename,
            onDismiss = { showRenameDialog = false },
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
            availablePlaylists = emptyList(),
            onPlayTracks = { _, _ -> },
            onPlayNext = {}, onAddToQueue = {}, onAddToPlaylist = { _, _ -> },
            onRename = {},
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
            availablePlaylists = emptyList(),
            onPlayTracks = { _, _ -> },
            onPlayNext = {}, onAddToQueue = {}, onAddToPlaylist = { _, _ -> },
            onRename = {},
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
