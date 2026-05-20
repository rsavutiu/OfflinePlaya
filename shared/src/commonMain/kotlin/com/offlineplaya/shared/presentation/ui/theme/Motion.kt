package com.offlineplaya.shared.presentation.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Animation tokens. Material 3 distinguishes "standard" motion (interactive
 * affordances) from "emphasized" motion (large transitions where the spatial
 * relationship of elements changes). Durations match Google's guidance:
 *
 *  - short = small, in-place changes (icon swap, color tint)
 *  - medium = local transitions (expanding row, switching tab)
 *  - long = full-screen transitions (NowPlaying expand)
 *
 * Always animate using these tokens rather than raw `tween(150)` calls so
 * the whole app feels like one piece in motion.
 */
object AppMotion {
    /** 100 ms — quick affordances (button press, icon morph). */
    const val short: Int = 100

    /** 250 ms — local container changes (expanding card, tab switch). */
    const val medium: Int = 250

    /** 400 ms — large transitions (full-screen sheet, hero element move). */
    const val long: Int = 400

    /** Standard easing: deceleration curve for most affordances. */
    val standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Emphasized easing: more dramatic decel for prominent transitions. */
    val emphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** Emphasized accelerate — for elements exiting the screen. */
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
}
