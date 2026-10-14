package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.audio.DacOutputState
import com.offlineplaya.shared.domain.audio.FallbackReason
import com.offlineplaya.shared.domain.audio.OutputDevice
import com.offlineplaya.shared.domain.audio.OutputMode
import com.offlineplaya.shared.domain.audio.PcmEncoding
import com.offlineplaya.shared.domain.audio.PcmFormat
import com.offlineplaya.shared.domain.model.PlaybackPreferences
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.molecules.SettingsSection
import com.offlineplaya.shared.presentation.ui.molecules.SwitchRow
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.dac_bit_perfect
import offlineplaya.shared.generated.resources.dac_bit_perfect_subtitle
import offlineplaya.shared.generated.resources.dac_no_device
import offlineplaya.shared.generated.resources.dac_reason_format
import offlineplaya.shared.generated.resources.dac_reason_hires
import offlineplaya.shared.generated.resources.dac_reason_os
import offlineplaya.shared.generated.resources.dac_reason_unsupported
import offlineplaya.shared.generated.resources.playback_bt_autoplay
import offlineplaya.shared.generated.resources.playback_bt_autoplay_subtitle
import offlineplaya.shared.generated.resources.playback_crossfade
import offlineplaya.shared.generated.resources.playback_crossfade_duration
import offlineplaya.shared.generated.resources.playback_crossfade_duration_value
import offlineplaya.shared.generated.resources.playback_crossfade_subtitle
import offlineplaya.shared.generated.resources.settings_section_playback
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Playback section: a crossfade on/off switch and — only when crossfade is
 * enabled — a duration slider. There is intentionally no separate "gapless"
 * control: gapless is always-on in ExoPlayer and is exactly what crossfade-off
 * gives you, so the subtitle calls that out instead of shipping a no-op toggle.
 *
 * Also hosts the bit-perfect USB DAC switch ([dacOutput]). It is per-DAC, so it
 * is only actionable while a DAC is connected and the platform supports it;
 * otherwise the row is disabled and says why.
 */
@Composable
fun PlaybackSettings(
    preferences: PlaybackPreferences,
    onCrossfadeEnabledChange: (Boolean) -> Unit,
    onCrossfadeDurationChange: (Int) -> Unit,
    onBluetoothAutoplayChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    dacOutput: DacOutputState = DacOutputState(),
    onBitPerfectChange: (Boolean) -> Unit = {},
) {
    SettingsSection(
        title = stringResource(Res.string.settings_section_playback),
        modifier = modifier,
    ) {
        SwitchRow(
            title = stringResource(Res.string.playback_crossfade),
            subtitle = stringResource(Res.string.playback_crossfade_subtitle),
            checked = preferences.crossfadeEnabled,
            onCheckedChange = onCrossfadeEnabledChange,
            modifier = Modifier.testTag(TestTags.Settings.CROSSFADE_TOGGLE),
        )
        SwitchRow(
            title = stringResource(Res.string.playback_bt_autoplay),
            subtitle = stringResource(Res.string.playback_bt_autoplay_subtitle),
            checked = preferences.bluetoothAutoplayEnabled,
            onCheckedChange = onBluetoothAutoplayChange,
            modifier = Modifier.testTag(TestTags.Settings.BT_AUTOPLAY_TOGGLE),
        )
        BitPerfectRow(
            state = dacOutput,
            onCheckedChange = onBitPerfectChange,
        )
        AnimatedVisibility(visible = preferences.crossfadeEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.playback_crossfade_duration),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            Res.string.playback_crossfade_duration_value,
                            preferences.crossfadeDurationSeconds,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Slider(
                    value = preferences.crossfadeDurationSeconds.toFloat(),
                    onValueChange = { onCrossfadeDurationChange(it.roundToInt()) },
                    valueRange = PlaybackPreferences.MIN_DURATION_SECONDS.toFloat()..
                        PlaybackPreferences.MAX_DURATION_SECONDS.toFloat(),
                    // One discrete stop per whole second between the endpoints.
                    steps = PlaybackPreferences.MAX_DURATION_SECONDS -
                        PlaybackPreferences.MIN_DURATION_SECONDS - 1,
                )
            }
        }
    }
}

@Composable
private fun BitPerfectRow(
    state: DacOutputState,
    onCheckedChange: (Boolean) -> Unit,
) {
    val reason = state.fallbackReason
    val device = state.device
    val available = device != null &&
        reason != FallbackReason.API_TOO_OLD &&
        reason != FallbackReason.DEVICE_NOT_SUPPORTED
    val subtitle = when {
        device == null -> stringResource(Res.string.dac_no_device)
        reason == FallbackReason.API_TOO_OLD -> stringResource(Res.string.dac_reason_os)
        reason == FallbackReason.DEVICE_NOT_SUPPORTED -> stringResource(Res.string.dac_reason_unsupported)
        else -> {
            val base = stringResource(Res.string.dac_bit_perfect_subtitle)
            // While switched on but not engaged for the current track, say why.
            val note = when (reason) {
                FallbackReason.HI_RES_SOURCE -> stringResource(Res.string.dac_reason_hires)
                FallbackReason.FORMAT_NOT_SUPPORTED,
                FallbackReason.SYSTEM_REJECTED -> stringResource(Res.string.dac_reason_format)
                else -> null
            }
            listOfNotNull(device.name, base, note).joinToString(separator = "\n")
        }
    }
    SwitchRow(
        title = stringResource(Res.string.dac_bit_perfect),
        subtitle = subtitle,
        checked = available && state.bitPerfectEnabled,
        onCheckedChange = onCheckedChange,
        enabled = available,
        modifier = Modifier.testTag(TestTags.Settings.BIT_PERFECT_TOGGLE),
    )
}

@PreviewScreenSizes
@Composable
private fun PlaybackSettingsEnabledPreview() {
    PreviewTheme {
        PlaybackSettings(
            preferences = PlaybackPreferences(crossfadeEnabled = true, crossfadeDurationSeconds = 6),
            onCrossfadeEnabledChange = {},
            onCrossfadeDurationChange = {},
            onBluetoothAutoplayChange = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun PlaybackSettingsDisabledPreview() {
    PreviewTheme(darkTheme = true) {
        PlaybackSettings(
            preferences = PlaybackPreferences(crossfadeEnabled = false, crossfadeDurationSeconds = 6),
            onCrossfadeEnabledChange = {},
            onCrossfadeDurationChange = {},
            onBluetoothAutoplayChange = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun PlaybackSettingsBitPerfectPreview() {
    PreviewTheme {
        PlaybackSettings(
            preferences = PlaybackPreferences(crossfadeEnabled = false, crossfadeDurationSeconds = 6),
            onCrossfadeEnabledChange = {},
            onCrossfadeDurationChange = {},
            onBluetoothAutoplayChange = {},
            dacOutput = DacOutputState(
                device = OutputDevice(id = 7, name = "FiiO K11", key = "usb:FiiO K11"),
                mode = OutputMode.BitPerfect(PcmFormat(44_100, PcmEncoding.PCM_16)),
                bitPerfectCapable = true,
            ),
        )
    }
}

@PreviewScreenSizes
@Composable
private fun PlaybackSettingsHiResFallbackPreview() {
    PreviewTheme(darkTheme = true) {
        PlaybackSettings(
            preferences = PlaybackPreferences(crossfadeEnabled = false, crossfadeDurationSeconds = 6),
            onCrossfadeEnabledChange = {},
            onCrossfadeDurationChange = {},
            onBluetoothAutoplayChange = {},
            dacOutput = DacOutputState(
                device = OutputDevice(id = 7, name = "FiiO K11", key = "usb:FiiO K11"),
                bitPerfectCapable = true,
                fallbackReason = FallbackReason.HI_RES_SOURCE,
            ),
        )
    }
}
