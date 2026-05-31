package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.FolderRow
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun FolderList(
    folders: List<Folder>,
    onFolderClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    emptyTitle: String = "No folders yet",
    emptySubtitle: String = "Scan a folder from the home page to populate the library.",
    previewTracksProvider: ((Long) -> Flow<List<Track>>)? = null,
) {
    if (folders.isEmpty()) {
        EmptyState(title = emptyTitle, subtitle = emptySubtitle, modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(items = folders, key = { it.id }) { folder ->
            val previewTracks: List<Track> = if (previewTracksProvider != null) {
                val state by produceState(initialValue = emptyList<Track>(), folder.id) {
                    previewTracksProvider(folder.id).collectLatest { value = it }
                }
                state
            } else {
                emptyList()
            }
            FolderRow(
                folder = folder,
                onClick = { onFolderClick(folder.id) },
                previewTracks = previewTracks,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun FolderListPopulatedPreview() {
    PreviewTheme {
        Surface {
            FolderList(
                folders = listOf(
                    Folder(1, "content://tree", "", "Music Library", null, 247),
                    Folder(2, "content://tree2", "", "Bootlegs", null, 412),
                ),
                onFolderClick = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun FolderListEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            FolderList(folders = emptyList(), onFolderClick = {})
        }
    }
}
