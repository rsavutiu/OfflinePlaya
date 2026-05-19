package com.offlineplaya.shared.presentation.ui.templates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.LibraryTab
import com.offlineplaya.shared.presentation.ui.molecules.LibraryTabRow
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Top-level library template: a [Scaffold] whose top section is an
 * [AppTopBar] stacked above a [LibraryTabRow]. Used by the three top-level
 * library pages (Artists / Folders / Flat). Detail pages (artist/album/
 * folder detail) use a plain [Scaffold] with just the top bar instead — no
 * tabs.
 */
@Composable
fun LibraryScaffold(
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                AppTopBar(title = "Library", onBack = onBack)
                LibraryTabRow(selected = selectedTab, onTabSelected = onTabSelected)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun LibraryScaffoldArtistsPreview() {
    PreviewTheme {
        LibraryScaffold(
            selectedTab = LibraryTab.ARTISTS,
            onTabSelected = {},
            onBack = {},
        ) {
            Surface {
                Text("Body content goes here", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
