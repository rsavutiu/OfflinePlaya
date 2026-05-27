package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.BuiltInPresets
import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.domain.model.EqMode
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.domain.model.EqPreset
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.SettingsSection
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Equalizer settings page. Three modes (Off / Manual / Auto), a horizontal
 * preset chooser visible in Manual, and a row of vertical band sliders
 * driven by the resolved [activePreset].
 *
 * The "Auto" hint line tells the user which canonical genre drove the
 * currently-active preset — making the magic visible. When no track is
 * playing it falls back to "Auto: Default".
 */
@Composable
fun EqualizerPage(
    preferences: EqPreferences,
    activePreset: EqPreset,
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
        topBar = { AppTopBar(title = "Equalizer", onBack = onBack) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection(title = "Mode") {
                ModeChooser(
                    selected = preferences.mode,
                    onModeChange = onModeChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                ModeHint(
                    mode = preferences.mode,
                    activePreset = activePreset,
                    autoGenreLabel = autoGenreLabel,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (preferences.mode == EqMode.MANUAL) {
                SettingsSection(title = "Preset") {
                    PresetChooser(
                        selectedName = preferences.manualPresetName,
                        onPresetChange = onPresetChange,
                    )
                }
            }

            SettingsSection(title = "Bands") {
                BandSliders(
                    gains = activePreset.gainsMillibels,
                    enabled = preferences.mode != EqMode.OFF,
                    onBandGainChange = onBandGainChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (preferences.mode == EqMode.MANUAL && preferences.manualGains.isNotEmpty()) {
                    TextButton(
                        onClick = onResetOverrides,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Text("Reset to preset")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChooser(
    selected: EqMode,
    onModeChange: (EqMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EqMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onModeChange(mode) },
                label = { Text(mode.userLabel) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun ModeHint(
    mode: EqMode,
    activePreset: EqPreset,
    autoGenreLabel: String?,
    modifier: Modifier = Modifier,
) {
    val text = when (mode) {
        EqMode.OFF -> "Equalizer is off — system audio effects (Dolby, OEM EQ) apply instead."
        EqMode.MANUAL -> "Manual: ${activePreset.name}"
        EqMode.AUTO -> "Auto: ${autoGenreLabel ?: "Default"} — preset updates on track change."
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun PresetChooser(
    selectedName: String,
    onPresetChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = BuiltInPresets.all, key = { it.name }) { preset ->
            FilterChip(
                selected = preset.name == selectedName,
                onClick = { onPresetChange(preset.name) },
                label = { Text(preset.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun BandSliders(
    gains: List<Int>,
    enabled: Boolean,
    onBandGainChange: (bandIndex: Int, millibels: Int, fullGains: List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Vertical sliders done via a rotated horizontal slider. Compose Material
    // doesn't ship a vertical Slider primitive; rotation is the established
    // workaround and gives us free a11y by reusing the standard component.
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        gains.forEachIndexed { index, gain ->
            BandSlider(
                label = bandFrequencyLabel(index, gains.size),
                gainMillibels = gain,
                enabled = enabled,
                onChange = { newMillibels ->
                    onBandGainChange(index, newMillibels, gains)
                },
            )
        }
    }
}

@Composable
private fun BandSlider(
    label: String,
    gainMillibels: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp),
    ) {
        // Gain readout (dB) above the slider.
        Text(
            text = formatGain(gainMillibels),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // 180-tall slot — vertical orientation achieved by rotating a 180-wide
        // horizontal slider 270°. Box width = slot height.
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(180.dp),
            contentAlignment = Alignment.Center,
        ) {
            Slider(
                value = gainMillibels.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                valueRange = MIN_GAIN_RANGE_MILLIBELS.toFloat()..MAX_GAIN_RANGE_MILLIBELS.toFloat(),
                enabled = enabled,
                modifier = Modifier
                    .width(180.dp)
                    .height(48.dp)
                    .graphicsLayer { rotationZ = 270f },
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun bandFrequencyLabel(index: Int, total: Int): String {
    // Approximate centers when we have 5 bands (the common case). For other
    // band counts the controller has the real centers from
    // `Equalizer.getBandCenterFrequency`, but we don't pipe those into the
    // UI yet; show generic labels in that case.
    if (total != 5) return "B${index + 1}"
    return when (index) {
        0 -> "60Hz"
        1 -> "230Hz"
        2 -> "910Hz"
        3 -> "3.6kHz"
        4 -> "14kHz"
        else -> "B${index + 1}"
    }
}

private fun formatGain(millibels: Int): String {
    val db = millibels / 100.0
    val rounded = (db * 10).toInt() / 10.0
    val sign = if (millibels > 0) "+" else ""
    return "$sign${rounded}dB"
}

/** Slider span: ±15 dB. Real per-band ranges from the platform are tighter; the controller clamps. */
private const val MIN_GAIN_RANGE_MILLIBELS = -1500
private const val MAX_GAIN_RANGE_MILLIBELS = 1500

private val EqMode.userLabel: String
    get() = when (this) {
        EqMode.OFF -> "Off"
        EqMode.MANUAL -> "Manual"
        EqMode.AUTO -> "Auto"
    }

/** Convert a canonical genre to a human label. */
fun CanonicalGenre.userLabel(): String = when (this) {
    CanonicalGenre.ROCK -> "Rock"
    CanonicalGenre.POP -> "Pop"
    CanonicalGenre.ELECTRONIC -> "Electronic"
    CanonicalGenre.JAZZ -> "Jazz"
    CanonicalGenre.CLASSICAL -> "Classical"
    CanonicalGenre.HIPHOP -> "Hip-Hop"
    CanonicalGenre.DEFAULT -> "Default"
}

@Preview
@Composable
private fun EqualizerPageManualPreview() {
    PreviewTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EqualizerPage(
                preferences = EqPreferences(EqMode.MANUAL, "Rock", emptyList()),
                activePreset = BuiltInPresets.ROCK,
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

@Preview
@Composable
private fun EqualizerPageAutoPreview() {
    PreviewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EqualizerPage(
                preferences = EqPreferences(EqMode.AUTO, "Default", emptyList()),
                activePreset = BuiltInPresets.JAZZ,
                autoGenreLabel = "Jazz",
                onModeChange = {},
                onPresetChange = {},
                onBandGainChange = { _, _, _ -> },
                onResetOverrides = {},
                onBack = {},
            )
        }
    }
}

@Preview
@Composable
private fun EqualizerPageOffPreview() {
    PreviewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EqualizerPage(
                preferences = EqPreferences(EqMode.OFF, "Default", emptyList()),
                activePreset = BuiltInPresets.FLAT,
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

