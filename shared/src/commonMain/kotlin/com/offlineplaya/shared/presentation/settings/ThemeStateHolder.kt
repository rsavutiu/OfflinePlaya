package com.offlineplaya.shared.presentation.settings

import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI-facing state holder for theme preferences. Exposes a hot [StateFlow] so
 * composables can collect a starting value synchronously, and offers
 * narrow mutators for the Settings screen.
 */
class ThemeStateHolder(
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    val preferences: StateFlow<ThemePreferences> = settings.observeThemePreferences()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = ThemePreferences.Default,
        )

    fun setColorMode(mode: ColorMode) {
        scope.launch {
            settings.setThemePreferences(preferences.value.copy(colorMode = mode))
        }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        scope.launch {
            settings.setThemePreferences(preferences.value.copy(useDynamicColor = enabled))
        }
    }

    fun setUseAlbumArtColor(enabled: Boolean) {
        scope.launch {
            settings.setThemePreferences(preferences.value.copy(useAlbumArtColor = enabled))
        }
    }
}
