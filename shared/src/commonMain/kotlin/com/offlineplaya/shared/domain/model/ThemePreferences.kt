package com.offlineplaya.shared.domain.model

/**
 * User-customizable theme preferences. Persisted by [SettingsRepository] and
 * observed by the UI to drive `OfflinePlayaTheme`.
 *
 * @property colorMode whether to follow the system, force light, or force dark.
 * @property useDynamicColor when `true` and the platform supports it (Android 12+),
 *   the color scheme is derived from the device wallpaper. Falls back to
 *   [defaultLightColors]/[defaultDarkColors] otherwise.
 */
data class ThemePreferences(
    val colorMode: ColorMode,
    val useDynamicColor: Boolean,
) {
    companion object {
        val Default = ThemePreferences(
            colorMode = ColorMode.SYSTEM,
            useDynamicColor = true,
        )
    }
}

enum class ColorMode(val dbValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromDbValue(value: String): ColorMode =
            entries.firstOrNull { it.dbValue == value } ?: SYSTEM
    }
}
