package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.BuiltInPresets
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Built-in EQ presets as filter chips in a single horizontally-scrollable row.
 * One line, swipe to reach the rest — this keeps the chooser short so the band
 * graph below gets the vertical room, even with a dozen-plus presets. Shown
 * only in Manual mode.
 *
 * A plain `Row` + `horizontalScroll` rather than `LazyRow`/`FlowRow`: the preset
 * set is tiny and fixed, so lazy recycling buys nothing, and it sidesteps the
 * binary-compat skew between Compose Multiplatform 1.7.3 (which the shared
 * module compiles against) and androidx.compose.foundation 1.9.x (the
 * androidApp runtime classpath) that bites `FlowRow`'s parameter list with a
 * `NoSuchMethodError`.
 */
@Composable
fun EqPresetChooser(
    selectedName: String,
    onPresetChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
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

@PreviewScreenSizes
@Composable
private fun EqPresetChooserPreview() {
    PreviewTheme {
        Surface {
            EqPresetChooser(selectedName = "Rock", onPresetChange = {})
        }
    }
}
