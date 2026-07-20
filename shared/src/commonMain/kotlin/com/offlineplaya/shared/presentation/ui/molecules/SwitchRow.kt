package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
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
            // toggleable (not clickable) so TalkBack reads the whole row as ONE
            // switch — "title, subtitle, on/off" — instead of an unlabeled tap
            // target plus a second bare Switch stop.
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
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
        val brand = LocalBrandAccent.current
        Switch(
            checked = checked,
            // null: the row's toggleable handles input; a non-null callback
            // here would make the thumb its own (duplicate) a11y target.
            onCheckedChange = null,
            enabled = enabled,
            // "On" state in the fixed brand accent so toggles read as active
            // regardless of the ambient album-art tint.
            colors = SwitchDefaults.colors(
                checkedTrackColor = brand.accent,
                checkedThumbColor = brand.onAccent,
            ),
        )
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
