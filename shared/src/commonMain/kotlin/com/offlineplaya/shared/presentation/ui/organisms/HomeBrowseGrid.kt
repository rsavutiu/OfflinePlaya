package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Track
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.home_label_albums
import offlineplaya.shared.generated.resources.home_label_albums_count
import offlineplaya.shared.generated.resources.home_label_artists
import offlineplaya.shared.generated.resources.home_label_artists_count
import offlineplaya.shared.generated.resources.home_label_playlists
import offlineplaya.shared.generated.resources.home_label_playlists_count
import offlineplaya.shared.generated.resources.home_label_songs_count
import offlineplaya.shared.generated.resources.home_title_all_tracks
import org.jetbrains.compose.resources.stringResource

/**
 * 2x2 grid of browse cards (All tracks / Albums / Artists / Playlists). Each
 * card carries a fanned stack of mini covers in its top-right corner so the
 * page feels like a real shelf of music rather than four labeled rectangles.
 *
 * The collage source is sliced four-at-a-time with index-based offsets so the
 * cards don't look identical even with a tiny library. Wraps modulo the
 * source length so blanks never appear.
 */
@Composable
fun HomeBrowseGrid(
    trackCount: Long,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    collageSource: List<Album>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    scanning: Boolean,
    onOpenAllTracks: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenPlaylists: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun slice(index: Int): List<Album> {
        if (collageSource.isEmpty()) return emptyList()
        return List(4) { i -> collageSource[(index * 4 + i) % collageSource.size] }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BrowseCard(
                icon = Icons.Default.MusicNote,
                title = stringResource(Res.string.home_title_all_tracks),
                subtitle = stringResource(Res.string.home_label_songs_count, trackCount.toInt()),
                albums = slice(0),
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                enabled = !scanning,
                onClick = onOpenAllTracks,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            )
            BrowseCard(
                icon = Icons.Default.Album,
                title = stringResource(Res.string.home_label_albums),
                subtitle = stringResource(Res.string.home_label_albums_count, albumCount),
                albums = slice(1),
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                enabled = !scanning,
                onClick = onOpenAlbums,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BrowseCard(
                icon = Icons.Default.Person,
                title = stringResource(Res.string.home_label_artists),
                subtitle = stringResource(Res.string.home_label_artists_count, artistCount),
                albums = slice(2),
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                enabled = !scanning,
                onClick = onOpenArtists,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            )
            BrowseCard(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = stringResource(Res.string.home_label_playlists),
                subtitle = stringResource(Res.string.home_label_playlists_count, playlistCount),
                albums = slice(3),
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                enabled = !scanning,
                onClick = onOpenPlaylists,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BrowseCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    albums: List<Album>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (albums.isNotEmpty()) {
            // Centered vertically on the right edge — the card is about the
            // art, so let the deck dominate. Padding only on the trailing edge;
            // the back layers of the fan spill leftward via negative offsets.
            CoverFan(
                albums = albums.take(5),
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverFan(
    albums: List<Album>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    modifier: Modifier = Modifier,
) {
    val coverSize = 72.dp
    Box(modifier = modifier.size(coverSize)) {
        val fan = albums.take(5)
        // Drawn back-to-front so the front cover (index 0) lands on top,
        // upright and fully opaque; layers behind rotate, shift, and dim.
        // Spread scales with cover size — at 72dp the original 8/3dp offsets
        // looked tight, so the deck spreads ~50% wider too.
        fan.indices.reversed().forEach { i ->
            FanItem(
                album = fan[i],
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                size = coverSize,
                rotation = -7f * i,
                translateX = (-12 * i).dp,
                translateY = (4 * i).dp,
                alpha = when (i) {
                    0 -> 1f
                    1 -> 0.8f
                    2 -> 0.6f
                    3 -> 0.45f
                    else -> 0.32f
                },
            )
        }
    }
}

@Composable
private fun FanItem(
    album: Album,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    size: Dp,
    rotation: Float,
    translateX: Dp,
    translateY: Dp,
    alpha: Float,
) {
    var track by remember(album.id) { mutableStateOf<Track?>(null) }
    LaunchedEffect(album.id) { track = representativeTrackOfAlbum(album.id) }
    val current = track ?: return
    // Render the slot ONLY when Coil resolves real artwork. Albums without art
    // simply leave their slot empty rather than showing a placeholder glyph.
    SubcomposeAsyncImage(
        model = current,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .offset(x = translateX, y = translateY)
            .graphicsLayer {
                rotationZ = rotation
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(6.dp)),
        loading = {},
        error = {},
        success = { SubcomposeAsyncImageContent() },
    )
}
