package com.offlineplaya.shared.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.presentation.navigation.AppDestination
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.sync.SyncStatus
import com.offlineplaya.shared.presentation.ui.pages.HomePage
import com.offlineplaya.shared.presentation.ui.pages.SettingsPage
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme

/**
 * Shared app entry. Renders whichever destination is on top of the
 * navigator's stack and applies the user's chosen theme. Platform hosts
 * inject the navigator + state holders via Koin and supply platform
 * callbacks (SAF picker on Android).
 */
@Composable
fun App(
    navigator: AppNavigator,
    themePreferences: ThemePreferences,
    syncStatus: SyncStatus,
    onPickFolder: () -> Unit,
    onColorModeChange: (ColorMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    dynamicColorSupported: Boolean,
) {
    OfflinePlayaTheme(preferences = themePreferences) {
        val stack by navigator.stack.collectAsState()
        val current = stack.last()

        AnimatedContent(
            targetState = current,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 180)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 180))
            },
            label = "destination",
        ) { dest ->
            when (dest) {
                AppDestination.Home -> HomePage(
                    status = syncStatus,
                    onPickFolder = onPickFolder,
                    onOpenSettings = { navigator.push(AppDestination.Settings) },
                )

                AppDestination.Settings -> SettingsPage(
                    preferences = themePreferences,
                    onColorModeChange = onColorModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onBack = { navigator.pop() },
                    dynamicColorSupported = dynamicColorSupported,
                )

                // Library destinations are reserved here and arrive in Substage C.
                // Falling back to Home avoids a blank screen if one is pushed early.
                AppDestination.LibraryArtists,
                is AppDestination.LibraryArtistDetail,
                is AppDestination.LibraryAlbumDetail,
                AppDestination.LibraryFolderRoots,
                is AppDestination.LibraryFolderDetail,
                AppDestination.LibraryFlat -> HomePage(
                    status = syncStatus,
                    onPickFolder = onPickFolder,
                    onOpenSettings = { navigator.push(AppDestination.Settings) },
                )
            }
        }
    }
}
