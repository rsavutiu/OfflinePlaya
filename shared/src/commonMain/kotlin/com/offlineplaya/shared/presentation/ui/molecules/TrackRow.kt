package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
import com.offlineplaya.shared.presentation.ui.nowPlayingSharedArtKey
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Track list row with album art, title, artist, and duration.
 * When [isPlaying] is true, the row gets a subtle accent tint, bold title
 * in the primary color, and a ▶ indicator replacing the track number —
 * matching the redesign's "playing state visible in list" guideline.
 *
 * [onClick] plays the track; [onLongClick] (when supplied) opens the track
 * actions sheet — the long-press → add-to-playlist / queue entry point.
 *
 * Set [sharedArtEnabled] to make this row's thumbnail the source of the
 * list → Now Playing shared-element morph. Only safe on lists where a track
 * appears at most once (album / folder / all-tracks / search); leave it off
 * for playlists and the queue, where the same track id can repeat and the
 * shared key would collide.
 *
 * [showTrackNumber] prefixes the artist sub-line with the in-album track
 * number ("4 · Artist"). Meaningful inside an album/folder; noise on a global
 * alphabetical list, so the all-tracks / search list passes false.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    sharedArtEnabled: Boolean = false,
    showTrackNumber: Boolean = true,
) {
    val bgModifier = if (isPlaying) {
        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(bgModifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtThumb(
            track = track,
            size = 44.dp,
            cornerRadius = 8.dp,
            sharedKey = if (sharedArtEnabled) nowPlayingSharedArtKey(track.id) else null,
        )
        Column(
            modifier = Modifier
                .padding(start = AppSpacing.lg)
                .weight(1f),
        ) {
            Text(
                text = track.title,
                style = if (isPlaying) {
                    MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = if (isPlaying) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = if (isPlaying) {
                "▶ ${track.artistName}"
            } else {
                track.trackNumber
                    ?.takeIf { showTrackNumber }
                    ?.let { "$it · ${track.artistName}" }
                    ?: track.artistName
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = track.durationMs.formatDuration(),
            style = MaterialTheme.typography.labelMedium,
            color = if (isPlaying) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun Long?.formatDuration(): String {
    val ms = this ?: return "—:—"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val ss = if (seconds < 10) "0$seconds" else "$seconds"
    if (hours == 0L) return "$minutes:$ss"
    val mm = if (minutes < 10) "0$minutes" else "$minutes"
    return "$hours:$mm:$ss"
}

@PreviewScreenSizes
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

@PreviewScreenSizes
@Composable
private fun TrackRowPlayingPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            TrackRow(
                track = previewTrack(
                    title = "Fainting Spells",
                    artistName = "Crystal Castles",
                    trackNumber = 1,
                    durationMs = 163_000L,
                ),
                onClick = {},
                isPlaying = true,
            )
        }
    }
}

@PreviewScreenSizes
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
