package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * The three top-level library view modes. Adding a mode means extending this
 * enum + handling it in App.kt's routing + the [LibraryTabRow] selection logic.
 */
enum class LibraryTab(val label: String) {
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    // Title Case to match the Home Browse card ("All Tracks").
    FLAT("All Tracks"),
}

@Composable
fun LibraryTabRow(
    selected: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Brand accent drives the indicator + selected-tab text so the nav
    // identity is constant even when album-art Palette repaints colorScheme.
    // The indicator is an explicit slot — M3's default underline is
    // colorScheme.primary and ignores contentColor, so it must be colored here.
    val brandAccent = LocalBrandAccent.current.accent
    TabRow(
        selectedTabIndex = selected.ordinal,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = brandAccent,
        indicator = { tabPositions ->
            if (selected.ordinal < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selected.ordinal]),
                    color = brandAccent,
                )
            }
        },
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
