package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import offlineplaya.shared.generated.resources.library_add_music_folder
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
