package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.cd_next
import offlineplaya.shared.generated.resources.cd_pause
import offlineplaya.shared.generated.resources.cd_play
import offlineplaya.shared.generated.resources.cd_previous
import org.jetbrains.compose.resources.stringResource

/**
 * Prev / Play-Pause / Next button trio. Size and spacing are tuned for the
 * MiniPlayer; the full NowPlayingPage uses [PlaybackControlsLarge].
 */
@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = stringResource(Res.string.cd_previous),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onPlayPause) {
            Crossfade(targetState = isPlaying, animationSpec = tween(150), label = "playPause") { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) stringResource(Res.string.cd_pause) else stringResource(
                        Res.string.cd_play
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = stringResource(Res.string.cd_next),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Larger variant for the NowPlayingPage. */
@Composable
fun PlaybackControlsLarge(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = stringResource(Res.string.cd_previous),
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onPlayPause, modifier = Modifier.size(72.dp).testTag(TestTags.NowPlaying.PLAY_PAUSE)) {
            Crossfade(targetState = isPlaying, animationSpec = tween(150), label = "playPauseLg") { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) stringResource(Res.string.cd_pause) else stringResource(
                        Res.string.cd_play
                    ),
                    modifier = Modifier.size(56.dp),
                    tint = LocalBrandAccent.current.accent,
                )
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = stringResource(Res.string.cd_next),
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun PlaybackControlsPlayingPreview() {
    PreviewTheme {
        Surface { PlaybackControls(isPlaying = true, onPlayPause = {}, onPrevious = {}, onNext = {}) }
    }
}

@PreviewScreenSizes
@Composable
private fun PlaybackControlsPausedPreview() {
    PreviewTheme(darkTheme = true) {
        Surface { PlaybackControls(isPlaying = false, onPlayPause = {}, onPrevious = {}, onNext = {}) }
    }
}

@PreviewScreenSizes
@Composable
private fun PlaybackControlsLargePreview() {
    PreviewTheme {
        Surface {
            PlaybackControlsLarge(isPlaying = true, onPlayPause = {}, onPrevious = {}, onNext = {})
        }
    }
}
