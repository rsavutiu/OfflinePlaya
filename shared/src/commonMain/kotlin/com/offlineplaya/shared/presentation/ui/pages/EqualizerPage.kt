package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.log10
import kotlin.math.roundToInt
import com.offlineplaya.shared.domain.model.BuiltInPresets
import com.offlineplaya.shared.domain.model.EqMode
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.domain.model.EqPreset
import com.offlineplaya.shared.presentation.ui.LocalOrientation
import com.offlineplaya.shared.presentation.ui.Orientation
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
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.eq_preamp_hint
import offlineplaya.shared.generated.resources.eq_reset_to_preset
import offlineplaya.shared.generated.resources.settings_section_bands
import offlineplaya.shared.generated.resources.settings_section_mode
import offlineplaya.shared.generated.resources.settings_section_preamp
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
    onBandGainChange: (bandIndex: Int, millibels: Int, fullGains: PersistentList<Int>) -> Unit,
    onResetOverrides: () -> Unit,
    onPreampChange: (Int) -> Unit,
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
        }
    ) { padding ->
        // The page is three groups: Mode (+ auto card / hint), Preset (Manual
        // only), and the interactive Bands graph. Portrait stacks them in one
        // scroll column; landscape splits the controls (left) from the band
        // graph (right) so the graph gets the wider pane and the bands are
        // easier to drag — a single page in two content columns, not a
        // navigation two-pane (see CLAUDE.md).
        @Composable
        fun modeSection() {
            SettingsSection(title = stringResource(Res.string.settings_section_mode)) {
                EqModeChooser(
                    selected = preferences.mode,
                    onModeChange = onModeChange,
                    modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                )
                if (preferences.mode == EqMode.AUTO) {
                    EqAutoDetectionCard(
                        nowPlayingTitle = nowPlayingTitle,
                        rawGenreTag = rawGenreTag,
                        detectedGenreLabel = autoGenreLabel,
                        appliedPresetName = activePreset.name,
                        modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                    )
                } else {
                    ModeHint(
                        mode = preferences.mode,
                        activePreset = activePreset,
                        modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                    )
                }
            }
        }

        @Composable
        fun presetSection() {
            if (preferences.mode == EqMode.MANUAL) {
                SettingsSection(title = stringResource(Res.string.settings_section_preset)) {
                    EqPresetChooser(
                        selectedName = preferences.manualPresetName,
                        onPresetChange = onPresetChange,
                    )
                }
            }
        }

        @Composable
        fun preampSection() {
            SettingsSection(title = stringResource(Res.string.settings_section_preamp)) {
                PreampSlider(
                    percent = preferences.preampPercent,
                    onPreampChange = onPreampChange,
                    modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                )
            }
        }

        @Composable
        fun bandsSection() {
            SettingsSection(title = stringResource(Res.string.settings_section_bands)) {
                EqualizerGraph(
                    gains = activePreset.gainsMillibels.toPersistentList(),
                    enabled = preferences.mode != EqMode.OFF,
                    onBandGainChange = onBandGainChange,
                    modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                )
                if (preferences.mode == EqMode.MANUAL && preferences.manualGains.isNotEmpty()) {
                    TextButton(
                        onClick = onResetOverrides,
                        modifier = Modifier.padding(horizontal = AppSpacing.lg),
                    ) {
                        Text(stringResource(Res.string.eq_reset_to_preset))
                    }
                }
            }
        }

        ResponsiveContent(modifier = Modifier.padding(padding)) {
            if (LocalOrientation.current == Orientation.LANDSCAPE) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        modeSection()
                        presetSection()
                        preampSection()
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        bandsSection()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    modeSection()
                    presetSection()
                    preampSection()
                    bandsSection()
                }
            }
        }
    }
}

/**
 * Loudness boost beyond 100% volume, 0..[EqPreferences.MAX_PREAMP_PERCENT] in
 * steps of [EqPreferences.PREAMP_STEP_PERCENT]. The label shows both the
 * percentage and the equivalent dB so the boost reads in familiar units.
 */
@Composable
private fun PreampSlider(
    percent: Int,
    onPreampChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val db = (200.0 * log10(1.0 + percent / 100.0)).roundToInt() / 10.0
        Text(
            text = if (percent == 0) "Off" else "+$percent% (+$db dB)",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = percent.toFloat(),
            onValueChange = {
                // Snap to the volume-key step so the slider and the buttons
                // land on the same values.
                val step = EqPreferences.PREAMP_STEP_PERCENT
                onPreampChange((it / step).roundToInt() * step)
            },
            valueRange = 0f..EqPreferences.MAX_PREAMP_PERCENT.toFloat(),
            steps = EqPreferences.MAX_PREAMP_PERCENT / EqPreferences.PREAMP_STEP_PERCENT - 1,
        )
        Text(
            text = stringResource(Res.string.eq_preamp_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                preferences = EqPreferences(EqMode.MANUAL, "Rock", emptyList(), preampPercent = 30),
                activePreset = BuiltInPresets.ROCK,
                nowPlayingTitle = null,
                rawGenreTag = null,
                autoGenreLabel = null,
                onModeChange = {},
                onPresetChange = {},
                onBandGainChange = { _, _, _ -> },
                onResetOverrides = {},
                onPreampChange = {},
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
                onPreampChange = {},
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
                onPreampChange = {},
                onBack = {},
            )
        }
    }
}
