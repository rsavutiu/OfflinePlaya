package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Playlist detail hero: large rounded tile with the playlist icon, then the
 * playlist name, track count and total runtime. The detail header has no
 * artwork — playlists don't have a single representative cover — so we use
 * an icon-on-secondary surface to give the header weight.
 */
@Composable
fun PlaylistDetailHeader(
    name: String,
    trackCount: Int,
    totalDurationMs: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.size(120.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
        Spacer(Modifier.height(AppSpacing.lg))
        Text(text = name, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(AppSpacing.sm))
        Text(
            text = formatSubtitle(trackCount, totalDurationMs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(AppSpacing.md))
    }
}

private fun formatSubtitle(trackCount: Int, totalDurationMs: Long): String {
    val countText = "$trackCount ${if (trackCount == 1) "track" else "tracks"}"
    val minutes = totalDurationMs / 60_000
    val durationText = if (minutes >= 60) {
        "${minutes / 60}h ${minutes % 60}m"
    } else {
        "${minutes}m"
    }
    return "$countText · $durationText"
}

@PreviewScreenSizes
@Composable
private fun PlaylistDetailHeaderPreview() {
    PreviewTheme {
        PlaylistDetailHeader(
            name = "Morning Run",
            trackCount = 18,
            totalDurationMs = 4_280_000L,
        )
    }
}
