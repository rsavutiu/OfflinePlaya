package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.home_label_folder
import offlineplaya.shared.generated.resources.home_label_folders
import offlineplaya.shared.generated.resources.home_label_total
import offlineplaya.shared.generated.resources.home_label_track
import offlineplaya.shared.generated.resources.home_label_tracks
import org.jetbrains.compose.resources.stringResource

/**
 * Three-cell stat strip rendered on the Home page: tracks, folders, total
 * duration. Pill-shaped, soft surface, thin vertical dividers between cells.
 * Estimates duration from track count (3.5 min average) — exact totals would
 * cost an extra query and aren't worth it for a glanceable stat.
 */
@Composable
fun HomeStatsRow(
    trackCount: Long,
    folderCount: Int,
    status: SyncStatus,
    modifier: Modifier = Modifier,
    onTracksClick: (() -> Unit)? = null,
    onFoldersClick: (() -> Unit)? = null,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val alpha0 by animateFloatAsState(if (entered) 1f else 0f, tween(400), label = "stat0")
    val alpha1 by animateFloatAsState(if (entered) 1f else 0f, tween(400, delayMillis = 80), label = "stat1")
    val alpha2 by animateFloatAsState(if (entered) 1f else 0f, tween(400, delayMillis = 160), label = "stat2")
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 10.dp),
    ) {
        StatCell(
            icon = Icons.Outlined.MusicNote,
            value = trackCount.toString(),
            label = stringResource(
                if (trackCount == 1L) Res.string.home_label_track else Res.string.home_label_tracks
            ),
            onClick = onTracksClick,
            modifier = Modifier.weight(1f).alpha(alpha0),
        )
        VerticalDivider(borderColor)
        StatCell(
            icon = Icons.Outlined.Folder,
            value = folderCount.toString(),
            label = stringResource(
                if (folderCount == 1) Res.string.home_label_folder else Res.string.home_label_folders
            ),
            onClick = onFoldersClick,
            modifier = Modifier.weight(1f).alpha(alpha1),
        )
        VerticalDivider(borderColor)
        StatCell(
            icon = Icons.Outlined.Schedule,
            value = formatTotalDuration(trackCount),
            label = stringResource(Res.string.home_label_total),
            modifier = Modifier.weight(1f).alpha(alpha2),
        )
    }
}

@Composable
private fun StatCell(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                // Decorative — the text label right below carries the meaning.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            // Secondary-text token, not the dimmer `outline` border color.
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VerticalDivider(color: androidx.compose.ui.graphics.Color) {
    Box(modifier = Modifier
        .width(1.dp)
        .height(32.dp)
        .background(color))
}

private fun formatTotalDuration(trackCount: Long): String {
    val totalMinutes = (trackCount * 3.5).toLong()
    val hours = totalMinutes / 60
    return if (hours > 0) "${hours}h" else "${totalMinutes}m"
}

@PreviewScreenSizes
@Composable
private fun HomeStatsRowPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            HomeStatsRow(
                trackCount = 212,
                folderCount = 22,
                status = SyncStatus.Idle,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
