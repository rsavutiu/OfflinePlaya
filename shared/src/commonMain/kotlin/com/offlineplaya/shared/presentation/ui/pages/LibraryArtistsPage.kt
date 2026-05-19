package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.organisms.ArtistList
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun LibraryArtistsPage(
    artists: List<Artist>,
    onArtistClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = "Artists", onBack = onBack) },
    ) { padding ->
        ArtistList(
            artists = artists,
            onArtistClick = onArtistClick,
            modifier = Modifier.padding(padding),
        )
    }
}

@Preview
@Composable
private fun LibraryArtistsPagePopulatedPreview() {
    PreviewTheme {
        LibraryArtistsPage(
            artists = listOf(
                Artist(1, "Aphex Twin", albumCount = 6, trackCount = 92),
                Artist(2, "Pearl Jam", albumCount = 11, trackCount = 142),
            ),
            onArtistClick = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun LibraryArtistsPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        LibraryArtistsPage(
            artists = emptyList(),
            onArtistClick = {},
            onBack = {},
        )
    }
}
