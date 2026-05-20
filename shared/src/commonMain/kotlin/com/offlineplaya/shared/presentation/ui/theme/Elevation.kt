package com.offlineplaya.shared.presentation.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Elevation tokens following Material 3's level system. M3 uses tonal
 * elevation rather than literal shadow on most surfaces, so these dp values
 * primarily drive the surface tint applied by `Surface(tonalElevation = ...)`.
 *
 * Reach for the level by semantic intent, not by the dp number:
 *  - resting surfaces use [level0]
 *  - subtle separation (card on background) uses [level1]
 *  - floating overlays (mini player, FAB) use [level3]
 *  - modal sheets use [level4]
 */
object AppElevation {
    /** 0dp — base surface; same plane as the background. */
    val level0 = 0.dp

    /** 1dp — subtle tint to separate a card from its background. */
    val level1 = 1.dp

    /** 3dp — for interactive elements at rest (top app bar after scroll). */
    val level2 = 3.dp

    /** 6dp — floating elements (mini player, FAB, snack bars). */
    val level3 = 6.dp

    /** 8dp — modal dialogs, navigation drawer at rest. */
    val level4 = 8.dp

    /** 12dp — emphasized modal surfaces, large prominent overlays. */
    val level5 = 12.dp
}
