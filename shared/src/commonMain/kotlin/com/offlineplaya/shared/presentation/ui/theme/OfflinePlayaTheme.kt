package com.offlineplaya.shared.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.ProvideOrientation
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.presentation.ui.preview.Preview

/**
 * Root theme wrapper. Applies the brand color palette, typography, and
 * (optionally) Material You dynamic colors on Android 12+. The
 * [preferences] object controls light/dark mode and whether to use dynamic
 * color; callers typically read it from [SettingsRepository] via a
 * state-holder.
 *
 * Previews should use [PreviewTheme] which forces a specific light/dark mode
 * without consulting the system or dynamic color.
 */
@Composable
fun OfflinePlayaTheme(
    preferences: ThemePreferences = ThemePreferences.Default,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (preferences.colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }

    val colors = if (preferences.useDynamicColor) {
        rememberDynamicColorSchemeOrNull(darkTheme)
            ?: if (darkTheme) DefaultDarkColors else DefaultLightColors
    } else {
        if (darkTheme) DefaultDarkColors else DefaultLightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = OfflinePlayaTypography,
        shapes = OfflinePlayaShapes,
        content = content,
    )
}

/** Convenience for previews — forces light/dark without dynamic color. */
@Composable
fun PreviewTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    OfflinePlayaTheme(
        preferences = ThemePreferences(
            colorMode = if (darkTheme) ColorMode.DARK else ColorMode.LIGHT,
            useDynamicColor = false,
        ),
    ) {
        // Measure the preview canvas so LocalOrientation reflects the actual
        // preview shape instead of the bare PORTRAIT default.
        ProvideOrientation(content = content)
    }
}

@PreviewScreenSizes
@Composable
private fun OfflinePlayaThemeLightPreview() {
    PreviewTheme(darkTheme = false) {
        Surface { Text("Light theme sample") }
    }
}

@PreviewScreenSizes
@Composable
private fun OfflinePlayaThemeDarkPreview() {
    PreviewTheme(darkTheme = true) {
        Surface { Text("Dark theme sample") }
    }
}
