package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.stream.PlayToPcLinks
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.play_to_pc_how_note
import offlineplaya.shared.generated.resources.play_to_pc_how_step1_body
import offlineplaya.shared.generated.resources.play_to_pc_how_step1_title
import offlineplaya.shared.generated.resources.play_to_pc_how_step2_body
import offlineplaya.shared.generated.resources.play_to_pc_how_step2_title
import offlineplaya.shared.generated.resources.play_to_pc_how_step3_body
import offlineplaya.shared.generated.resources.play_to_pc_how_step3_title
import offlineplaya.shared.generated.resources.play_to_pc_how_title
import offlineplaya.shared.generated.resources.play_to_pc_link_copied
import offlineplaya.shared.generated.resources.play_to_pc_link_copy
import offlineplaya.shared.generated.resources.play_to_pc_link_header
import offlineplaya.shared.generated.resources.play_to_pc_link_hint
import offlineplaya.shared.generated.resources.play_to_pc_link_open
import offlineplaya.shared.generated.resources.play_to_pc_link_share
import org.jetbrains.compose.resources.stringResource

/**
 * The self-explaining half of the Play-to-PC picker: what the feature does, the
 * three steps to get there, and — the part that needs care — how to get the
 * receiver onto the computer.
 *
 * The link is the awkward bit. The download only matters *on the PC*, and the
 * user is holding a phone, so opening GitHub here is the least useful thing we
 * could do. Deliberately no QR code (it needs a second camera-holding device
 * and explains nothing): instead the short URL is printed as selectable
 * monospace text, with three ways to move it — clipboard, the system share
 * sheet (mail-to-self, Quick Share, Phone Link…), or opening it here for
 * someone who only wants to read the guide.
 *
 * Stateless apart from the "Link copied" acknowledgement, and takes [onShare]
 * as a lambda so the page stays previewable without DI.
 */
@Composable
fun PlayToPcGuide(
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val accent = LocalBrandAccent.current.accent
    var copied by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(stringResource(Res.string.play_to_pc_how_title))
        Spacer(Modifier.height(AppSpacing.sm))

        GuideStepCard(
            title = stringResource(Res.string.play_to_pc_how_step1_title),
            body = stringResource(Res.string.play_to_pc_how_step1_body),
        )
        Spacer(Modifier.height(AppSpacing.sm))
        GuideStepCard(
            title = stringResource(Res.string.play_to_pc_how_step2_title),
            body = stringResource(Res.string.play_to_pc_how_step2_body),
        )
        Spacer(Modifier.height(AppSpacing.sm))
        GuideStepCard(
            title = stringResource(Res.string.play_to_pc_how_step3_title),
            body = stringResource(Res.string.play_to_pc_how_step3_body),
        )

        Spacer(Modifier.height(AppSpacing.md))
        Text(
            text = stringResource(Res.string.play_to_pc_how_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(AppSpacing.lg))
        SectionLabel(stringResource(Res.string.play_to_pc_link_header))
        Spacer(Modifier.height(AppSpacing.xs))
        Text(
            text = stringResource(Res.string.play_to_pc_link_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(AppSpacing.sm))

        // Monospace + selectable: someone has to read this off the screen and
        // type it on a keyboard, so legibility beats prettiness. The anchored
        // form goes to the clipboard; only the short form is shown.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = accent.copy(alpha = 0.10f),
            shape = MaterialTheme.shapes.small,
        ) {
            SelectionContainer {
                Text(
                    text = PlayToPcLinks.GUIDE_SHORT,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(
                        horizontal = AppSpacing.md,
                        vertical = AppSpacing.sm,
                    ),
                )
            }
        }

        Spacer(Modifier.height(AppSpacing.sm))
        // Stacked, full-width, NOT a FlowRow: the shared module compiles against
        // Compose Multiplatform 1.7.3 while androidApp runs
        // androidx.compose.foundation 1.9.x, whose FlowRow parameter list differs
        // — it compiles and then dies at first composition with
        // NoSuchMethodError (same skew EqPresetChooser's comment warns about).
        // Full-width also keeps these labels intact in the wordier locales
        // ("Zum Computer versenden" has no chance at half width).
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            LinkAction(
                icon = Icons.Outlined.ContentCopy,
                label = stringResource(
                    if (copied) Res.string.play_to_pc_link_copied
                    else Res.string.play_to_pc_link_copy,
                ),
                onClick = {
                    clipboard.setText(AnnotatedString(PlayToPcLinks.GUIDE))
                    copied = true
                },
            )
            LinkAction(
                icon = Icons.Outlined.Share,
                label = stringResource(Res.string.play_to_pc_link_share),
                onClick = onShare,
            )
            LinkAction(
                icon = Icons.Outlined.OpenInNew,
                label = stringResource(Res.string.play_to_pc_link_open),
                onClick = { runCatching { uriHandler.openUri(PlayToPcLinks.GUIDE) } },
            )
        }
    }
}

/** Bold sub-head used to break the page into "how it works" / "get the app". */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/** One of the three ways to move the link somewhere useful. */
@Composable
private fun LinkAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(label, modifier = Modifier.padding(start = AppSpacing.sm))
    }
}

/** One numbered step, mirroring the EmptyLibraryGuide checklist card. */
@Composable
private fun GuideStepCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(AppSpacing.lg)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(AppSpacing.xs))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun PlayToPcGuidePreview() {
    PreviewTheme {
        Surface {
            PlayToPcGuide(onShare = {}, modifier = Modifier.padding(AppSpacing.lg))
        }
    }
}
