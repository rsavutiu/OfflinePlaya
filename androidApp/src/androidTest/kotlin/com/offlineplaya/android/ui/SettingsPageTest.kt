package com.offlineplaya.android.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.ArtworkPreferences
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.LyricsPreferences
import com.offlineplaya.shared.domain.model.ManagedTreeRoot
import com.offlineplaya.shared.domain.model.PlaybackPreferences
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.domain.usecase.EmbedReport
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.SettingsPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Isolated-screen test for [SettingsPage]: param-wired (no Koin). Verifies the
 * two key playback/appearance toggles route their negated value back to the
 * caller — album-art color (starts on → off) and crossfade (starts off → on).
 */
@OptIn(ExperimentalTestApi::class)
class SettingsPageTest {

    @Test
    fun album_art_color_and_crossfade_toggles_route_their_negated_value() = runComposeUiTest {
        var albumArtColor: Boolean? = null
        var crossfade: Boolean? = null
        setContent {
            PreviewTheme {
                SettingsPage(
                    preferences = ThemePreferences(
                        ColorMode.SYSTEM,
                        useDynamicColor = true,
                        useAlbumArtColor = true,
                    ),
                    artworkPreferences = ArtworkPreferences.Default,
                    lyricsPreferences = LyricsPreferences.Default,
                    playbackPreferences = PlaybackPreferences(
                        crossfadeEnabled = false,
                        crossfadeDurationSeconds = 6,
                    ),
                    managedRoots = persistentListOf(
                        ManagedTreeRoot(1, "content://a", "Music Library", 0, 1_700_000_000),
                    ),
                    isScanning = false,
                    burnReport = EmbedReport.Idle,
                    onColorModeChange = {},
                    onDynamicColorChange = {},
                    onAlbumArtColorChange = { albumArtColor = it },
                    onDownloadRemoteArtChange = {},
                    onDownloadRemoteLyricsChange = {},
                    onSaveLyricsAsSidecarChange = {},
                    onCrossfadeEnabledChange = { crossfade = it },
                    onCrossfadeDurationChange = {},
                    onBurnMetadataClick = {},
                    onAcknowledgeBurnReport = {},
                    onPickFolder = {},
                    onRescanAll = {},
                    onRemoveManagedRoot = {},
                    onOpenEqualizer = {},
                    onOpenDesignSystem = {},
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.Settings.ROOT).assertIsDisplayed()

        onNodeWithTag(TestTags.Settings.ALBUM_ART_COLOR_TOGGLE).performScrollTo().performClick()
        assertEquals("album-art color toggle should emit !checked", false, albumArtColor)

        onNodeWithTag(TestTags.Settings.CROSSFADE_TOGGLE).performScrollTo().performClick()
        assertEquals("crossfade toggle should emit !checked", true, crossfade)
    }
}
