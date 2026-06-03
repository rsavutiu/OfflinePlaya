package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Playlist
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.molecules.AddToPlaylistDialog
import com.offlineplaya.shared.presentation.ui.molecules.CreatePlaylistDialog
import com.offlineplaya.shared.presentation.ui.molecules.formatDuration
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.track_details_add_to_playlist
import offlineplaya.shared.generated.resources.track_details_bitrate
import offlineplaya.shared.generated.resources.track_details_codec
import offlineplaya.shared.generated.resources.track_details_duration
import offlineplaya.shared.generated.resources.track_details_genre
import offlineplaya.shared.generated.resources.track_details_path
import offlineplaya.shared.generated.resources.track_details_play_next
import offlineplaya.shared.generated.resources.track_details_play_now
import offlineplaya.shared.generated.resources.track_details_playing_in_plurals
import offlineplaya.shared.generated.resources.track_details_queue
import offlineplaya.shared.generated.resources.track_details_status
import offlineplaya.shared.generated.resources.track_details_year
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom sheet showing track metadata + actions: Play next, Add to queue,
 * Add to playlist. Reached by long-pressing a track row (a plain tap plays
 * the track in its list context, so [onPlay] is optional and normally null —
 * when null the in-sheet play affordance is hidden entirely).
 *
 * "Add to playlist" opens [AddToPlaylistDialog], populated by
 * [availablePlaylists]: picking one calls [onAddToExistingPlaylist]; choosing
 * "+ New playlist" prompts for a name and calls [onCreatePlaylistAndAdd].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailsSheet(
    track: Track,
    availablePlaylists: PersistentList<Playlist>,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToExistingPlaylist: (playlistId: Long) -> Unit,
    onCreatePlaylistAndAdd: (name: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onPlay: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

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
                onAddToExistingPlaylist(id)
                showAddToPlaylistDialog = false
                onDismiss()
            },
            onCreateNew = {
                showAddToPlaylistDialog = false
                showCreatePlaylistDialog = true
            },
            onDismiss = { showAddToPlaylistDialog = false },
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onCreate = { name ->
                onCreatePlaylistAndAdd(name)
                showCreatePlaylistDialog = false
                onDismiss()
            },
            onDismiss = { showCreatePlaylistDialog = false },
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
            AutoPlayButton(onPlay = onPlay)
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
                    ) { Text(stringResource(Res.string.track_details_play_next)) }
                }
                if (onAddToQueue != null) {
                    OutlinedButton(
                        onClick = onAddToQueue,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(Res.string.track_details_queue)) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (onShowAddToPlaylist != null) {
            OutlinedButton(
                onClick = onShowAddToPlaylist,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.track_details_add_to_playlist)) }
            Spacer(modifier = Modifier.height(20.dp))
        }

        DetailRow(
            stringResource(Res.string.track_details_duration),
            track.durationMs.formatDuration()
        )
        track.year?.let { DetailRow(stringResource(Res.string.track_details_year), it.toString()) }
        track.genre?.let { DetailRow(stringResource(Res.string.track_details_genre), it) }
        track.codec?.let { DetailRow(stringResource(Res.string.track_details_codec), it) }
        track.bitrate?.let {
            DetailRow(
                stringResource(Res.string.track_details_bitrate),
                "${it / 1000} kbps"
            )
        }
        DetailRow(stringResource(Res.string.track_details_path), track.relativePath)
        DetailRow(
            stringResource(Res.string.track_details_status),
            track.scanStatus.name.lowercase().replaceFirstChar { it.titlecase() })
    }
}

/**
 * Play button that begins a ~2s countdown the moment it appears. A filled
 * progress band drains across the surface; when it reaches the end, [onPlay]
 * fires. Tapping the button at any point cancels the countdown — pressing
 * again restarts it.
 */
@Composable
private fun AutoPlayButton(
    onPlay: () -> Unit,
    countdownMillis: Int = 2_000,
) {
    val progress = remember { Animatable(0f) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        progress.snapTo(0f)
        val result = progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = countdownMillis, easing = LinearEasing),
        )
        // Cancellation throws; reaching here means the timer expired cleanly.
        if (result.endReason == androidx.compose.animation.core.AnimationEndReason.Finished) {
            onPlay()
        }
    }

    val containerShape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(containerShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable {
                if (running) {
                    // Tap during countdown → cancel auto-play; button reverts
                    // to a normal Play affordance the user can press at will.
                    running = false
                } else {
                    onPlay()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Filled progress band drains left→right while the timer runs.
        if (running) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.value)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.CenterStart),
            )
        }
        val secondsLeft = if (running) {
            val remainingMs = (countdownMillis * (1f - progress.value)).toLong()
            (remainingMs / 1000) + 1
        } else 0L

        Text(
            text = if (running) {
                pluralStringResource(
                    Res.plurals.track_details_playing_in_plurals,
                    secondsLeft.toInt(),
                    secondsLeft.toInt()
                )
            } else {
                stringResource(Res.string.track_details_play_now)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
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

@PreviewScreenSizes
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

@PreviewScreenSizes
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
