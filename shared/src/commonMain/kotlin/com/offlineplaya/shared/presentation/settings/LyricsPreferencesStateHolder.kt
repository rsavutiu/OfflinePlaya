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
 * [ArtworkStateHolder]: hot flow + a single mutator for the download
 * toggle. The repository reads the same preference each lookup, so
 * flipping this off immediately disables remote LRCLIB lookups — already
 * cached lyrics keep showing.
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
            settings.setLyricsPreferences(
                preferences.value.copy(downloadRemoteLyrics = enabled),
            )
        }
    }
}
