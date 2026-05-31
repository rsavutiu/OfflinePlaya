package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tall, interactive equalizer graph. Renders, back-to-front:
 *  - a dB grid (lines every 5 dB across the ±15 dB span) with scale labels,
 *  - a filled bar per band from the 0 dB centre line to its gain,
 *  - a soft gradient fill under the response curve,
 *  - a smooth Catmull-Rom → cubic-bézier curve through every band point,
 *  - a draggable thumb (with its dB readout) at each band.
 *
 * Interaction: drag locks to whichever band the gesture started over; tapping
 * sets that band directly. Pointer-Y maps linearly to gain. Disabled mode
 * dims everything and ignores input.
 */
@Composable
fun EqualizerGraph(
    gains: List<Int>,
    enabled: Boolean,
    onBandGainChange: (bandIndex: Int, millibels: Int, fullGains: List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bandCount = gains.size
    if (bandCount == 0) return

    val accentBase = MaterialTheme.colorScheme.primary
    val accent = if (enabled) accentBase else accentBase.copy(alpha = 0.30f)
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
    val centreColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
    val thumbCore = MaterialTheme.colorScheme.surface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val readoutColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val gutterPx = with(density) { GUTTER.toPx() }
    val vPadPx = with(density) { V_PAD.toPx() }

    // Drag callback reads the freshest gain vector so concurrent edits aren't
    // clobbered by a stale capture.
    val latestGains = rememberUpdatedState(gains)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(GRAPH_HEIGHT)
                .clip(RoundedCornerShape(AppSpacing.md))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!enabled) Modifier else Modifier.pointerInput(bandCount) {
                            fun emit(pos: Offset) {
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val colW = (w - gutterPx) / bandCount
                                if (colW <= 0f) return
                                val band = (((pos.x - gutterPx) / colW)).toInt()
                                    .coerceIn(0, bandCount - 1)
                                val span = h / 2f - vPadPx
                                val gain = (((h / 2f - pos.y) / span) * MAX_GAIN_RANGE_MILLIBELS)
                                    .roundToInt()
                                    .coerceIn(MIN_GAIN_RANGE_MILLIBELS, MAX_GAIN_RANGE_MILLIBELS)
                                onBandGainChange(band, gain, latestGains.value)
                            }
                            detectDragGestures(
                                onDragStart = { emit(it) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    emit(change.position)
                                },
                            )
                        },
                    )
                    .then(
                        if (!enabled) Modifier else Modifier.pointerInput(bandCount) {
                            detectTapGestures { pos ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val colW = (w - gutterPx) / bandCount
                                if (colW <= 0f) return@detectTapGestures
                                val band = (((pos.x - gutterPx) / colW)).toInt()
                                    .coerceIn(0, bandCount - 1)
                                val span = h / 2f - vPadPx
                                val gain = (((h / 2f - pos.y) / span) * MAX_GAIN_RANGE_MILLIBELS)
                                    .roundToInt()
                                    .coerceIn(MIN_GAIN_RANGE_MILLIBELS, MAX_GAIN_RANGE_MILLIBELS)
                                onBandGainChange(band, gain, latestGains.value)
                            }
                        },
                    ),
            ) {
                val w = size.width
                val h = size.height
                val centreY = h / 2f
                val span = h / 2f - vPadPx
                val colW = (w - gutterPx) / bandCount

                fun yForGain(g: Int): Float =
                    centreY - (g.toFloat() / MAX_GAIN_RANGE_MILLIBELS) * span

                // dB grid + scale labels
                var line = MIN_GAIN_RANGE_MILLIBELS
                while (line <= MAX_GAIN_RANGE_MILLIBELS) {
                    val y = yForGain(line)
                    val isCentre = line == 0
                    drawLine(
                        color = if (isCentre) centreColor else gridColor,
                        start = Offset(gutterPx, y),
                        end = Offset(w, y),
                        strokeWidth = if (isCentre) 2f else 1f,
                    )
                    val db = line / 100
                    val label = if (db > 0) "+$db" else "$db"
                    val layout = textMeasurer.measure(
                        text = label,
                        style = TextStyle(color = labelColor, fontSize = 9.sp),
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            x = gutterPx - layout.size.width - 4f,
                            y = y - layout.size.height / 2f,
                        ),
                    )
                    line += GRID_STEP_MILLIBELS
                }

                // band points
                val points = ArrayList<Offset>(bandCount)
                for (i in 0 until bandCount) {
                    val x = gutterPx + (i + 0.5f) * colW
                    points.add(Offset(x, yForGain(gains[i])))
                }

                // bars (centre → value)
                val barWidth = colW * 0.42f
                for (i in 0 until bandCount) {
                    val p = points[i]
                    val top = minOf(p.y, centreY)
                    val barH = abs(p.y - centreY)
                    drawRoundRect(
                        color = accent.copy(alpha = if (enabled) 0.22f else 0.10f),
                        topLeft = Offset(p.x - barWidth / 2f, top),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(barWidth / 2f),
                    )
                }

                // response curve + gradient fill
                val curve = buildSmoothPath(points)
                if (bandCount >= 2) {
                    val fill = Path().apply {
                        addPath(curve)
                        lineTo(points.last().x, h)
                        lineTo(points.first().x, h)
                        close()
                    }
                    drawPath(
                        path = fill,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accent.copy(alpha = if (enabled) 0.28f else 0.08f),
                                accent.copy(alpha = 0.0f),
                            ),
                        ),
                    )
                }
                drawPath(
                    path = curve,
                    color = accent,
                    style = Stroke(width = 6f, cap = StrokeCap.Round),
                )

                // thumbs + per-band dB readout
                for (i in 0 until bandCount) {
                    val p = points[i]
                    drawCircle(color = accent, radius = 11f, center = p)
                    drawCircle(color = thumbCore, radius = 5f, center = p)

                    val db = gains[i] / 100.0
                    val rounded = (db * 10).toInt() / 10.0
                    val sign = if (gains[i] > 0) "+" else ""
                    val readout = "$sign${rounded}"
                    val layout = textMeasurer.measure(
                        text = readout,
                        style = TextStyle(
                            color = readoutColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    val above = p.y - 16f - layout.size.height
                    val labelY = if (above < 2f) p.y + 16f else above
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            x = (p.x - layout.size.width / 2f)
                                .coerceIn(0f, w - layout.size.width),
                            y = labelY,
                        ),
                    )
                }
            }
        }

        // Frequency labels aligned under each band column.
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.xs)) {
            Spacer(Modifier.width(GUTTER))
            for (i in 0 until bandCount) {
                Text(
                    text = bandFrequencyLabel(i, bandCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Build a smooth path through [points] using a Catmull-Rom spline converted to
 * cubic béziers (tension 1/6). Endpoints duplicate their neighbour so the curve
 * doesn't overshoot at the edges. Falls back to straight segments for < 3
 * points.
 */
private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size < 3) {
        for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
        return path
    }
    for (i in 0 until points.size - 1) {
        val p0 = points[if (i == 0) 0 else i - 1]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[if (i + 2 <= points.lastIndex) i + 2 else points.lastIndex]
        val c1x = p1.x + (p2.x - p0.x) / 6f
        val c1y = p1.y + (p2.y - p0.y) / 6f
        val c2x = p2.x - (p3.x - p1.x) / 6f
        val c2y = p2.y - (p3.y - p1.y) / 6f
        path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
    }
    return path
}

private fun bandFrequencyLabel(index: Int, total: Int): String {
    // Standard 10-band graphic-EQ centre frequencies. The graph shows the
    // *template* band layout (10 bands by convention), not the hardware bands —
    // the controller resamples onto hardware.
    val tenBand = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    if (total == 10) return tenBand[index]
    if (total == 5) return when (index) {
        0 -> "60"
        1 -> "230"
        2 -> "910"
        3 -> "3.6k"
        4 -> "14k"
        else -> "B${index + 1}"
    }
    return "B${index + 1}"
}

private val GRAPH_HEIGHT = 300.dp
private val GUTTER = 32.dp
private val V_PAD = 20.dp
private const val MIN_GAIN_RANGE_MILLIBELS = -1500
private const val MAX_GAIN_RANGE_MILLIBELS = 1500
private const val GRID_STEP_MILLIBELS = 500

@PreviewScreenSizes
@Composable
private fun EqualizerGraphPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            EqualizerGraph(
                gains = listOf(400, 300, 200, 100, 0, -100, -200, 100, 300, 500),
                enabled = true,
                onBandGainChange = { _, _, _ -> },
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun EqualizerGraphDisabledPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            EqualizerGraph(
                gains = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                enabled = false,
                onBandGainChange = { _, _, _ -> },
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
