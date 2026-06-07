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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin
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
import offlineplaya.shared.generated.resources.vinyl_record
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * 2x2 grid of browse cards (All tracks / Albums / Artists / Playlists).
 * Each card carries a faint themed motif watermark (waveform / vinyl /
 * performer / playlist) so it reads as a "kind of music" at a glance rather
 * than a labelled rectangle.
 */
@Composable
fun HomeBrowseGrid(
    trackCount: Long,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    onOpenAllTracks: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenPlaylists: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                title = stringResource(Res.string.home_title_all_tracks),
                subtitle = stringResource(Res.string.home_label_songs_count, trackCount.toInt()),
                motif = BrowseMotif.WAVEFORM,
                onClick = onOpenAllTracks,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            )
            BrowseCard(
                title = stringResource(Res.string.home_label_albums),
                subtitle = stringResource(Res.string.home_label_albums_count, albumCount),
                motif = BrowseMotif.VINYL,
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
                title = stringResource(Res.string.home_label_artists),
                subtitle = stringResource(Res.string.home_label_artists_count, artistCount),
                motif = BrowseMotif.PERFORMER,
                onClick = onOpenArtists,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            )
            BrowseCard(
                title = stringResource(Res.string.home_label_playlists),
                subtitle = stringResource(Res.string.home_label_playlists_count, playlistCount),
                motif = BrowseMotif.PLAYLIST,
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
    title: String,
    subtitle: String,
    motif: BrowseMotif,
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
        // the accent at the same weight as the vector motifs.
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
        // The vinyl motif is a monochrome trace of a real turntable photo
        // (vinyl_record vector), tinted to the accent as a watermark — same
        // treatment as the performer silhouette.
        if (motif == BrowseMotif.VINYL) {
            Image(
                painter = painterResource(Res.drawable.vinyl_record),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                colorFilter = ColorFilter.tint(motifColor.copy(alpha = 0.28f)),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            // Title/subtitle pinned to the bottom (the top-left icon was
            // removed; the per-card motif watermark carries the identity).
            verticalArrangement = Arrangement.Bottom,
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    // Accented so each browse card's label reacts to the album
                    // color, matching the home hero title. The subtitle below
                    // stays neutral for hierarchy.
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

/** Which themed watermark a [BrowseCard] paints behind its content. */
private enum class BrowseMotif { WAVEFORM, VINYL, PERFORMER, PLAYLIST }

/**
 * Paints the per-card motif: a soft violet radial glow anchored under the
 * motif's focal point, then bold vector strokes on top. Everything is
 * vector — strokes, fills and the gradient are sized off the card's own
 * dimensions — so it stays crisp at any density and costs no image assets.
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
        // VINYL and PERFORMER are tinted Image overlays in BrowseCard; here we
        // only lay down the shared gradient glow behind them.
        BrowseMotif.VINYL -> Unit
        BrowseMotif.PERFORMER -> Unit
        BrowseMotif.PLAYLIST -> drawPlaylist(base)
    }
}

private fun DrawScope.drawWaveform(base: Color) {
    val color = base.copy(alpha = 0.30f)
    val barCount = 11
    // Inset on all borders so the waveform reads as a contained motif
    // rather than running into the card's rounded edge.
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
