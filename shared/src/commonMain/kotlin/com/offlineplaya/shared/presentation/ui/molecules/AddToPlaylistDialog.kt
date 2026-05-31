package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.add_to_playlist_dialog_title
import offlineplaya.shared.generated.resources.playlist_dialog_cancel
import offlineplaya.shared.generated.resources.playlist_empty_message
import org.jetbrains.compose.resources.stringResource

/**
 * Lists existing playlists. Tapping one calls [onPickPlaylist] and dismisses.
 * Falls back to a friendly empty state when no playlists exist yet —
 * creation lives on the Playlists page so this dialog stays narrowly scoped.
 */
@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onPickPlaylist: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_to_playlist_dialog_title)) },
        text = {
            if (playlists.isEmpty()) {
                Text(
                    text = stringResource(Res.string.playlist_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                ) {
                    items(items = playlists, key = { it.id }) { playlist ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPickPlaylist(playlist.id)
                                    onDismiss()
                                }
                                .padding(horizontal = 8.dp, vertical = 14.dp),
                        ) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.playlist_dialog_cancel)) }
        },
    )
}

@PreviewScreenSizes
@Composable
private fun AddToPlaylistDialogPopulatedPreview() {
    PreviewTheme {
        AddToPlaylistDialog(
            playlists = listOf(
                Playlist(1, "Morning Run", 0, 0),
                Playlist(2, "Focus", 0, 0),
                Playlist(3, "Late Drives", 0, 0),
            ),
            onPickPlaylist = {},
            onDismiss = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun AddToPlaylistDialogEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        AddToPlaylistDialog(playlists = emptyList(), onPickPlaylist = {}, onDismiss = {})
    }
}
