package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.BuiltInPresets
import com.offlineplaya.shared.domain.model.EqMode
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.domain.model.EqPreset
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.EqModeChooser
import com.offlineplaya.shared.presentation.ui.molecules.EqPresetChooser
import com.offlineplaya.shared.presentation.ui.molecules.SettingsSection
import com.offlineplaya.shared.presentation.ui.organisms.EqAutoDetectionCard
import com.offlineplaya.shared.presentation.ui.organisms.EqualizerGraph
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.settings_section_bands
import offlineplaya.shared.generated.resources.settings_section_mode
import offlineplaya.shared.generated.resources.settings_section_preset
import offlineplaya.shared.generated.resources.top_bar_equalizer
import org.jetbrains.compose.resources.stringResource

/**
 * Equalizer settings page. Three modes (Off / Manual / Auto), a horizontal
 * preset chooser visible in Manual, and an interactive band graph driven by
 * the resolved [activePreset]. In Auto mode the [EqAutoDetectionCard] surfaces
 * the genre inference so the user can see *why* a given curve applies.
 */
@Composable
fun EqualizerPage(
    preferences: EqPreferences,
    activePreset: EqPreset,
    nowPlayingTitle: String?,
    rawGenreTag: String?,
    autoGenreLabel: String?,
    onModeChange: (EqMode) -> Unit,
    onPresetChange: (String) -> Unit,
    onBandGainChange: (bandIndex: Int, millibels: Int, fullGains: List<Int>) -> Unit,
    onResetOverrides: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.top_bar_equalizer),
                onBack = onBack
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        ResponsiveContent(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                SettingsSection(title = stringResource(Res.string.settings_section_mode)) {
                    EqModeChooser(
                        selected = preferences.mode,
                        onModeChange = onModeChange,
                        modifier = Modifier.padding(
                            horizontal = AppSpacing.lg,
                            vertical = AppSpacing.sm
                        ),
                    )
                    if (preferences.mode == EqMode.AUTO) {
                        EqAutoDetectionCard(
                            nowPlayingTitle = nowPlayingTitle,
                            rawGenreTag = rawGenreTag,
                            detectedGenreLabel = autoGenreLabel,
                            appliedPresetName = activePreset.name,
                            modifier = Modifier.padding(
                                horizontal = AppSpacing.lg,
                                vertical = AppSpacing.sm
                            ),
                        )
                    } else {
                        ModeHint(
                            mode = preferences.mode,
                            activePreset = activePreset,
                            modifier = Modifier.padding(
                                horizontal = AppSpacing.lg,
                                vertical = AppSpacing.sm
                            ),
                        )
                    }
                }

                if (preferences.mode == EqMode.MANUAL) {
                    SettingsSection(title = stringResource(Res.string.settings_section_preset)) {
                        EqPresetChooser(
                            selectedName = preferences.manualPresetName,
                            onPresetChange = onPresetChange,
                        )
                    }
                }

                SettingsSection(title = stringResource(Res.string.settings_section_bands)) {
                    EqualizerGraph(
                        gains = activePreset.gainsMillibels,
                        enabled = preferences.mode != EqMode.OFF,
                        onBandGainChange = onBandGainChange,
                        modifier = Modifier.padding(
                            horizontal = AppSpacing.lg,
                            vertical = AppSpacing.sm
                        ),
                    )
                    if (preferences.mode == EqMode.MANUAL && preferences.manualGains.isNotEmpty()) {
                        TextButton(
                            onClick = onResetOverrides,
                            modifier = Modifier.padding(horizontal = AppSpacing.lg),
                        ) {
                            Text("Reset to preset")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeHint(
    mode: EqMode,
    activePreset: EqPreset,
    modifier: Modifier = Modifier,
) {
    val text = when (mode) {
        EqMode.OFF -> "Equalizer is off — system audio effects (Dolby, OEM EQ) apply instead."
        EqMode.MANUAL -> "Manual: ${activePreset.name}"
        EqMode.AUTO -> ""
    }
    if (text.isEmpty()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@PreviewScreenSizes
@Composable
private fun EqualizerPageManualPreview() {
    PreviewTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EqualizerPage(
                preferences = EqPreferences(EqMode.MANUAL, "Rock", emptyList()),
                activePreset = BuiltInPresets.ROCK,
                nowPlayingTitle = null,
                rawGenreTag = null,
                autoGenreLabel = null,
                onModeChange = {},
                onPresetChange = {},
                onBandGainChange = { _, _, _ -> },
                onResetOverrides = {},
                onBack = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun EqualizerPageAutoPreview() {
    PreviewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EqualizerPage(
                preferences = EqPreferences(EqMode.AUTO, "Default", emptyList()),
                activePreset = BuiltInPresets.ELECTRONIC,
                nowPlayingTitle = "Strobe",
                rawGenreTag = "EDM",
                autoGenreLabel = "Electronic",
                onModeChange = {},
                onPresetChange = {},
                onBandGainChange = { _, _, _ -> },
                onResetOverrides = {},
                onBack = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun EqualizerPageOffPreview() {
    PreviewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EqualizerPage(
                preferences = EqPreferences(EqMode.OFF, "Default", emptyList()),
                activePreset = BuiltInPresets.FLAT,
                nowPlayingTitle = null,
                rawGenreTag = null,
                autoGenreLabel = null,
                onModeChange = {},
                onPresetChange = {},
                onBandGainChange = { _, _, _ -> },
                onResetOverrides = {},
                onBack = {},
            )
        }
    }
}
