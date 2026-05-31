package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.runtime.Composable
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.playlist_dialog_create_confirm
import offlineplaya.shared.generated.resources.playlist_dialog_new
import org.jetbrains.compose.resources.stringResource

/**
 * Thin alias over [PlaylistNameDialog] preserving the original Create-only
 * API. New code can call PlaylistNameDialog directly for the rename variant.
 */
@Composable
fun CreatePlaylistDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    PlaylistNameDialog(
        title = stringResource(Res.string.playlist_dialog_new),
        confirmLabel = stringResource(Res.string.playlist_dialog_create_confirm),
        initialName = "",
        onConfirm = onCreate,
        onDismiss = onDismiss,
    )
}

@PreviewScreenSizes
@Composable
private fun CreatePlaylistDialogPreview() {
    PreviewTheme {
        CreatePlaylistDialog(onCreate = {}, onDismiss = {})
    }
}
