package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column {
                ProgressLine(state = state)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onExpand)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArtThumb(track = track, size = 44.dp, cornerRadius = 8.dp)
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
                            modifier = Modifier.basicMarquee(),
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
}


@Composable
private fun ProgressLine(state: PlaybackState) {
    val total = state.durationMs.coerceAtLeast(1L)
    val progress = (state.positionMs.toFloat() / total).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress)
                .height(3.dp)
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
