package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * The three top-level library view modes. Adding a mode means extending this
 * enum + handling it in App.kt's routing + the [LibraryTabRow] selection logic.
 */
enum class LibraryTab(val label: String) {
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    FLAT("All tracks"),
}

@Composable
fun LibraryTabRow(
    selected: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(
        selectedTabIndex = selected.ordinal,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        LibraryTab.entries.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryTabRowArtistsPreview() {
    PreviewTheme {
        Surface { LibraryTabRow(selected = LibraryTab.ARTISTS, onTabSelected = {}) }
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryTabRowFoldersDarkPreview() {
    PreviewTheme(darkTheme = true) {
        Surface { LibraryTabRow(selected = LibraryTab.FOLDERS, onTabSelected = {}) }
    }
}
