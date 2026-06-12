package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
import com.offlineplaya.shared.presentation.ui.molecules.EmptyState
import com.offlineplaya.shared.presentation.ui.molecules.formatDuration
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.cd_now_playing
import offlineplaya.shared.generated.resources.cd_remove_from_queue
import offlineplaya.shared.generated.resources.cd_reorder_queue
import offlineplaya.shared.generated.resources.empty_queue_subtitle
import offlineplaya.shared.generated.resources.empty_queue_title
import org.jetbrains.compose.resources.stringResource

/**
 * Vertical list of queued tracks. The track at [currentIndex] is highlighted.
 * Each row has a close button that calls [onRemove] with the row index.
 * Tapping a row jumps to that track via [onJumpTo]. Dragging a row's handle
 * reorders it; [onMove] is committed once, on drop, with (from, to) indices.
 */
@Composable
fun QueueList(
    tracks: PersistentList<Track>,
    currentIndex: Int,
    onJumpTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onMove: ((from: Int, to: Int) -> Unit)? = null,
) {
    if (tracks.isEmpty()) {
        EmptyState(
            title = stringResource(Res.string.empty_queue_title),
            subtitle = stringResource(Res.string.empty_queue_subtitle),
            modifier = modifier,
        )
        return
    }
    // Local working copy so the drag reorders visually without waiting on
    // the player round-trip; the move is committed once at drag end and the
    // authoritative queue then flows back in (resetting this copy). Each
    // entry gets a uid so LazyColumn keys stay stable across local swaps —
    // raw track ids can repeat in a queue, and index-based keys would kill
    // the in-flight drag gesture on the first swap.
    val localEntries = remember(tracks) {
        tracks.mapIndexed { i, t -> QueueEntry(uid = i, track = t) }.toMutableStateList()
    }
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragStartIndex by remember { mutableStateOf(-1) }
    var dragDelta by remember { mutableStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        itemsIndexed(items = localEntries, key = { _, entry -> entry.uid }) { idx, entry ->
            val track = entry.track
            val isDragging = idx == draggingIndex
            // pointerInput never restarts (key = uid), so the gesture lambdas
            // must read the row's *current* index through state, not capture.
            val liveIdx by rememberUpdatedState(idx)
            QueueRow(
                track = track,
                isCurrent = idx == currentIndex,
                onClick = { onJumpTo(idx) },
                onRemove = { onRemove(idx) },
                modifier = Modifier
                    .animateItem()
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragging) dragDelta else 0f },
                dragHandle = if (onMove == null) null else {
                    {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = stringResource(Res.string.cd_reorder_queue),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.pointerInput(entry.uid) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingIndex = liveIdx
                                        dragStartIndex = liveIdx
                                        dragDelta = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragDelta += amount.y
                                        // Swap with the neighbor whose band the
                                        // dragged row's center has entered. Uses
                                        // draggingIndex (live state), not the
                                        // captured idx — the lambda may be stale
                                        // after intermediate swaps.
                                        val items = listState.layoutInfo.visibleItemsInfo
                                        val dragging = items.find { it.index == draggingIndex }
                                            ?: return@detectDragGestures
                                        val centerY = dragging.offset + dragDelta + dragging.size / 2f
                                        val target = items.find { item ->
                                            item.index != draggingIndex &&
                                                centerY >= item.offset &&
                                                centerY < item.offset + item.size
                                        } ?: return@detectDragGestures
                                        localEntries.add(
                                            target.index,
                                            localEntries.removeAt(draggingIndex),
                                        )
                                        // Keep the row visually still: compensate
                                        // the delta for the layout slot it jumped.
                                        dragDelta += dragging.offset - target.offset
                                        draggingIndex = target.index
                                    },
                                    onDragEnd = {
                                        if (dragStartIndex >= 0 && draggingIndex >= 0 &&
                                            dragStartIndex != draggingIndex
                                        ) {
                                            onMove(dragStartIndex, draggingIndex)
                                        }
                                        draggingIndex = -1
                                        dragStartIndex = -1
                                        dragDelta = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = -1
                                        dragStartIndex = -1
                                        dragDelta = 0f
                                    },
                                )
                            },
                        )
                    }
                },
            )
        }
    }
}

/** One queue slot: a stable [uid] (snapshot position) + the track in it. */
private class QueueEntry(val uid: Int, val track: Track)

@Composable
private fun QueueRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    // The "currently playing" highlight uses surfaceVariant (subtle tint)
    // plus a leading equalizer-glyph instead of a full primaryContainer fill.
    // The old fill overpowered the queue — only the playing row was visible
    // and every other entry felt like a backdrop. M3's emphasis pattern is
    // "marker + slight tint", not "flood the row with brand color".
    val rowBackground = if (isCurrent) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(44.dp)) {
            AlbumArtThumb(track = track, size = 44.dp, cornerRadius = 8.dp)
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = stringResource(Res.string.cd_now_playing),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .padding(start = AppSpacing.lg)
                .weight(1f),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.artistName} · ${track.durationMs.formatDuration()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        dragHandle?.invoke()
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.cd_remove_from_queue),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun QueueListPopulatedPreview() {
    PreviewTheme {
        Surface {
            QueueList(
                tracks = persistentListOf(
                    sampleQueueTrack(1, "Once", "Pearl Jam"),
                    sampleQueueTrack(2, "Even Flow", "Pearl Jam"),
                    sampleQueueTrack(3, "Alive", "Pearl Jam"),
                ),
                currentIndex = 1,
                onJumpTo = {},
                onRemove = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun QueueListEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            QueueList(tracks = persistentListOf(), currentIndex = -1, onJumpTo = {}, onRemove = {})
        }
    }
}

private fun sampleQueueTrack(id: Long, title: String, artist: String) = Track(
    id = id,
    documentUri = "u/$id",
    treeUri = "t",
    relativePath = "$artist/Ten/$title.flac",
    fileName = "$title.flac",
    title = title,
    artistName = artist,
    albumArtistName = null,
    albumName = "Ten",
    genre = "Rock",
    year = 1991,
    trackNumber = id.toInt(),
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
