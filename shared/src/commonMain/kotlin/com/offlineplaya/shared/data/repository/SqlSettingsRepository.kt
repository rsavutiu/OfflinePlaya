package com.offlineplaya.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.offlineplaya.shared.database.OfflinePlayaDatabase
import com.offlineplaya.shared.database.Setting
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class SqlSettingsRepository(
    private val db: OfflinePlayaDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
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
            }
        }

    private fun List<Setting>.toThemePreferences(): ThemePreferences {
        val map: Map<String, String> = associate { it.key to it.value_ }
        val defaults = ThemePreferences.Default
        val rawMode = map[KEY_COLOR_MODE]
        val rawDynamic = map[KEY_DYNAMIC_COLOR]
        return ThemePreferences(
            colorMode = if (rawMode != null) ColorMode.fromDbValue(rawMode) else defaults.colorMode,
            useDynamicColor = rawDynamic?.toBoolean() ?: defaults.useDynamicColor,
        )
    }

    private companion object {
        const val KEY_COLOR_MODE = "theme.color_mode"
        const val KEY_DYNAMIC_COLOR = "theme.dynamic_color"
    }
}
