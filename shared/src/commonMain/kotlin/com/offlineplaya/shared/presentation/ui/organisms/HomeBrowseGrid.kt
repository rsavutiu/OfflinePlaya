package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Track
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.freddie_silhouette
import offlineplaya.shared.generated.resources.home_label_albums
import offlineplaya.shared.generated.resources.home_label_albums_count
import offlineplaya.shared.generated.resources.home_label_artists
import offlineplaya.shared.generated.resources.home_label_artists_count
import offlineplaya.shared.generated.resources.home_label_playlists
import offlineplaya.shared.generated.resources.home_label_playlists_count
import offlineplaya.shared.generated.resources.home_label_songs_count
import offlineplaya.shared.generated.resources.home_title_all_tracks
import org.jetbrains.compose.resources.painterResource
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
                motif = BrowseMotif.WAVEFORM,
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
                motif = BrowseMotif.VINYL,
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
                motif = BrowseMotif.PERFORMER,
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
                motif = BrowseMotif.PLAYLIST,
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
    motif: BrowseMotif,
    albumIds: PersistentList<Long>,
    representativeTrackOfAlbum: suspend (Long) -> Track?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Captured outside the draw lambda — MaterialTheme isn't readable inside
    // DrawScope. Each card paints a faint themed motif (waveform / vinyl /
    // mic / playlist) as a violet watermark between the surface fill and the
    // foreground content, so the cards read as "kinds of music" at a glance.
    val motifColor = MaterialTheme.colorScheme.primary
    val cardShape = RoundedCornerShape(16.dp)
    // Always clickable. Previous versions disabled the cards while a
    // library scan was in flight, but that hid a real bug — the
    // browse destinations are designed to handle their own empty /
    // loading state, and locking out navigation just made the home
    // page look unresponsive during the most common first-launch flow
    // (the initial scan).
    Box(
        modifier = modifier
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .drawBehind { drawBrowseMotif(motif, motifColor) }
            .border(width = 1.dp, color = motifColor.copy(alpha = 0.35f), shape = cardShape)
            .clickable(onClick = onClick),
    ) {
        // The performer silhouette is a real traced asset (tintable PNG),
        // not a primitive drawing — rendered behind everything, tinted to
        // the accent at the same weight as the vector motifs, biased left
        // so it clears the corner cover fan.
        if (motif == BrowseMotif.PERFORMER) {
            Image(
                painter = painterResource(Res.drawable.freddie_silhouette),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomStart,
                colorFilter = ColorFilter.tint(motifColor.copy(alpha = 0.30f)),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, bottom = 6.dp, start = 6.dp, end = 44.dp),
            )
        }
        if (albumIds.isNotEmpty()) {
            CoverFan(
                albumIds = albumIds,
                representativeTrackOfAlbum = representativeTrackOfAlbum,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp),
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
                    // Accented so each browse card's label reacts to the album
                    // color, matching the home hero title. The subtitle below
                    // stays neutral (outline) for hierarchy.
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        // Secondary-text token, not `outline` (a border color too
                        // dim for text — failed WCAG AA on the dark surface).
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val coverSize = 48.dp
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
                rotation = -4f * i,
                translateX = (-5 * i).dp,
                translateY = (2 * i).dp,
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

/** Which themed watermark a [BrowseCard] paints behind its content. */
private enum class BrowseMotif { WAVEFORM, VINYL, PERFORMER, PLAYLIST }

/**
 * Paints the per-card motif: a soft violet radial glow anchored under the
 * motif's focal point, then bold vector strokes on top. Everything is
 * vector — strokes, fills and the gradient are sized off the card's own
 * dimensions — so it stays crisp at any density and costs no image assets.
 * Motifs are biased toward the left/centre, away from the small cover fan
 * tucked in the top-right corner.
 */
private fun DrawScope.drawBrowseMotif(motif: BrowseMotif, base: Color) {
    val anchor = when (motif) {
        BrowseMotif.WAVEFORM -> Offset(size.width * 0.40f, size.height * 0.62f)
        BrowseMotif.VINYL -> Offset(size.width * 0.44f, size.height * 0.60f)
        BrowseMotif.PERFORMER -> Offset(size.width * 0.42f, size.height * 0.55f)
        BrowseMotif.PLAYLIST -> Offset(size.width * 0.36f, size.height * 0.52f)
    }
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(base.copy(alpha = 0.22f), Color.Transparent),
            center = anchor,
            radius = size.maxDimension * 0.85f,
        ),
    )
    when (motif) {
        BrowseMotif.WAVEFORM -> drawWaveform(base)
        BrowseMotif.VINYL -> drawVinyl(base)
        // PERFORMER's figure is a tinted Image overlay in BrowseCard; here we
        // only lay down the shared gradient glow behind it.
        BrowseMotif.PERFORMER -> Unit
        BrowseMotif.PLAYLIST -> drawPlaylist(base)
    }
}

private fun DrawScope.drawWaveform(base: Color) {
    val color = base.copy(alpha = 0.30f)
    val barCount = 11
    // Inset on all borders so the waveform reads as a contained motif
    // rather than running into the card's rounded edge. The cover fan
    // already lives in the top-right corner, so the top padding also
    // keeps the bars from crowding it.
    val padX = size.width * 0.08f
    val padY = size.height * 0.10f
    val drawW = size.width - padX * 2f
    val drawH = size.height - padY * 2f
    val midY = padY + drawH * 0.52f
    val maxBar = drawH * 0.96f
    val slotW = drawW / barCount
    val barW = slotW * 0.44f
    for (i in 0 until barCount) {
        val rel = 0.28f + 0.72f * abs(sin(i * 0.8f + 0.4f))
        val h = maxBar * rel
        val cx = padX + slotW * (i + 0.5f)
        drawRoundRect(
            color = color,
            topLeft = Offset(cx - barW / 2f, midY - h / 2f),
            size = Size(barW, h),
            cornerRadius = CornerRadius(barW / 2f, barW / 2f),
        )
    }
}

private fun DrawScope.drawVinyl(base: Color) {
    val center = Offset(size.width * 0.44f, size.height * 0.60f)
    val outer = size.minDimension * 0.72f
    // Solid disc body — this is what makes it read as a record rather than
    // a wireframe of rings.
    drawCircle(color = base.copy(alpha = 0.32f), radius = outer, center = center)
    // Grooves cut into the disc: thin concentric rings, a touch brighter
    // than the body so they catch the eye like real grooves.
    val grooveWidth = size.minDimension * 0.011f
    val rings = 7
    for (i in 1..rings) {
        val r = outer * (0.30f + 0.68f * i / rings)
        drawCircle(
            color = base.copy(alpha = 0.16f),
            radius = r,
            center = center,
            style = Stroke(width = grooveWidth),
        )
    }
    // Solid centre label + bright spindle pin.
    drawCircle(color = base.copy(alpha = 0.55f), radius = outer * 0.26f, center = center)
    drawCircle(color = base.copy(alpha = 0.85f), radius = outer * 0.05f, center = center)
}

private fun DrawScope.drawPlaylist(base: Color) {
    val color = base.copy(alpha = 0.30f)
    val w = size.width
    val h = size.height
    val rows = 4
    val startX = w * 0.16f
    val bulletR = w * 0.034f
    val top = h * 0.30f
    val gap = h * 0.15f
    for (i in 0 until rows) {
        val y = top + gap * i
        drawCircle(color = color, radius = bulletR, center = Offset(startX, y))
        val lineLen = w * (0.56f - i * 0.07f)
        drawLine(
            color = color,
            start = Offset(startX + bulletR * 2.4f, y),
            end = Offset(startX + bulletR * 2.4f + lineLen, y),
            strokeWidth = h * 0.026f,
            cap = StrokeCap.Round,
        )
    }
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
