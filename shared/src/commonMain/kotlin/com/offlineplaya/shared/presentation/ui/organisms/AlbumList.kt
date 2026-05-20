package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.molecules.AlbumRow
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun AlbumList(
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    representativeTrackProvider: suspend (Long) -> Track? = { null },
) {
    if (albums.isEmpty()) {
        EmptyState(
            title = "No albums",
            subtitle = "This artist has no albums in the scanned library.",
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(items = albums, key = { it.id }) { album ->
            AlbumRow(
                album = album,
                onClick = { onAlbumClick(album.id) },
                representativeTrackProvider = representativeTrackProvider,
            )
        }
    }
}

@Preview
@Composable
private fun AlbumListPopulatedPreview() {
    PreviewTheme {
        Surface {
            AlbumList(
                albums = listOf(
                    Album(1, "Ten", artistId = 1, year = 1991, trackCount = 11, durationMs = 0),
                    Album(2, "Vs.", artistId = 1, year = 1993, trackCount = 12, durationMs = 0),
                    Album(3, "Live at the Gorge", artistId = 1, year = null, trackCount = 24, durationMs = 0),
                ),
                onAlbumClick = {},
            )
        }
    }
}
