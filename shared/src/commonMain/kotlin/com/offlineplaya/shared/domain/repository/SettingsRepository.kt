package com.offlineplaya.shared.domain.repository

import com.offlineplaya.shared.domain.model.ArtworkPreferences
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.domain.model.LyricsPreferences
import com.offlineplaya.shared.domain.model.PlaybackPreferences
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

    /** Live lyrics preferences (download-remote toggle). */
    fun observeLyricsPreferences(): Flow<LyricsPreferences>

    /** One-shot read of lyrics preferences. */
    suspend fun getLyricsPreferences(): LyricsPreferences

    /** Replace the stored lyrics preferences atomically. */
    suspend fun setLyricsPreferences(preferences: LyricsPreferences)

    /** Live equalizer preferences (mode + manual preset + per-band overrides). */
    fun observeEqPreferences(): Flow<EqPreferences>

    /** One-shot read of equalizer preferences. */
    suspend fun getEqPreferences(): EqPreferences

    /** Replace stored equalizer preferences atomically. */
    suspend fun setEqPreferences(preferences: EqPreferences)

    /** Live playback-engine preferences (crossfade toggle + duration). */
    fun observePlaybackPreferences(): Flow<PlaybackPreferences>

    /** One-shot read of playback-engine preferences. */
    suspend fun getPlaybackPreferences(): PlaybackPreferences

    /** Replace stored playback-engine preferences atomically. */
    suspend fun setPlaybackPreferences(preferences: PlaybackPreferences)

    /**
     * The ARGB seed color extracted from the last song's album art, or `null`
     * if nothing has played yet (or the user cleared it). Persisted so the app
     * opens already tinted to the last-played album instead of flashing the
     * brand palette until the first track change of the session.
     */
    suspend fun getLastSeedColor(): Int?

    /** Persist (or clear, with `null`) the last album-art seed color. */
    suspend fun setLastSeedColor(argb: Int?)
}
