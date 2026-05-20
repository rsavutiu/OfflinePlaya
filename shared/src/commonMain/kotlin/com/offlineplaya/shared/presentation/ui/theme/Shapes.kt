package com.offlineplaya.shared.presentation.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shape tokens for OfflinePlaya. Material 3 ships a default [Shapes] scale
 * (extraSmall / small / medium / large / extraLarge) which we customize a bit
 * — chips read pillier, dialogs feel calmer with a softer corner.
 *
 * Use [OfflinePlayaShapes] inside [MaterialTheme]; reach for [AppShapes]
 * directly when you need a slot that M3 doesn't define (the full-pill
 * playback button, the album-art tile rounding).
 */
internal val OfflinePlayaShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),       // softer than M3's 16
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Component-specific shapes. Prefer naming a shape after the *thing* it
 * shapes (`card`, `dialog`) rather than the radius — that way swapping the
 * radius later doesn't break every call site's semantics.
 */
object AppShapes {
    /** Album art / artist avatar tiles. Slightly softer than M3 small. */
    val tile: Shape = RoundedCornerShape(10.dp)

    /** Track / artist / album / folder list rows when used as standalone cards. */
    val card: Shape = RoundedCornerShape(16.dp)

    /** Modal dialogs and bottom sheets. */
    val dialog: Shape = RoundedCornerShape(24.dp)

    /** Chips, segmented controls, filter tags. Fully pill-shaped. */
    val pill: Shape = CircleShape

    /** Mini player and floating overlays — slightly rounder than card. */
    val floating: Shape = RoundedCornerShape(20.dp)
}
