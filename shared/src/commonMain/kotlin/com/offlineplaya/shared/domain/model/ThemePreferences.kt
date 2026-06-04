package com.offlineplaya.shared.domain.model

/**
 * User-customizable theme preferences. Persisted by [SettingsRepository] and
 * observed by the UI to drive `OfflinePlayaTheme`.
 *
 * @property colorMode whether to follow the system, force light, or force dark.
 * @property useDynamicColor when `true` and the platform supports it (Android 12+),
 *   the color scheme is derived from the device *wallpaper* (Material You).
 *   Falls back to [DefaultLightColors]/[DefaultDarkColors] otherwise.
 *
 *   Defaults to `false`: wallpaper-derived Material You frequently desaturates
 *   the violet brand into a flat grey on neutral wallpapers, which reads as
 *   "the app has no theme." The brand palette is the intended default look, and
 *   the album-art reactive tint ([useAlbumArtColor]) is the on-by-default
 *   dynamic source instead — it's far more intentional than the wallpaper.
 * @property useAlbumArtColor when `true` (the default), the whole app recolors
 *   to a Material 3 scheme generated from the album art of the last song
 *   played. Takes precedence over [useDynamicColor] when a seed color is
 *   available; falls back to the brand palette before anything has played.
 */
data class ThemePreferences(
    val colorMode: ColorMode,
    val useDynamicColor: Boolean,
    val useAlbumArtColor: Boolean,
) {
    companion object {
        val Default = ThemePreferences(
            colorMode = ColorMode.SYSTEM,
            useDynamicColor = false,
            useAlbumArtColor = true,
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
