package com.offlineplaya.shared.presentation.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

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
        CompositionLocalProvider(LocalOrientation provides orientation) {
            content()
        }
    }
}
