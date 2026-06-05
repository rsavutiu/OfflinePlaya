package com.offlineplaya.shared.presentation.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Carries the [SharedTransitionScope] from the app-level `SharedTransitionLayout`
 * down to the leaf composables (album-art thumbnails) that participate in the
 * list → Now Playing morph, without prop-drilling through every page. `null`
 * outside a `SharedTransitionLayout` — e.g. in `@Preview`s — so consumers
 * degrade to no shared element.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/**
 * The [AnimatedVisibilityScope] of the current navigation `AnimatedContent`
 * pane. Paired with [LocalSharedTransitionScope] to drive a shared-element
 * transition. `null` when not inside the nav `AnimatedContent`.
 */
val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * The shared-element key for a track's album art on the list → Now Playing
 * morph. Both the source row's thumbnail and the Now Playing art panel build
 * the key from the same [trackId] so the cover flies between them. Keyed by
 * track (not album) because album-detail lists show many rows of one album —
 * an album key would collide across those rows.
 */
fun nowPlayingSharedArtKey(trackId: Long): String = "np-art-$trackId"

/**
 * Applies a shared-element transition to this modifier when [key] is non-null
 * AND both the [SharedTransitionScope] and an [AnimatedVisibilityScope] are
 * present in the composition. Otherwise returns the modifier unchanged, so the
 * same composable renders fine in previews, the mini-player, and any context
 * outside the nav transition.
 *
 * Must be called from a `@Composable` because it reads composition locals and
 * remembers the shared-content state.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedAlbumArt(key: String?): Modifier {
    if (key == null) return this
    val transitionScope = LocalSharedTransitionScope.current ?: return this
    val visibilityScope = LocalNavAnimatedVisibilityScope.current ?: return this
    with(transitionScope) {
        return this@sharedAlbumArt.sharedElement(
            state = rememberSharedContentState(key = key),
            animatedVisibilityScope = visibilityScope,
        )
    }
}
