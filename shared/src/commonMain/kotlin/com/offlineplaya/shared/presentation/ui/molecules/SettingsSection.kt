package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.LocalOrientation
import com.offlineplaya.shared.presentation.ui.Orientation
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * A titled group of related settings rows. Header uses the primary color to
 * separate sections visually without adding dividers.
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val localOrientation = LocalOrientation.current
    val body = @Composable {
        Text(
            text = title.uppercase(),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }

    if (localOrientation == Orientation.PORTRAIT) {
        Column(modifier = modifier) { body() }
    } else {
        Row(modifier = modifier) { body() }
    }
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
