package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.presentation.ui.atoms.AlbumArtThumb
import com.offlineplaya.shared.presentation.ui.atoms.PlayButton
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppShapes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

// [LOCAL-LLM] (model: qwen3:8b-ctx16k) — initial draft generated from the
// ArtistRow template; Claude added the missing `layout.size` import and
// removed an unused `ui.draw.clip` import before committing.

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderRow(
    folder: Folder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    previewTracks: PersistentList<Track> = persistentListOf(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FolderThumb(previewTracks = previewTracks)
        Column(modifier = Modifier
            .padding(start = AppSpacing.lg)
            .weight(1f)) {
            Text(
                text = folder.displayName,
                style = MaterialTheme.typography.titleMedium,
                // 2 lines accommodate the "Depeche Mode - Music for the Masses
                // (Deluxe Edition)"-shaped folder names that come out of
                // tracker downloads. Single-line truncated mid-word in the
                // audit; this lets them wrap once before ellipsis.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary(folder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onPlay != null) {
            PlayButton(onPlay = onPlay)
        }
    }
}

@Composable
private fun FolderThumb(
    previewTracks: PersistentList<Track>,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = AppShapes.tile,
        modifier = modifier.size(48.dp),
    ) {
        if (previewTracks.isEmpty()) {
            // Fall back to the muted folder glyph when no album art is
            // available yet (mid-scan, or genuinely empty folder).
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
        } else {
            FolderArtCollage(previewTracks)
        }
    }
}

/**
 * 2×2 grid of album-art tiles. Renders 1–4 tiles; if fewer than 4 are
 * available, repeats the first to keep the grid filled rather than leaving
 * blank cells — matches the user's "even if it repeats" guidance.
 */
@Composable
private fun FolderArtCollage(tracks: List<Track>) {
    val padded = if (tracks.size >= 4) tracks.take(4) else List(4) { tracks[it % tracks.size] }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                CollageTile(padded[0], Modifier.weight(1f))
                CollageTile(padded[1], Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                CollageTile(padded[2], Modifier.weight(1f))
                CollageTile(padded[3], Modifier.weight(1f))
            }
        }
        // Folder badge in the bottom-right corner keeps the "this is a
        // folder" affordance readable even when the collage fills the tile.
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shape = AppShapes.tile,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
                .size(16.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun CollageTile(track: Track, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // 0.dp corners so the tiles butt against each other; the outer
        // Surface shape clips the whole collage to AppShapes.tile.
        AlbumArtThumb(
            track = track,
            modifier = Modifier.fillMaxSize(),
            size = null,
            cornerRadius = 0.dp,
        )
    }
}

private fun summary(folder: Folder): String {
    return "${folder.trackCount} ${if (folder.trackCount == 1) "track" else "tracks"}"
}

@PreviewScreenSizes
@Composable
private fun FolderRowPopulatedPreview() {
    PreviewTheme {
        Surface {
            FolderRow(
                folder = Folder(
                    id = 1,
                    treeUri = "content://tree",
                    relativePath = "Pearl Jam",
                    displayName = "Pearl Jam",
                    parentId = null,
                    trackCount = 142,
                ),
                onClick = {},
                onPlay = {},
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun FolderRowEmptyPreview() {
    PreviewTheme {
        Surface {
            FolderRow(
                folder = Folder(
                    id = 2,
                    treeUri = "content://tree",
                    relativePath = "Empty",
                    displayName = "Empty Folder",
                    parentId = null,
                    trackCount = 0,
                ),
                onClick = {},
            )
        }
    }
}
