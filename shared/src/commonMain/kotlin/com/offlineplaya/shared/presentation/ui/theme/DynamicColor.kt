package com.offlineplaya.shared.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Returns a platform-derived color scheme (Material You wallpaper colors on
 * Android 12+) or `null` if the platform doesn't support dynamic theming.
 * Callers fall back to the brand defaults.
 */
@Composable
expect fun rememberDynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme?
