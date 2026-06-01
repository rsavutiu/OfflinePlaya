package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.offlineplaya.shared.presentation.ui.molecules.PlaylistNameDialog
import com.offlineplaya.shared.presentation.ui.molecules.TrackRow
import com.offlineplaya.shared.presentation.ui.organisms.PlaylistDetailHeader
import com.offlineplaya.shared.presentation.ui.organisms.TrackDetailsSheet
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.cd_delete_playlist
import offlineplaya.shared.generated.resources.cd_rename_playlist
import offlineplaya.shared.generated.resources.empty_playlist_tracks_subtitle
import offlineplaya.shared.generated.resources.empty_playlist_tracks_title
import offlineplaya.shared.generated.resources.playlist_dialog_rename
import offlineplaya.shared.generated.resources.playlist_dialog_rename_confirm
import offlineplaya.shared.generated.resources.playlist_play_all
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlaylistDetailPage(
    playlistName: String,
    tracks: PersistentList<Track>,
    availablePlaylists: PersistentList<Playlist>,
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
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = playlistName,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.cd_rename_playlist)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.cd_delete_playlist)
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (tracks.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onPlayTracks(tracks, 0) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text(stringResource(Res.string.playlist_play_all)) },
                )
            }
        },
    ) { padding ->
        if (tracks.isEmpty()) {
            EmptyState(
                title = stringResource(Res.string.empty_playlist_tracks_title),
                subtitle = stringResource(Res.string.empty_playlist_tracks_subtitle),
                modifier = Modifier.padding(padding),
            )
        } else {
            ResponsiveContent(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "header") {
                    PlaylistDetailHeader(
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
            title = stringResource(Res.string.playlist_dialog_rename),
            confirmLabel = stringResource(Res.string.playlist_dialog_rename_confirm),
            initialName = playlistName,
            onConfirm = onRename,
            onDismiss = { showRenameDialog = false },
        )
    }
}

@PreviewScreenSizes
@Composable
private fun PlaylistDetailPagePopulatedPreview() {
    PreviewTheme {
        PlaylistDetailPage(
            playlistName = "Morning Run",
            tracks = persistentListOf(
                samplePlaylistTrack(1, "Once", "Pearl Jam"),
                samplePlaylistTrack(2, "Cherub Rock", "Smashing Pumpkins"),
            ),
            availablePlaylists = persistentListOf(),
            onPlayTracks = { _, _ -> },
            onPlayNext = {}, onAddToQueue = {}, onAddToPlaylist = { _, _ -> },
            onRename = {},
            onDelete = {},
            onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun PlaylistDetailPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        PlaylistDetailPage(
            playlistName = "Empty",
            tracks = persistentListOf(),
            availablePlaylists = persistentListOf(),
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
