package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.lyrics.LyricLine
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.lyrics.LyricsUiState
import com.offlineplaya.shared.presentation.ui.LocalSharedTransitionScope
import com.offlineplaya.shared.presentation.ui.molecules.NowPlayingArtPanel
import com.offlineplaya.shared.presentation.ui.molecules.NowPlayingHeadline
import com.offlineplaya.shared.presentation.ui.molecules.SeekRow
import com.offlineplaya.shared.presentation.ui.molecules.TransportRow

/**
 * Now Playing body. Adaptive layout: side-by-side when the canvas is wider
 * than tall and ≥600dp (tablet / landscape / car), stacked otherwise. The
 * caller is responsible for the top bar and the empty-state fallback.
 *
 * The album-art face is a [HorizontalPager] over the play queue, so a
 * horizontal swipe skips to the adjacent track (and an external skip / track
 * end animates the pager to the new page). Tapping the art still flips to the
 * synced-lyrics view.
 *
 * [sharedArtTrack] is the track whose art should carry the shared-element key
 * for the list → Now Playing morph; it's usually the same as
 * [PlaybackState.currentTrack] but is threaded separately so the morph still
 * fires during the brief window where the player hasn't caught up to the
 * tapped track yet.
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
    onSkipToIndex: (Int) -> Unit = {},
    sharedArtTrack: Track? = null,
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
                    state = state,
                    lyricsState = lyricsState,
                    onSeekToLine = onSeekToLine,
                    onSkipToIndex = onSkipToIndex,
                    sharedArtTrack = sharedArtTrack,
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
                    state = state,
                    lyricsState = lyricsState,
                    onSeekToLine = onSeekToLine,
                    onSkipToIndex = onSkipToIndex,
                    sharedArtTrack = sharedArtTrack,
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
 * The album-art area, tappable to flip to the synced-lyrics view and back.
 * The art face is a swipeable pager over the queue (see [QueueArtPager]);
 * lyric taps inside [SyncedLyricsView] consume the gesture (seek), so only
 * taps on empty space toggle back to the art.
 */
@Composable
private fun FlippableArt(
    state: PlaybackState,
    lyricsState: LyricsUiState,
    onSeekToLine: (LyricLine) -> Unit,
    onSkipToIndex: (Int) -> Unit,
    sharedArtTrack: Track?,
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
                QueueArtPager(
                    state = state,
                    onSkipToIndex = onSkipToIndex,
                    sharedArtTrack = sharedArtTrack,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * Horizontal pager of album-art panels over the play queue. Swiping settles on
 * a new page and calls [onSkipToIndex]; an external index change (skip button,
 * track end) animates the pager to the matching page. The guard on
 * [PlaybackState.queueIndex] keeps the two directions from forming a feedback
 * loop. Falls back to a single static panel when the queue has 0–1 entries.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun QueueArtPager(
    state: PlaybackState,
    onSkipToIndex: (Int) -> Unit,
    sharedArtTrack: Track?,
    modifier: Modifier = Modifier,
) {
    val queue = state.queue
    val currentTrack = state.currentTrack
    // The cover we want on screen: the just-tapped track during the async
    // window before the player catches up, otherwise the live track.
    val effectiveTrack = sharedArtTrack ?: currentTrack ?: queue.firstOrNull() ?: return

    // Is the queue loaded AND aligned so that the page at queueIndex is the
    // track we're keying the morph on? Until it is, render a single static
    // panel for that track so the shared-element morph lands on frame one
    // (the pager would otherwise still be showing the previous queue).
    val aligned = queue.size > 1 &&
        state.queueIndex in queue.indices &&
        queue[state.queueIndex].id == effectiveTrack.id

    // Don't swap single-panel → pager *during* the list → Now Playing morph:
    // tearing down and recreating the shared-element host node mid-flight makes
    // the cover jump. Keep the single keyed panel until the transition settles,
    // then swap to the swipeable pager.
    val morphActive = LocalSharedTransitionScope.current?.isTransitionActive == true

    if (!aligned || morphActive) {
        NowPlayingArtPanel(
            track = effectiveTrack,
            sharedArtTrack = effectiveTrack,
            modifier = modifier,
        )
        return
    }

    val pagerState = rememberPagerState(
        initialPage = state.queueIndex,
        pageCount = { queue.size },
    )

    // External skip / track-end → animate the pager to the new page. Use a
    // deliberate tween (~420ms) rather than the pager's default fast spring so
    // a Next-button tap / auto-advance glides instead of snapping across.
    LaunchedEffect(state.queueIndex) {
        if (state.queueIndex in queue.indices && state.queueIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(
                page = state.queueIndex,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            )
        }
    }

    // Only a *user-dragged* settle should seek the player. A settle caused by
    // our own animateScrollToPage (re-syncing to an external skip) or by a
    // queue swap must NOT call onSkipToIndex — otherwise the pager and the
    // player ping-pong and a stale page seeks the audio to the wrong track.
    // The pager's interactionSource tells a drag apart from a programmatic
    // scroll; settledPage (not currentPage) means we fire once, after the fling.
    var userDragged by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState) {
        pagerState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) userDragged = true
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (userDragged && page != state.queueIndex && page in queue.indices) {
                onSkipToIndex(page)
            }
            userDragged = false
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        pageSpacing = 16.dp,
    ) { page ->
        val pageTrack = queue[page]
        NowPlayingArtPanel(
            track = pageTrack,
            // Only the page matching the shared art track carries the morph
            // key, so the list → Now Playing transition lands on the right
            // cover; neighbouring pages render plain.
            sharedArtTrack = sharedArtTrack,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
