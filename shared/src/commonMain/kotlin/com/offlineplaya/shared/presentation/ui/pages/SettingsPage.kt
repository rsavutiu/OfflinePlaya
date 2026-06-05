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
import com.offlineplaya.shared.domain.model.LyricsPreferences
import com.offlineplaya.shared.domain.model.ManagedTreeRoot
import com.offlineplaya.shared.domain.model.PlaybackPreferences
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.domain.usecase.EmbedReport
import com.offlineplaya.shared.presentation.ui.LocalOrientation
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.molecules.RemoveManagedRootDialog
import com.offlineplaya.shared.presentation.ui.molecules.SettingsLinkSection
import com.offlineplaya.shared.presentation.ui.organisms.AppearanceSettings
import com.offlineplaya.shared.presentation.ui.organisms.BurnMetadataSettings
import com.offlineplaya.shared.presentation.ui.organisms.LibrarySettings
import com.offlineplaya.shared.presentation.ui.organisms.LyricsSettings
import com.offlineplaya.shared.presentation.ui.organisms.PlaybackSettings
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.top_bar_settings
import org.jetbrains.compose.resources.stringResource

/**
 * Settings page. Sections stack vertically in portrait and split into two
 * columns in landscape so the page fits on small screens. Removing a folder
 * is gated by a confirmation dialog because it drops every track row that
 * came from that tree.
 */
@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    preferences: ThemePreferences,
    artworkPreferences: ArtworkPreferences,
    lyricsPreferences: LyricsPreferences,
    playbackPreferences: PlaybackPreferences,
    managedRoots: PersistentList<ManagedTreeRoot>,
    isScanning: Boolean,
    burnReport: EmbedReport,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAlbumArtColorChange: (Boolean) -> Unit,
    onDownloadRemoteArtChange: (Boolean) -> Unit,
    onDownloadRemoteLyricsChange: (Boolean) -> Unit,
    onCrossfadeEnabledChange: (Boolean) -> Unit,
    onCrossfadeDurationChange: (Int) -> Unit,
    onBurnMetadataClick: () -> Unit,
    onAcknowledgeBurnReport: () -> Unit,
    onPickFolder: () -> Unit,
    onRescanAll: () -> Unit,
    onRemoveManagedRoot: (String) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenDesignSystem: () -> Unit,
    onBack: () -> Unit,
    dynamicColorSupported: Boolean = true,
) {
    var pendingRemoval by remember { mutableStateOf<ManagedTreeRoot?>(null) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.top_bar_settings),
                onBack = onBack
            )
        },
    ) { padding ->
        ResponsiveContent(modifier = Modifier.padding(padding)) {
            val appearance: @Composable () -> Unit = {
                AppearanceSettings(
                    preferences = preferences,
                    dynamicColorSupported = dynamicColorSupported,
                    onColorModeChange = onColorModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onAlbumArtColorChange = onAlbumArtColorChange,
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
            val lyrics: @Composable () -> Unit = {
                LyricsSettings(
                    preferences = lyricsPreferences,
                    onDownloadRemoteLyricsChange = onDownloadRemoteLyricsChange,
                )
            }
            val library: @Composable () -> Unit = {
                LibrarySettings(
                    managedRoots = managedRoots,
                    isScanning = isScanning,
                    onAddFolder = onPickFolder,
                    onRescanAll = onRescanAll,
                    onRemoveManagedRoot = { pendingRemoval = it },
                )
            }
            val playback: @Composable () -> Unit = {
                PlaybackSettings(
                    preferences = playbackPreferences,
                    onCrossfadeEnabledChange = onCrossfadeEnabledChange,
                    onCrossfadeDurationChange = onCrossfadeDurationChange,
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
                            metadata()
                            lyrics()
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            library()
                            playback()
                            audio()
                            developer()
                        }
                    }
                } else {
                    appearance()
                    metadata()
                    lyrics()
                    library()
                    playback()
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
            preferences = ThemePreferences(ColorMode.SYSTEM, useDynamicColor = true, useAlbumArtColor = true),
            managedRoots = persistentListOf(
                ManagedTreeRoot(1, "content://a", "Music Library", 0, 1_700_000_000),
                ManagedTreeRoot(2, "content://b", "Bootlegs", 0, null),
            ),
            isScanning = false,
            artworkPreferences = ArtworkPreferences.Default,
            playbackPreferences = PlaybackPreferences.Default,
            burnReport = EmbedReport.Idle,
            lyricsPreferences = LyricsPreferences.Default,
            onColorModeChange = {}, onDynamicColorChange = {}, onAlbumArtColorChange = {},
            onDownloadRemoteArtChange = {}, onDownloadRemoteLyricsChange = {},
            onBurnMetadataClick = {},
            onCrossfadeEnabledChange = {}, onCrossfadeDurationChange = {},
            onAcknowledgeBurnReport = {},
            onPickFolder = {}, onRescanAll = {}, onRemoveManagedRoot = {},
            onOpenEqualizer = {}, onOpenDesignSystem = {},
            onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsPageDarkPreview() {
    PreviewTheme(darkTheme = true) {
        SettingsPage(
            preferences = ThemePreferences(ColorMode.DARK, useDynamicColor = false, useAlbumArtColor = true),
            managedRoots = persistentListOf(),
            isScanning = false,
            artworkPreferences = ArtworkPreferences.Default,
            playbackPreferences = PlaybackPreferences(crossfadeEnabled = false, crossfadeDurationSeconds = 6),
            burnReport = EmbedReport.Idle,
            lyricsPreferences = LyricsPreferences.Default,
            onColorModeChange = {}, onDynamicColorChange = {}, onAlbumArtColorChange = {},
            onDownloadRemoteArtChange = {}, onDownloadRemoteLyricsChange = {},
            onBurnMetadataClick = {},
            onCrossfadeEnabledChange = {}, onCrossfadeDurationChange = {},
            onAcknowledgeBurnReport = {},
            onPickFolder = {}, onRescanAll = {}, onRemoveManagedRoot = {},
            onOpenEqualizer = {}, onOpenDesignSystem = {},
            onBack = {},
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsPageScanningPreview() {
    PreviewTheme {
        SettingsPage(
            preferences = ThemePreferences(ColorMode.LIGHT, useDynamicColor = false, useAlbumArtColor = true),
            managedRoots = persistentListOf(
                ManagedTreeRoot(1, "content://a", "Music Library", 0, 1_700_000_000),
            ),
            isScanning = true,
            artworkPreferences = ArtworkPreferences(
                downloadRemoteArt = true,
                embedDownloadedArt = true,
            ),
            playbackPreferences = PlaybackPreferences(crossfadeEnabled = true, crossfadeDurationSeconds = 10),
            burnReport = EmbedReport.Running(10, 100, 8, 2),
            lyricsPreferences = LyricsPreferences.Default,
            onColorModeChange = {}, onDynamicColorChange = {}, onAlbumArtColorChange = {},
            onDownloadRemoteArtChange = {}, onDownloadRemoteLyricsChange = {},
            onBurnMetadataClick = {},
            onCrossfadeEnabledChange = {}, onCrossfadeDurationChange = {},
            onAcknowledgeBurnReport = {},
            onPickFolder = {}, onRescanAll = {}, onRemoveManagedRoot = {},
            onOpenEqualizer = {}, onOpenDesignSystem = {},
            onBack = {},
            dynamicColorSupported = false,
        )
    }
}
