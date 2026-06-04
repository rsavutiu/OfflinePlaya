package com.offlineplaya.shared.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.presentation.ui.ProvideOrientation
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes

/**
 * Root theme wrapper. Resolves a Material 3 color scheme from one of three
 * sources, in priority order:
 *
 *  1. **Album-art color** — when [ThemePreferences.useAlbumArtColor] is on
 *     (the default) and a [seedColor] is available, the whole app recolors to
 *     a tonal scheme generated from the last song's cover (material-kolor).
 *  2. **Material You** — when [ThemePreferences.useDynamicColor] is on and the
 *     platform supports it, the wallpaper-derived scheme.
 *  3. **Brand** — the violet [DefaultLightColors]/[DefaultDarkColors].
 *
 * Album art wins over wallpaper because it's the more intentional signal: the
 * user is looking at that cover. Before anything has played ([seedColor] null),
 * it falls through to the brand palette so the app always has an identity.
 *
 * Previews use [PreviewTheme], which forces light/dark and supplies no seed.
 */
@Composable
fun OfflinePlayaTheme(
    preferences: ThemePreferences = ThemePreferences.Default,
    seedColor: Int? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (preferences.colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }

    val brand = if (darkTheme) DefaultDarkColors else DefaultLightColors
    val colors = when (resolveThemeColorSource(preferences, seedColor)) {
        // seedColor is guaranteed non-null here by resolveThemeColorSource.
        ThemeColorSource.ALBUM_ART ->
            rememberDynamicColorScheme(
                seedColor = Color(seedColor!!),
                isDark = darkTheme,
                // false → dark surfaces keep a subtle seed tint instead of going
                // pure black, so the whole app visibly takes on the album color.
                isAmoled = false,
                // Vibrant carries more of the album's chroma into the neutral
                // surfaces/backgrounds (not just the accents), so even the
                // high-contrast body text sits on colored ground. TonalSpot (the
                // default) was too neutral — the app barely looked tinted. Body
                // text intentionally stays near-black/white for legibility; the
                // tint comes from what's *behind* it.
                style = PaletteStyle.Vibrant,
            )
        ThemeColorSource.WALLPAPER ->
            rememberDynamicColorSchemeOrNull(darkTheme) ?: brand
        ThemeColorSource.BRAND -> brand
    }

    MaterialTheme(
        colorScheme = colors,
        typography = OfflinePlayaTypography,
        shapes = OfflinePlayaShapes,
        content = content,
    )
}

/** Which of the three color sources the theme should draw from. */
internal enum class ThemeColorSource { ALBUM_ART, WALLPAPER, BRAND }

/**
 * Pure precedence decision for the theme's color source, factored out of the
 * composable so it can be unit-tested without a Compose harness.
 *
 *  - Album art wins whenever it's enabled AND a seed color exists (something
 *    has played). It's the most intentional signal — the user is looking at
 *    that cover.
 *  - Otherwise wallpaper Material You, if enabled. (The composable still falls
 *    back to the brand palette when the platform returns no wallpaper scheme.)
 *  - Otherwise the brand palette.
 */
internal fun resolveThemeColorSource(
    preferences: ThemePreferences,
    seedColor: Int?,
): ThemeColorSource = when {
    preferences.useAlbumArtColor && seedColor != null -> ThemeColorSource.ALBUM_ART
    preferences.useDynamicColor -> ThemeColorSource.WALLPAPER
    else -> ThemeColorSource.BRAND
}

/** Convenience for previews — forces light/dark without dynamic color. */
@Composable
fun PreviewTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    OfflinePlayaTheme(
        preferences = ThemePreferences(
            colorMode = if (darkTheme) ColorMode.DARK else ColorMode.LIGHT,
            useDynamicColor = false,
            useAlbumArtColor = false,
        ),
    ) {
        // Measure the preview canvas so LocalOrientation reflects the actual
        // preview shape instead of the bare PORTRAIT default.
        ProvideOrientation(content = content)
    }
}

@PreviewScreenSizes
@Composable
private fun OfflinePlayaThemeLightPreview() {
    PreviewTheme(darkTheme = false) {
        Surface { Text("Light theme sample") }
    }
}

@PreviewScreenSizes
@Composable
private fun OfflinePlayaThemeDarkPreview() {
    PreviewTheme(darkTheme = true) {
        Surface { Text("Dark theme sample") }
    }
}
