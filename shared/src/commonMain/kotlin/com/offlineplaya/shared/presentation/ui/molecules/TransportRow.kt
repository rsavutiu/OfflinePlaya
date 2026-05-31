package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.presentation.ui.atoms.RepeatToggle
import com.offlineplaya.shared.presentation.ui.atoms.ShuffleToggle

/**
 * Bottom transport strip on Now Playing: shuffle on the left, big play /
 * skip cluster in the middle, repeat on the right.
 */
@Composable
fun TransportRow(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatChange: (RepeatMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShuffleToggle(enabled = state.shuffleEnabled, onToggle = onShuffleToggle)
        PlaybackControlsLarge(
            isPlaying = state.isPlaying,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
        )
        RepeatToggle(mode = state.repeatMode, onCycle = onRepeatChange)
    }
}
