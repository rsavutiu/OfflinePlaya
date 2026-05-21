package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.AppShapes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

// [LOCAL-LLM] (model: qwen3:8b-ctx16k) — initial draft generated from the
// ArtistRow template; Claude added the missing `layout.size` import and
// removed an unused `ui.draw.clip` import before committing.

@Composable
fun FolderRow(
    folder: Folder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FolderThumb()
        Column(modifier = Modifier.padding(start = AppSpacing.lg).weight(1f)) {
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
    }
}

@Composable
private fun FolderThumb(modifier: Modifier = Modifier) {
    // Outlined folder icon tinted to onSurfaceVariant instead of the bright
    // yellow "📁" glyph. Reads as part of the muted palette instead of
    // shouting against it.
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = AppShapes.tile,
        modifier = modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private fun summary(folder: Folder): String {
    return "${folder.trackCount} ${if (folder.trackCount == 1) "track" else "tracks"}"
}

@Preview
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
            )
        }
    }
}

@Preview
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
