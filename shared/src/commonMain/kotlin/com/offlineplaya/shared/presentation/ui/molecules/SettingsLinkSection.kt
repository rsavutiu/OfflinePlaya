package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * A settings group whose body is a single labelled action (open another
 * screen). Used by the Audio and Developer sections — the page-level setting
 * IS the link to a sub-page. Avoids spelling out the same "Section + button"
 * boilerplate twice.
 */
@Composable
fun SettingsLinkSection(
    sectionTitle: String,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(title = sectionTitle, modifier = modifier) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .heightIn(max = 44.dp),
        ) {
            Text(actionLabel)
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsLinkSectionPreview() {
    PreviewTheme {
        Surface {
            SettingsLinkSection(
                sectionTitle = "Audio",
                actionLabel = "Equalizer",
                onClick = {},
            )
        }
    }
}
