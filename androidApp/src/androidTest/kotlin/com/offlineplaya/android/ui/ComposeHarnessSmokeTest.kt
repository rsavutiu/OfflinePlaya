package com.offlineplaya.android.ui

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

/**
 * Phase-0 smoke test for the instrumentation plan: proves the Compose UI-test
 * harness (createComposeRule + testTag finders) is wired and runs on the
 * device, before any real screen test depends on it.
 *
 * Intentionally trivial — no app theme, resources, or state holders, so a
 * failure here points squarely at the harness/dependency setup rather than at
 * a page. Run with: `./gradlew :androidApp:connectedDebugAndroidTest` (needs a
 * connected device or emulator).
 */
class ComposeHarnessSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun harness_renders_and_finds_a_tagged_node() {
        composeRule.setContent {
            Text("smoke", modifier = Modifier.testTag(SMOKE_TAG))
        }

        composeRule.onNodeWithTag(SMOKE_TAG).assertIsDisplayed()
    }

    private companion object {
        const val SMOKE_TAG = "compose_harness_smoke"
    }
}
