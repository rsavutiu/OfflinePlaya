package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Artist
import com.offlineplaya.shared.domain.model.PlayedTrack
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.atoms.SectionLabel
import com.offlineplaya.shared.presentation.ui.molecules.TopTrackRow
import com.offlineplaya.shared.presentation.ui.organisms.AlbumList
import com.offlineplaya.shared.presentation.ui.organisms.ArtistDetailHeader
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.artist_top_tracks
import offlineplaya.shared.generated.resources.common_loading
import offlineplaya.shared.generated.resources.home_label_albums
import offlineplaya.shared.generated.resources.library_shuffle_artist
import org.jetbrains.compose.resources.stringResource

/**
 * Artist detail: adaptive header + a "Top tracks" section (this artist's
 * most-played, fed by PlayHistory — absent until something was played) +
 * grid of the artist's albums. A "Shuffle Artist" FAB sits at the bottom
 * when there's at least one album to play.
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
    topTracks: PersistentList<PlayedTrack> = persistentListOf(),
    onPlayTopTrack: (Int) -> Unit = {},
    onTopTrackLongPress: (Track) -> Unit = {},
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
                val brand = LocalBrandAccent.current
                ExtendedFloatingActionButton(
                    onClick = onPlayArtist,
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text(stringResource(Res.string.library_shuffle_artist)) },
                    containerColor = brand.accent,
                    contentColor = brand.onAccent,
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
                    leadingContent = if (topTracks.isEmpty()) null else {
                        {
                            item(
                                key = "top-tracks-label",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                SectionLabel(
                                    text = stringResource(Res.string.artist_top_tracks).uppercase(),
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            topTracks.forEachIndexed { index, played ->
                                item(
                                    key = "top-track-${played.track.id}",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
                                    TopTrackRow(
                                        rank = index + 1,
                                        playedTrack = played,
                                        onClick = { onPlayTopTrack(index) },
                                        onLongClick = { onTopTrackLongPress(played.track) },
                                    )
                                }
                            }
                            item(
                                key = "top-tracks-albums-label",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                SectionLabel(
                                    text = stringResource(Res.string.home_label_albums).uppercase(),
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    },
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
            topTracks = persistentListOf(
                PlayedTrack(previewArtistTrack("Alive"), 42),
                PlayedTrack(previewArtistTrack("Black"), 17),
            ),
            onAlbumClick = {}, onPlayAlbum = {}, onPlayArtist = {}, onBack = {},
        )
    }
}

private fun previewArtistTrack(title: String) = Track(
    id = title.hashCode().toLong(),
    documentUri = "preview/$title",
    treeUri = "preview",
    relativePath = "x/$title",
    fileName = "$title.mp3",
    title = title,
    artistName = "Pearl Jam",
    albumArtistName = null,
    albumName = "Ten",
    genre = null,
    year = 1991,
    trackNumber = null,
    discNumber = null,
    durationMs = 224_000L,
    bitrate = null,
    sampleRate = null,
    channels = null,
    codec = null,
    artistId = 1L,
    albumId = 1L,
    folderId = 1L,
    scanStatus = com.offlineplaya.shared.domain.model.ScanStatus.SCANNED,
)
