package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.atoms.AppCaption
import com.offlineplaya.shared.presentation.ui.atoms.AppHeadline
import com.offlineplaya.shared.presentation.ui.molecules.HomeHeader
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.empty_library_open_settings
import offlineplaya.shared.generated.resources.empty_library_permission_note
import offlineplaya.shared.generated.resources.empty_library_step1_body
import offlineplaya.shared.generated.resources.empty_library_step1_title
import offlineplaya.shared.generated.resources.empty_library_step2_body
import offlineplaya.shared.generated.resources.empty_library_step2_title
import offlineplaya.shared.generated.resources.empty_library_step3_body
import offlineplaya.shared.generated.resources.empty_library_step3_title
import offlineplaya.shared.generated.resources.empty_library_subtitle
import offlineplaya.shared.generated.resources.empty_library_title
import org.jetbrains.compose.resources.stringResource

/**
 * Home-page state shown when the library is empty: there are zero indexed
 * tracks, so there is nothing to browse or play. Rather than a bare "no
 * results" line, this walks the user through *getting* music onto the device —
 * the app is strictly offline (no streaming), so the only way the library fills
 * up is files the user puts there themselves.
 *
 * The header is kept so search/settings stay reachable, and the body is
 * vertically scrollable because the multi-step copy clips in landscape and on
 * short phones.
 *
 * Gating happens upstream on a *loaded-and-empty* signal, never raw
 * `trackCount == 0`, so a returning user with a large library doesn't flash
 * this guide while `observeCount()` is still resolving (see `App.kt`).
 */
@Composable
fun EmptyLibraryGuide(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(TestTags.Home.EMPTY_GUIDE),
    ) {
        HomeHeader(onOpenSearch = onOpenSearch, onOpenSettings = onOpenSettings)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(AppSpacing.sm))
            GuideIcon()
            Spacer(Modifier.height(AppSpacing.lg))
            AppHeadline(text = stringResource(Res.string.empty_library_title))
            Spacer(Modifier.height(AppSpacing.sm))
            AppCaption(text = stringResource(Res.string.empty_library_subtitle))

            Spacer(Modifier.height(AppSpacing.xl))

            GuideStep(
                title = stringResource(Res.string.empty_library_step1_title),
                body = stringResource(Res.string.empty_library_step1_body),
            )
            Spacer(Modifier.height(AppSpacing.md))
            GuideStep(
                title = stringResource(Res.string.empty_library_step2_title),
                body = stringResource(Res.string.empty_library_step2_body),
            )
            Spacer(Modifier.height(AppSpacing.md))
            GuideStep(
                title = stringResource(Res.string.empty_library_step3_title),
                body = stringResource(Res.string.empty_library_step3_body),
            )

            Spacer(Modifier.height(AppSpacing.xl))

            Text(
                text = stringResource(Res.string.empty_library_permission_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(AppSpacing.lg))

            OutlinedButton(onClick = onOpenSettings) {
                Text(stringResource(Res.string.empty_library_open_settings))
            }

            Spacer(Modifier.height(AppSpacing.xl))
        }
    }
}

/** Brand-accent disc holding a music icon — the hero of the empty state. */
@Composable
private fun GuideIcon() {
    val brand = LocalBrandAccent.current
    Surface(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape),
        color = brand.accent.copy(alpha = 0.14f),
        shape = CircleShape,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.LibraryMusic,
                contentDescription = null,
                tint = brand.accent,
                modifier = Modifier.size(44.dp),
            )
        }
    }
}

/**
 * One numbered instruction: a bold step title with its supporting line. Laid
 * out as a left-aligned card so the steps read as a checklist, not centered
 * prose.
 */
@Composable
private fun GuideStep(title: String, body: String) {
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
private fun EmptyLibraryGuidePreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            EmptyLibraryGuide(onOpenSearch = {}, onOpenSettings = {})
        }
    }
}
