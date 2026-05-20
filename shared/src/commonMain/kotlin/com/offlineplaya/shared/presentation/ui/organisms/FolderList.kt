package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.FolderRow
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun FolderList(
    folders: List<Folder>,
    onFolderClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    emptyTitle: String = "No folders yet",
    emptySubtitle: String = "Scan a folder from the home page to populate the library.",
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
            FolderRow(folder = folder, onClick = { onFolderClick(folder.id) })
        }
    }
}

@Preview
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

@Preview
@Composable
private fun FolderListEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            FolderList(folders = emptyList(), onFolderClick = {})
        }
    }
}
