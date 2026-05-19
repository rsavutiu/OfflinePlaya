package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.ColorModeChooser
import com.offlineplaya.shared.presentation.ui.molecules.SettingsSection
import com.offlineplaya.shared.presentation.ui.molecules.SwitchRow
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * The Settings screen. Currently exposes Appearance controls (color mode +
 * Material You toggle). More sections — Library re-scan, About — land in
 * later phases.
 *
 * Pure-state composable: take the current [preferences] and emit callbacks.
 * Production wires this up via `ThemeStateHolder`; previews pass literal
 * values.
 */
@Composable
fun SettingsPage(
    preferences: ThemePreferences,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    dynamicColorSupported: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = "Settings", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection(title = "Appearance") {
                ColorModeChooser(
                    selected = preferences.colorMode,
                    onSelectionChanged = onColorModeChange,
                )
                SwitchRow(
                    title = "Material You",
                    subtitle = if (dynamicColorSupported) {
                        "Use wallpaper-based colors"
                    } else {
                        "Requires Android 12 or later"
                    },
                    checked = preferences.useDynamicColor && dynamicColorSupported,
                    onCheckedChange = onDynamicColorChange,
                    enabled = dynamicColorSupported,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingsPageLightPreview() {
    PreviewTheme {
        SettingsPage(
            preferences = ThemePreferences(
                colorMode = ColorMode.SYSTEM,
                useDynamicColor = true,
            ),
            onColorModeChange = {},
            onDynamicColorChange = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun SettingsPageDarkPreview() {
    PreviewTheme(darkTheme = true) {
        SettingsPage(
            preferences = ThemePreferences(
                colorMode = ColorMode.DARK,
                useDynamicColor = false,
            ),
            onColorModeChange = {},
            onDynamicColorChange = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun SettingsPageDynamicUnsupportedPreview() {
    PreviewTheme {
        SettingsPage(
            preferences = ThemePreferences(
                colorMode = ColorMode.LIGHT,
                useDynamicColor = false,
            ),
            onColorModeChange = {},
            onDynamicColorChange = {},
            onBack = {},
            dynamicColorSupported = false,
        )
    }
}
