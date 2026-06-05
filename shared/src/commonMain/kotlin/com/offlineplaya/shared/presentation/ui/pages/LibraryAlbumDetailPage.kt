package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.TrackRow
import com.offlineplaya.shared.presentation.ui.organisms.AlbumDetailHeader
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * Album detail page: gradient header (art + metadata + Play/Shuffle) followed
 * by the track listing. Tapping a row plays the album from that track;
 * long-pressing raises the global track-actions sheet via [onTrackLongPress].
 */
@Composable
fun LibraryAlbumDetailPage(
    album: Album?,
    artistName: String?,
    tracks: PersistentList<Track>,
    representativeTrack: Track?,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { AppTopBar(title = album?.name ?: "Loading…", onBack = onBack) },
    ) { padding ->
        ResponsiveContent(modifier = Modifier.padding(padding)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item(key = "header") {
                    AlbumDetailHeader(
                        album = album,
                        artistName = artistName,
                        representativeTrack = representativeTrack,
                        trackCount = tracks.size,
                        totalDurationMs = tracks.sumOf { it.durationMs ?: 0L },
                        canPlay = tracks.isNotEmpty(),
                        onPlay = { onPlayTracks(tracks, 0) },
                        onShuffle = {
                            val shuffled = tracks.shuffled()
                            if (shuffled.isNotEmpty()) onPlayTracks(shuffled, 0)
                        },
                    )
                }
                itemsIndexed(items = tracks, key = { _, t -> t.id }) { index, track ->
                    TrackRow(
                        track = track,
                        onClick = { onPlayTracks(tracks, index) },
                        onLongClick = { onTrackLongPress(track) },
                        sharedArtEnabled = true,
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryAlbumDetailPagePreview() {
    PreviewTheme {
        LibraryAlbumDetailPage(
            album = Album(1, "Ten", 1, 1991, 3, 672_000),
            artistName = "Pearl Jam",
            representativeTrack = null,
            tracks = persistentListOf(
                sampleTrack(1, 1, "Once"),
                sampleTrack(2, 2, "Even Flow"),
                sampleTrack(3, 3, "Alive"),
            ),
            onPlayTracks = { _, _ -> },
            onTrackLongPress = {},
            onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryAlbumDetailPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        LibraryAlbumDetailPage(
            album = Album(1, "Empty Album", 1, null, 0, 0),
            artistName = "Unknown",
            representativeTrack = null,
            tracks = persistentListOf(),
            onPlayTracks = { _, _ -> },
            onTrackLongPress = {},
            onBack = {},
        )
    }
}

private fun sampleTrack(id: Long, n: Int, title: String) = Track(
    id = id,
    documentUri = "u/$id",
    treeUri = "tree",
    relativePath = "Pearl Jam/Ten/$title.flac",
    fileName = "$title.flac",
    title = title,
    artistName = "Pearl Jam",
    albumArtistName = null,
    albumName = "Ten",
    genre = "Rock",
    year = 1991,
    trackNumber = n,
    discNumber = 1,
    durationMs = 224_000L,
    bitrate = 1_000_000,
    sampleRate = 44_100,
    channels = 2,
    codec = "flac",
    artistId = 1L,
    albumId = 1L,
    folderId = 1L,
    scanStatus = ScanStatus.SCANNED,
)
