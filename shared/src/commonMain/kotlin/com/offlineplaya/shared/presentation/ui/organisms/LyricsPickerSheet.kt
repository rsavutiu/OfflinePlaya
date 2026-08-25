package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.lyrics.LyricsCandidate
import com.offlineplaya.shared.presentation.lyrics.LyricsPickerState
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.lyrics_pick_empty
import offlineplaya.shared.generated.resources.lyrics_pick_loading
import offlineplaya.shared.generated.resources.lyrics_pick_synced
import offlineplaya.shared.generated.resources.lyrics_pick_title
import org.jetbrains.compose.resources.stringResource

/**
 * "Pick from matches" bottom sheet: shows the LRCLIB candidates for the current
 * track so the user can replace a wrong/missing auto-match. Rendered as a modal
 * sheet; returns nothing when [state] is [LyricsPickerState.Hidden].
 *
 * The visible content is factored into [LyricsPickerContent] so it can carry a
 * real preview (a modal sheet can't be previewed directly).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsPickerSheet(
    state: LyricsPickerState,
    onChoose: (LyricsCandidate) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is LyricsPickerState.Hidden) return
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        LyricsPickerContent(state = state, onChoose = onChoose)
    }
}

@Composable
private fun LyricsPickerContent(
    state: LyricsPickerState,
    onChoose: (LyricsCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag(TestTags.Lyrics.PICKER_SHEET),
    ) {
        Text(
            text = stringResource(Res.string.lyrics_pick_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        when (state) {
            // Not reachable — the sheet isn't shown when hidden — but the when
            // must be exhaustive.
            LyricsPickerState.Hidden -> Unit
            LyricsPickerState.Loading -> Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(Res.string.lyrics_pick_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LyricsPickerState.Empty -> Text(
                text = stringResource(Res.string.lyrics_pick_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            )
            is LyricsPickerState.Loaded -> LazyColumn(
                // Bounded so a long list scrolls inside the sheet instead of
                // pushing it past the screen.
                modifier = Modifier.heightIn(max = 480.dp),
            ) {
                items(state.candidates, key = { it.id }) { candidate ->
                    CandidateRow(candidate = candidate, onChoose = onChoose)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: LyricsCandidate,
    onChoose: (LyricsCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onChoose(candidate) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.trackName.ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = buildString {
                append(candidate.artistName)
                candidate.albumName?.let { if (it.isNotBlank()) append(" • ").append(it) }
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatDuration(candidate.durationSec),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (candidate.synced) {
                Text(
                    text = stringResource(Res.string.lyrics_pick_synced),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalBrandAccent.current.accent,
                )
            }
        }
    }
}

/** Seconds → "m:ss"; blank for a missing/zero duration. */
private fun formatDuration(totalSec: Int): String {
    if (totalSec <= 0) return "--:--"
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

@PreviewScreenSizes
@Composable
private fun LyricsPickerLoadedPreview() {
    PreviewTheme {
        Surface {
            LyricsPickerContent(
                state = LyricsPickerState.Loaded(
                    listOf(
                        LyricsCandidate(1, "Fire Rides", "MØ", "No Mythologies To Follow", 218, true, "[00:01.00]…"),
                        LyricsCandidate(2, "Fire Rides", "MØ", "No Mythologies To Follow (Deluxe)", 221, false, "…"),
                        LyricsCandidate(3, "Fire Rides (Night Version)", "MØ", null, 240, true, "[00:01.00]…"),
                    ),
                ),
                onChoose = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun LyricsPickerEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            LyricsPickerContent(state = LyricsPickerState.Empty, onChoose = {})
        }
    }
}
