package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Track number column, right-aligned, fixed width so titles line up.
        Text(
            text = track.trackNumber?.toString() ?: "—",
            modifier = Modifier.width(28.dp), // fixed digit-column width, not a spacing token
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
        Column(
            modifier = Modifier
                .padding(start = AppSpacing.lg)
                .weight(1f),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = track.durationMs.formatDuration(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun Long?.formatDuration(): String {
    val ms = this ?: return "—:—"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val secStr = if (seconds < 10) "0$seconds" else "$seconds"
    return "$minutes:$secStr"
}

@Preview
@Composable
private fun TrackRowFullPreview() {
    PreviewTheme {
        Surface {
            TrackRow(
                track = previewTrack(
                    title = "Once",
                    artistName = "Pearl Jam",
                    trackNumber = 1,
                    durationMs = 224_000L,
                ),
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun TrackRowMissingTrackNumberPreview() {
    PreviewTheme {
        Surface {
            TrackRow(
                track = previewTrack(
                    title = "Unknown track",
                    artistName = "Unknown Artist",
                    trackNumber = null,
                    durationMs = null,
                ),
                onClick = {},
            )
        }
    }
}

private fun previewTrack(
    title: String,
    artistName: String,
    trackNumber: Int?,
    durationMs: Long?,
) = Track(
    id = 0L,
    documentUri = "preview",
    treeUri = "preview",
    relativePath = "x/$title",
    fileName = "$title.mp3",
    title = title,
    artistName = artistName,
    albumArtistName = null,
    albumName = "Album",
    genre = null,
    year = 1991,
    trackNumber = trackNumber,
    discNumber = null,
    durationMs = durationMs,
    bitrate = 320_000,
    sampleRate = 44_100,
    channels = 2,
    codec = "mp3",
    artistId = 1L,
    albumId = 1L,
    folderId = 1L,
    scanStatus = com.offlineplaya.shared.domain.model.ScanStatus.SCANNED,
)
