package com.offlineplaya.android.ui

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test

/**
 * Phase-0 smoke test for the instrumentation plan: proves the Compose
 * Multiplatform UI-test harness (runComposeUiTest + testTag finders) is wired
 * and runs on the device, before any real screen test depends on it.
 *
 * Uses [runComposeUiTest] — the CMP test API — rather than the AndroidX
 * createComposeRule: the UI is built with the JetBrains Compose runtime, and
 * the raw AndroidX rule can't see that composition's root ("No compose
 * hierarchies found"). runComposeUiTest is also the same API the portable
 * commonTest screen tests will use.
 *
 * Intentionally trivial — no app theme, resources, or state holders, so a
 * failure points at the harness, not a page. Run with:
 * `./gradlew :androidApp:connectedDebugAndroidTest` (needs a device).
 */
@OptIn(ExperimentalTestApi::class)
class ComposeHarnessSmokeTest {

    @Test
    fun harness_renders_and_finds_a_tagged_node() = runComposeUiTest {
        setContent {
            Text("smoke", modifier = Modifier.testTag(SMOKE_TAG))
        }

        onNodeWithTag(SMOKE_TAG).assertIsDisplayed()
    }

    private companion object {
        const val SMOKE_TAG = "compose_harness_smoke"
    }
}
