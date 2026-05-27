package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import androidx.compose.ui.text.font.FontWeight
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.atoms.RepeatToggle
import com.offlineplaya.shared.presentation.ui.atoms.ShuffleToggle
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.PlaybackControlsLarge
import com.offlineplaya.shared.presentation.ui.molecules.formatDuration
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import org.jetbrains.compose.resources.stringResource
import offlineplaya.shared.generated.resources.*

/**
 * Full-screen now-playing view: big album art placeholder, title + artist,
 * seek bar, transport controls. Volume/shuffle/repeat are in the engine
 * already; their UI affordances arrive in Phase 5/6 polish.
 */
@Composable
fun NowPlayingPage(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatChange: (com.offlineplaya.shared.domain.model.RepeatMode) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.now_playing_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizer",
                        )
                    }
                    IconButton(onClick = onOpenQueue) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = stringResource(Res.string.now_playing_up_next),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val track = state.currentTrack
        if (track == null) {
            EmptyState(
                title = "Nothing is playing",
                subtitle = "Pick a track from the library to start playback.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            ArtPanel(track = track)
            TrackHeadline(track = track)
            SeekRow(state = state, onSeek = onSeek)
            Row(
                modifier = Modifier.fillMaxWidth(),
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
    }
}

@Composable
private fun ArtPanel(track: Track, modifier: Modifier = Modifier) {
    // BoxWithConstraints lets the thumb fill the available square panel; the
    // atom handles loading + fallback rendering uniformly.
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        AlbumArtThumb(
            track = track,
            size = maxWidth,
            cornerRadius = 16.dp,
            glyphStyle = MaterialTheme.typography.displayLarge,
        )
    }
}

@Composable
private fun TrackHeadline(track: Track) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = track.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Text(
            text = "${track.artistName} — ${track.albumName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SeekRow(
    state: PlaybackState,
    onSeek: (Long) -> Unit,
) {
    // Local slider state so dragging is smooth, only commit on release.
    var dragging by remember { mutableStateOf(false) }
    var draftMs by remember { mutableStateOf(state.positionMs) }
    val displayedMs = if (dragging) draftMs else state.positionMs

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = displayedMs.toFloat(),
            valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
            onValueChange = {
                dragging = true
                draftMs = it.toLong()
            },
            onValueChangeFinished = {
                dragging = false
                onSeek(draftMs)
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = displayedMs.formatDuration(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = state.durationMs.formatDuration(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Preview
@Composable
private fun NowPlayingPagePopulatedPreview() {
    PreviewTheme {
        NowPlayingPage(
            state = PlaybackState(
                currentTrack = previewTrack("Once", "Pearl Jam", "Ten"),
                isPlaying = true,
                positionMs = 74_000L,
                durationMs = 224_000L,
                shuffleEnabled = false,
                repeatMode = RepeatMode.OFF,
                queue = emptyList(),
                queueIndex = 0,
                volume = 1f,
            ),
            onPlayPause = {}, onPrevious = {}, onNext = {}, onSeek = {},
            onShuffleToggle = {}, onRepeatChange = {}, onOpenQueue = {}, onOpenEqualizer = {}, onBack = {},
        )
    }
}

@Preview
@Composable
private fun NowPlayingPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        NowPlayingPage(
            state = PlaybackState.Empty,
            onPlayPause = {}, onPrevious = {}, onNext = {}, onSeek = {},
            onShuffleToggle = {}, onRepeatChange = {}, onOpenQueue = {}, onOpenEqualizer = {}, onBack = {},
        )
    }
}

private fun previewTrack(title: String, artist: String, album: String) = Track(
    id = 1L,
    documentUri = "preview",
    treeUri = "preview",
    relativePath = "$artist/$album/$title.flac",
    fileName = "$title.flac",
    title = title,
    artistName = artist,
    albumArtistName = null,
    albumName = album,
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
