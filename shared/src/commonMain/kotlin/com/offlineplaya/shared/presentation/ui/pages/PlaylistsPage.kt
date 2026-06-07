package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.WindowInsets
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
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.cd_new_playlist
import offlineplaya.shared.generated.resources.top_bar_playlists
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlaylistsPage(
    playlists: PersistentList<Playlist>,
    onPlaylistClick: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.top_bar_playlists),
                onBack = onBack
            )
        },
        floatingActionButton = {
            val brand = LocalBrandAccent.current
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = brand.accent,
                contentColor = brand.onAccent,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.cd_new_playlist)
                )
            }
        },
    ) { padding ->
        ResponsiveContent {
            PlaylistList(
                playlists = playlists,
                onPlaylistClick = onPlaylistClick,
                contentPadding = padding,
            )
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onCreate = { onCreate(it) },
            onDismiss = { showCreateDialog = false },
        )
    }
}

@PreviewScreenSizes
@Composable
private fun PlaylistsPagePopulatedPreview() {
    PreviewTheme {
        PlaylistsPage(
            playlists = persistentListOf(
                Playlist(1, "Morning Run", 0, 0),
                Playlist(2, "Focus", 0, 0),
            ),
            onPlaylistClick = {}, onCreate = {}, onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun PlaylistsPageFullListPreview() {
    PreviewTheme {
        PlaylistsPage(
            playlists = persistentListOf(
                Playlist(1, "Morning Run", 0, 0),
                Playlist(2, "Focus", 0, 0),
                Playlist(3, "Late Night Drive", 0, 0),
                Playlist(4, "Workout Bangers", 0, 0),
                Playlist(5, "Chill Acoustic", 0, 0),
                Playlist(6, "90s Throwbacks", 0, 0),
                Playlist(7, "Deep House", 0, 0),
                Playlist(8, "Rainy Day Jazz", 0, 0),
                Playlist(9, "Road Trip", 0, 0),
                Playlist(10, "Sunday Coffee", 0, 0),
                Playlist(11, "Party Starters", 0, 0),
                Playlist(12, "Sleep & Ambient", 0, 0),
            ),
            onPlaylistClick = {}, onCreate = {}, onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun PlaylistsPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        PlaylistsPage(
            playlists = persistentListOf(),
            onPlaylistClick = {}, onCreate = {}, onBack = {},
        )
    }
}
