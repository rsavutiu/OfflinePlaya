package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Two-line settings row with a trailing [Switch]. Tapping anywhere on the
 * row toggles the switch.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@PreviewScreenSizes
@Composable
private fun SwitchRowOnPreview() {
    PreviewTheme {
        Surface {
            SwitchRow(
                title = "Material You",
                subtitle = "Use wallpaper-based colors (Android 12+)",
                checked = true,
                onCheckedChange = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SwitchRowOffPreview() {
    PreviewTheme {
        Surface {
            SwitchRow(
                title = "Material You",
                subtitle = "Use wallpaper-based colors (Android 12+)",
                checked = false,
                onCheckedChange = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SwitchRowDisabledPreview() {
    PreviewTheme {
        Surface {
            SwitchRow(
                title = "Material You",
                subtitle = "Requires Android 12 or later",
                checked = false,
                onCheckedChange = {},
                enabled = false,
            )
        }
    }
}
