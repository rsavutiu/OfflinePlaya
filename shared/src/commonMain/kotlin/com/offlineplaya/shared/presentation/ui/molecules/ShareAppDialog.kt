package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.offlineplaya.shared.presentation.review.ShareNudgeCoordinator
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.share_nudge_body
import offlineplaya.shared.generated.resources.share_nudge_dismiss
import offlineplaya.shared.generated.resources.share_nudge_message
import offlineplaya.shared.generated.resources.share_nudge_share
import offlineplaya.shared.generated.resources.share_nudge_title
import org.jetbrains.compose.resources.stringResource

/**
 * The "please share this free app" nudge. Shown by [ShareNudgeCoordinator]
 * after a week of use with some listening; dismissable, and it comes back on
 * its own a month later until the user shares. Copy leans on the honest pitch:
 * free, no ads, no tracking, no way to reach people except word of mouth.
 *
 * [onShare] receives the fully-formatted, localized share text (message +
 * Play Store link) so the host just hands it to the platform share sheet.
 */
@Composable
fun ShareAppDialog(
    onShare: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val shareText = stringResource(Res.string.share_nudge_message, ShareNudgeCoordinator.PLAY_STORE_URL)
    val accent = LocalBrandAccent.current.accent
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Favorite, contentDescription = null, tint = accent) },
        title = { Text(stringResource(Res.string.share_nudge_title)) },
        text = { Text(stringResource(Res.string.share_nudge_body)) },
        confirmButton = {
            TextButton(onClick = { onShare(shareText) }) {
                Text(stringResource(Res.string.share_nudge_share), color = accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.share_nudge_dismiss)) }
        },
    )
}

@PreviewScreenSizes
@Composable
private fun ShareAppDialogPreview() {
    PreviewTheme {
        ShareAppDialog(onShare = {}, onDismiss = {})
    }
}
