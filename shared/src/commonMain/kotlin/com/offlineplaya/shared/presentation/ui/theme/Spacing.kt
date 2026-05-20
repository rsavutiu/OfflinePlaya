package com.offlineplaya.shared.presentation.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale for OfflinePlaya. Every padding / arrangement / margin in the
 * UI should reference one of these tokens rather than hard-coding a raw `dp`
 * literal — that's how we keep visual rhythm consistent across every page.
 *
 * Steps follow a 4/8 base so half-steps still land on the pixel grid at common
 * densities. Names are t-shirt sizes (xs..xxxl) so they read at the call site:
 * `Modifier.padding(AppSpacing.lg)` is immediately clearer than
 * `Modifier.padding(16.dp)`.
 *
 * If you find yourself reaching for a value not on this scale, push back on
 * the design first — adding a new step here is fine, but bypassing the scale
 * is how an app ends up looking subtly off.
 */
object AppSpacing {
    /** 0dp — for explicit "no space" arrangements. */
    val none = 0.dp

    /** 2dp — hairline; ornament-level spacing between glyphs or borders. */
    val xxs = 2.dp

    /** 4dp — between tightly-related elements (icon ↔ label inside one chip). */
    val xs = 4.dp

    /** 8dp — default inter-element spacing inside a single component. */
    val sm = 8.dp

    /** 12dp — comfortable medium step between elements of the same row. */
    val md = 12.dp

    /** 16dp — Material 3 default; page horizontal padding, between sections. */
    val lg = 16.dp

    /** 24dp — between unrelated content blocks. */
    val xl = 24.dp

    /** 32dp — between major page sections. */
    val xxl = 32.dp

    /** 48dp — large hero spacing (NowPlaying art, empty-state padding). */
    val xxxl = 48.dp
}
