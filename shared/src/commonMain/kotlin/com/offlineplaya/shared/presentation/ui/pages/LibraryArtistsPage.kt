package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.molecules.LibraryTab
import com.offlineplaya.shared.presentation.ui.organisms.ArtistList
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.LibraryScaffold
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun LibraryArtistsPage(
    artists: PersistentList<Artist>,
    onArtistClick: (Long) -> Unit,
    onPlayArtist: (Artist) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LibraryScaffold(
        selectedTab = LibraryTab.ARTISTS,
        onTabSelected = onTabSelected,
        onBack = onBack,
        modifier = modifier.testTag(TestTags.Artists.ROOT),
    ) {
        ArtistList(
            artists = artists,
            onArtistClick = onArtistClick,
            onPlayArtist = onPlayArtist,
        )
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryArtistsPagePopulatedPreview() {
    PreviewTheme {
        LibraryArtistsPage(
            artists = persistentListOf(
                Artist(1, "Aphex Twin", albumCount = 6, trackCount = 92),
                Artist(2, "Pearl Jam", albumCount = 11, trackCount = 142),
            ),
            onArtistClick = {},
            onPlayArtist = {},
            onTabSelected = {},
            onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryArtistsPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        LibraryArtistsPage(
            artists = persistentListOf(),
            onArtistClick = {},
            onPlayArtist = {},
            onTabSelected = {},
            onBack = {},
        )
    }
}
