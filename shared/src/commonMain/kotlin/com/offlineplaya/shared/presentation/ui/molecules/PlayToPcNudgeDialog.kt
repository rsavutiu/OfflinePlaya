package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.offlineplaya.shared.presentation.review.PlayToPcNudgeCoordinator
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.play_to_pc_nudge_body
import offlineplaya.shared.generated.resources.play_to_pc_nudge_dismiss
import offlineplaya.shared.generated.resources.play_to_pc_nudge_get_app
import offlineplaya.shared.generated.resources.play_to_pc_nudge_title
import org.jetbrains.compose.resources.stringResource

/**
 * The "did you know you can stream to your PC?" feature-discovery nudge, shown
 * by [PlayToPcNudgeCoordinator] after a couple of plays. "Get the PC app" opens
 * the desktop receiver's GitHub releases page (where the Windows/macOS/Linux
 * downloads live) and counts as engaging with the feature, so [onActioned]
 * hides it for good; "Not now" dismisses it (it returns later, up to a cap).
 *
 * The link opens via [LocalUriHandler] so the host needs no Intent plumbing;
 * the open is wrapped since a device may have no browser.
 */
@Composable
fun PlayToPcNudgeDialog(
    onActioned: () -> Unit,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val accent = LocalBrandAccent.current.accent
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Computer, contentDescription = null, tint = accent) },
        title = { Text(stringResource(Res.string.play_to_pc_nudge_title)) },
        text = { Text(stringResource(Res.string.play_to_pc_nudge_body)) },
        confirmButton = {
            TextButton(onClick = {
                runCatching { uriHandler.openUri(PlayToPcNudgeCoordinator.GITHUB_RELEASES_URL) }
                onActioned()
            }) {
                Text(stringResource(Res.string.play_to_pc_nudge_get_app), color = accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.play_to_pc_nudge_dismiss)) }
        },
    )
}

@PreviewScreenSizes
@Composable
private fun PlayToPcNudgeDialogPreview() {
    PreviewTheme {
        PlayToPcNudgeDialog(onActioned = {}, onDismiss = {})
    }
}
