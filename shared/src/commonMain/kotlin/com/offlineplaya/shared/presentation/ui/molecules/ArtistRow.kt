package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * A row in the artist list. Leading initial avatar, name, and a count summary.
 */
@Composable
fun ArtistRow(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialAvatar(text = artist.name.firstLetterOrPlaceholder())
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary(artist),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InitialAvatar(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .let { it },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

private fun String.firstLetterOrPlaceholder(): String {
    val first = firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()
    return first?.toString() ?: "?"
}

private fun summary(artist: Artist): String {
    val albumPart = "${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"}"
    val trackPart = "${artist.trackCount} ${if (artist.trackCount == 1) "track" else "tracks"}"
    return "$albumPart · $trackPart"
}

@Preview
@Composable
private fun ArtistRowPreview() {
    PreviewTheme {
        Surface {
            ArtistRow(
                artist = Artist(id = 1, name = "Pearl Jam", albumCount = 11, trackCount = 142),
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun ArtistRowSingletonsPreview() {
    PreviewTheme {
        Surface {
            ArtistRow(
                artist = Artist(id = 1, name = "Solo Project", albumCount = 1, trackCount = 1),
                onClick = {},
            )
        }
    }
}
