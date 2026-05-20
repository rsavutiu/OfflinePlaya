package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.presentation.ui.molecules.ArtistRow
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun ArtistList(
    artists: List<Artist>,
    onArtistClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (artists.isEmpty()) {
        EmptyState(
            title = "No artists yet",
            subtitle = "Scan a folder from the home page to populate the library.",
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(items = artists, key = { it.id }) { artist ->
            ArtistRow(artist = artist, onClick = { onArtistClick(artist.id) })
        }
    }
}

@Preview
@Composable
private fun ArtistListPopulatedPreview() {
    PreviewTheme {
        Surface {
            ArtistList(
                artists = listOf(
                    Artist(1, "Aphex Twin", albumCount = 6, trackCount = 92),
                    Artist(2, "Boards of Canada", albumCount = 4, trackCount = 48),
                    Artist(3, "Pearl Jam", albumCount = 11, trackCount = 142),
                ),
                onArtistClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun ArtistListEmptyPreview() {
    PreviewTheme {
        Surface {
            ArtistList(artists = emptyList(), onArtistClick = {})
        }
    }
}
