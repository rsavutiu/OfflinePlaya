package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.PlayedTrack
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.stats_plays
import org.jetbrains.compose.resources.pluralStringResource

/**
 * Ranked most-played row: rank numeral, art, title/artist, trailing play
 * count. Used by the artist-detail "Top tracks" section and the stats page —
 * anywhere a track's *play count* (not its duration) is the story.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopTrackRow(
    rank: Int,
    playedTrack: PlayedTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = LocalBrandAccent.current.accent,
            modifier = Modifier.width(24.dp),
        )
        AlbumArtThumb(
            track = playedTrack.track,
            size = 40.dp,
            cornerRadius = 8.dp,
        )
        Column(
            modifier = Modifier
                .padding(start = AppSpacing.lg)
                .weight(1f),
        ) {
            Text(
                text = playedTrack.track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = playedTrack.track.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = pluralStringResource(
                Res.plurals.stats_plays,
                playedTrack.playCount.toInt(),
                playedTrack.playCount.toInt(),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewScreenSizes
@Composable
private fun TopTrackRowPreview() {
    PreviewTheme {
        Surface {
            Column {
                TopTrackRow(
                    rank = 1,
                    playedTrack = PlayedTrack(previewTopTrack("Alive", "Pearl Jam"), 42),
                    onClick = {},
                )
                TopTrackRow(
                    rank = 2,
                    playedTrack = PlayedTrack(previewTopTrack("Black", "Pearl Jam"), 1),
                    onClick = {},
                )
            }
        }
    }
}

private fun previewTopTrack(title: String, artistName: String) = Track(
    id = title.hashCode().toLong(),
    documentUri = "preview/$title",
    treeUri = "preview",
    relativePath = "x/$title",
    fileName = "$title.mp3",
    title = title,
    artistName = artistName,
    albumArtistName = null,
    albumName = "Ten",
    genre = null,
    year = 1991,
    trackNumber = 3,
    discNumber = null,
    durationMs = 224_000L,
    bitrate = 320_000,
    sampleRate = 44_100,
    channels = 2,
    codec = "mp3",
    artistId = 1L,
    albumId = 1L,
    folderId = 1L,
    scanStatus = ScanStatus.SCANNED,
)
