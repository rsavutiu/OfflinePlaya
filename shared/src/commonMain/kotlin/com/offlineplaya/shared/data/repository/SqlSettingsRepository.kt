package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.database.Setting
import com.offlineplaya.shared.domain.model.ArtworkPreferences
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.EqMode
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.domain.model.LyricsPreferences
import com.offlineplaya.shared.domain.model.PlaybackPreferences
import com.offlineplaya.shared.domain.model.ReviewPromptState
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class SqlSettingsRepository(
    private val db: OfflinePlayaDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SettingsRepository {

    private val queries get() = db.settingQueries

    override fun observeThemePreferences(): Flow<ThemePreferences> =
        queries.selectAll().asFlow().mapToList(ioDispatcher).map { rows ->
            rows.toThemePreferences()
        }

    override suspend fun getThemePreferences(): ThemePreferences = withContext(ioDispatcher) {
        queries.selectAll().executeAsList().toThemePreferences()
    }

    override suspend fun setThemePreferences(preferences: ThemePreferences) =
        withContext(ioDispatcher) {
            queries.transaction {
                queries.insertOrReplace(KEY_COLOR_MODE, preferences.colorMode.dbValue)
                queries.insertOrReplace(KEY_DYNAMIC_COLOR, preferences.useDynamicColor.toString())
                queries.insertOrReplace(KEY_ALBUM_ART_COLOR, preferences.useAlbumArtColor.toString())
            }
        }

    override suspend fun getLastSeedColor(): Int? = withContext(ioDispatcher) {
        queries.selectByKey(KEY_LAST_SEED_COLOR).executeAsOneOrNull()?.toIntOrNull()
    }

    override suspend fun setLastSeedColor(argb: Int?) = withContext(ioDispatcher) {
        if (argb == null) {
            queries.deleteByKey(KEY_LAST_SEED_COLOR)
        } else {
            queries.insertOrReplace(KEY_LAST_SEED_COLOR, argb.toString())
        }
    }

    override suspend fun getReviewPromptState(): ReviewPromptState = withContext(ioDispatcher) {
        val rows = queries.selectAll().executeAsList().associate { it.key to it.value_ }
        val defaults = ReviewPromptState.Default
        ReviewPromptState(
            playsCounted = rows[KEY_REVIEW_PLAYS]?.toIntOrNull() ?: defaults.playsCounted,
            highestMilestoneFired = rows[KEY_REVIEW_MILESTONE]?.toIntOrNull()
                ?: defaults.highestMilestoneFired,
            lastPromptAtMillis = rows[KEY_REVIEW_LAST_PROMPT]?.toLongOrNull()
                ?: defaults.lastPromptAtMillis,
        )
    }

    override suspend fun setReviewPromptState(state: ReviewPromptState) = withContext(ioDispatcher) {
        queries.transaction {
            queries.insertOrReplace(KEY_REVIEW_PLAYS, state.playsCounted.toString())
            queries.insertOrReplace(KEY_REVIEW_MILESTONE, state.highestMilestoneFired.toString())
            queries.insertOrReplace(KEY_REVIEW_LAST_PROMPT, state.lastPromptAtMillis.toString())
        }
    }

    override fun observeArtworkPreferences(): Flow<ArtworkPreferences> =
        queries.selectAll().asFlow().mapToList(ioDispatcher).map { rows ->
            rows.toArtworkPreferences()
        }

    override suspend fun getArtworkPreferences(): ArtworkPreferences = withContext(ioDispatcher) {
        queries.selectAll().executeAsList().toArtworkPreferences()
    }

    override suspend fun setArtworkPreferences(preferences: ArtworkPreferences) =
        withContext(ioDispatcher) {
            queries.transaction {
                queries.insertOrReplace(KEY_DOWNLOAD_REMOTE_ART, preferences.downloadRemoteArt.toString())
                queries.insertOrReplace(KEY_EMBED_DOWNLOADED_ART, preferences.embedDownloadedArt.toString())
            }
        }

    override fun observeLyricsPreferences(): Flow<LyricsPreferences> =
        queries.selectAll().asFlow().mapToList(ioDispatcher).map { rows ->
            rows.toLyricsPreferences()
        }

    override suspend fun getLyricsPreferences(): LyricsPreferences = withContext(ioDispatcher) {
        queries.selectAll().executeAsList().toLyricsPreferences()
    }

    override suspend fun setLyricsPreferences(preferences: LyricsPreferences) =
        withContext(ioDispatcher) {
            queries.transaction {
                queries.insertOrReplace(KEY_DOWNLOAD_REMOTE_LYRICS, preferences.downloadRemoteLyrics.toString())
                queries.insertOrReplace(KEY_SAVE_LYRICS_SIDECAR, preferences.saveLyricsAsSidecar.toString())
            }
        }

    override fun observeEqPreferences(): Flow<EqPreferences> =
        queries.selectAll().asFlow().mapToList(ioDispatcher).map { rows ->
            rows.toEqPreferences()
        }

    override suspend fun getEqPreferences(): EqPreferences = withContext(ioDispatcher) {
        queries.selectAll().executeAsList().toEqPreferences()
    }

    override suspend fun setEqPreferences(preferences: EqPreferences) =
        withContext(ioDispatcher) {
            queries.transaction {
                queries.insertOrReplace(KEY_EQ_MODE, preferences.mode.dbValue)
                queries.insertOrReplace(KEY_EQ_MANUAL_PRESET, preferences.manualPresetName)
                // Empty list serializes as empty string; the reader treats that
                // as "no override" and falls back to the preset's own gains.
                queries.insertOrReplace(
                    KEY_EQ_MANUAL_GAINS,
                    preferences.manualGains.joinToString(separator = ","),
                )
                queries.insertOrReplace(KEY_EQ_PREAMP, preferences.preampPercent.toString())
            }
        }

    override fun observePlaybackPreferences(): Flow<PlaybackPreferences> =
        queries.selectAll().asFlow().mapToList(ioDispatcher).map { rows ->
            rows.toPlaybackPreferences()
        }

    override suspend fun getPlaybackPreferences(): PlaybackPreferences = withContext(ioDispatcher) {
        queries.selectAll().executeAsList().toPlaybackPreferences()
    }

    override suspend fun setPlaybackPreferences(preferences: PlaybackPreferences) =
        withContext(ioDispatcher) {
            queries.transaction {
                queries.insertOrReplace(KEY_CROSSFADE_ENABLED, preferences.crossfadeEnabled.toString())
                queries.insertOrReplace(
                    KEY_CROSSFADE_DURATION,
                    preferences.crossfadeDurationSeconds.toString(),
                )
            }
        }

    private fun List<Setting>.toThemePreferences(): ThemePreferences {
        val map: Map<String, String> = associate { it.key to it.value_ }
        val defaults = ThemePreferences.Default
        val rawMode = map[KEY_COLOR_MODE]
        val rawDynamic = map[KEY_DYNAMIC_COLOR]
        val rawAlbumArt = map[KEY_ALBUM_ART_COLOR]
        return ThemePreferences(
            colorMode = if (rawMode != null) ColorMode.fromDbValue(rawMode) else defaults.colorMode,
            useDynamicColor = rawDynamic?.toBoolean() ?: defaults.useDynamicColor,
            useAlbumArtColor = rawAlbumArt?.toBoolean() ?: defaults.useAlbumArtColor,
        )
    }

    private fun List<Setting>.toArtworkPreferences(): ArtworkPreferences {
        val map: Map<String, String> = associate { it.key to it.value_ }
        val defaults = ArtworkPreferences.Default
        return ArtworkPreferences(
            downloadRemoteArt = map[KEY_DOWNLOAD_REMOTE_ART]?.toBoolean() ?: defaults.downloadRemoteArt,
            embedDownloadedArt = map[KEY_EMBED_DOWNLOADED_ART]?.toBoolean() ?: defaults.embedDownloadedArt,
        )
    }

    private fun List<Setting>.toLyricsPreferences(): LyricsPreferences {
        val map: Map<String, String> = associate { it.key to it.value_ }
        val defaults = LyricsPreferences.Default
        return LyricsPreferences(
            downloadRemoteLyrics = map[KEY_DOWNLOAD_REMOTE_LYRICS]?.toBoolean()
                ?: defaults.downloadRemoteLyrics,
            saveLyricsAsSidecar = map[KEY_SAVE_LYRICS_SIDECAR]?.toBoolean()
                ?: defaults.saveLyricsAsSidecar,
        )
    }

    private fun List<Setting>.toEqPreferences(): EqPreferences {
        val map: Map<String, String> = associate { it.key to it.value_ }
        val defaults = EqPreferences.Default
        val mode = EqMode.fromDbValue(map[KEY_EQ_MODE])
        val manualName = map[KEY_EQ_MANUAL_PRESET]?.takeIf { it.isNotBlank() }
            ?: defaults.manualPresetName
        val rawGains = map[KEY_EQ_MANUAL_GAINS].orEmpty()
        val gains = if (rawGains.isBlank()) {
            emptyList()
        } else {
            // mapNotNull tolerates corrupted entries by silently dropping them;
            // the caller treats a mismatched-length result as "no override".
            rawGains.split(',').mapNotNull { it.trim().toIntOrNull() }
        }
        return EqPreferences(
            mode = mode,
            manualPresetName = manualName,
            manualGains = gains,
            preampPercent = (map[KEY_EQ_PREAMP]?.toIntOrNull() ?: defaults.preampPercent)
                .coerceIn(0, EqPreferences.MAX_PREAMP_PERCENT),
        )
    }

    private fun List<Setting>.toPlaybackPreferences(): PlaybackPreferences {
        val map: Map<String, String> = associate { it.key to it.value_ }
        val defaults = PlaybackPreferences.Default
        val rawDuration = map[KEY_CROSSFADE_DURATION]?.toIntOrNull()
        return PlaybackPreferences(
            crossfadeEnabled = map[KEY_CROSSFADE_ENABLED]?.toBoolean() ?: defaults.crossfadeEnabled,
            crossfadeDurationSeconds = (rawDuration ?: defaults.crossfadeDurationSeconds)
                .coerceIn(
                    PlaybackPreferences.MIN_DURATION_SECONDS,
                    PlaybackPreferences.MAX_DURATION_SECONDS,
                ),
        )
    }

    private companion object {
        const val KEY_COLOR_MODE = "theme.color_mode"
        const val KEY_DYNAMIC_COLOR = "theme.dynamic_color"
        const val KEY_ALBUM_ART_COLOR = "theme.album_art_color"
        const val KEY_LAST_SEED_COLOR = "theme.last_seed_color"
        const val KEY_DOWNLOAD_REMOTE_ART = "artwork.download_remote"
        const val KEY_EMBED_DOWNLOADED_ART = "artwork.embed_downloaded"
        const val KEY_DOWNLOAD_REMOTE_LYRICS = "lyrics.download_remote"
        const val KEY_SAVE_LYRICS_SIDECAR = "lyrics.save_sidecar"
        const val KEY_EQ_MODE = "eq.mode"
        const val KEY_EQ_MANUAL_PRESET = "eq.manual_preset"
        const val KEY_EQ_MANUAL_GAINS = "eq.manual_gains"
        const val KEY_EQ_PREAMP = "eq.preamp_percent"
        const val KEY_CROSSFADE_ENABLED = "playback.crossfade_enabled"
        const val KEY_CROSSFADE_DURATION = "playback.crossfade_duration_s"
        const val KEY_REVIEW_PLAYS = "review.plays_counted"
        const val KEY_REVIEW_MILESTONE = "review.highest_milestone"
        const val KEY_REVIEW_LAST_PROMPT = "review.last_prompt_at"
    }
}
