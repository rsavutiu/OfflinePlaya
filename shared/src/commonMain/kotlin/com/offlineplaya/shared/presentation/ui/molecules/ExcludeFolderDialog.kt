package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.offlineplaya.shared.domain.model.Folder
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.exclude_button
import offlineplaya.shared.generated.resources.exclude_folder_body
import offlineplaya.shared.generated.resources.exclude_folder_title
import offlineplaya.shared.generated.resources.playlist_dialog_cancel
import org.jetbrains.compose.resources.stringResource

/**
 * Confirmation dialog for hiding a folder from the library. Triggered by a
 * long-press on a folder row; the exclusion can be undone from Settings.
 */
@Composable
fun ExcludeFolderDialog(
    folder: Folder,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.exclude_folder_title)) },
        text = {
            Text(stringResource(Res.string.exclude_folder_body, folder.displayName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.exclude_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.playlist_dialog_cancel)) }
        },
    )
}
