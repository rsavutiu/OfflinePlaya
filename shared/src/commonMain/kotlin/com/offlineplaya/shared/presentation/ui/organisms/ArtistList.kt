package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.presentation.ui.molecules.ArtistRow
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun ArtistList(
    artists: List<Artist>,
    onArtistClick: (Long) -> Unit,
    onPlayArtist: (Artist) -> Unit,
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
    // Adaptive columns: one column on a portrait phone (cells wider than the
    // min), two-plus once the screen is wide enough (landscape / tablet / car),
    // so browse lists fill the width instead of leaving empty side gutters.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = COLUMN_MIN_WIDTH),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = artists, key = { it.id }) { artist ->
            ArtistRow(
                artist = artist,
                onClick = { onArtistClick(artist.id) },
                onPlay = { onPlayArtist(artist) }
            )
        }
    }
}

/** Min cell width before the grid adds another column. Tuned so portrait phones stay single-column. */
private val COLUMN_MIN_WIDTH = 360.dp

@PreviewScreenSizes
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
                onPlayArtist = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun ArtistListEmptyPreview() {
    PreviewTheme {
        Surface {
            ArtistList(artists = emptyList(), onArtistClick = {}, onPlayArtist = {})
        }
    }
}
