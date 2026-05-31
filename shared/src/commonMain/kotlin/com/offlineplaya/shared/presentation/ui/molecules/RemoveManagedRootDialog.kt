package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.offlineplaya.shared.domain.model.ManagedTreeRoot

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
        title = { Text("Remove this folder?") },
        text = {
            Text(
                "\"${root.displayName}\" will disappear from your library, " +
                        "along with the tracks scanned from it. " +
                        "The files on disk aren't touched.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Remove") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
