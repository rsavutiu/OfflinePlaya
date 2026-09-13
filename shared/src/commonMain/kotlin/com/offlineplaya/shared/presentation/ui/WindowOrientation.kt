package com.offlineplaya.shared.presentation.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Coarse screen orientation, derived purely from the available width vs height
 * of the content area — no platform APIs (`LocalConfiguration`, `UIDevice`,
 * window managers) involved, so it lives in `commonMain` and works identically
 * on Android today and any future iOS/desktop target.
 *
 * "Landscape" here means *the content box is at least as wide as it is tall*.
 * That's intentionally a layout question, not a device-sensor question: a
 * split-screen or freeform window that's taller than wide should lay out like
 * a portrait phone even on a landscape-held tablet, and this captures that.
 */
enum class Orientation {
    PORTRAIT,
    LANDSCAPE;

    val isLandscape: Boolean get() = this == LANDSCAPE
    val isPortrait: Boolean get() = this == PORTRAIT

    companion object {
        /** Classify a content box from its measured dimensions. */
        fun fromSize(widthPx: Float, heightPx: Float): Orientation =
            if (widthPx >= heightPx) LANDSCAPE else PORTRAIT
    }
}

/**
 * The current [Orientation] of the app's content area. Provided once by [App]
 * (which measures the window with `BoxWithConstraints`); any composable in the
 * tree can read `LocalOrientation.current` to branch its layout.
 *
 * Defaults to [Orientation.PORTRAIT] so previews and tests that don't set it up
 * still render the phone-portrait path.
 */
val LocalOrientation = staticCompositionLocalOf { Orientation.PORTRAIT }

/**
 * Coarse width bucket of the app's content area, in the spirit of Material 3
 * window size classes — but derived purely from the measured content-box width
 * (in dp) so it lives in `commonMain` and needs no platform window APIs.
 *
 * Unlike [Orientation], which only answers "wider than tall?", this answers
 * "how much horizontal room is there?" — the distinction a landscape *phone*
 * (COMPACT/MEDIUM) and a 10" *tablet* (EXPANDED) need but orientation alone
 * can't make. Pages branch on this to go multi-column on genuinely wide
 * surfaces without letting a landscape phone trigger tablet layouts.
 *
 * Breakpoints follow the Material 3 width classes:
 * - [COMPACT]  < 600dp  — phone portrait, small split-screen
 * - [MEDIUM]   600–839dp — foldable inner (portrait), landscape phone, small tablet
 * - [EXPANDED] ≥ 840dp  — tablet landscape, foldable inner (landscape), desktop
 */
enum class WindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED;

    /** True once there's enough width to justify side-by-side content columns. */
    val isExpanded: Boolean get() = this == EXPANDED

    /** True at [MEDIUM] or wider — some horizontal breathing room, but not a full tablet. */
    val isAtLeastMedium: Boolean get() = this != COMPACT

    companion object {
        val MediumBreakpoint: Dp = 600.dp
        val ExpandedBreakpoint: Dp = 840.dp

        /** Classify a content box from its measured width in dp. */
        fun fromWidthDp(widthDp: Dp): WindowWidthClass = when {
            widthDp >= ExpandedBreakpoint -> EXPANDED
            widthDp >= MediumBreakpoint -> MEDIUM
            else -> COMPACT
        }
    }
}

/**
 * The current [WindowWidthClass] of the app's content area. Provided once by
 * [ProvideOrientation] alongside [LocalOrientation].
 *
 * Defaults to [WindowWidthClass.COMPACT] so previews and tests that don't set
 * it up render the phone path. **This is exactly why [PreviewTheme] must also
 * provide it** — otherwise a wide `@PreviewScreenSizes` canvas would still
 * report COMPACT and silently hide the tablet layout it's meant to check.
 */
val LocalWindowWidthClass = staticCompositionLocalOf { WindowWidthClass.COMPACT }

/**
 * Measures the available space and provides [LocalOrientation] to [content].
 *
 * Use this to root the orientation-aware tree. [App] wraps the whole app in it,
 * and [com.offlineplaya.shared.presentation.ui.theme.PreviewTheme] wraps every
 * `@PreviewScreenSizes` in it too — so previews classify orientation from their own canvas
 * size (a wide preview reports LANDSCAPE) instead of being stuck on the
 * PORTRAIT default that the bare [LocalOrientation] would hand back.
 */
@Composable
fun ProvideOrientation(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val orientation = Orientation.fromSize(
            widthPx = constraints.maxWidth.toFloat(),
            heightPx = constraints.maxHeight.toFloat(),
        )
        // Orientation is a raw-pixel aspect-ratio question; the width class is a
        // dp-threshold question, so it must go through density. Comparing the
        // px maxWidth to a dp breakpoint directly would misclassify on every
        // non-mdpi screen.
        val widthDp = with(LocalDensity.current) { constraints.maxWidth.toDp() }
        val widthClass = WindowWidthClass.fromWidthDp(widthDp)
        CompositionLocalProvider(
            LocalOrientation provides orientation,
            LocalWindowWidthClass provides widthClass,
        ) {
            content()
        }
    }
}
