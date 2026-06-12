package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
import com.offlineplaya.shared.presentation.ui.molecules.PlaybackControls
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf

/**
 * Vertical footprint of the [MiniPlayer]'s content above the navigation-bar
 * inset (3 dp progress line + 64 dp row). Use [MiniPlayerReservedSpace] to
 * leave matching room when no track is queued so the page above doesn't
 * reflow the moment playback starts.
 */
val MiniPlayerContentHeight: Dp = 67.dp

/**
 * Empty bottom strip that occupies the same vertical space as a rendered
 * [MiniPlayer]. Render this in the bottom-bar slot when nothing is queued
 * so the layout above is identical whether or not music is playing.
 */
@Composable
fun MiniPlayerReservedSpace(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(Modifier
            .fillMaxWidth()
            .height(MiniPlayerContentHeight))
    }
}

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
    // Docked to the bottom edge — the surface colour extends *behind* the
    // nav bar so there's no jarring cut between the player and the system
    // chrome. Only the tappable content row inside is shifted up by the
    // nav-bar inset so controls stay reachable.
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column {
            ProgressLine(state = state)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpand)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtThumb(track = track, size = 44.dp, cornerRadius = 8.dp)
                AnimatedContent(
                    targetState = track,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    contentKey = { it.id },
                    label = "miniTrackInfo",
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) { t ->
                    Column {
                        Text(
                            text = t.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                        )
                        Text(
                            text = t.artistName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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
private fun ProgressLine(state: PlaybackState) {
    val total = state.durationMs.coerceAtLeast(1L)
    val raw = (state.positionMs.toFloat() / total).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = raw,
        animationSpec = tween(durationMillis = 300),
        label = "miniProgress",
    )
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

@PreviewScreenSizes
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
                queue = persistentListOf(),
                queueIndex = 0,
                volume = 1f,
            ),
            onExpand = {}, onPlayPause = {}, onPrevious = {}, onNext = {},
        )
    }
}

@PreviewScreenSizes
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
                queue = persistentListOf(),
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
