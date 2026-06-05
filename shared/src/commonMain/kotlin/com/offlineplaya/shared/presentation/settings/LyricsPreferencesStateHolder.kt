package com.offlineplaya.shared.presentation.settings

import com.offlineplaya.shared.domain.model.LyricsPreferences
import com.offlineplaya.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI-facing state holder for [LyricsPreferences]. Mirrors
 * [ArtworkStateHolder]: hot flow + narrow mutators. The "save sidecar"
 * toggle is automatically cleared if the user turns off "download" —
 * there's no point saving something we can't download.
 */
class LyricsPreferencesStateHolder(
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    val preferences: StateFlow<LyricsPreferences> = settings.observeLyricsPreferences()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = LyricsPreferences.Default,
        )

    fun setDownloadRemoteLyrics(enabled: Boolean) {
        scope.launch {
            val current = preferences.value
            settings.setLyricsPreferences(
                current.copy(
                    downloadRemoteLyrics = enabled,
                    // Saving a sidecar requires downloading; cascade-disable.
                    saveLyricsAsSidecar = current.saveLyricsAsSidecar && enabled,
                ),
            )
        }
    }

    fun setSaveLyricsAsSidecar(enabled: Boolean) {
        scope.launch {
            val current = preferences.value
            // Don't allow saving sidecars without downloading — UI will gate
            // the toggle too, but this keeps state consistent if both paths
            // fire concurrently.
            if (enabled && !current.downloadRemoteLyrics) return@launch
            settings.setLyricsPreferences(current.copy(saveLyricsAsSidecar = enabled))
        }
    }
}
