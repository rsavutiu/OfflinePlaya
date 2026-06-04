package com.offlineplaya.shared.presentation.ui.theme

import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.ThemePreferences
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Precedence rules for the theme's color source. This is the logic the user
 * actually feels — "album art recolors the app, unless I turn it off" — so it
 * gets pinned here rather than left implicit in the composable.
 */
class ThemeColorSourceTest {

    private fun prefs(
        albumArt: Boolean,
        dynamic: Boolean,
    ) = ThemePreferences(
        colorMode = ColorMode.SYSTEM,
        useDynamicColor = dynamic,
        useAlbumArtColor = albumArt,
    )

    private val seed = 0x7C5CBF

    @Test
    fun `album art on with a seed wins`() {
        assertEquals(
            ThemeColorSource.ALBUM_ART,
            resolveThemeColorSource(prefs(albumArt = true, dynamic = false), seed),
        )
    }

    @Test
    fun `album art beats wallpaper when both are enabled`() {
        // Album art is the more intentional signal, so it takes precedence over
        // Material You even when the user has wallpaper colors on too.
        assertEquals(
            ThemeColorSource.ALBUM_ART,
            resolveThemeColorSource(prefs(albumArt = true, dynamic = true), seed),
        )
    }

    @Test
    fun `album art on but no seed yet falls through`() {
        // Nothing has played: album art can't apply, so wallpaper (if on) or
        // the brand takes over — the app is never left without a palette.
        assertEquals(
            ThemeColorSource.WALLPAPER,
            resolveThemeColorSource(prefs(albumArt = true, dynamic = true), seedColor = null),
        )
        assertEquals(
            ThemeColorSource.BRAND,
            resolveThemeColorSource(prefs(albumArt = true, dynamic = false), seedColor = null),
        )
    }

    @Test
    fun `album art off ignores the seed`() {
        assertEquals(
            ThemeColorSource.WALLPAPER,
            resolveThemeColorSource(prefs(albumArt = false, dynamic = true), seed),
        )
        assertEquals(
            ThemeColorSource.BRAND,
            resolveThemeColorSource(prefs(albumArt = false, dynamic = false), seed),
        )
    }

    @Test
    fun `everything off is the brand palette`() {
        assertEquals(
            ThemeColorSource.BRAND,
            resolveThemeColorSource(prefs(albumArt = false, dynamic = false), seedColor = null),
        )
    }
}
