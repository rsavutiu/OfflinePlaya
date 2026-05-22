package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Album
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.TrackRow
import com.offlineplaya.shared.presentation.ui.molecules.formatDuration
import com.offlineplaya.shared.presentation.ui.organisms.TrackDetailsSheet
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

@Composable
fun LibraryAlbumDetailPage(
    album: Album?,
    artistName: String?,
    tracks: List<Track>,
    representativeTrack: Track?,
    availablePlaylists: List<Playlist>,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track, Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = { AppTopBar(title = album?.name ?: "Loading…", onBack = onBack) },
        floatingActionButton = {
            if (tracks.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onPlayTracks(tracks, 0) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text("Play All") },
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item(key = "header") {
                AlbumHeader(
                    album = album,
                    artistName = artistName,
                    representativeTrack = representativeTrack,
                    trackCount = tracks.size,
                    totalDurationMs = tracks.sumOf { it.durationMs ?: 0L },
                )
            }

            itemsIndexed(items = tracks, key = { _, t -> t.id }) { _, track ->
                TrackRow(
                    track = track,
                    onClick = { selectedTrack = track },
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    selectedTrack?.let { track ->
        TrackDetailsSheet(
            track = track,
            availablePlaylists = availablePlaylists,
            onPlay = {
                val index = tracks.indexOf(track).coerceAtLeast(0)
                onPlayTracks(tracks, index)
                selectedTrack = null
            },
            onPlayNext = {
                onPlayNext(track)
                selectedTrack = null
            },
            onAddToQueue = {
                onAddToQueue(track)
                selectedTrack = null
            },
            onAddToPlaylist = { playlistId ->
                onAddToPlaylist(track, playlistId)
                selectedTrack = null
            },
            onDismiss = { selectedTrack = null },
        )
    }
}

@Composable
private fun AlbumHeader(
    album: Album?,
    artistName: String?,
    representativeTrack: Track?,
    trackCount: Int,
    totalDurationMs: Long,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1f),
        ) {
            AlbumArtThumb(
                track = representativeTrack,
                size = 999.dp,
                cornerRadius = 16.dp,
                glyphStyle = MaterialTheme.typography.displayLarge,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(AppSpacing.lg))

        Text(
            text = album?.name ?: "",
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (artistName != null) {
            Text(
                text = artistName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(AppSpacing.sm))
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            val yearText = album?.year?.toString()
            val countText = "$trackCount ${if (trackCount == 1) "track" else "tracks"}"
            val durationText = totalDurationMs.formatAlbumDuration()
            Text(
                text = listOfNotNull(yearText, countText, durationText).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(AppSpacing.md))
    }
}

private fun Long.formatAlbumDuration(): String {
    val totalMinutes = this / 60_000
    return if (totalMinutes >= 60) {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        "${h}h ${m}m"
    } else {
        "${totalMinutes}m"
    }
}

@Preview
@Composable
private fun LibraryAlbumDetailPagePreview() {
    PreviewTheme {
        LibraryAlbumDetailPage(
            album = Album(id = 1, name = "Ten", artistId = 1, year = 1991, trackCount = 3, durationMs = 672_000),
            artistName = "Pearl Jam",
            representativeTrack = null,
            tracks = listOf(
                sampleTrack(1, 1, "Once"),
                sampleTrack(2, 2, "Even Flow"),
                sampleTrack(3, 3, "Alive"),
            ),
            availablePlaylists = emptyList(),
            onPlayTracks = { _, _ -> },
            onPlayNext = {}, onAddToQueue = {}, onAddToPlaylist = { _, _ -> },
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun LibraryAlbumDetailPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        LibraryAlbumDetailPage(
            album = Album(id = 1, name = "Empty Album", artistId = 1, year = null, trackCount = 0, durationMs = 0),
            artistName = "Unknown",
            representativeTrack = null,
            tracks = emptyList(),
            availablePlaylists = emptyList(),
            onPlayTracks = { _, _ -> },
            onPlayNext = {}, onAddToQueue = {}, onAddToPlaylist = { _, _ -> },
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
    scanStatus = com.offlineplaya.shared.domain.model.ScanStatus.SCANNED,
)
