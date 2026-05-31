package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
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

@PreviewScreenSizes
@Composable
private fun CreatePlaylistDialogPreview() {
    PreviewTheme {
        CreatePlaylistDialog(onCreate = {}, onDismiss = {})
    }
}
