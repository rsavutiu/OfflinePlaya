package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.molecules.LibraryTab
import com.offlineplaya.shared.presentation.ui.organisms.TrackList
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.LibraryScaffold
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun LibraryFlatPage(
    tracks: PersistentList<Track>,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onTrackLongPress: (Track) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LibraryScaffold(
        selectedTab = LibraryTab.FLAT,
        onTabSelected = onTabSelected,
        onBack = onBack,
        modifier = modifier.testTag(TestTags.Flat.ROOT),
    ) {
        TrackList(
            tracks = tracks,
            onTrackClick = { onPlayTracks(tracks, tracks.indexOf(it).coerceAtLeast(0)) },
            onTrackLongPress = onTrackLongPress,
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
            onPlayTracks = { _, _ -> },
            onTrackLongPress = {},
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
            onPlayTracks = { _, _ -> },
            onTrackLongPress = {},
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
