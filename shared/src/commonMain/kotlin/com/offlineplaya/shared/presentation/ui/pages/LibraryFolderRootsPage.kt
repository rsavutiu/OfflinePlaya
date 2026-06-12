package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.molecules.LibraryTab
import com.offlineplaya.shared.presentation.ui.organisms.FolderList
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.LibraryScaffold
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow

@Composable
fun LibraryFolderRootsPage(
    roots: PersistentList<Folder>,
    onFolderClick: (Long) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    previewTracksProvider: ((Long) -> Flow<List<Track>>)? = null,
    onFolderLongPress: ((Folder) -> Unit)? = null,
    onFolderPlay: ((Folder) -> Unit)? = null,
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
            onFolderLongPress = onFolderLongPress,
            onFolderPlay = onFolderPlay,
        )
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryFolderRootsPagePreview() {
    PreviewTheme {
        LibraryFolderRootsPage(
            roots = persistentListOf(
                Folder(1, "content://A", "", "Main Library", null, 247),
                Folder(2, "content://B", "", "Bootlegs", null, 412),
            ),
            onFolderClick = {},
            onTabSelected = {},
            onBack = {},
        )
    }
}
