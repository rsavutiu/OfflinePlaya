package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.offlineplaya.shared.domain.model.EqMode
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Three-way segmented picker for the equalizer mode (Off / Manual / Auto).
 * Rendered as filter chips so the selected mode is unmistakable at a glance.
 */
@Composable
fun EqModeChooser(
    selected: EqMode,
    onModeChange: (EqMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        EqMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onModeChange(mode) },
                label = { Text(mode.userLabel) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

private val EqMode.userLabel: String
    get() = when (this) {
        EqMode.OFF -> "Off"
        EqMode.MANUAL -> "Manual"
        EqMode.AUTO -> "Auto"
    }

@PreviewScreenSizes
@Composable
private fun EqModeChooserPreview() {
    PreviewTheme {
        Surface {
            EqModeChooser(selected = EqMode.MANUAL, onModeChange = {})
        }
    }
}
