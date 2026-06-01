package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.molecules.LibraryTab
import com.offlineplaya.shared.presentation.ui.organisms.TrackDetailsSheet
import com.offlineplaya.shared.presentation.ui.organisms.TrackList
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.LibraryScaffold
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun LibraryFlatPage(
    tracks: PersistentList<Track>,
    availablePlaylists: PersistentList<Playlist>,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track, Long) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    LibraryScaffold(
        selectedTab = LibraryTab.FLAT,
        onTabSelected = onTabSelected,
        onBack = onBack,
        modifier = modifier,
    ) {
        TrackList(
            tracks = tracks,
            onTrackClick = { selectedTrack = it },
        )
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
            onPlayNext = { onPlayNext(track); selectedTrack = null },
            onAddToQueue = { onAddToQueue(track); selectedTrack = null },
            onAddToPlaylist = { id -> onAddToPlaylist(track, id); selectedTrack = null },
            onDismiss = { selectedTrack = null },
        )
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryFlatPagePreview() {
    PreviewTheme {
        LibraryFlatPage(
            tracks = persistentListOf(
                sampleFlatTrack(1, "Aphex Twin", "Alberto Balsalm"),
                sampleFlatTrack(2, "Boards of Canada", "Roygbiv"),
                sampleFlatTrack(3, "Pearl Jam", "Once"),
            ),
            availablePlaylists = persistentListOf(),
            onPlayTracks = { _, _ -> },
            onPlayNext = {}, onAddToQueue = {}, onAddToPlaylist = { _, _ -> },
            onTabSelected = {},
            onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryFlatPageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        LibraryFlatPage(
            tracks = persistentListOf(),
            availablePlaylists = persistentListOf(),
            onPlayTracks = { _, _ -> },
            onPlayNext = {}, onAddToQueue = {}, onAddToPlaylist = { _, _ -> },
            onTabSelected = {},
            onBack = {},
        )
    }
}

private fun sampleFlatTrack(id: Long, artist: String, title: String) = Track(
    id = id,
    documentUri = "u/$id",
    treeUri = "t",
    relativePath = "$artist/$title.flac",
    fileName = "$title.flac",
    title = title,
    artistName = artist,
    albumArtistName = null,
    albumName = "Sample Album",
    genre = null,
    year = 1995,
    trackNumber = id.toInt(),
    discNumber = 1,
    durationMs = 240_000L,
    bitrate = 1_000_000,
    sampleRate = 44_100,
    channels = 2,
    codec = "flac",
    artistId = id,
    albumId = id,
    folderId = id,
    scanStatus = ScanStatus.SCANNED,
)
