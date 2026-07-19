package com.offlineplaya.shared.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Brand color tokens for OfflinePlaya.
 *
 * Two roles, deliberately separated:
 *  - **Ambient violet** (the `BrandViolet*` family + Material `ColorScheme`):
 *    a *default* tonal palette that the dynamic Palette / Material You pipeline
 *    is free to overwrite at runtime. When album art drives the scheme,
 *    `colorScheme.primary` becomes whatever the cover seeds.
 *  - **Brand accent** ([BrandAccent]): a FIXED Walkman orange that the dynamic
 *    pipeline must NOT touch. Used for identity surfaces — primary action
 *    buttons, FAB, the active tab underline, toggle "on" state, seek-fill —
 *    so the app has a recognisable identity that pops against any ambient
 *    tint Palette throws at us.
 *
 * Read [BrandAccent] via `LocalBrandAccent.current` from a Composable; reading
 * `colorScheme.primary` is correct for ambient/tonal surfaces but wrong for
 * brand-identity surfaces.
 */
private val BrandViolet = Color(0xFF7C5CBF)
private val BrandVioletLight = Color(0xFFB0A0F0)
private val BrandVioletDim = Color(0xFF9B8CE0)
private val BrandVioletDeep = Color(0xFF3D2E7C)

/**
 * Fixed brand accent — classic Walkman orange. Lives outside the Material
 * `ColorScheme` so the album-art / Material You recolor pipeline cannot
 * overwrite it. See [BrandAccentColors] / `LocalBrandAccent`.
 */
internal val BrandAccent: Color = Color(0xFFF47B20)
internal val OnBrandAccent: Color = Color.White

/**
 * Bundle of fixed brand accent colors exposed via `LocalBrandAccent`.
 * Grouped so callsites that need both bg + foreground can read one composition
 * local instead of two.
 */
data class BrandAccentColors(
    val accent: Color,
    val onAccent: Color,
)

internal val DefaultBrandAccentColors: BrandAccentColors = BrandAccentColors(
    accent = BrandAccent,
    onAccent = OnBrandAccent,
)

// Light scheme: a deliberate violet WASH, not a near-white neutral. The old
// values (#FCF9FF background, gray-lavender variant) read as a stock Material
// template next to the dark theme's ink-violet ambience. Surfaces carry a
// visible lavender tint, the surfaceContainer ladder is set explicitly so
// cards / sheets / dialogs / bottom bars inherit the wash instead of M3's
// neutral defaults, and primary is deepened a step so controls punch on the
// tinted ground. All on-colors keep ≥ 4.5:1 on their surfaces.
internal val DefaultLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF6A45B8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7DCFF),
    onPrimaryContainer = Color(0xFF21105E),

    secondary = Color(0xFF6A45B8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7DCFF),
    onSecondaryContainer = Color(0xFF21105E),

    tertiary = Color(0xFF6A45B8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE7DCFF),
    onTertiaryContainer = Color(0xFF21105E),

    background = Color(0xFFF5F0FB),
    onBackground = Color(0xFF1C1729),
    surface = Color(0xFFF5F0FB),
    onSurface = Color(0xFF1C1729),
    surfaceVariant = Color(0xFFE7DEF4),
    onSurfaceVariant = Color(0xFF4A4361),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFE8F8),
    surfaceContainer = Color(0xFFEAE1F5),
    surfaceContainerHigh = Color(0xFFE4D9F1),
    surfaceContainerHighest = Color(0xFFDDD1ED),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = Color(0xFF7B7292),
    outlineVariant = Color(0xFFCBC0DF),
)

internal val DefaultDarkColors: ColorScheme = darkColorScheme(
    primary = BrandVioletLight,
    onPrimary = BrandVioletDeep,
    primaryContainer = BrandViolet,
    onPrimaryContainer = Color(0xFFE3DDFF),

    secondary = BrandVioletDim,
    onSecondary = BrandVioletDeep,
    secondaryContainer = Color(0xFF2A1E4C),
    onSecondaryContainer = Color(0xFFE3DDFF),

    tertiary = BrandVioletDim,
    onTertiary = BrandVioletDeep,
    tertiaryContainer = Color(0xFF2A1E4C),
    onTertiaryContainer = Color(0xFFE3DDFF),

    background = Color(0xFF0C0C12),
    onBackground = Color(0xFFF0F0F5),
    surface = Color(0xFF0C0C12),
    onSurface = Color(0xFFF0F0F5),
    surfaceVariant = Color(0xFF14141C),
    onSurfaceVariant = Color(0xFFD0D0E8),

    error = Color(0xFFEF6070),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF5A5A7A),
)
