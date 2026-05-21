package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
import com.offlineplaya.shared.presentation.ui.atoms.PlayButton
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * @param representativeTrackProvider lazy lookup for one track of this album,
 *   used purely to drive the album-art thumbnail through Coil's `TrackArtFetcher`.
 *   Default returns `null` so previews / call sites without a backing
 *   repository still render — the row falls back to the placeholder glyph
 *   exactly like before.
 */
@Composable
fun AlbumRow(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    representativeTrackProvider: suspend (Long) -> Track? = { null },
    onPlay: () -> Unit
) {
    var representativeTrack by remember(album.id) { mutableStateOf<Track?>(null) }
    LaunchedEffect(album.id) {
        representativeTrack = representativeTrackProvider(album.id)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtThumb(
            track = representativeTrack,
            size = 48.dp,
            cornerRadius = 10.dp, // matches AppShapes.tile
        )
        Column(modifier = Modifier.padding(start = AppSpacing.lg).weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitleFor(album),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PlayButton(onPlay = onPlay)
    }
}

private fun subtitleFor(album: Album): String {
    val yearPart = album.year?.toString()
    val trackPart = "${album.trackCount} ${if (album.trackCount == 1) "track" else "tracks"}"
    return listOfNotNull(yearPart, trackPart).joinToString(" · ")
}

@Preview
@Composable
private fun AlbumRowFullPreview() {
    PreviewTheme {
        Surface {
            AlbumRow(
                album = Album(id = 1, name = "Ten", artistId = 1, year = 1991, trackCount = 11, durationMs = 0),
                onClick = {},
                onPlay = {}
            )
        }
    }
}

@Preview
@Composable
private fun AlbumRowNoYearPreview() {
    PreviewTheme {
        Surface {
            AlbumRow(
                album = Album(id = 2, name = "Untitled Bootleg", artistId = 1, year = null, trackCount = 7, durationMs = 0),
                onClick = {},
                onPlay = {}
            )
        }
    }
}
