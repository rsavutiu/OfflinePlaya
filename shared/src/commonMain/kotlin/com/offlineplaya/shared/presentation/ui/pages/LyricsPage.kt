package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.lyrics.LyricLine
import com.offlineplaya.shared.presentation.lyrics.LyricsUiState
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.organisms.SyncedLyricsView

import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.lyrics_title
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen lyrics page reached from the Now Playing aux row. Top bar titled
 * with the track name (falling back to "Lyrics") over the shared
 * [SyncedLyricsView].
 */
@Composable
fun LyricsPage(
    state: LyricsUiState,
    trackTitle: String?,
    onSeekToLine: (LyricLine) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = trackTitle ?: stringResource(Res.string.lyrics_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        SyncedLyricsView(
            state = state,
            onSeekToLine = onSeekToLine,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        )
    }
}

@PreviewScreenSizes
@Composable
private fun LyricsPageSyncedPreview() {
    PreviewTheme {
        LyricsPage(
            state = LyricsUiState.Synced(
                lines = listOf(
                    LyricLine(0L, "Lights will guide you home"),
                    LyricLine(4_000L, "And ignite your bones"),
                    LyricLine(8_000L, "And I will try to fix you"),
                ),
                activeIndex = 1,
            ),
            trackTitle = "Fix You",
            onSeekToLine = {},
            onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun LyricsPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        LyricsPage(
            state = LyricsUiState.None,
            trackTitle = null,
            onSeekToLine = {},
            onBack = {},
        )
    }
}
