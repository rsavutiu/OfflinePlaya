package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.domain.lyrics.LyricLine
import com.offlineplaya.shared.domain.lyrics.LyricsCandidate
import com.offlineplaya.shared.presentation.lyrics.LyricsPickerState
import com.offlineplaya.shared.presentation.lyrics.LyricsUiState
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.organisms.LyricsPickerSheet
import com.offlineplaya.shared.presentation.ui.organisms.SyncedLyricsView

import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.lyrics_pick
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
    canPick: Boolean = true,
    pickerState: LyricsPickerState = LyricsPickerState.Hidden,
    onOpenPicker: () -> Unit = {},
    onChooseCandidate: (LyricsCandidate) -> Unit = {},
    onDismissPicker: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.testTag(TestTags.Lyrics.ROOT),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = trackTitle ?: stringResource(Res.string.lyrics_title),
                onBack = onBack,
                actions = {
                    // "Pick from matches" — re-query LRCLIB and let the user
                    // choose the right lyrics when the auto-match is wrong.
                    // A labelled text+icon button (not a bare icon) so the
                    // affordance is obvious. Hidden when remote lyrics are off:
                    // with no source to search, the picker would only ever say
                    // "no matches", which would misinform rather than help.
                    if (canPick) {
                        TextButton(
                            onClick = onOpenPicker,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = LocalBrandAccent.current.accent,
                            ),
                            modifier = Modifier.testTag(TestTags.Lyrics.PICK_OPEN),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.lyrics_pick),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                },
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

    LyricsPickerSheet(
        state = pickerState,
        onChoose = onChooseCandidate,
        onDismiss = onDismissPicker,
    )
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
