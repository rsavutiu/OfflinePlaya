package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * A titled group of related settings rows: the header sits above its content.
 * Pages that want columns in landscape (Settings, Equalizer) arrange whole
 * sections side by side themselves — a section is always an internal vertical
 * group so its header never ends up beside its own rows.
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        SettingsSectionHeader(title)
        content()
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@PreviewScreenSizes
@Composable
private fun SettingsSectionPreview() {
    PreviewTheme {
        Surface {
            SettingsSection(title = "Appearance") {
                Text("Body content", modifier = Modifier.padding(PaddingValues(16.dp)))
            }
        }
    }
}
