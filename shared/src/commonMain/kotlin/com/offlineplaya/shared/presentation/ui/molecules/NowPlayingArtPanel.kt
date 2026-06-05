package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
import com.offlineplaya.shared.presentation.ui.nowPlayingSharedArtKey

/**
 * Now-playing album-art container. The art is always a square sized to the
 * smaller of the available width/height so wide / landscape / car displays
 * don't blow it up taller than the screen.
 *
 * When [sharedArtTrack] matches [track], the cover carries the shared-element
 * key so it morphs from the tapped list row (see [nowPlayingSharedArtKey]).
 * Neighbouring pager pages pass a non-matching [sharedArtTrack] (or `null`)
 * and render without the morph.
 */
@Composable
fun NowPlayingArtPanel(
    track: Track,
    modifier: Modifier = Modifier,
    sharedArtTrack: Track? = null,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val side = if (maxHeight < maxWidth) maxHeight else maxWidth
        val sharedKey = if (sharedArtTrack?.id == track.id) {
            nowPlayingSharedArtKey(track.id)
        } else {
            null
        }
        AlbumArtThumb(
            track = track,
            size = side,
            cornerRadius = 16.dp,
            glyphStyle = MaterialTheme.typography.displayLarge,
            sharedKey = sharedKey,
        )
    }
}
