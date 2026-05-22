package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.molecules.LibraryTab
import com.offlineplaya.shared.presentation.ui.organisms.FolderList
import kotlinx.coroutines.flow.Flow
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.templates.LibraryScaffold
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun LibraryFolderRootsPage(
    roots: List<Folder>,
    onFolderClick: (Long) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    previewTracksProvider: ((Long) -> Flow<List<Track>>)? = null,
) {
    LibraryScaffold(
        selectedTab = LibraryTab.FOLDERS,
        onTabSelected = onTabSelected,
        onBack = onBack,
        modifier = modifier,
    ) {
        FolderList(
            folders = roots,
            onFolderClick = onFolderClick,
            previewTracksProvider = previewTracksProvider,
        )
    }
}

@Preview
@Composable
private fun LibraryFolderRootsPagePreview() {
    PreviewTheme {
        LibraryFolderRootsPage(
            roots = listOf(
                Folder(1, "content://A", "", "Main Library", null, 247),
                Folder(2, "content://B", "", "Bootlegs", null, 412),
            ),
            onFolderClick = {},
            onTabSelected = {},
            onBack = {},
        )
    }
}
