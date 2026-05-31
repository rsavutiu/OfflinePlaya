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

/**
 * Now-playing album-art container. The art is always a square sized to the
 * smaller of the available width/height so wide / landscape / car displays
 * don't blow it up taller than the screen.
 */
@Composable
fun NowPlayingArtPanel(
    track: Track,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val side = if (maxHeight < maxWidth) maxHeight else maxWidth
        AlbumArtThumb(
            track = track,
            size = side,
            cornerRadius = 16.dp,
            glyphStyle = MaterialTheme.typography.displayLarge,
        )
    }
}
