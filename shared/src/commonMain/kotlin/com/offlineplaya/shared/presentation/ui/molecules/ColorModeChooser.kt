package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Radio group letting the user pick System / Light / Dark color mode.
 */
@Composable
fun ColorModeChooser(
    selected: ColorMode,
    onSelectionChanged: (ColorMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ColorMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = mode == selected,
                        onClick = { onSelectionChanged(mode) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == selected, onClick = null)
                Text(
                    text = mode.label,
                    modifier = Modifier.padding(start = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

private val ColorMode.label: String
    get() = when (this) {
        ColorMode.SYSTEM -> "Follow system"
        ColorMode.LIGHT -> "Light"
        ColorMode.DARK -> "Dark"
    }

@Preview
@Composable
private fun ColorModeChooserSystemPreview() {
    PreviewTheme {
        Surface { ColorModeChooser(selected = ColorMode.SYSTEM, onSelectionChanged = {}) }
    }
}

@Preview
@Composable
private fun ColorModeChooserDarkPreview() {
    PreviewTheme(darkTheme = true) {
        Surface { ColorModeChooser(selected = ColorMode.DARK, onSelectionChanged = {}) }
    }
}
