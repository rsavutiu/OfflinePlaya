package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.PlayDisabled
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.SmartPlaylistKind
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppShapes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.smart_forgotten_favorites
import offlineplaya.shared.generated.resources.smart_most_played
import offlineplaya.shared.generated.resources.smart_never_played
import offlineplaya.shared.generated.resources.smart_recently_added
import offlineplaya.shared.generated.resources.smart_recently_played
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Display name for a smart playlist, shared by the row and the page title. */
fun SmartPlaylistKind.titleRes(): StringResource = when (this) {
    SmartPlaylistKind.RECENTLY_PLAYED -> Res.string.smart_recently_played
    SmartPlaylistKind.MOST_PLAYED -> Res.string.smart_most_played
    SmartPlaylistKind.RECENTLY_ADDED -> Res.string.smart_recently_added
    SmartPlaylistKind.FORGOTTEN_FAVORITES -> Res.string.smart_forgotten_favorites
    SmartPlaylistKind.NEVER_PLAYED -> Res.string.smart_never_played
}

private fun SmartPlaylistKind.icon(): ImageVector = when (this) {
    SmartPlaylistKind.RECENTLY_PLAYED -> Icons.Outlined.History
    SmartPlaylistKind.MOST_PLAYED -> Icons.Outlined.TrendingUp
    SmartPlaylistKind.RECENTLY_ADDED -> Icons.Outlined.NewReleases
    SmartPlaylistKind.FORGOTTEN_FAVORITES -> Icons.Outlined.Restore
    SmartPlaylistKind.NEVER_PLAYED -> Icons.Outlined.PlayDisabled
}

/**
 * One smart playlist in the Playlists page's "Smart playlists" section:
 * accent-tinted icon tile + name. Mirrors the user-playlist rows but with a
 * glyph instead of cover art.
 */
@Composable
fun SmartPlaylistRow(
    kind: SmartPlaylistKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    AppShapes.tile,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = kind.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = stringResource(kind.titleRes()),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = AppSpacing.lg),
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SmartPlaylistRowPreview() {
    PreviewTheme {
        Surface {
            SmartPlaylistRow(kind = SmartPlaylistKind.MOST_PLAYED, onClick = {})
        }
    }
}
