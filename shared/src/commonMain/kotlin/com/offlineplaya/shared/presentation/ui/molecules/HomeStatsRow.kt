package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
) {
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
            // Singular label when the count is exactly 1 ("1 Track", not "1 Tracks").
            label = stringResource(
                if (trackCount == 1L) Res.string.home_label_track else Res.string.home_label_tracks
            ),
            modifier = Modifier.weight(1f),
        )
        VerticalDivider(borderColor)
        StatCell(
            icon = Icons.Outlined.Folder,
            value = folderCount.toString(),
            label = stringResource(
                if (folderCount == 1) Res.string.home_label_folder else Res.string.home_label_folders
            ),
            modifier = Modifier.weight(1f),
        )
        VerticalDivider(borderColor)
        StatCell(
            icon = Icons.Outlined.Schedule,
            value = formatTotalDuration(trackCount),
            label = stringResource(Res.string.home_label_total),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCell(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
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
