package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.organisms.FolderDetailContent
import com.offlineplaya.shared.presentation.ui.organisms.TrackDetailsSheet
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun LibraryFolderDetailPage(
    folderName: String,
    subfolders: List<Folder>,
    tracks: List<Track>,
    availablePlaylists: List<Playlist>,
    onFolderClick: (Long) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track, Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    previewTracksProvider: ((Long) -> Flow<List<Track>>)? = null,
) {
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { AppTopBar(title = folderName, onBack = onBack) },
    ) { padding ->
        ResponsiveContent(modifier = Modifier.padding(padding)) {
            FolderDetailContent(
                subfolders = subfolders,
                tracks = tracks,
                onFolderClick = onFolderClick,
                onTrackClick = { selectedTrack = it },
                previewTracksProvider = previewTracksProvider,
            )
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
}

@PreviewScreenSizes
@Composable
private fun LibraryFolderDetailPagePreview() {
    PreviewTheme {
        LibraryFolderDetailPage(
            folderName = "Pearl Jam",
            subfolders = listOf(
                Folder(10, "t", "Pearl Jam/Ten", "Ten", 1L, 11),
                Folder(11, "t", "Pearl Jam/Vs.", "Vs.", 1L, 12),
            ),
            tracks = emptyList(),
            availablePlaylists = emptyList(),
            onFolderClick = {},
            onPlayTracks = { _, _ -> },
            onPlayNext = {}, onAddToQueue = {}, onAddToPlaylist = { _, _ -> },
            onBack = {},
        )
    }
}
