package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.presentation.ui.molecules.ColorModeChooser
import com.offlineplaya.shared.presentation.ui.molecules.SettingsSection
import com.offlineplaya.shared.presentation.ui.molecules.SwitchRow
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.appearance_material_you
import offlineplaya.shared.generated.resources.appearance_material_you_supported
import offlineplaya.shared.generated.resources.appearance_material_you_unsupported
import offlineplaya.shared.generated.resources.settings_section_appearance
import org.jetbrains.compose.resources.stringResource

/**
 * Appearance section: light/dark/system chooser + Material You toggle. The
 * dynamic-color toggle disables itself on Android 11 and earlier, with the
 * subtitle calling out why.
 */
@Composable
fun AppearanceSettings(
    preferences: ThemePreferences,
    dynamicColorSupported: Boolean,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(
        title = stringResource(Res.string.settings_section_appearance),
        modifier = modifier
    ) {
        ColorModeChooser(
            modifier = Modifier.fillMaxWidth(0.5f),
            selected = preferences.colorMode,
            onSelectionChanged = onColorModeChange,
        )
        SwitchRow(
            title = stringResource(Res.string.appearance_material_you),
            subtitle = if (dynamicColorSupported) {
                stringResource(Res.string.appearance_material_you_supported)
            } else {
                stringResource(Res.string.appearance_material_you_unsupported)
            },
            checked = preferences.useDynamicColor && dynamicColorSupported,
            onCheckedChange = onDynamicColorChange,
            enabled = dynamicColorSupported,
        )
    }
}

@PreviewScreenSizes
@Composable
private fun AppearanceSettingsPreview() {
    PreviewTheme {
        AppearanceSettings(
            preferences = ThemePreferences(ColorMode.SYSTEM, useDynamicColor = true),
            dynamicColorSupported = true,
            onColorModeChange = {},
            onDynamicColorChange = {},
        )
    }
}
