package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun AlbumRow(
    album: Album,
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
        AlbumThumb()
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitleFor(album),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlbumThumb() {
    // Album art arrives in Phase 6 polish; for now a tinted rounded-square block.
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "♫",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

private fun subtitleFor(album: Album): String {
    val yearPart = album.year?.toString()
    val trackPart = "${album.trackCount} ${if (album.trackCount == 1) "track" else "tracks"}"
    return listOfNotNull(yearPart, trackPart).joinToString(" · ")
}

@Preview
@Composable
private fun AlbumRowFullPreview() {
    PreviewTheme {
        Surface {
            AlbumRow(
                album = Album(id = 1, name = "Ten", artistId = 1, year = 1991, trackCount = 11, durationMs = 0),
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun AlbumRowNoYearPreview() {
    PreviewTheme {
        Surface {
            AlbumRow(
                album = Album(id = 2, name = "Untitled Bootleg", artistId = 1, year = null, trackCount = 7, durationMs = 0),
                onClick = {},
            )
        }
    }
}
