package com.offlineplaya.android.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.BuiltInPresets
import com.offlineplaya.shared.domain.model.EqMode
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.EqualizerPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import org.junit.Test

/**
 * Isolated-screen test for [EqualizerPage]. The page is a complex multi-mode
 * control surface whose affordances are localized chrome, so this asserts it
 * renders across the meaningful modes (Manual / Off) rather than fighting
 * localized strings for a routing tap.
 */
@OptIn(ExperimentalTestApi::class)
class EqualizerPageTest {

    @Test
    fun renders_manual_mode() = runComposeUiTest {
        setContent {
            PreviewTheme {
                EqualizerPage(
                    preferences = EqPreferences(EqMode.MANUAL, "Rock", emptyList(), preampPercent = 30),
                    activePreset = BuiltInPresets.ROCK,
                    nowPlayingTitle = null,
                    rawGenreTag = null,
                    autoGenreLabel = null,
                    onModeChange = {},
                    onPresetChange = {},
                    onBandGainChange = { _, _, _ -> },
                    onResetOverrides = {},
                    onPreampChange = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.Equalizer.ROOT),
                )
            }
        }
        onNodeWithTag(TestTags.Equalizer.ROOT).assertIsDisplayed()
    }

    @Test
    fun renders_off_mode() = runComposeUiTest {
        setContent {
            PreviewTheme {
                EqualizerPage(
                    preferences = EqPreferences(EqMode.OFF, "Default", emptyList()),
                    activePreset = BuiltInPresets.FLAT,
                    nowPlayingTitle = null,
                    rawGenreTag = null,
                    autoGenreLabel = null,
                    onModeChange = {},
                    onPresetChange = {},
                    onBandGainChange = { _, _, _ -> },
                    onResetOverrides = {},
                    onPreampChange = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.Equalizer.ROOT),
                )
            }
        }
        onNodeWithTag(TestTags.Equalizer.ROOT).assertIsDisplayed()
    }
}
