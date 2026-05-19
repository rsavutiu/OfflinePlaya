package com.offlineplaya.shared.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Brand color tokens for OfflinePlaya. The palette leans on a muted purple
 * (think vinyl sleeve ink) for primary and a warm amber accent for tertiary —
 * meant to read as "music app, but not Spotify-green".
 *
 * Material 3's tonal system generates the rest of the on-/container colors
 * from these seeds.
 */
private val BrandPurple = Color(0xFF5E4DAB)
private val BrandPurpleLight = Color(0xFFB8AEFF)
private val BrandAmber = Color(0xFFE19F23)
private val BrandAmberLight = Color(0xFFFFCB6F)
private val BrandTeal = Color(0xFF3E8278)
private val BrandTealLight = Color(0xFF87D0C3)

internal val DefaultLightColors: ColorScheme = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3DDFF),
    onPrimaryContainer = Color(0xFF1B0F5C),

    secondary = BrandTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBCEBE1),
    onSecondaryContainer = Color(0xFF002822),

    tertiary = BrandAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0AC),
    onTertiaryContainer = Color(0xFF291800),

    background = Color(0xFFFCF9FF),
    onBackground = Color(0xFF1B1B22),
    surface = Color(0xFFFCF9FF),
    onSurface = Color(0xFF1B1B22),
    surfaceVariant = Color(0xFFE5E0EC),
    onSurfaceVariant = Color(0xFF49454E),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = Color(0xFF7A757F),
)

internal val DefaultDarkColors: ColorScheme = darkColorScheme(
    primary = BrandPurpleLight,
    onPrimary = Color(0xFF2C1D7F),
    primaryContainer = Color(0xFF453596),
    onPrimaryContainer = Color(0xFFE3DDFF),

    secondary = BrandTealLight,
    onSecondary = Color(0xFF003830),
    secondaryContainer = Color(0xFF1E5147),
    onSecondaryContainer = Color(0xFFBCEBE1),

    tertiary = BrandAmberLight,
    onTertiary = Color(0xFF442B00),
    tertiaryContainer = Color(0xFF604000),
    onTertiaryContainer = Color(0xFFFFE0AC),

    background = Color(0xFF13131A),
    onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF13131A),
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4CF),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF948F99),
)
