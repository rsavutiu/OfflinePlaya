package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
 * the in-app Play to PC page and counts as engaging with the feature, so
 * [onActioned] hides it for good; "Not now" dismisses it (it returns later, up
 * to a cap).
 *
 * It used to open the GitHub guide straight from here, which was a dead end: the
 * receiver installs on the *computer*, and the user is holding a phone. The page
 * explains the feature and hands the link over in copyable / shareable form, so
 * that is where this sends them.
 *
 * The link opens via [LocalUriHandler] so the host needs no Intent plumbing;
 * the open is wrapped since a device may have no browser.
 */
@Composable
fun PlayToPcNudgeDialog(
    onOpenPlayToPc: () -> Unit,
    onActioned: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = LocalBrandAccent.current.accent
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Computer, contentDescription = null, tint = accent) },
        title = { Text(stringResource(Res.string.play_to_pc_nudge_title)) },
        text = { Text(stringResource(Res.string.play_to_pc_nudge_body)) },
        confirmButton = {
            TextButton(onClick = {
                onOpenPlayToPc()
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
        PlayToPcNudgeDialog(onOpenPlayToPc = {}, onActioned = {}, onDismiss = {})
    }
}
