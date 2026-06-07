package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent

/**
 * Playback seek slider with elapsed / total labels. Drags update a local draft
 * so the thumb tracks the finger smoothly; the seek is only committed on
 * release to avoid spamming the player with intermediate positions.
 */
@Composable
fun SeekRow(
    state: PlaybackState,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    var draftMs by remember { mutableStateOf(state.positionMs) }
    val displayedMs = if (dragging) draftMs else state.positionMs
    val brand = LocalBrandAccent.current.accent

    Column(modifier = modifier.fillMaxWidth()) {
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
            // Brand accent on the thumb + filled track so the playhead pops
            // against whatever ambient album-art tint the Now Playing wears.
            colors = SliderDefaults.colors(
                thumbColor = brand,
                activeTrackColor = brand,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
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
