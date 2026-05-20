package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.ArtworkPreferences
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.ManagedTreeRoot
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.domain.usecase.EmbedReport
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.ColorModeChooser
import com.offlineplaya.shared.presentation.ui.molecules.SettingsSection
import com.offlineplaya.shared.presentation.ui.molecules.SwitchRow
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Settings page: appearance controls + library management.
 *
 * Library section shows the managed tree roots with a remove button each
 * and a "re-scan everything" action. Re-scan is disabled while a scan is
 * already in flight.
 */
@Composable
fun SettingsPage(
    preferences: ThemePreferences,
    artworkPreferences: ArtworkPreferences,
    managedRoots: List<ManagedTreeRoot>,
    isScanning: Boolean,
    hasWritePermission: Boolean,
    embedReport: EmbedReport,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDownloadRemoteArtChange: (Boolean) -> Unit,
    onEmbedDownloadedArtChange: (Boolean) -> Unit,
    onRequestWritePermission: () -> Unit,
    onEmbedMissingArt: () -> Unit,
    onAcknowledgeEmbedReport: () -> Unit,
    onRescanAll: () -> Unit,
    onRemoveManagedRoot: (String) -> Unit,
    onBack: () -> Unit,
    dynamicColorSupported: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = { AppTopBar(title = "Settings", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection(title = "Appearance") {
                ColorModeChooser(
                    selected = preferences.colorMode,
                    onSelectionChanged = onColorModeChange,
                )
                SwitchRow(
                    title = "Material You",
                    subtitle = if (dynamicColorSupported) {
                        "Use wallpaper-based colors"
                    } else {
                        "Requires Android 12 or later"
                    },
                    checked = preferences.useDynamicColor && dynamicColorSupported,
                    onCheckedChange = onDynamicColorChange,
                    enabled = dynamicColorSupported,
                )
            }

            SettingsSection(title = "Album art") {
                SwitchRow(
                    title = "Download album art",
                    subtitle = "Look up missing covers from MusicBrainz and Cover Art Archive.",
                    checked = artworkPreferences.downloadRemoteArt,
                    onCheckedChange = onDownloadRemoteArtChange,
                )
                SwitchRow(
                    title = "Embed art into files",
                    subtitle = when {
                        !artworkPreferences.downloadRemoteArt ->
                            "Enable downloading first."
                        !hasWritePermission ->
                            "Grant write access below to enable."
                        else ->
                            "Write downloaded covers back into the audio files. " +
                                "This modifies your files — back them up first."
                    },
                    checked = artworkPreferences.embedDownloadedArt &&
                        artworkPreferences.downloadRemoteArt &&
                        hasWritePermission,
                    onCheckedChange = onEmbedDownloadedArtChange,
                    enabled = artworkPreferences.downloadRemoteArt && hasWritePermission,
                )
                if (artworkPreferences.downloadRemoteArt && !hasWritePermission) {
                    OutlinedButton(
                        onClick = onRequestWritePermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text("Grant write access (re-pick folder)")
                    }
                }

                EmbedReportBlock(
                    report = embedReport,
                    canEmbed = artworkPreferences.downloadRemoteArt &&
                        artworkPreferences.embedDownloadedArt &&
                        hasWritePermission,
                    onEmbed = onEmbedMissingArt,
                    onAcknowledge = onAcknowledgeEmbedReport,
                )
            }

            SettingsSection(title = "Library") {
                if (managedRoots.isEmpty()) {
                    Text(
                        text = "No folders added yet. Pick one from the home page.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    managedRoots.forEach { root ->
                        ManagedFolderRow(
                            root = root,
                            onRemove = { onRemoveManagedRoot(root.treeUri) },
                        )
                    }
                }
                OutlinedButton(
                    onClick = onRescanAll,
                    enabled = !isScanning && managedRoots.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(if (isScanning) "Scanning…" else "Re-scan all folders")
                }
            }
        }
    }
}

@Composable
private fun EmbedReportBlock(
    report: EmbedReport,
    canEmbed: Boolean,
    onEmbed: () -> Unit,
    onAcknowledge: () -> Unit,
) {
    when (report) {
        EmbedReport.Idle -> {
            OutlinedButton(
                onClick = onEmbed,
                enabled = canEmbed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Find missing art and embed it now")
            }
        }
        is EmbedReport.Running -> {
            Text(
                text = if (report.total > 0) {
                    "Embedding… ${report.processed}/${report.total} • ${report.embedded} written"
                } else {
                    "Preparing…"
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        is EmbedReport.Completed -> {
            Text(
                text = buildString {
                    append("Embedded ${report.embedded} covers")
                    if (report.failed > 0) append(" (${report.failed} failed)")
                    append(".")
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onAcknowledge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text("Dismiss") }
        }
        is EmbedReport.Failed -> {
            Text(
                text = "Embed pass failed: ${report.message}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(
                onClick = onAcknowledge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text("Dismiss") }
        }
    }
}

@Composable
private fun ManagedFolderRow(
    root: ManagedTreeRoot,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = root.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (root.lastScannedAt != null) "Scanned" else "Not yet scanned",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun SettingsPageLightPreview() {
    PreviewTheme {
        SettingsPage(
            preferences = ThemePreferences(ColorMode.SYSTEM, useDynamicColor = true),
            managedRoots = listOf(
                ManagedTreeRoot(1, "content://a", "Music Library", 0, 1_700_000_000),
                ManagedTreeRoot(2, "content://b", "Bootlegs", 0, null),
            ),
            isScanning = false,
            hasWritePermission = false,
            artworkPreferences = ArtworkPreferences.Default,
            onColorModeChange = {},
            onDynamicColorChange = {},
            onDownloadRemoteArtChange = {},
            onEmbedDownloadedArtChange = {},
            onRequestWritePermission = {},
            embedReport = EmbedReport.Idle,
            onEmbedMissingArt = {},
            onAcknowledgeEmbedReport = {},
            onRescanAll = {},
            onRemoveManagedRoot = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun SettingsPageDarkPreview() {
    PreviewTheme(darkTheme = true) {
        SettingsPage(
            preferences = ThemePreferences(ColorMode.DARK, useDynamicColor = false),
            managedRoots = emptyList(),
            isScanning = false,
            hasWritePermission = false,
            artworkPreferences = ArtworkPreferences.Default,
            onColorModeChange = {},
            onDynamicColorChange = {},
            onDownloadRemoteArtChange = {},
            onEmbedDownloadedArtChange = {},
            onRequestWritePermission = {},
            embedReport = EmbedReport.Idle,
            onEmbedMissingArt = {},
            onAcknowledgeEmbedReport = {},
            onRescanAll = {},
            onRemoveManagedRoot = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun SettingsPageScanningPreview() {
    PreviewTheme {
        SettingsPage(
            preferences = ThemePreferences(ColorMode.LIGHT, useDynamicColor = false),
            managedRoots = listOf(
                ManagedTreeRoot(1, "content://a", "Music Library", 0, 1_700_000_000),
            ),
            isScanning = true,
            hasWritePermission = true,
            artworkPreferences = ArtworkPreferences(
                downloadRemoteArt = true,
                embedDownloadedArt = true,
            ),
            onColorModeChange = {},
            onDynamicColorChange = {},
            onDownloadRemoteArtChange = {},
            onEmbedDownloadedArtChange = {},
            onRequestWritePermission = {},
            embedReport = EmbedReport.Idle,
            onEmbedMissingArt = {},
            onAcknowledgeEmbedReport = {},
            onRescanAll = {},
            onRemoveManagedRoot = {},
            onBack = {},
            dynamicColorSupported = false,
        )
    }
}
