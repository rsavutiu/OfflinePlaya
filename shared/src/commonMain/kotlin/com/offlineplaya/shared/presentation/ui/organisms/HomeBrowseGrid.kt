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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import offlineplaya.shared.generated.resources.playlist_motif
import offlineplaya.shared.generated.resources.vinyl_record
import offlineplaya.shared.generated.resources.waveform_motif
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
    // The grid fills the vertical space the page hands it (~60% of the
    // screen) — two tall rows. Overlap is prevented by layout instead of
    // size: motif art is confined to the right side, labels to bottom-left.
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
    // Vertical split: the motif art owns the full width of the card above
    // the label strip, so it renders large without ever sitting under the
    // text (the original full-bleed version drew labels on top of the art).
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(width = 1.dp, color = motifColor.copy(alpha = 0.35f), shape = cardShape)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawBehind { drawBrowseMotif(motif, motifColor) },
        ) {
            // The performer silhouette is a real traced asset (tintable),
            // tinted to the accent at the same weight as the vector motifs.
            if (motif == BrowseMotif.PERFORMER) {
                Image(
                    painter = painterResource(Res.drawable.freddie_silhouette),
                    contentDescription = null,
                    // Fit (not Crop) so the whole silhouette is visible —
                    // cropping cut Freddie's head off at the card edge.
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    colorFilter = ColorFilter.tint(motifColor.copy(alpha = 0.30f)),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                )
            }
            // The vinyl motif is a monochrome trace of a real turntable photo
            // (vinyl_record vector) — same watermark treatment.
            if (motif == BrowseMotif.VINYL) {
                Image(
                    painter = painterResource(Res.drawable.vinyl_record),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    colorFilter = ColorFilter.tint(motifColor.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (motif == BrowseMotif.WAVEFORM) {
                Image(
                    painter = painterResource(Res.drawable.waveform_motif),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    colorFilter = ColorFilter.tint(motifColor.copy(alpha = 0.32f)),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            if (motif == BrowseMotif.PLAYLIST) {
                Image(
                    painter = painterResource(Res.drawable.playlist_motif),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    colorFilter = ColorFilter.tint(motifColor.copy(alpha = 0.32f)),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp),
        ) {
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

/** Which themed watermark a [BrowseCard] paints behind its content. */
private enum class BrowseMotif { WAVEFORM, VINYL, PERFORMER, PLAYLIST }

/**
 * Soft violet radial glow behind each card's motif art. The motifs
 * themselves are tinted vector assets (waveform_motif / vinyl_record /
 * freddie_silhouette / playlist_motif) drawn as Image overlays.
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
}
