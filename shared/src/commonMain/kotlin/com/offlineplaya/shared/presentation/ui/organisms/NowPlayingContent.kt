package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.presentation.ui.molecules.NowPlayingArtPanel
import com.offlineplaya.shared.presentation.ui.molecules.NowPlayingHeadline
import com.offlineplaya.shared.presentation.ui.molecules.SeekRow
import com.offlineplaya.shared.presentation.ui.molecules.TransportRow

/**
 * Now Playing body. Adaptive layout: side-by-side when the canvas is wider
 * than tall and ≥600dp (tablet / landscape / car), stacked otherwise. The
 * caller is responsible for the top bar and the empty-state fallback.
 */
@Composable
fun NowPlayingContent(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatChange: (RepeatMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.currentTrack ?: return
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp && maxWidth > maxHeight
        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NowPlayingArtPanel(
                    track = track,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
                ) {
                    NowPlayingHeadline(track = track)
                    SeekRow(state = state, onSeek = onSeek)
                    TransportRow(
                        state = state,
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onShuffleToggle = onShuffleToggle,
                        onRepeatChange = onRepeatChange,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            ) {
                NowPlayingArtPanel(track = track, modifier = Modifier.weight(1f, fill = false))
                NowPlayingHeadline(track = track)
                SeekRow(state = state, onSeek = onSeek)
                TransportRow(
                    state = state,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onShuffleToggle = onShuffleToggle,
                    onRepeatChange = onRepeatChange,
                )
            }
        }
    }
}
