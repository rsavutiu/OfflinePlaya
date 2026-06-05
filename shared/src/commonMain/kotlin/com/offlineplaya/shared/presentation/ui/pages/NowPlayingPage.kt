package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.lyrics.LyricLine
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.lyrics.LyricsUiState
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.NowPlayingArtPanel
import com.offlineplaya.shared.presentation.ui.organisms.NowPlayingContent
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.cd_equalizer
import offlineplaya.shared.generated.resources.cd_lyrics
import offlineplaya.shared.generated.resources.now_playing_empty_subtitle
import offlineplaya.shared.generated.resources.now_playing_empty_title
import offlineplaya.shared.generated.resources.now_playing_title
import offlineplaya.shared.generated.resources.now_playing_up_next
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen now-playing surface. Top bar + adaptive content body or an
 * empty-state when nothing is loaded into the player.
 */
@Composable
fun NowPlayingPage(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatChange: (RepeatMode) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLyrics: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    lyricsState: LyricsUiState = LyricsUiState.None,
    onSeekToLine: (LyricLine) -> Unit = {},
    onSkipToIndex: (Int) -> Unit = {},
    sharedArtTrack: Track? = null,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.now_playing_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onOpenLyrics) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = stringResource(Res.string.cd_lyrics),
                        )
                    }
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = stringResource(Res.string.cd_equalizer),
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
        if (state.currentTrack == null && sharedArtTrack == null) {
            EmptyState(
                title = stringResource(Res.string.now_playing_empty_title),
                subtitle = stringResource(Res.string.now_playing_empty_subtitle),
                modifier = Modifier.padding(padding),
            )
        } else if (state.currentTrack == null) {
            // Cold-start window: the user tapped a track with nothing playing,
            // so the player hasn't loaded currentTrack yet. Render just the
            // keyed hero art for the tapped track so the list → Now Playing
            // morph still lands; the full content swaps in a frame later.
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                NowPlayingArtPanel(
                    track = sharedArtTrack!!,
                    sharedArtTrack = sharedArtTrack,
                    modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            NowPlayingContent(
                state = state,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
                onShuffleToggle = onShuffleToggle,
                onRepeatChange = onRepeatChange,
                modifier = Modifier.padding(padding),
                lyricsState = lyricsState,
                onSeekToLine = onSeekToLine,
                onSkipToIndex = onSkipToIndex,
                sharedArtTrack = sharedArtTrack,
            )
        }
    }
}

@PreviewScreenSizes
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
                queue = persistentListOf(),
                queueIndex = 0,
                volume = 1f,
            ),
            onPlayPause = {}, onPrevious = {}, onNext = {}, onSeek = {},
            onShuffleToggle = {}, onRepeatChange = {}, onOpenQueue = {}, onOpenEqualizer = {}, onOpenLyrics = {}, onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun NowPlayingPageWidePreview() {
    PreviewTheme {
        NowPlayingPage(
            state = PlaybackState(
                currentTrack = previewTrack("Black", "Pearl Jam", "Ten"),
                isPlaying = true,
                positionMs = 120_000L,
                durationMs = 343_000L,
                shuffleEnabled = true,
                repeatMode = RepeatMode.ALL,
                queue = persistentListOf(),
                queueIndex = 0,
                volume = 1f,
            ),
            onPlayPause = {},
            onPrevious = {},
            onNext = {},
            onSeek = {},
            onShuffleToggle = {},
            onRepeatChange = {},
            onOpenQueue = {},
            onOpenEqualizer = {},
            onOpenLyrics = {},
            onBack = {},
            modifier = Modifier.size(width = 900.dp, height = 480.dp),
        )
    }
}

@PreviewScreenSizes
@Composable
private fun NowPlayingPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        NowPlayingPage(
            state = PlaybackState.Empty,
            onPlayPause = {}, onPrevious = {}, onNext = {}, onSeek = {},
            onShuffleToggle = {}, onRepeatChange = {}, onOpenQueue = {}, onOpenEqualizer = {}, onOpenLyrics = {}, onBack = {},
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
