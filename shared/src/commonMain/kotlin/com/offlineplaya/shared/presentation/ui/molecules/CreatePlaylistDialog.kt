package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.runtime.Composable
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

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
        title = "New playlist",
        confirmLabel = "Create",
        initialName = "",
        onConfirm = onCreate,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun CreatePlaylistDialogPreview() {
    PreviewTheme {
        CreatePlaylistDialog(onCreate = {}, onDismiss = {})
    }
}
