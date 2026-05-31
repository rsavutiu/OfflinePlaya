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
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.ArtworkPreferences
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.ManagedTreeRoot
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.presentation.ui.LocalOrientation
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.RemoveManagedRootDialog
import com.offlineplaya.shared.presentation.ui.molecules.SettingsLinkSection
import com.offlineplaya.shared.presentation.ui.organisms.AlbumArtSettings
import com.offlineplaya.shared.presentation.ui.organisms.AppearanceSettings
import com.offlineplaya.shared.presentation.ui.organisms.LibrarySettings
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Settings page. Five sections in portrait, two columns in landscape so the
 * page fits on small screens. Remove-folder is gated by a confirmation dialog.
 */
@Composable
fun SettingsPage(
    preferences: ThemePreferences,
    artworkPreferences: ArtworkPreferences,
    managedRoots: List<ManagedTreeRoot>,
    isScanning: Boolean,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDownloadRemoteArtChange: (Boolean) -> Unit,
    onEmbedFolderClick: () -> Unit,
    onAddFolder: () -> Unit,
    onRescanAll: () -> Unit,
    onRemoveManagedRoot: (String) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenDesignSystem: () -> Unit,
    onBack: () -> Unit,
    dynamicColorSupported: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var pendingRemoval by remember { mutableStateOf<ManagedTreeRoot?>(null) }

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
            val albumArt: @Composable () -> Unit = {
                AlbumArtSettings(
                    artworkPreferences = artworkPreferences,
                    onDownloadRemoteArtChange = onDownloadRemoteArtChange,
                    onEmbedFolderClick = onEmbedFolderClick,
                )
            }
            val library: @Composable () -> Unit = {
                LibrarySettings(
                    managedRoots = managedRoots,
                    isScanning = isScanning,
                    onAddFolder = onAddFolder,
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
                SettingsLinkSection(
                    sectionTitle = "Developer",
                    actionLabel = "Design system gallery",
                    onClick = onOpenDesignSystem,
                )
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
                            albumArt()
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            library()
                            audio()
                            developer()
                        }
                    }
                } else {
                    appearance()
                    albumArt()
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
}

@PreviewScreenSizes
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
            artworkPreferences = ArtworkPreferences.Default,
            onColorModeChange = {}, onDynamicColorChange = {},
            onDownloadRemoteArtChange = {}, onEmbedFolderClick = {},
            onAddFolder = {}, onRescanAll = {}, onRemoveManagedRoot = {},
            onOpenEqualizer = {}, onOpenDesignSystem = {}, onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsPageDarkPreview() {
    PreviewTheme(darkTheme = true) {
        SettingsPage(
            preferences = ThemePreferences(ColorMode.DARK, useDynamicColor = false),
            managedRoots = emptyList(),
            isScanning = false,
            artworkPreferences = ArtworkPreferences.Default,
            onColorModeChange = {}, onDynamicColorChange = {},
            onDownloadRemoteArtChange = {}, onEmbedFolderClick = {},
            onAddFolder = {}, onRescanAll = {}, onRemoveManagedRoot = {},
            onOpenEqualizer = {}, onOpenDesignSystem = {}, onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsPageScanningPreview() {
    PreviewTheme {
        SettingsPage(
            preferences = ThemePreferences(ColorMode.LIGHT, useDynamicColor = false),
            managedRoots = listOf(
                ManagedTreeRoot(1, "content://a", "Music Library", 0, 1_700_000_000),
            ),
            isScanning = true,
            artworkPreferences = ArtworkPreferences(
                downloadRemoteArt = true,
                embedDownloadedArt = true,
            ),
            onColorModeChange = {}, onDynamicColorChange = {},
            onDownloadRemoteArtChange = {}, onEmbedFolderClick = {},
            onAddFolder = {}, onRescanAll = {}, onRemoveManagedRoot = {},
            onOpenEqualizer = {}, onOpenDesignSystem = {}, onBack = {},
            dynamicColorSupported = false,
        )
    }
}
