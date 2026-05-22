package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.formatDuration
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Vertical list of queued tracks. The track at [currentIndex] is highlighted.
 * Each row has a close button that calls [onRemove] with the row index.
 * Tapping a row jumps to that track via [onJumpTo].
 */
@Composable
fun QueueList(
    tracks: List<Track>,
    currentIndex: Int,
    onJumpTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (tracks.isEmpty()) {
        EmptyState(
            title = "Queue is empty",
            subtitle = "Start playback from the library to populate the queue.",
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        itemsIndexed(items = tracks, key = { idx, track -> "$idx-${track.id}" }) { idx, track ->
            QueueRow(
                track = track,
                isCurrent = idx == currentIndex,
                onClick = { onJumpTo(idx) },
                onRemove = { onRemove(idx) },
            )
        }
    }
}

@Composable
private fun QueueRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The "currently playing" highlight uses surfaceVariant (subtle tint)
    // plus a leading equalizer-glyph instead of a full primaryContainer fill.
    // The old fill overpowered the queue — only the playing row was visible
    // and every other entry felt like a backdrop. M3's emphasis pattern is
    // "marker + slight tint", not "flood the row with brand color".
    val rowBackground = if (isCurrent) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(44.dp)) {
            AlbumArtThumb(track = track, size = 44.dp, cornerRadius = 8.dp)
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Now playing",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .padding(start = AppSpacing.lg)
                .weight(1f),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.artistName} · ${track.durationMs.formatDuration()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove from queue",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun QueueListPopulatedPreview() {
    PreviewTheme {
        Surface {
            QueueList(
                tracks = listOf(
                    sampleQueueTrack(1, "Once", "Pearl Jam"),
                    sampleQueueTrack(2, "Even Flow", "Pearl Jam"),
                    sampleQueueTrack(3, "Alive", "Pearl Jam"),
                ),
                currentIndex = 1,
                onJumpTo = {},
                onRemove = {},
            )
        }
    }
}

@Preview
@Composable
private fun QueueListEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            QueueList(tracks = emptyList(), currentIndex = -1, onJumpTo = {}, onRemove = {})
        }
    }
}

private fun sampleQueueTrack(id: Long, title: String, artist: String) = Track(
    id = id,
    documentUri = "u/$id",
    treeUri = "t",
    relativePath = "$artist/Ten/$title.flac",
    fileName = "$title.flac",
    title = title,
    artistName = artist,
    albumArtistName = null,
    albumName = "Ten",
    genre = "Rock",
    year = 1991,
    trackNumber = id.toInt(),
    discNumber = 1,
    durationMs = 224_000L,
    bitrate = 1_000_000,
    sampleRate = 44_100,
    channels = 2,
    codec = "flac",
    artistId = 1L,
    albumId = 1L,
    folderId = 1L,
    scanStatus = ScanStatus.SCANNED,
)
