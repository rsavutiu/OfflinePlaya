package com.offlineplaya.android.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.DesignSystemGalleryPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import org.junit.Test

/**
 * Isolated-screen smoke test for [DesignSystemGalleryPage]: the design-token
 * catalog renders. A render-only test is sufficient — it is a developer
 * reference page with no routing behaviour.
 */
@OptIn(ExperimentalTestApi::class)
class DesignSystemGalleryPageTest {

    @Test
    fun renders_token_catalog() = runComposeUiTest {
        setContent {
            PreviewTheme {
                DesignSystemGalleryPage(
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.DesignGallery.ROOT),
                )
            }
        }
        onNodeWithTag(TestTags.DesignGallery.ROOT).assertIsDisplayed()
    }
}
