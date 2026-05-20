package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.organisms.AlbumList
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun LibraryArtistDetailPage(
    artistName: String,
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = { AppTopBar(title = artistName, onBack = onBack) },
    ) { padding ->
        AlbumList(
            albums = albums,
            onAlbumClick = onAlbumClick,
            contentPadding = padding,
        )
    }
}

@Preview
@Composable
private fun LibraryArtistDetailPagePreview() {
    PreviewTheme {
        LibraryArtistDetailPage(
            artistName = "Pearl Jam",
            albums = listOf(
                Album(1, "Ten", artistId = 1, year = 1991, trackCount = 11, durationMs = 0),
                Album(2, "Vs.", artistId = 1, year = 1993, trackCount = 12, durationMs = 0),
            ),
            onAlbumClick = {},
            onBack = {},
        )
    }
}
