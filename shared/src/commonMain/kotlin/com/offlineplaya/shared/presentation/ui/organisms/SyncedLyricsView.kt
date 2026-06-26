package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.lyrics.LyricLine
import com.offlineplaya.shared.presentation.lyrics.LyricsUiState
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.lyrics_empty_subtitle
import offlineplaya.shared.generated.resources.lyrics_empty_title
import org.jetbrains.compose.resources.stringResource

/**
 * Renders the four [LyricsUiState] cases. Shared by the full Lyrics page and
 * the tap-to-flip view on Now Playing.
 *
 * Synced lyrics highlight the active line, dim the rest, auto-scroll the active
 * line toward the vertical center, and let the user tap any line to seek to it.
 */
@Composable
fun SyncedLyricsView(
    state: LyricsUiState,
    onSeekToLine: (LyricLine) -> Unit,
    modifier: Modifier = Modifier,
) {
    // NB: the TestTags.Lyrics.* state tags below live in this shared view, which
    // NowPlaying also reuses for its tap-to-flip lyrics face. A future
    // NowPlaying-lyrics test would therefore also match these tags.
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            LyricsUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.testTag(TestTags.Lyrics.LOADING),
            )
            LyricsUiState.None -> EmptyState(
                title = stringResource(Res.string.lyrics_empty_title),
                subtitle = stringResource(Res.string.lyrics_empty_subtitle),
                modifier = Modifier.testTag(TestTags.Lyrics.EMPTY),
            )
            is LyricsUiState.Plain -> PlainLyrics(state.text)
            is LyricsUiState.Synced -> SyncedLyrics(state, onSeekToLine)
        }
        // Top + bottom fade so lines dissolve at the edges instead of being
        // hard-clipped mid-sentence against the header / transport. Only the
        // scrolling states need it; the centered empty/loading states don't.
        if (state is LyricsUiState.Synced || state is LyricsUiState.Plain) {
            EdgeFade(top = true, modifier = Modifier.align(Alignment.TopCenter))
            EdgeFade(top = false, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** A vertical scrim that fades the surface color into transparent. */
@Composable
private fun EdgeFade(top: Boolean, modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    val colors = if (top) listOf(surface, androidx.compose.ui.graphics.Color.Transparent)
    else listOf(androidx.compose.ui.graphics.Color.Transparent, surface)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Brush.verticalGradient(colors)),
    )
}

@Composable
private fun PlainLyrics(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag(TestTags.Lyrics.PLAIN),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SyncedLyrics(
    state: LyricsUiState.Synced,
    onSeekToLine: (LyricLine) -> Unit,
) {
    val listState = rememberLazyListState()

    // Keep the active line near the vertical center as it advances.
    LaunchedEffect(state.activeIndex) {
        val index = state.activeIndex
        if (index >= 0) {
            val viewportHeight = listState.layoutInfo.viewportSize.height
            // Negative offset pulls the target up toward the middle; falls back
            // to a plain scroll-to before the first layout pass measures height.
            listState.animateScrollToItem(index, scrollOffset = -(viewportHeight / 2))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag(TestTags.Lyrics.SYNCED),
        contentPadding = PaddingValues(vertical = 120.dp, horizontal = 24.dp),
    ) {
        itemsIndexed(state.lines) { index, line ->
            val isActive = index == state.activeIndex
            val color by animateColorAsState(
                targetValue = if (isActive) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                label = "lyricLineColor",
            )
            Text(
                // Empty (instrumental) lines show a music glyph so the scroll
                // still has something to land on.
                text = line.text.ifBlank { "♪" },
                style = if (isActive) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeekToLine(line) }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SyncedLyricsViewSyncedPreview() {
    PreviewTheme {
        SyncedLyricsView(
            state = LyricsUiState.Synced(
                lines = listOf(
                    LyricLine(0L, "First line of the song"),
                    LyricLine(3_000L, "Second line goes here"),
                    LyricLine(6_000L, "This is the current line"),
                    LyricLine(9_000L, "And the next one after"),
                    LyricLine(12_000L, ""),
                    LyricLine(15_000L, "Final line"),
                ),
                activeIndex = 2,
            ),
            onSeekToLine = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SyncedLyricsViewPlainPreview() {
    PreviewTheme {
        SyncedLyricsView(
            state = LyricsUiState.Plain(
                "These lyrics have no timestamps\nso they scroll as a block\nwith no highlighting",
            ),
            onSeekToLine = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SyncedLyricsViewEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        SyncedLyricsView(state = LyricsUiState.None, onSeekToLine = {})
    }
}
