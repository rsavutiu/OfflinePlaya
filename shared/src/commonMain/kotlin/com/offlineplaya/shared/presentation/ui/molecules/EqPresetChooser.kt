package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.BuiltInPresets
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlin.math.max

/**
 * Built-in EQ presets as filter chips that wrap onto multiple rows (no
 * horizontal scroll), so the full set is visible at once — important in the
 * narrow left column of the landscape equalizer. Shown only in Manual mode.
 *
 * Uses a hand-rolled wrap layout rather than `FlowRow` to avoid a binary
 * compatibility skew between Compose Multiplatform 1.7.3 (which the shared
 * module compiles against) and androidx.compose.foundation 1.9.x (which the
 * androidApp's runtime classpath resolves to). The two versions disagree on
 * `FlowRow`'s parameter list, so calls compiled against one fail with
 * `NoSuchMethodError` against the other.
 */
@Composable
fun EqPresetChooser(
    selectedName: String,
    onPresetChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    WrapRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg),
        horizontalSpacing = AppSpacing.sm,
        verticalSpacing = AppSpacing.sm,
    ) {
        BuiltInPresets.all.forEach { preset ->
            FilterChip(
                selected = preset.name == selectedName,
                onClick = { onPresetChange(preset.name) },
                label = { Text(preset.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun WrapRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    verticalSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val hGap = horizontalSpacing.roundToPx()
        val vGap = verticalSpacing.roundToPx()
        val maxWidth = constraints.maxWidth
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(itemConstraints) }

        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()
        var current = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentWidth = 0
        var currentHeight = 0
        placeables.forEach { p ->
            val gap = if (current.isEmpty()) 0 else hGap
            if (current.isNotEmpty() && currentWidth + gap + p.width > maxWidth) {
                rows += current
                rowWidths += currentWidth
                rowHeights += currentHeight
                current = mutableListOf()
                currentWidth = 0
                currentHeight = 0
            }
            if (current.isNotEmpty()) currentWidth += hGap
            current += p
            currentWidth += p.width
            currentHeight = max(currentHeight, p.height)
        }
        if (current.isNotEmpty()) {
            rows += current
            rowWidths += currentWidth
            rowHeights += currentHeight
        }

        val totalHeight = rowHeights.sum() + vGap * max(0, rowHeights.size - 1)
        val totalWidth = (rowWidths.maxOrNull() ?: 0).coerceAtLeast(constraints.minWidth)

        layout(totalWidth, totalHeight.coerceAtLeast(constraints.minHeight)) {
            var y = 0
            rows.forEachIndexed { idx, row ->
                var x = 0
                row.forEachIndexed { i, p ->
                    if (i > 0) x += hGap
                    p.placeRelative(x, y)
                    x += p.width
                }
                y += rowHeights[idx] + if (idx < rows.lastIndex) vGap else 0
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun EqPresetChooserPreview() {
    PreviewTheme {
        Surface {
            EqPresetChooser(selectedName = "Rock", onPresetChange = {})
        }
    }
}
