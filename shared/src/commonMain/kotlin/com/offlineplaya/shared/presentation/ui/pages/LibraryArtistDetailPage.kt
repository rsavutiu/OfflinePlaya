package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.organisms.AlbumList
import com.offlineplaya.shared.presentation.ui.organisms.ArtistDetailHeader
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.common_loading
import offlineplaya.shared.generated.resources.library_shuffle_artist
import org.jetbrains.compose.resources.stringResource

/**
 * Artist detail: adaptive header + grid of the artist's albums. A "Shuffle
 * Artist" FAB sits at the bottom when there's at least one album to play.
 */
@Composable
fun LibraryArtistDetailPage(
    artist: Artist?,
    albums: PersistentList<Album>,
    onAlbumClick: (Long) -> Unit,
    onPlayAlbum: (Album) -> Unit,
    onPlayArtist: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    representativeTrackProvider: suspend (Long) -> Track? = { null },
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = artist?.name ?: stringResource(Res.string.common_loading),
                onBack = onBack,
            )
        },
        floatingActionButton = {
            if (albums.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onPlayArtist,
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text(stringResource(Res.string.library_shuffle_artist)) },
                )
            }
        },
    ) { padding ->
        ResponsiveContent(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ArtistDetailHeader(artist = artist)
                AlbumList(
                    albums = albums,
                    onAlbumClick = onAlbumClick,
                    onPlayAlbum = onPlayAlbum,
                    representativeTrackProvider = representativeTrackProvider,
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryArtistDetailPagePreview() {
    PreviewTheme {
        LibraryArtistDetailPage(
            artist = Artist(1, "Pearl Jam", 11, 142),
            albums = persistentListOf(
                Album(1, "Ten", artistId = 1, year = 1991, trackCount = 11, durationMs = 0),
                Album(2, "Vs.", artistId = 1, year = 1993, trackCount = 12, durationMs = 0),
            ),
            onAlbumClick = {}, onPlayAlbum = {}, onPlayArtist = {}, onBack = {},
        )
    }
}
