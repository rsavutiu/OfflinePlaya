package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.FolderRow
import com.offlineplaya.shared.presentation.ui.molecules.TrackRow
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.empty_folder_subtitle
import offlineplaya.shared.generated.resources.empty_folder_title
import offlineplaya.shared.generated.resources.folder_section_folders
import offlineplaya.shared.generated.resources.folder_section_tracks
import org.jetbrains.compose.resources.stringResource

/**
 * Folder detail body: section of subfolders, then a section of tracks directly
 * inside this folder. Either or both sections may be empty.
 */
@Composable
fun FolderDetailContent(
    subfolders: PersistentList<Folder>,
    tracks: PersistentList<Track>,
    onFolderClick: (Long) -> Unit,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
    onTrackLongPress: ((Track) -> Unit)? = null,
    previewTracksProvider: ((Long) -> Flow<List<Track>>)? = null,
    onFolderLongPress: ((Folder) -> Unit)? = null,
    onFolderPlay: ((Folder) -> Unit)? = null,
) {
    if (subfolders.isEmpty() && tracks.isEmpty()) {
        EmptyState(
            title = stringResource(Res.string.empty_folder_title),
            subtitle = stringResource(Res.string.empty_folder_subtitle),
            modifier = modifier,
        )
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (subfolders.isNotEmpty()) {
            item { SectionHeader(stringResource(Res.string.folder_section_folders)) }
            items(items = subfolders, key = { "f-${it.id}" }) { folder ->
                val previewTracks: PersistentList<Track> = if (previewTracksProvider != null) {
                    val state by produceState(initialValue = persistentListOf<Track>(), folder.id) {
                        previewTracksProvider(folder.id).collectLatest {
                            value = it.toPersistentList()
                        }
                    }
                    state
                } else {
                    persistentListOf()
                }
                FolderRow(
                    folder = folder,
                    onClick = { onFolderClick(folder.id) },
                    onLongClick = onFolderLongPress?.let { handler -> { handler(folder) } },
                    onPlay = onFolderPlay?.let { handler -> { handler(folder) } },
                    previewTracks = previewTracks,
                )
            }
        }
        if (tracks.isNotEmpty()) {
            item { SectionHeader(stringResource(Res.string.folder_section_tracks)) }
            items(items = tracks, key = { "t-${it.id}" }) { track ->
                TrackRow(
                    track = track,
                    onClick = { onTrackClick(track) },
                    onLongClick = onTrackLongPress?.let { { it(track) } },
                    // Folder listings show each file once → safe shared key.
                    sharedArtEnabled = true,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@PreviewScreenSizes
@Composable
private fun FolderDetailContentMixedPreview() {
    PreviewTheme {
        Surface {
            FolderDetailContent(
                subfolders = persistentListOf(
                    Folder(10, "t", "Pearl Jam/Live", "Live", 1L, 24),
                ),
                tracks = persistentListOf(
                    sampleTrack(1, 1, "Once"),
                    sampleTrack(2, 2, "Even Flow"),
                ),
                onFolderClick = {},
                onTrackClick = {},
            )
        }
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
