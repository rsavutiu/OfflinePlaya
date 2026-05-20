package com.offlineplaya.shared.presentation.settings

import com.offlineplaya.shared.domain.model.ArtworkPreferences
import com.offlineplaya.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI-facing state holder for [ArtworkPreferences]. Mirrors
 * [ThemeStateHolder]: hot flow + narrow mutators. The "embed" toggle is
 * automatically cleared if the user turns off "download" — there's no point
 * embedding something we can't download.
 */
class ArtworkStateHolder(
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    val preferences: StateFlow<ArtworkPreferences> = settings.observeArtworkPreferences()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = ArtworkPreferences.Default,
        )

    fun setDownloadRemoteArt(enabled: Boolean) {
        scope.launch {
            val current = preferences.value
            settings.setArtworkPreferences(
                current.copy(
                    downloadRemoteArt = enabled,
                    // Embedding requires downloading; cascade-disable.
                    embedDownloadedArt = current.embedDownloadedArt && enabled,
                ),
            )
        }
    }

    fun setEmbedDownloadedArt(enabled: Boolean) {
        scope.launch {
            val current = preferences.value
            // Don't allow embedding without downloading — UI will gate the
            // toggle too, but this keeps state consistent if both paths fire.
            if (enabled && !current.downloadRemoteArt) return@launch
            settings.setArtworkPreferences(current.copy(embedDownloadedArt = enabled))
        }
    }
}
