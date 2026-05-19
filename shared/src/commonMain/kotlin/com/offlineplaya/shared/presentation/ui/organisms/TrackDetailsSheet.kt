package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.molecules.AddToPlaylistDialog
import com.offlineplaya.shared.presentation.ui.molecules.formatDuration
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Bottom sheet showing track metadata + actions: Play, Play next,
 * Add to queue, Add to playlist. The caller decides what "Play" means
 * (typically: set the parent list as the queue starting at this track).
 * The "Add to playlist" action shows a dialog populated by
 * [availablePlaylists]; picking one calls [onAddToPlaylist] and dismisses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailsSheet(
    track: Track,
    availablePlaylists: List<Playlist>,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (playlistId: Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        TrackDetailsContent(
            track = track,
            onPlay = onPlay,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onShowAddToPlaylist = { showAddToPlaylistDialog = true },
        )
    }

    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = availablePlaylists,
            onPickPlaylist = { id ->
                onAddToPlaylist(id)
                showAddToPlaylistDialog = false
            },
            onDismiss = { showAddToPlaylistDialog = false },
        )
    }
}

/** Public content composable so the same body can be used in previews without the modal. */
@Composable
fun TrackDetailsContent(
    track: Track,
    modifier: Modifier = Modifier,
    onPlay: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onShowAddToPlaylist: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = track.title,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "${track.artistName} — ${track.albumName}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        if (onPlay != null) {
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Play")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (onPlayNext != null || onAddToQueue != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onPlayNext != null) {
                    OutlinedButton(
                        onClick = onPlayNext,
                        modifier = Modifier.weight(1f),
                    ) { Text("Play next") }
                }
                if (onAddToQueue != null) {
                    OutlinedButton(
                        onClick = onAddToQueue,
                        modifier = Modifier.weight(1f),
                    ) { Text("Add to queue") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (onShowAddToPlaylist != null) {
            OutlinedButton(
                onClick = onShowAddToPlaylist,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add to playlist") }
            Spacer(modifier = Modifier.height(20.dp))
        }

        DetailRow("Duration", track.durationMs.formatDuration())
        track.year?.let { DetailRow("Year", it.toString()) }
        track.genre?.let { DetailRow("Genre", it) }
        track.codec?.let { DetailRow("Codec", it) }
        track.bitrate?.let { DetailRow("Bitrate", "${it / 1000} kbps") }
        DetailRow("Path", track.relativePath)
        DetailRow("Status", track.scanStatus.name.lowercase().replaceFirstChar { it.titlecase() })
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview
@Composable
private fun TrackDetailsContentFullPreview() {
    PreviewTheme {
        Surface {
            TrackDetailsContent(
                track = sampleTrack(),
                onPlay = {},
                onPlayNext = {},
                onAddToQueue = {},
                onShowAddToPlaylist = {},
            )
        }
    }
}

@Preview
@Composable
private fun TrackDetailsContentMinimalPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            TrackDetailsContent(
                track = Track(
                    id = 1, documentUri = "u", treeUri = "t",
                    relativePath = "untitled.mp3",
                    fileName = "untitled.mp3",
                    title = "untitled.mp3",
                    artistName = "Unknown Artist",
                    albumArtistName = null,
                    albumName = "Unknown Album",
                    genre = null,
                    year = null, trackNumber = null, discNumber = null,
                    durationMs = null,
                    bitrate = null, sampleRate = null, channels = null,
                    codec = null,
                    artistId = null, albumId = null, folderId = null,
                    scanStatus = ScanStatus.PENDING,
                ),
            )
        }
    }
}

private fun sampleTrack() = Track(
    id = 1, documentUri = "u", treeUri = "t",
    relativePath = "Pearl Jam/Ten/01 Once.flac",
    fileName = "01 Once.flac",
    title = "Once",
    artistName = "Pearl Jam",
    albumArtistName = null,
    albumName = "Ten",
    genre = "Rock",
    year = 1991, trackNumber = 1, discNumber = 1,
    durationMs = 224_000L,
    bitrate = 1_000_000, sampleRate = 44_100, channels = 2,
    codec = "flac",
    artistId = 1L, albumId = 1L, folderId = 1L,
    scanStatus = ScanStatus.SCANNED,
)
