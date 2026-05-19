package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Shared name-prompt dialog used for both creating and renaming playlists.
 * The [title] / [confirmLabel] decide which intent is presented; the rest of
 * the logic (trim, disable-on-blank, submit-and-dismiss) is identical.
 */
@Composable
fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val trimmed = name.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        },
        confirmButton = {
            TextButton(
                enabled = trimmed.isNotEmpty(),
                onClick = {
                    onConfirm(trimmed)
                    onDismiss()
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Preview
@Composable
private fun PlaylistNameDialogCreatePreview() {
    PreviewTheme {
        PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            initialName = "",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun PlaylistNameDialogRenamePreview() {
    PreviewTheme(darkTheme = true) {
        PlaylistNameDialog(
            title = "Rename playlist",
            confirmLabel = "Rename",
            initialName = "Morning Run",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
