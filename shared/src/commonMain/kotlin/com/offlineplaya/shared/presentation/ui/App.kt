package com.offlineplaya.shared.presentation.ui

import androidx.compose.runtime.Composable
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.pages.HomePage
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme

/**
 * App entry. The host (Android Activity / iOS UIViewController / Desktop window)
 * is responsible for observing [com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator]
 * and wiring a platform folder-picker into [onPickFolder]. This keeps the shared
 * UI free of platform APIs.
 */
@Composable
fun App(
    status: SyncStatus,
    onPickFolder: () -> Unit,
) {
    OfflinePlayaTheme {
        HomePage(status = status, onPickFolder = onPickFolder)
    }
}
