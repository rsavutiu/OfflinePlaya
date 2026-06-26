package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.organisms.QueueList
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.cd_clear_queue
import offlineplaya.shared.generated.resources.queue_title
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen queue browser. Highlights the current track and lets the user
 * jump to any item or remove individual tracks. A "clear all" action in the
 * top app bar wipes the queue.
 */
@Composable
fun QueuePage(
    state: PlaybackState,
    onJumpTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onMove: ((from: Int, to: Int) -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier.testTag(TestTags.Queue.ROOT),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.queue_title),
                onBack = onBack,
                actions = {
                    if (state.queue.isNotEmpty()) {
                        IconButton(onClick = onClearQueue) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = stringResource(Res.string.cd_clear_queue),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        ResponsiveContent {
            QueueList(
                tracks = state.queue.toPersistentList(),
                currentIndex = state.queueIndex,
                onJumpTo = onJumpTo,
                onRemove = onRemove,
                contentPadding = padding,
                onMove = onMove,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun QueuePagePopulatedPreview() {
    PreviewTheme {
        QueuePage(
            state = PlaybackState(
                currentTrack = sampleQueueTrack(2, "Even Flow", "Pearl Jam"),
                isPlaying = true,
                positionMs = 0L,
                durationMs = 296_000L,
                shuffleEnabled = false,
                repeatMode = RepeatMode.OFF,
                queue = persistentListOf(
                    sampleQueueTrack(1, "Once", "Pearl Jam"),
                    sampleQueueTrack(2, "Even Flow", "Pearl Jam"),
                    sampleQueueTrack(3, "Alive", "Pearl Jam"),
                ),
                queueIndex = 1,
                volume = 1f,
            ),
            onJumpTo = {}, onRemove = {}, onClearQueue = {}, onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun QueuePageEmptyPreview() {
    PreviewTheme(darkTheme = true) {
        QueuePage(
            state = PlaybackState.Empty,
            onJumpTo = {}, onRemove = {}, onClearQueue = {}, onBack = {},
        )
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
