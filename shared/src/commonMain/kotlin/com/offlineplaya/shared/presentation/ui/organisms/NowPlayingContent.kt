package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.lyrics.LyricLine
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.lyrics.LyricsUiState
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
    lyricsState: LyricsUiState = LyricsUiState.None,
    onSeekToLine: (LyricLine) -> Unit = {},
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
                FlippableArt(
                    track = track,
                    lyricsState = lyricsState,
                    onSeekToLine = onSeekToLine,
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
                FlippableArt(
                    track = track,
                    lyricsState = lyricsState,
                    onSeekToLine = onSeekToLine,
                    modifier = Modifier.weight(1f, fill = false),
                )
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

/**
 * The album-art panel, tappable to flip to the synced-lyrics view and back.
 * Lyric taps inside [SyncedLyricsView] consume the gesture (seek), so only taps
 * on empty space toggle back to the art.
 */
@Composable
private fun FlippableArt(
    track: Track,
    lyricsState: LyricsUiState,
    onSeekToLine: (LyricLine) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLyrics by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.clickable { showLyrics = !showLyrics },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = showLyrics,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "artLyricsFlip",
        ) { lyrics ->
            if (lyrics) {
                SyncedLyricsView(
                    state = lyricsState,
                    onSeekToLine = onSeekToLine,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                NowPlayingArtPanel(track = track, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
