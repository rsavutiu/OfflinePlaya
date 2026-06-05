package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.TrackRow
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.empty_tracks_subtitle
import offlineplaya.shared.generated.resources.empty_tracks_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun TrackList(
    tracks: PersistentList<Track>,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onTrackLongPress: ((Track) -> Unit)? = null,
) {
    if (tracks.isEmpty()) {
        EmptyState(
            title = stringResource(Res.string.empty_tracks_title),
            subtitle = stringResource(Res.string.empty_tracks_subtitle),
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(items = tracks, key = { it.id }) { track ->
            TrackRow(
                track = track,
                onClick = { onTrackClick(track) },
                onLongClick = onTrackLongPress?.let { { it(track) } },
                // Both callers (all-tracks, search) list each track once, so
                // the per-track shared-element key can't collide here.
                sharedArtEnabled = true,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun TrackListPopulatedPreview() {
    PreviewTheme {
        Surface {
            TrackList(
                tracks = persistentListOf(
                    sampleTrack(id = 1, n = 1, title = "Once", ms = 224_000L),
                    sampleTrack(id = 2, n = 2, title = "Even Flow", ms = 296_000L),
                    sampleTrack(id = 3, n = 3, title = "Alive", ms = 341_000L),
                ),
                onTrackClick = {},
            )
        }
    }
}

private fun sampleTrack(id: Long, n: Int, title: String, ms: Long) = Track(
    id = id,
    documentUri = "preview://$id",
    treeUri = "preview://tree",
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
    durationMs = ms,
    bitrate = 1_000_000,
    sampleRate = 44_100,
    channels = 2,
    codec = "flac",
    artistId = 1L,
    albumId = 1L,
    folderId = 1L,
    scanStatus = ScanStatus.SCANNED,
)
