package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.LocalOrientation
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.library_album_detail_play
import offlineplaya.shared.generated.resources.library_album_detail_track
import offlineplaya.shared.generated.resources.library_album_detail_tracks
import org.jetbrains.compose.resources.stringResource

/**
 * Album-detail page hero: gradient background, album art, title + metadata,
 * Play and Shuffle primary actions. Portrait stacks (art + text, then full-
 * width buttons); landscape compresses into a single inline row so the track
 * list isn't pushed off-screen on short windows.
 */
@Composable
fun AlbumDetailHeader(
    album: Album?,
    artistName: String?,
    representativeTrack: Track?,
    trackCount: Int,
    totalDurationMs: Long,
    canPlay: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.background,
        ),
    )
    val landscape = LocalOrientation.current.isLandscape
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(
                horizontal = AppSpacing.lg,
                vertical = if (landscape) AppSpacing.sm else AppSpacing.md,
            ),
    ) {
        if (landscape) {
            LandscapeBody(
                album = album,
                artistName = artistName,
                representativeTrack = representativeTrack,
                trackCount = trackCount,
                totalDurationMs = totalDurationMs,
                canPlay = canPlay,
                onPlay = onPlay,
                onShuffle = onShuffle,
            )
        } else {
            PortraitBody(
                album = album,
                artistName = artistName,
                representativeTrack = representativeTrack,
                trackCount = trackCount,
                totalDurationMs = totalDurationMs,
                canPlay = canPlay,
                onPlay = onPlay,
                onShuffle = onShuffle,
            )
        }
    }
}

@Composable
private fun PortraitBody(
    album: Album?,
    artistName: String?,
    representativeTrack: Track?,
    trackCount: Int,
    totalDurationMs: Long,
    canPlay: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Box(modifier = Modifier.size(72.dp)) {
            AlbumArtThumb(
                track = representativeTrack,
                size = 72.dp,
                cornerRadius = 10.dp,
                glyphStyle = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(Modifier.width(AppSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album?.name ?: "",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (artistName != null) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AlbumMetaText(album, trackCount, totalDurationMs)
        }
    }
    Spacer(Modifier.height(AppSpacing.md))
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Button(
            onClick = onPlay,
            enabled = canPlay,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(Res.string.library_album_detail_play))
        }
        OutlinedButton(
            onClick = onShuffle,
            enabled = canPlay,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.Shuffle, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Shuffle")
        }
    }
    Spacer(Modifier.height(AppSpacing.sm))
}

@Composable
private fun LandscapeBody(
    album: Album?,
    artistName: String?,
    representativeTrack: Track?,
    trackCount: Int,
    totalDurationMs: Long,
    canPlay: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        AlbumArtThumb(
            track = representativeTrack,
            size = 56.dp,
            cornerRadius = 8.dp,
            glyphStyle = MaterialTheme.typography.titleMedium,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album?.name ?: "",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val parts = buildList {
                if (artistName != null) add(artistName)
                album?.year?.let { add(it.toString()) }
                add(tracksText(trackCount))
                add(totalDurationMs.formatAlbumDuration())
            }
            Text(
                text = parts.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onPlay,
            enabled = canPlay,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            contentPadding = PaddingValues(
                horizontal = AppSpacing.md,
                vertical = AppSpacing.xs,
            ),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(Res.string.library_album_detail_play))
        }
        OutlinedButton(
            onClick = onShuffle,
            enabled = canPlay,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(
                horizontal = AppSpacing.md,
                vertical = AppSpacing.xs,
            ),
        ) {
            Icon(Icons.Default.Shuffle, contentDescription = null)
        }
    }
}

@Composable
private fun AlbumMetaText(album: Album?, trackCount: Int, totalDurationMs: Long) {
    val yearText = album?.year?.toString()
    val countText = tracksText(trackCount)
    val durationText = totalDurationMs.formatAlbumDuration()
    Text(
        text = listOfNotNull(yearText, countText, durationText).joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun tracksText(count: Int): String {
    val word = if (count == 1) {
        stringResource(Res.string.library_album_detail_track)
    } else {
        stringResource(Res.string.library_album_detail_tracks)
    }
    return "$count $word"
}

private fun Long.formatAlbumDuration(): String {
    val totalMinutes = this / 60_000
    return if (totalMinutes >= 60) {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        "${h}h ${m}m"
    } else {
        "${totalMinutes}m"
    }
}

@PreviewScreenSizes
@Composable
private fun AlbumDetailHeaderPreview() {
    PreviewTheme(darkTheme = true) {
        AlbumDetailHeader(
            album = Album(1, "Ten", 1, 1991, 11, 3_320_000),
            artistName = "Pearl Jam",
            representativeTrack = null,
            trackCount = 11,
            totalDurationMs = 3_320_000,
            canPlay = true,
            onPlay = {}, onShuffle = {},
        )
    }
}
