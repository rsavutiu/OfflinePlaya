package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.ManagedTreeRoot
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.cd_remove
import offlineplaya.shared.generated.resources.managed_folder_not_yet_scanned
import offlineplaya.shared.generated.resources.managed_folder_scanned
import org.jetbrains.compose.resources.stringResource

/**
 * One row of the managed-folders list in Settings: display name, scan status
 * subtitle, trailing delete affordance. Removal is destructive enough to want
 * a confirmation dialog — that's the caller's job.
 */
@Composable
fun ManagedFolderRow(
    root: ManagedTreeRoot,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = root.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (root.lastScannedAt != null) stringResource(Res.string.managed_folder_scanned) else stringResource(
                    Res.string.managed_folder_not_yet_scanned
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(Res.string.cd_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun ManagedFolderRowPreview() {
    PreviewTheme {
        Surface {
            ManagedFolderRow(
                root = ManagedTreeRoot(1, "content://x", "Music Library", 0, 1_700_000_000),
                onRemove = {},
            )
        }
    }
}
