package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.organisms.FolderDetailContent
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow

@Composable
fun LibraryFolderDetailPage(
    folderName: String,
    subfolders: PersistentList<Folder>,
    tracks: PersistentList<Track>,
    onFolderClick: (Long) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    previewTracksProvider: ((Long) -> Flow<List<Track>>)? = null,
    onFolderLongPress: ((Folder) -> Unit)? = null,
    onFolderPlay: ((Folder) -> Unit)? = null,
) {
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
                onTrackClick = { onPlayTracks(tracks, tracks.indexOf(it).coerceAtLeast(0)) },
                onTrackLongPress = onTrackLongPress,
                previewTracksProvider = previewTracksProvider,
                onFolderLongPress = onFolderLongPress,
                onFolderPlay = onFolderPlay,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryFolderDetailPagePreview() {
    PreviewTheme {
        LibraryFolderDetailPage(
            folderName = "Pearl Jam",
            subfolders = persistentListOf(
                Folder(10, "t", "Pearl Jam/Ten", "Ten", 1L, 11),
                Folder(11, "t", "Pearl Jam/Vs.", "Vs.", 1L, 12),
            ),
            tracks = persistentListOf(),
            onFolderClick = {},
            onPlayTracks = { _, _ -> },
            onTrackLongPress = {},
            onBack = {},
        )
    }
}
