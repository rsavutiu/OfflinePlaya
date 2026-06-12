package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "cardPress",
    )
    // One slow, seamlessly-looping phase drives every motif's idle motion:
    // 0..2π with linear easing, so any sin/cos(n·phase) is continuous across
    // the wrap and the loop never visibly restarts. The value is read only
    // inside graphicsLayer / drawBehind lambdas, so the motion lives entirely
    // in the draw phase — no recomposition per frame.
    val motifTransition = rememberInfiniteTransition(label = "motifLife")
    val phase by motifTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 6000, easing = LinearEasing)),
        label = "motifPhase",
    )
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(width = 1.dp, color = motifColor.copy(alpha = 0.35f), shape = cardShape)
            .clickable(interactionSource = interactionSource, indication = androidx.compose.material3.ripple(), onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawBehind {
                    drawBrowseMotif(motif, motifColor, pulse = 0.8f + 0.2f * sin(phase))
                    when (motif) {
                        // Drawn (not Images) so their elements can really
                        // move: bars pulse, the record spins, the queue
                        // scrolls upward.
                        BrowseMotif.WAVEFORM -> drawLivingWaveform(motifColor, phase)
                        BrowseMotif.VINYL -> drawSpinningVinyl(motifColor, phase)
                        BrowseMotif.PLAYLIST -> drawScrollingQueue(motifColor, phase)
                        BrowseMotif.PERFORMER -> Unit
                    }
                },
        ) {
            // The performer silhouette is a real traced asset (tintable),
            // tinted to the accent at the same weight as the drawn motifs.
            // Static: a single raster has no elements to animate, and
            // transform tricks (sway) read as wrong rather than alive.
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
 * Soft violet radial glow behind each card's motif art, breathing with
 * [pulse] (≈0.6..1.0 multiplier on the base alpha). The vinyl / performer /
 * playlist motifs are tinted assets drawn as Image overlays; the waveform is
 * drawn live by [drawLivingWaveform].
 */
private fun DrawScope.drawBrowseMotif(motif: BrowseMotif, base: Color, pulse: Float = 1f) {
    val anchor = when (motif) {
        BrowseMotif.WAVEFORM -> Offset(size.width * 0.40f, size.height * 0.62f)
        BrowseMotif.VINYL -> Offset(size.width * 0.44f, size.height * 0.60f)
        BrowseMotif.PERFORMER -> Offset(size.width * 0.42f, size.height * 0.55f)
        BrowseMotif.PLAYLIST -> Offset(size.width * 0.36f, size.height * 0.52f)
    }
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(base.copy(alpha = 0.22f * pulse), Color.Transparent),
            center = anchor,
            radius = size.maxDimension * 0.85f,
        ),
    )
}

/**
 * Per-bar amplitudes traced from the original `waveform_motif` vector
 * (520x260 viewport, one bar every 30 units), so the living version is
 * pixel-faithful to the static asset it replaces.
 */
private val WAVE_AMPS = floatArrayOf(
    12f, 30f, 54f, 84f, 40f, 68f, 104f, 58f, 22f, 76f, 110f, 46f, 90f, 34f, 64f, 20f, 8f,
)

/**
 * The waveform motif, drawn live: the same mirrored rounded bars as the old
 * static vector, but each bar's amplitude breathes around its traced value at
 * its own phase offset — a level meter idling to inaudible music. `2·phase`
 * keeps the loop seamless across the master phase wrap.
 */
private fun DrawScope.drawLivingWaveform(base: Color, phase: Float) {
    // Fit the 520x260 design viewport in the available area, centered —
    // the same footprint the static Image had with ContentScale.Fit.
    val s = min(size.width / 520f, size.height / 260f)
    val ox = (size.width - 520f * s) / 2f
    val midY = (size.height - 260f * s) / 2f + 130f * s
    val barColor = base.copy(alpha = 0.32f)
    WAVE_AMPS.forEachIndexed { i, amp ->
        val x = ox + (18f + i * 30f) * s
        val a = amp * (1f + 0.22f * sin(2f * phase + i * 0.9f)) * s
        drawLine(
            color = barColor,
            start = Offset(x, midY - a),
            end = Offset(x, midY + a),
            strokeWidth = 13f * s,
            cap = StrokeCap.Round,
        )
    }
    // Faint center line, as in the original asset.
    drawLine(
        color = barColor.copy(alpha = barColor.alpha * 0.3f),
        start = Offset(ox + 10f * s, midY),
        end = Offset(ox + 510f * s, midY),
        strokeWidth = 4f * s,
        cap = StrokeCap.Round,
    )
}

/**
 * A vinyl record that actually spins: platter edge, groove band, label and
 * spindle hole are rotation-invariant circles; the visible motion comes from
 * two sheen arcs sweeping the groove band and a dot riding the label —
 * 3 revolutions per phase loop ≈ 2 s/rev, a 33⅓ feel.
 */
private fun DrawScope.drawSpinningVinyl(base: Color, phase: Float) {
    val c = center
    val r = min(size.width, size.height) * 0.42f
    drawCircle(base.copy(alpha = 0.30f), radius = r, center = c, style = Stroke(width = r * 0.045f))
    val groove = base.copy(alpha = 0.14f)
    for (i in 0..3) {
        drawCircle(groove, radius = r * (0.56f + i * 0.105f), center = c, style = Stroke(width = r * 0.018f))
    }
    drawCircle(base.copy(alpha = 0.20f), radius = r * 0.32f, center = c)
    drawCircle(base.copy(alpha = 0.40f), radius = r * 0.05f, center = c)
    rotate(degrees = 3f * phase * (180f / PI.toFloat()), pivot = c) {
        // Light catching the grooves: a wide-stroke arc pair over the band.
        val band = r * 0.79f
        for (start in floatArrayOf(-25f, 155f)) {
            drawArc(
                color = base.copy(alpha = 0.16f),
                startAngle = start,
                sweepAngle = 50f,
                useCenter = false,
                topLeft = Offset(c.x - band, c.y - band),
                size = Size(band * 2f, band * 2f),
                style = Stroke(width = r * 0.30f),
            )
        }
        drawCircle(base.copy(alpha = 0.38f), radius = r * 0.045f, center = Offset(c.x + r * 0.20f, c.y))
    }
}

/** One queue row of the scrolling playlist motif: marker + title + subtitle lengths. */
private class QueueRowSpec(val titleLen: Float, val subtitleLen: Float, val play: Boolean)

/** Row shapes traced from the original `playlist_motif` vector (520x260 viewport). */
private val QUEUE_ROWS = listOf(
    QueueRowSpec(titleLen = 322f, subtitleLen = 164f, play = true),
    QueueRowSpec(titleLen = 258f, subtitleLen = 132f, play = false),
    QueueRowSpec(titleLen = 294f, subtitleLen = 148f, play = false),
)

/**
 * The playlist motif as a queue that scrolls steadily upward, rows fading in
 * at the bottom and out at the top. One full row-pattern (3 rows) passes per
 * phase loop, so the wrap is seamless. The beamed eighth notes stay anchored
 * on the right, as in the original asset.
 */
private fun DrawScope.drawScrollingQueue(base: Color, phase: Float) {
    val s = min(size.width / 520f, size.height / 260f)
    val ox = (size.width - 520f * s) / 2f
    val oy = (size.height - 260f * s) / 2f
    val col = base.copy(alpha = 0.32f)
    val spacing = 68f * s
    val top = oy + 8f * s
    val bottom = oy + 252f * s
    val fadeLen = spacing * 0.6f
    val period = spacing * QUEUE_ROWS.size
    val scroll = (phase / (2f * PI.toFloat())) * period
    val baseY = oy + 50f * s
    val rFirst = floor((top - baseY + scroll) / spacing).toInt() - 1
    val rLast = rFirst + ((bottom - top) / spacing).toInt() + 3
    for (rowIdx in rFirst..rLast) {
        val y = baseY + rowIdx * spacing - scroll
        val fade = ((y - top) / fadeLen).coerceIn(0f, 1f) * ((bottom - y) / fadeLen).coerceIn(0f, 1f)
        if (fade <= 0f) continue
        val spec = QUEUE_ROWS[((rowIdx % QUEUE_ROWS.size) + QUEUE_ROWS.size) % QUEUE_ROWS.size]
        val rowCol = col.copy(alpha = col.alpha * fade)
        val dimCol = col.copy(alpha = col.alpha * 0.4f * fade)
        if (spec.play) {
            val tri = Path().apply {
                moveTo(ox + 30f * s, y - 26f * s)
                lineTo(ox + 76f * s, y)
                lineTo(ox + 30f * s, y + 26f * s)
                close()
            }
            drawPath(tri, rowCol)
        } else {
            drawCircle(rowCol, radius = 14f * s, center = Offset(ox + 50f * s, y))
        }
        drawLine(
            color = rowCol,
            start = Offset(ox + 104f * s, y),
            end = Offset(ox + (104f + spec.titleLen) * s, y),
            strokeWidth = 20f * s,
            cap = StrokeCap.Round,
        )
        val subY = y + 32f * s
        drawLine(
            color = dimCol,
            start = Offset(ox + 104f * s, subY),
            end = Offset(ox + (104f + spec.subtitleLen) * s, subY),
            strokeWidth = 10f * s,
            cap = StrokeCap.Round,
        )
    }
    // Anchored beamed eighth notes (geometry from the original vector).
    drawCircle(col, radius = 15f * s, center = Offset(ox + 448f * s, oy + 196f * s))
    drawCircle(col, radius = 15f * s, center = Offset(ox + 494f * s, oy + 182f * s))
    drawLine(col, Offset(ox + 463f * s, oy + 194f * s), Offset(ox + 463f * s, oy + 108f * s), strokeWidth = 9f * s, cap = StrokeCap.Round)
    drawLine(col, Offset(ox + 509f * s, oy + 180f * s), Offset(ox + 509f * s, oy + 94f * s), strokeWidth = 9f * s, cap = StrokeCap.Round)
    val beam = Path().apply {
        moveTo(ox + 458f * s, oy + 100f * s)
        lineTo(ox + 514f * s, oy + 86f * s)
        lineTo(ox + 514f * s, oy + 112f * s)
        lineTo(ox + 458f * s, oy + 126f * s)
        close()
    }
    drawPath(beam, col)
}
