package com.offlineplaya.shared.domain.image

import com.offlineplaya.shared.domain.model.Track

/**
 * Pulls a single ARGB seed color out of a track's album art, for the
 * album-art reactive theme. The seed is expanded into a full Material 3
 * color scheme upstream (see the theme layer) — this contract only has to
 * answer "what's the defining color of this cover?".
 *
 * Implementations run the platform's color-quantization (Android: `Palette`)
 * over the same art the player displays, so the theme always matches the
 * cover on screen. Returns `null` when there's no art or the platform can't
 * extract a color — callers keep whatever seed they already had.
 */
interface AlbumArtColorExtractor {
    /** ARGB seed color for [track]'s cover, or `null` if none can be derived. */
    suspend fun seedColor(track: Track): Int?
}
