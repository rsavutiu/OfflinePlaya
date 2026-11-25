package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.ReplayGainMode
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.replay_gain_album
import offlineplaya.shared.generated.resources.replay_gain_hint
import offlineplaya.shared.generated.resources.replay_gain_off
import offlineplaya.shared.generated.resources.replay_gain_track
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Volume-normalisation picker (Off / Track / Album) with an explanatory hint.
 * Selected mode is shown as a filled chip, matching [EqModeChooser].
 */
@Composable
fun ReplayGainChooser(
    selected: ReplayGainMode,
    onModeChange: (ReplayGainMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            ReplayGainMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == selected,
                    onClick = { onModeChange(mode) },
                    label = { Text(stringResource(mode.label)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
        Text(
            text = stringResource(Res.string.replay_gain_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val ReplayGainMode.label: StringResource
    get() = when (this) {
        ReplayGainMode.OFF -> Res.string.replay_gain_off
        ReplayGainMode.TRACK -> Res.string.replay_gain_track
        ReplayGainMode.ALBUM -> Res.string.replay_gain_album
    }

@PreviewScreenSizes
@Composable
private fun ReplayGainChooserPreview() {
    PreviewTheme {
        Surface {
            ReplayGainChooser(selected = ReplayGainMode.TRACK, onModeChange = {})
        }
    }
}
