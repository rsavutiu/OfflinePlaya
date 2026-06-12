package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.SmartPlaylistKind
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.titleRes
import com.offlineplaya.shared.presentation.ui.organisms.TrackList
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

/**
 * A smart playlist's live track list. Tapping a track plays the whole list
 * from that position — the playlist *is* the queue, same as user playlists.
 */
@Composable
fun SmartPlaylistPage(
    kind: SmartPlaylistKind,
    tracks: PersistentList<Track>,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(title = stringResource(kind.titleRes()), onBack = onBack)
        },
    ) { padding ->
        ResponsiveContent(modifier = Modifier.padding(padding)) {
            TrackList(
                tracks = tracks,
                onTrackClick = { onPlayTracks(tracks, tracks.indexOf(it).coerceAtLeast(0)) },
                onTrackLongPress = onTrackLongPress,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SmartPlaylistPagePreview() {
    PreviewTheme {
        SmartPlaylistPage(
            kind = SmartPlaylistKind.MOST_PLAYED,
            tracks = persistentListOf(
                Track(
                    id = 1, documentUri = "u", treeUri = "t", relativePath = "Queen/x.mp3",
                    fileName = "x.mp3", title = "Bohemian Rhapsody", artistName = "Queen",
                    albumArtistName = "Queen", albumName = "A Night at the Opera",
                    genre = "Rock", year = 1975, trackNumber = 11, discNumber = 1,
                    durationMs = 354_000L, bitrate = 320_000, sampleRate = 44_100,
                    channels = 2, codec = "mp3", artistId = 1L, albumId = 1L,
                    folderId = 1L, scanStatus = ScanStatus.SCANNED,
                ),
            ),
            onPlayTracks = { _, _ -> },
            onTrackLongPress = {},
            onBack = {},
        )
    }
}
