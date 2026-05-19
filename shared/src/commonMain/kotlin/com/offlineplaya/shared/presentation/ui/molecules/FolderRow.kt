package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FolderThumb(folder = folder)
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(
                text = folder.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
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
private fun FolderThumb(folder: Folder, modifier: Modifier = Modifier) {
    @Suppress("UNUSED_PARAMETER") val _kept = folder
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "📁",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
