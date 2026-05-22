package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Square album-art thumbnail. Renders the embedded album art for [track]
 * via Coil's custom `TrackArtFetcher`; falls back to a tinted ♫ glyph when
 * art is missing or the track is `null` (e.g. while loading state).
 *
 * The Surface itself stays edge-to-edge with a rounded corner so adjacent
 * layouts (rows, NowPlaying art panel) can drop it in at any size.
 */
@Composable
fun AlbumArtThumb(
    track: Track?,
    modifier: Modifier = Modifier,
    size: Dp? = 40.dp,
    cornerRadius: Dp = 6.dp,
    glyphStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
) {
    Surface(
        modifier = if (size != null) modifier.size(size) else modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(cornerRadius),
    ) {
        if (track == null) {
            Placeholder(glyphStyle)
        } else {
            SubcomposeAsyncImage(
                model = track,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { Placeholder(glyphStyle) },
                error = { Placeholder(glyphStyle) },
                success = { SubcomposeAsyncImageContent() },
            )
        }
    }
}

@Composable
private fun Placeholder(style: androidx.compose.ui.text.TextStyle) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "♫",
            style = style,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Preview
@Composable
private fun AlbumArtThumbSmallPreview() {
    PreviewTheme {
        Surface { AlbumArtThumb(track = null, size = 40.dp) }
    }
}

@Preview
@Composable
private fun AlbumArtThumbLargePreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            AlbumArtThumb(
                track = null,
                size = 200.dp,
                cornerRadius = 16.dp,
                glyphStyle = MaterialTheme.typography.displayLarge,
            )
        }
    }
}
