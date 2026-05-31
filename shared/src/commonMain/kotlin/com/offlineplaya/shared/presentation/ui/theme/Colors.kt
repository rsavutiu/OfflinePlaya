package com.offlineplaya.shared.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Brand color tokens for OfflinePlaya. Single-accent violet (#7C5CBF) design
 * system — every accent surface uses this family. The deep dark background
 * (#0C0C12) maximises contrast on AMOLED screens.
 */
private val BrandViolet = Color(0xFF7C5CBF)
private val BrandVioletLight = Color(0xFFB0A0F0)
private val BrandVioletDim = Color(0xFF9B8CE0)
private val BrandVioletDeep = Color(0xFF3D2E7C)

internal val DefaultLightColors: ColorScheme = lightColorScheme(
    primary = BrandViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3DDFF),
    onPrimaryContainer = Color(0xFF1B0F5C),

    secondary = BrandViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3DDFF),
    onSecondaryContainer = Color(0xFF1B0F5C),

    tertiary = BrandViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE3DDFF),
    onTertiaryContainer = Color(0xFF1B0F5C),

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
