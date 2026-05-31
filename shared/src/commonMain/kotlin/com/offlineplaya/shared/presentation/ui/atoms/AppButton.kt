package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme

/**
 * The primary call-to-action atom. Themed Material3 [Button] with a single
 * text label — keeps callers from reaching for nested layouts inside a button.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text)
    }
}

@PreviewScreenSizes
@Composable
private fun AppButtonEnabledPreview() {
    OfflinePlayaTheme {
        Surface {
            AppButton(text = "Pick music folder", onClick = {})
        }
    }
}

@PreviewScreenSizes
@Composable
private fun AppButtonDisabledPreview() {
    OfflinePlayaTheme {
        Surface {
            AppButton(text = "Scanning…", onClick = {}, enabled = false)
        }
    }
}
