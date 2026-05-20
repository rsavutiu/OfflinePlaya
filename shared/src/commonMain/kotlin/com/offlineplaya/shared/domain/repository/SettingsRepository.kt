package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.ArtworkPreferences
import com.offlineplaya.shared.domain.model.ThemePreferences
import kotlinx.coroutines.flow.Flow

/**
 * Typed access to the Setting key/value table. Keep specific accessors for
 * domain-meaningful settings so callers can't fat-finger string keys.
 */
interface SettingsRepository {

    /** Live theme preferences (defaults applied when missing). */
    fun observeThemePreferences(): Flow<ThemePreferences>

    /** One-shot read of theme preferences (defaults applied when missing). */
    suspend fun getThemePreferences(): ThemePreferences

    /** Replace the stored theme preferences atomically. */
    suspend fun setThemePreferences(preferences: ThemePreferences)

    /** Live artwork preferences (download/embed toggles). */
    fun observeArtworkPreferences(): Flow<ArtworkPreferences>

    /** One-shot read of artwork preferences. */
    suspend fun getArtworkPreferences(): ArtworkPreferences

    /** Replace the stored artwork preferences atomically. */
    suspend fun setArtworkPreferences(preferences: ArtworkPreferences)
}
