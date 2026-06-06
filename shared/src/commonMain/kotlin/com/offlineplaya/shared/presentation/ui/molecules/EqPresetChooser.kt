package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * Built-in EQ presets as filter chips that wrap onto multiple rows (no
 * horizontal scroll), so the full set is visible at once — important in the
 * narrow left column of the landscape equalizer. Shown only in Manual mode.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqPresetChooser(
    selectedName: String,
    onPresetChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
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
