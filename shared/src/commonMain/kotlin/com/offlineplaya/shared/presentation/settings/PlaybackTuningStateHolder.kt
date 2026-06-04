package com.offlineplaya.shared.presentation.settings

import com.offlineplaya.shared.domain.model.PlaybackPreferences
import com.offlineplaya.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI-facing state holder for [PlaybackPreferences]. Mirrors
 * [ArtworkStateHolder] / [ThemeStateHolder]: a hot flow plus narrow mutators
 * that funnel every change through [SettingsRepository] so persistence is
 * single-sourced. The Android `CrossfadeController` observes the same
 * repository flow, so toggling crossfade here takes effect on the next track
 * boundary without any extra wiring.
 */
class PlaybackTuningStateHolder(
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    val preferences: StateFlow<PlaybackPreferences> = settings.observePlaybackPreferences()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = PlaybackPreferences.Default,
        )

    fun setCrossfadeEnabled(enabled: Boolean) {
        scope.launch {
            settings.setPlaybackPreferences(preferences.value.copy(crossfadeEnabled = enabled))
        }
    }

    fun setCrossfadeDurationSeconds(seconds: Int) {
        scope.launch {
            val clamped = seconds.coerceIn(
                PlaybackPreferences.MIN_DURATION_SECONDS,
                PlaybackPreferences.MAX_DURATION_SECONDS,
            )
            settings.setPlaybackPreferences(
                preferences.value.copy(crossfadeDurationSeconds = clamped),
            )
        }
    }
}
