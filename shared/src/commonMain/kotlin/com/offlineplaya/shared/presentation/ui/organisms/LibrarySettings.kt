package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.ExcludedFolder
import com.offlineplaya.shared.domain.model.ManagedTreeRoot
import com.offlineplaya.shared.presentation.ui.molecules.ManagedFolderRow
import com.offlineplaya.shared.presentation.ui.molecules.SettingsSection
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.empty_library_no_folders
import offlineplaya.shared.generated.resources.library_add_folder_hint
import offlineplaya.shared.generated.resources.library_add_music_folder
import offlineplaya.shared.generated.resources.library_excluded_folders
import offlineplaya.shared.generated.resources.library_excluded_remove
import offlineplaya.shared.generated.resources.library_rescan_all
import offlineplaya.shared.generated.resources.library_scanning
import offlineplaya.shared.generated.resources.settings_section_library
import org.jetbrains.compose.resources.stringResource

/**
 * Library section: Add Folder primary button, a list of managed roots with
 * per-row remove, and a Re-scan All trailing action. Both action buttons are
 * disabled while a scan is in flight so the user can't queue up overlapping
 * scans.
 */
@Composable
fun LibrarySettings(
    managedRoots: PersistentList<ManagedTreeRoot>,
    isScanning: Boolean,
    onAddFolder: () -> Unit,
    onRescanAll: () -> Unit,
    onRemoveManagedRoot: (ManagedTreeRoot) -> Unit,
    modifier: Modifier = Modifier,
    excludedFolders: PersistentList<ExcludedFolder> = persistentListOf(),
    onRemoveExcludedFolder: (ExcludedFolder) -> Unit = {},
) {
    SettingsSection(
        title = stringResource(Res.string.settings_section_library),
        modifier = modifier
    ) {
        Column {
            val brand = LocalBrandAccent.current
            Button(
                onClick = onAddFolder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = brand.accent,
                    contentColor = brand.onAccent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(Res.string.library_add_music_folder))
            }
            // Why adding a folder matters beyond scanning: it's the only way
            // the app gets WRITE access, which the burn-metadata / lyrics /
            // tag-edit features need to modify the audio files themselves.
            Text(
                text = stringResource(Res.string.library_add_folder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (managedRoots.isEmpty()) {
                Text(
                    text = stringResource(Res.string.empty_library_no_folders),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                managedRoots.forEach { root ->
                    ManagedFolderRow(root = root, onRemove = { onRemoveManagedRoot(root) })
                }
            }
            if (excludedFolders.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.library_excluded_folders).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                excludedFolders.forEach { excluded ->
                    ExcludedFolderRow(
                        excluded = excluded,
                        onRemove = { onRemoveExcludedFolder(excluded) },
                    )
                }
            }
            OutlinedButton(
                onClick = onRescanAll,
                enabled = !isScanning && managedRoots.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .heightIn(max = 44.dp),
            ) {
                Text(
                    if (isScanning) stringResource(Res.string.library_scanning) else stringResource(
                        Res.string.library_rescan_all
                    )
                )
            }
        }
    }
}

/**
 * One excluded folder: name + path, with an un-exclude action that brings the
 * folder back into the library on the next rescan.
 */
@Composable
private fun ExcludedFolderRow(
    excluded: ExcludedFolder,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = excluded.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (excluded.relativePath.isNotEmpty()) {
                Text(
                    text = excluded.relativePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.library_excluded_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun LibrarySettingsPopulatedPreview() {
    PreviewTheme {
        LibrarySettings(
            managedRoots = persistentListOf(
                ManagedTreeRoot(1, "content://a", "Music Library", 0, 1_700_000_000),
                ManagedTreeRoot(2, "content://b", "Bootlegs", 0, null),
            ),
            isScanning = false,
            onAddFolder = {},
            onRescanAll = {},
            onRemoveManagedRoot = {},
            excludedFolders = persistentListOf(
                ExcludedFolder(1, "device://audio", "WhatsApp/Media/WhatsApp Audio", "WhatsApp Audio"),
            ),
        )
    }
}

@PreviewScreenSizes
@Composable
private fun LibrarySettingsEmptyPreview() {
    PreviewTheme {
        LibrarySettings(
            managedRoots = persistentListOf(),
            isScanning = false,
            onAddFolder = {},
            onRescanAll = {},
            onRemoveManagedRoot = {},
        )
    }
}
