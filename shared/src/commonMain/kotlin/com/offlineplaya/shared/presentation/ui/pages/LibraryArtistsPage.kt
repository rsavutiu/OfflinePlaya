package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.presentation.ui.molecules.LibraryTab
import com.offlineplaya.shared.presentation.ui.organisms.ArtistList
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.templates.LibraryScaffold
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun LibraryArtistsPage(
    artists: List<Artist>,
    onArtistClick: (Long) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LibraryScaffold(
        selectedTab = LibraryTab.ARTISTS,
        onTabSelected = onTabSelected,
        onBack = onBack,
        modifier = modifier,
    ) {
        ArtistList(artists = artists, onArtistClick = onArtistClick)
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
            onTabSelected = {},
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
            onTabSelected = {},
            onBack = {},
        )
    }
}
