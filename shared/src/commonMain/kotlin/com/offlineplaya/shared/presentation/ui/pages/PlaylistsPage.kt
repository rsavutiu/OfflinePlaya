package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.CreatePlaylistDialog
import com.offlineplaya.shared.presentation.ui.organisms.PlaylistList
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun PlaylistsPage(
    playlists: List<Playlist>,
    onPlaylistClick: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = { AppTopBar(title = "Playlists", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New playlist")
            }
        },
    ) { padding ->
        PlaylistList(
            playlists = playlists,
            onPlaylistClick = onPlaylistClick,
            modifier = Modifier.padding(padding),
        )
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onCreate = { onCreate(it) },
            onDismiss = { showCreateDialog = false },
        )
    }
}

@Preview
@Composable
private fun PlaylistsPagePopulatedPreview() {
    PreviewTheme {
        PlaylistsPage(
            playlists = listOf(
                Playlist(1, "Morning Run", 0, 0),
                Playlist(2, "Focus", 0, 0),
            ),
            onPlaylistClick = {}, onCreate = {}, onBack = {},
        )
    }
}

@Preview
@Composable
private fun PlaylistsPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        PlaylistsPage(
            playlists = emptyList(),
            onPlaylistClick = {}, onCreate = {}, onBack = {},
        )
    }
}
