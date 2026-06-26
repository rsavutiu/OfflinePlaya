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
import com.offlineplaya.shared.domain.model.PlaybackPreferences
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.molecules.SettingsSection
import com.offlineplaya.shared.presentation.ui.molecules.SwitchRow
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
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
 */
@Composable
fun PlaybackSettings(
    preferences: PlaybackPreferences,
    onCrossfadeEnabledChange: (Boolean) -> Unit,
    onCrossfadeDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
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

@PreviewScreenSizes
@Composable
private fun PlaybackSettingsEnabledPreview() {
    PreviewTheme {
        PlaybackSettings(
            preferences = PlaybackPreferences(crossfadeEnabled = true, crossfadeDurationSeconds = 6),
            onCrossfadeEnabledChange = {},
            onCrossfadeDurationChange = {},
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
        )
    }
}
