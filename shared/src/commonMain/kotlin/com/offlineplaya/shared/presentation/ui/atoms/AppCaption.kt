package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme
import com.offlineplaya.shared.presentation.ui.preview.Preview

/**
 * The secondary-text atom for short, supporting copy beneath headlines.
 */
@Composable
fun AppCaption(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Preview
@Composable
private fun AppCaptionPreview() {
    OfflinePlayaTheme {
        Surface {
            AppCaption(text = "Phase 1 scaffold — playback engine arrives in Phase 3.")
        }
    }
}
