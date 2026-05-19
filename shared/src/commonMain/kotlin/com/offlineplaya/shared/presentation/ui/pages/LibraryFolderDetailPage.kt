package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.organisms.FolderDetailContent
import com.offlineplaya.shared.presentation.ui.organisms.TrackDetailsSheet
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun LibraryFolderDetailPage(
    folderName: String,
    subfolders: List<Folder>,
    tracks: List<Track>,
    onFolderClick: (Long) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = folderName, onBack = onBack) },
    ) { padding ->
        FolderDetailContent(
            subfolders = subfolders,
            tracks = tracks,
            onFolderClick = onFolderClick,
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
private fun LibraryFolderDetailPagePreview() {
    PreviewTheme {
        LibraryFolderDetailPage(
            folderName = "Pearl Jam",
            subfolders = listOf(
                Folder(10, "t", "Pearl Jam/Ten", "Ten", 1L, 11),
                Folder(11, "t", "Pearl Jam/Vs.", "Vs.", 1L, 12),
            ),
            tracks = emptyList(),
            onFolderClick = {},
            onPlayTracks = { _, _ -> },
            onBack = {},
        )
    }
}
