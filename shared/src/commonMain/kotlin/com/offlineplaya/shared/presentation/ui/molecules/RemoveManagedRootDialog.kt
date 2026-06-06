package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.offlineplaya.shared.domain.model.ManagedTreeRoot
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.playlist_dialog_cancel
import offlineplaya.shared.generated.resources.remove_button
import offlineplaya.shared.generated.resources.remove_folder_body
import offlineplaya.shared.generated.resources.remove_folder_confirmation
import org.jetbrains.compose.resources.stringResource

/**
 * Confirmation dialog for removing a managed library root. The destructive
 * action drops every Track scanned from this URI; the file copies on disk are
 * untouched but we say so plainly so the user isn't worried.
 */
@Composable
fun RemoveManagedRootDialog(
    root: ManagedTreeRoot,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.remove_folder_confirmation)) },
        text = {
            Text(stringResource(Res.string.remove_folder_body, root.displayName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.remove_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.playlist_dialog_cancel)) }
        },
    )
}
