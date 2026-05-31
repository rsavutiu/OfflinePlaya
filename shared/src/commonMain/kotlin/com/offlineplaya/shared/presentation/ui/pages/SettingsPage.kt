package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.ArtworkPreferences
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.ManagedTreeRoot
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.domain.usecase.EmbedReport
import com.offlineplaya.shared.presentation.ui.LocalOrientation
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.FolderPickerBrowser
import com.offlineplaya.shared.presentation.ui.molecules.RemoveManagedRootDialog
import com.offlineplaya.shared.presentation.ui.molecules.SettingsLinkSection
import com.offlineplaya.shared.presentation.ui.organisms.AppearanceSettings
import com.offlineplaya.shared.presentation.ui.organisms.BurnMetadataSettings
import com.offlineplaya.shared.presentation.ui.organisms.LibrarySettings
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * Settings page. Five sections in portrait, two columns in landscape so the
 * page fits on small screens. Remove-folder is gated by a confirmation dialog.
 */
@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    preferences: ThemePreferences,
    artworkPreferences: ArtworkPreferences,
    managedRoots: PersistentList<ManagedTreeRoot>,
    isScanning: Boolean,
    burnReport: EmbedReport,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDownloadRemoteArtChange: (Boolean) -> Unit,
    onBurnMetadataClick: () -> Unit,
    onAcknowledgeBurnReport: () -> Unit,
    onAddDirectFolder: (String) -> Unit,
    onRescanAll: () -> Unit,
    onRemoveManagedRoot: (String) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenDesignSystem: () -> Unit,
    onManageExternalStorageClick: () -> Unit,
    onBack: () -> Unit,
    dynamicColorSupported: Boolean = true,
) {
    var pendingRemoval by remember { mutableStateOf<ManagedTreeRoot?>(null) }
    var showDirectPicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { AppTopBar(title = "Settings", onBack = onBack) },
    ) { padding ->
        ResponsiveContent(modifier = Modifier.padding(padding)) {
            val appearance: @Composable () -> Unit = {
                AppearanceSettings(
                    preferences = preferences,
                    dynamicColorSupported = dynamicColorSupported,
                    onColorModeChange = onColorModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                )
            }
            val metadata: @Composable () -> Unit = {
                BurnMetadataSettings(
                    artworkPreferences = artworkPreferences,
                    onDownloadRemoteArtChange = onDownloadRemoteArtChange,
                    report = burnReport,
                    onBurnClick = onBurnMetadataClick,
                    onAcknowledgeReport = onAcknowledgeBurnReport
                )
            }
            val library: @Composable () -> Unit = {
                LibrarySettings(
                    managedRoots = managedRoots,
                    isScanning = isScanning,
                    onAddFolder = { showDirectPicker = true },
                    onRescanAll = onRescanAll,
                    onRemoveManagedRoot = { pendingRemoval = it },
                )
            }
            val audio: @Composable () -> Unit = {
                SettingsLinkSection(
                    sectionTitle = "Audio",
                    actionLabel = "Equalizer",
                    onClick = onOpenEqualizer,
                )
            }
            val developer: @Composable () -> Unit = {
                Column {
                    SettingsLinkSection(
                        sectionTitle = "Permissions",
                        actionLabel = if (android.os.Build.VERSION.SDK_INT >= 30) "All files access (Android 11+)" else "Refresh Media Library",
                        onClick = onManageExternalStorageClick,
                    )
                    SettingsLinkSection(
                        sectionTitle = "Developer",
                        actionLabel = "Design system gallery",
                        onClick = onOpenDesignSystem,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (LocalOrientation.current.isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            appearance()
                            metadata()
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            library()
                            audio()
                            developer()
                        }
                    }
                } else {
                    appearance()
                    metadata()
                    library()
                    audio()
                    developer()
                }
            }
        }
    }

    pendingRemoval?.let { root ->
        RemoveManagedRootDialog(
            root = root,
            onConfirm = {
                onRemoveManagedRoot(root.treeUri)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null },
        )
    }

    if (showDirectPicker) {
        FolderPickerBrowser(
            onFolderSelected = { path ->
                onAddDirectFolder(path)
                showDirectPicker = false
            },
            onDismiss = { showDirectPicker = false }
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsPageLightPreview() {
    PreviewTheme {
        SettingsPage(
            preferences = ThemePreferences(ColorMode.SYSTEM, useDynamicColor = true),
            managedRoots = persistentListOf(
                ManagedTreeRoot(1, "content://a", "Music Library", 0, 1_700_000_000),
                ManagedTreeRoot(2, "content://b", "Bootlegs", 0, null),
            ),
            isScanning = false,
            artworkPreferences = ArtworkPreferences.Default,
            burnReport = EmbedReport.Idle,
            onColorModeChange = {}, onDynamicColorChange = {},
            onDownloadRemoteArtChange = {}, onBurnMetadataClick = {},
            onAcknowledgeBurnReport = {},
            onAddDirectFolder = {}, onRescanAll = {}, onRemoveManagedRoot = {},
            onOpenEqualizer = {}, onOpenDesignSystem = {},
            onManageExternalStorageClick = {}, onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsPageDarkPreview() {
    PreviewTheme(darkTheme = true) {
        SettingsPage(
            preferences = ThemePreferences(ColorMode.DARK, useDynamicColor = false),
            managedRoots = persistentListOf(),
            isScanning = false,
            artworkPreferences = ArtworkPreferences.Default,
            burnReport = EmbedReport.Idle,
            onColorModeChange = {}, onDynamicColorChange = {},
            onDownloadRemoteArtChange = {}, onBurnMetadataClick = {},
            onAcknowledgeBurnReport = {},
            onAddDirectFolder = {}, onRescanAll = {}, onRemoveManagedRoot = {},
            onOpenEqualizer = {}, onOpenDesignSystem = {},
            onManageExternalStorageClick = {}, onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsPageScanningPreview() {
    PreviewTheme {
        SettingsPage(
            preferences = ThemePreferences(ColorMode.LIGHT, useDynamicColor = false),
            managedRoots = persistentListOf(
                ManagedTreeRoot(1, "content://a", "Music Library", 0, 1_700_000_000),
            ),
            isScanning = true,
            artworkPreferences = ArtworkPreferences(
                downloadRemoteArt = true,
                embedDownloadedArt = true,
            ),
            burnReport = EmbedReport.Running(10, 100, 8, 2),
            onColorModeChange = {}, onDynamicColorChange = {},
            onDownloadRemoteArtChange = {}, onBurnMetadataClick = {},
            onAcknowledgeBurnReport = {},
            onAddDirectFolder = {}, onRescanAll = {}, onRemoveManagedRoot = {},
            onOpenEqualizer = {}, onOpenDesignSystem = {},
            onManageExternalStorageClick = {}, onBack = {},
            dynamicColorSupported = false,
        )
    }
}
