package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.molecules.PlaybackControls
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Persistent bottom-anchored mini-player shown above the page content while
 * something is queued. Tap the body to open the full NowPlayingPage; the
 * transport buttons act inline.
 *
 * Renders nothing when [state] has no current track.
 */
@Composable
fun MiniPlayer(
    state: PlaybackState,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.currentTrack ?: return
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        // Surface fills the bottomBar slot and extends behind the navigation
        // bar; the inner Column pads upward off the nav bar so the tap targets
        // stay reachable. This gives edge-to-edge visual continuity without
        // breaking ergonomics.
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            ProgressLine(state = state)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpand)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumThumb()
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PlaybackControls(
                    isPlaying = state.isPlaying,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
            }
        }
    }
}

@Composable
private fun AlbumThumb() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.size(40.dp),
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Text(
                text = "♫",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun ProgressLine(state: PlaybackState) {
    val total = state.durationMs.coerceAtLeast(1L)
    val progress = (state.positionMs.toFloat() / total).coerceIn(0f, 1f)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Preview
@Composable
private fun MiniPlayerPlayingPreview() {
    PreviewTheme {
        MiniPlayer(
            state = PlaybackState(
                currentTrack = sampleTrack("Once", "Pearl Jam"),
                isPlaying = true,
                positionMs = 60_000L,
                durationMs = 224_000L,
                shuffleEnabled = false,
                repeatMode = com.offlineplaya.shared.domain.model.RepeatMode.OFF,
                queue = emptyList(),
                queueIndex = 0,
                volume = 1f,
            ),
            onExpand = {}, onPlayPause = {}, onPrevious = {}, onNext = {},
        )
    }
}

@Preview
@Composable
private fun MiniPlayerPausedDarkPreview() {
    PreviewTheme(darkTheme = true) {
        MiniPlayer(
            state = PlaybackState(
                currentTrack = sampleTrack("Even Flow", "Pearl Jam"),
                isPlaying = false,
                positionMs = 12_000L,
                durationMs = 296_000L,
                shuffleEnabled = false,
                repeatMode = com.offlineplaya.shared.domain.model.RepeatMode.OFF,
                queue = emptyList(),
                queueIndex = 0,
                volume = 1f,
            ),
            onExpand = {}, onPlayPause = {}, onPrevious = {}, onNext = {},
        )
    }
}

private fun sampleTrack(title: String, artist: String) = Track(
    id = 1L,
    documentUri = "preview",
    treeUri = "preview",
    relativePath = "$artist/Ten/$title.flac",
    fileName = "$title.flac",
    title = title,
    artistName = artist,
    albumArtistName = null,
    albumName = "Ten",
    genre = "Rock",
    year = 1991,
    trackNumber = 1,
    discNumber = 1,
    durationMs = 224_000L,
    bitrate = 1_000_000,
    sampleRate = 44_100,
    channels = 2,
    codec = "flac",
    artistId = 1L,
    albumId = 1L,
    folderId = 1L,
    scanStatus = ScanStatus.SCANNED,
)
