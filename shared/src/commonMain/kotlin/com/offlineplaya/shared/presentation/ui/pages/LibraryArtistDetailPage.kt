package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.atoms.ArtistAvatar
import com.offlineplaya.shared.presentation.ui.organisms.AlbumList
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun LibraryArtistDetailPage(
    artist: Artist?,
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
    onPlayAlbum: (Album) -> Unit,
    onPlayArtist: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    representativeTrackProvider: suspend (Long) -> Track? = { null },
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = { AppTopBar(title = artist?.name ?: "Loading…", onBack = onBack) },
        floatingActionButton = {
            if (albums.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onPlayArtist,
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text("Shuffle Artist") },
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ArtistHeader(artist)
            AlbumList(
                albums = albums,
                onAlbumClick = onAlbumClick,
                onPlayAlbum = onPlayAlbum,
                representativeTrackProvider = representativeTrackProvider,
            )
        }
    }
}

@Composable
private fun ArtistHeader(artist: Artist?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val model: Any? = artist?.imageUrl ?: artist
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ArtistAvatar(artist = null, size = 120.dp)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ArtistAvatar(artist = artist, size = 120.dp)
                    }
                },
                success = { SubcomposeAsyncImageContent() },
            )
        }
        if (artist != null) {
            Spacer(Modifier.height(AppSpacing.sm))
            val albumPart = "${artist.albumCount} ${if (artist.albumCount == 1) "album" else "albums"}"
            val trackPart = "${artist.trackCount} ${if (artist.trackCount == 1) "track" else "tracks"}"
            Text(
                text = "$albumPart · $trackPart",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = AppSpacing.sm),
            )
        }
    }
}

@Preview
@Composable
private fun LibraryArtistDetailPagePreview() {
    PreviewTheme {
        LibraryArtistDetailPage(
            artist = Artist(1, "Pearl Jam", 11, 142),
            albums = listOf(
                Album(1, "Ten", artistId = 1, year = 1991, trackCount = 11, durationMs = 0),
                Album(2, "Vs.", artistId = 1, year = 1993, trackCount = 12, durationMs = 0),
            ),
            onAlbumClick = {},
            onPlayAlbum = {},
            onPlayArtist = {},
            onBack = {},
        )
    }
}
