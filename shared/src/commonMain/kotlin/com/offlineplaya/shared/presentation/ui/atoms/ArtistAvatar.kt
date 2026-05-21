package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Circular artist avatar. Shows the downloaded artist image via Coil;
 * falls back to a primaryContainer-colored circle with the artist's initial.
 */
@Composable
fun ArtistAvatar(
    artist: Artist?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Surface(
        modifier = modifier.size(size),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
    ) {
        val initial = artist?.name?.firstLetterOrPlaceholder() ?: "?"
        
        SubcomposeAsyncImage(
            model = artist,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { InitialPlaceholder(initial) },
            error = { InitialPlaceholder(initial) },
            success = { SubcomposeAsyncImageContent() },
        )
    }
}

@Composable
private fun InitialPlaceholder(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private fun String.firstLetterOrPlaceholder(): String {
    val first = firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()
    return first?.toString() ?: "?"
}

@Preview
@Composable
private fun ArtistAvatarPreview() {
    PreviewTheme {
        ArtistAvatar(
            artist = Artist(1, "Pearl Jam", 11, 142),
            size = 40.dp
        )
    }
}
