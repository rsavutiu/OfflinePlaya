package com.offlineplaya.shared.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.offlineplaya.shared.presentation.ui.preview.Preview

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun OfflinePlayaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

@Preview
@Composable
private fun OfflinePlayaThemeLightPreview() {
    OfflinePlayaTheme(darkTheme = false) {
        Surface { Text("Light theme sample") }
    }
}

@Preview
@Composable
private fun OfflinePlayaThemeDarkPreview() {
    OfflinePlayaTheme(darkTheme = true) {
        Surface { Text("Dark theme sample") }
    }
}
