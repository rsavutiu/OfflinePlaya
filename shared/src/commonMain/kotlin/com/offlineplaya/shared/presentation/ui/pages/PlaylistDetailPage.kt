package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.PlaylistNameDialog
import com.offlineplaya.shared.presentation.ui.molecules.TrackRow
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.formatDuration
import com.offlineplaya.shared.presentation.ui.organisms.TrackDetailsSheet
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

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
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename playlist")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete playlist")
                    }
                },
            )
        },
        floatingActionButton = {
            if (tracks.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onPlayTracks(tracks, 0) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text("Play All") },
                )
            }
        },
    ) { padding ->
        if (tracks.isEmpty()) {
            EmptyState(
                title = "No tracks yet",
                subtitle = "Add tracks from the library using the menu on any track.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                item(key = "header") {
                    PlaylistHeader(
                        name = playlistName,
                        trackCount = tracks.size,
                        totalDurationMs = tracks.sumOf { it.durationMs ?: 0L },
                    )
                }

                itemsIndexed(items = tracks, key = { _, t -> t.id }) { _, track ->
                    TrackRow(
                        track = track,
                        onClick = { selectedTrack = track },
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
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

@Composable
private fun PlaylistHeader(
    name: String,
    trackCount: Int,
    totalDurationMs: Long,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.size(120.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
        Spacer(Modifier.height(AppSpacing.lg))
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(AppSpacing.sm))
        val countText = "$trackCount ${if (trackCount == 1) "track" else "tracks"}"
        val durationMinutes = totalDurationMs / 60_000
        val durationText = if (durationMinutes >= 60) {
            "${durationMinutes / 60}h ${durationMinutes % 60}m"
        } else {
            "${durationMinutes}m"
        }
        Text(
            text = "$countText · $durationText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(AppSpacing.md))
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
