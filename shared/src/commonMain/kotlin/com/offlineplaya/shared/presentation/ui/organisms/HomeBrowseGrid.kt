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
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
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
 * 2x2 grid of browse cards (All tracks / Albums / Artists / Playlists).
 * Each card carries a fanned stack of mini covers in its top-right
 * corner so the page feels like a real shelf of music rather than four
 * labelled rectangles.
 *
 * [collageSource] is the persisted "recently played" album list, empty
 * on a fresh install. All four cards share the same fan — the first
 * [COVERS_PER_CARD] recent albums — so a single play populates every
 * card at once. The source only changes when the user plays something,
 * never during a background scan, so the fans never flicker mid-scan.
 */
@Composable
fun HomeBrowseGrid(
    trackCount: Long,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    collageSource: PersistentList<Album>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    onOpenAllTracks: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenPlaylists: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Per-card persistent assignment. Outer list is per-card (size 4),
    // inner list is the album IDs currently displayed in that card's fan.
    // Every card draws its fan from the same pool — the first few recent
    // albums. They share covers (which is fine — "you can mix them I
    // dont care"), so all four cards populate the instant the user plays
    // one album, instead of needing 5/9/13 distinct plays to fill the
    // later cards. The recent-albums list only changes on a play action,
    // never during a background scan, so this can't flicker mid-scan.
    val fanIds = remember(collageSource) {
        collageSource.take(COVERS_PER_CARD).map { it.id }.toPersistentList()
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
                albumIds = fanIds,
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                onClick = onOpenAllTracks,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            )
            BrowseCard(
                icon = Icons.Default.Album,
                title = stringResource(Res.string.home_label_albums),
                subtitle = stringResource(Res.string.home_label_albums_count, albumCount),
                albumIds = fanIds,
                representativeTrackOfAlbum = representativeTrackOfAlbum,
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
                albumIds = fanIds,
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                onClick = onOpenArtists,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            )
            BrowseCard(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = stringResource(Res.string.home_label_playlists),
                subtitle = stringResource(Res.string.home_label_playlists_count, playlistCount),
                albumIds = fanIds,
                representativeTrackOfAlbum = representativeTrackOfAlbum,
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
    albumIds: PersistentList<Long>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Always clickable. Previous versions disabled the cards while a
    // library scan was in flight, but that hid a real bug — the
    // browse destinations are designed to handle their own empty /
    // loading state, and locking out navigation just made the home
    // page look unresponsive during the most common first-launch flow
    // (the initial scan).
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        if (albumIds.isNotEmpty()) {
            CoverFan(
                albumIds = albumIds,
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
    albumIds: PersistentList<Long>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    modifier: Modifier = Modifier,
) {
    val coverSize = 72.dp
    Box(modifier = modifier.size(coverSize)) {
        // Front-most cover renders last so it lands on top, upright and
        // fully opaque. Back layers rotate, shift, and dim.
        //
        // Tuned for a 6-deep fan: the per-layer offsets are tighter than
        // a naive linear spread would be so all six covers fit inside the
        // card quadrant. Total horizontal spread is 5 × 8dp = 40dp (only
        // ~4dp wider than the old 4-deep fan), and the rotation tops out
        // at 5 × 5° = 25° on the backmost layer instead of running away.
        // The card's outer Box clips overflow and draws the title text on
        // top of the fan, so the faint back layers never hurt legibility.
        val take = albumIds.take(COVERS_PER_CARD)
        take.indices.reversed().forEach { i ->
            FanItem(
                albumId = take[i],
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                size = coverSize,
                rotation = -5f * i,
                translateX = (-8 * i).dp,
                translateY = (3 * i).dp,
                // Even-ish opacity decay from 1.0 (front) to ~0.32 (back)
                // across all six layers, so each cover reads as a distinct
                // sliver of depth rather than a flat faint stack.
                alpha = when (i) {
                    0 -> 1f
                    1 -> 0.84f
                    2 -> 0.68f
                    3 -> 0.54f
                    4 -> 0.42f
                    else -> 0.32f
                },
            )
        }
    }
}

@Composable
private fun FanItem(
    albumId: Long,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    size: Dp,
    rotation: Float,
    translateX: Dp,
    translateY: Dp,
    alpha: Float,
) {
    // Resolve the representative Track for this album exactly once per
    // albumId. The `remember(albumId)` key ties the Track state to the
    // ID — recomposition that keeps the same ID (the common case now
    // that fans are append-only) never re-issues the suspend lookup, so
    // Coil keeps the cached bitmap.
    var track by remember(albumId) { mutableStateOf<Track?>(null) }
    LaunchedEffect(albumId) { track = representativeTrackOfAlbum(albumId) }
    val current = track ?: return
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

/**
 * Number of cover-art slots in each Browse card's fan. Every card shows
 * the first [COVERS_PER_CARD] recent albums (shared across all four
 * cards), so the fans fill the moment the user plays a single album
 * rather than needing many distinct plays.
 *
 * Bumping this also requires re-tuning the per-layer offset/rotation/
 * alpha in [CoverFan] — the geometry is hand-fitted to this count so
 * the back layers stay inside the card quadrant and remain visually
 * distinct. See the comment block there.
 */
internal const val COVERS_PER_CARD = 6
