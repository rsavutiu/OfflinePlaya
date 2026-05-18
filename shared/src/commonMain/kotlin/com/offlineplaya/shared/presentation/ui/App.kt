package com.offlineplaya.shared.presentation.ui

import androidx.compose.runtime.Composable
import com.offlineplaya.shared.presentation.ui.pages.PlaceholderPage
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme

/**
 * App entry point: applies the theme and routes to the current page. Navigation
 * between pages will be introduced in Phase 4.
 */
@Composable
fun App() {
    OfflinePlayaTheme {
        PlaceholderPage()
    }
}
