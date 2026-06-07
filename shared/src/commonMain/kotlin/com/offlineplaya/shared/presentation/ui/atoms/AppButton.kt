package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme

/**
 * The primary call-to-action atom. Themed Material3 [Button] with a single
 * text label — keeps callers from reaching for nested layouts inside a button.
 *
 * Filled in the fixed brand accent (Walkman orange), not the dynamic
 * `colorScheme.primary`, so primary CTAs keep a constant identity even when
 * album-art Palette repaints the scheme.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val brand = LocalBrandAccent.current
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = brand.accent,
            contentColor = brand.onAccent,
        ),
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
