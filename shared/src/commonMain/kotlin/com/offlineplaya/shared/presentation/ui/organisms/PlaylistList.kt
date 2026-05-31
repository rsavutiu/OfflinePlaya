package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.PlaylistRow
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun PlaylistList(
    playlists: List<Playlist>,
    onPlaylistClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (playlists.isEmpty()) {
        EmptyState(
            title = "No playlists yet",
            subtitle = "Tap the + button to create one.",
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(items = playlists, key = { it.id }) { playlist ->
            PlaylistRow(playlist = playlist, onClick = { onPlaylistClick(playlist.id) })
        }
    }
}

@PreviewScreenSizes
@Composable
private fun PlaylistListPopulatedPreview() {
    PreviewTheme {
        Surface {
            PlaylistList(
                playlists = listOf(
                    Playlist(1, "Morning Run", 0, 0),
                    Playlist(2, "Focus", 0, 0),
                    Playlist(3, "Late Drives", 0, 0),
                ),
                onPlaylistClick = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun PlaylistListEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        Surface { PlaylistList(playlists = emptyList(), onPlaylistClick = {}) }
    }
}
