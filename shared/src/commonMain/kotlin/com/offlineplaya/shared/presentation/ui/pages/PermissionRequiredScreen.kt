package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.atoms.AppButton
import com.offlineplaya.shared.presentation.ui.atoms.AppCaption
import com.offlineplaya.shared.presentation.ui.atoms.AppHeadline
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.permission_required_body
import offlineplaya.shared.generated.resources.permission_required_denied_note
import offlineplaya.shared.generated.resources.permission_required_grant
import offlineplaya.shared.generated.resources.permission_required_open_settings
import offlineplaya.shared.generated.resources.permission_required_title
import org.jetbrains.compose.resources.stringResource

/**
 * Hard gate shown by the Android host whenever the audio-read permission is
 * missing — the app is an offline player over the user's own files and is
 * useless without it, so this blocks everything (onboarding and the main app)
 * until access is granted. Not skippable by design.
 *
 * [onGrant] fires the system permission request; [onOpenAppSettings] is the
 * escape hatch when the user previously chose "Don't allow" and the system no
 * longer shows the dialog — it deep-links to the app's settings page.
 */
@Composable
fun PermissionRequiredScreen(
    onGrant: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroDisc()
            Spacer(Modifier.height(AppSpacing.lg))
            AppHeadline(text = stringResource(Res.string.permission_required_title))
            Spacer(Modifier.height(AppSpacing.sm))
            AppCaption(text = stringResource(Res.string.permission_required_body))
            Spacer(Modifier.height(AppSpacing.xl))
            AppButton(
                text = stringResource(Res.string.permission_required_grant),
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppSpacing.sm))
            OutlinedButton(onClick = onOpenAppSettings) {
                Text(stringResource(Res.string.permission_required_open_settings))
            }
            Spacer(Modifier.height(AppSpacing.md))
            Text(
                text = stringResource(Res.string.permission_required_denied_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HeroDisc() {
    val brand = LocalBrandAccent.current
    Surface(
        modifier = Modifier.size(96.dp),
        color = brand.accent.copy(alpha = 0.14f),
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.LibraryMusic,
                contentDescription = null,
                tint = brand.accent,
                modifier = Modifier.size(44.dp),
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun PermissionRequiredScreenPreview() {
    PreviewTheme(darkTheme = true) {
        PermissionRequiredScreen(onGrant = {}, onOpenAppSettings = {})
    }
}
