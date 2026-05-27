package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.model.BuiltInPresets
import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.domain.model.EqMode
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.domain.model.EqPreset
import com.offlineplaya.shared.domain.model.Track

/**
 * Resolve the equalizer preset that should be active right now.
 *
 *  - [EqMode.OFF] → [BuiltInPresets.FLAT] (controller will further disable
 *    the AudioFx instance entirely, but resolving to FLAT keeps callers
 *    simple).
 *  - [EqMode.MANUAL] → the user's named preset, optionally overridden by
 *    [EqPreferences.manualGains] when the user has dragged a slider.
 *  - [EqMode.AUTO] → the preset mapped from the track's stored
 *    canonical_genre, or DEFAULT if the track has none yet.
 */
object PickPresetForTrack {

    fun pick(
        track: Track?,
        preferences: EqPreferences,
        builtIns: List<EqPreset> = BuiltInPresets.all,
        byGenre: Map<CanonicalGenre, EqPreset> = BuiltInPresets.byGenre,
    ): EqPreset = when (preferences.mode) {
        EqMode.OFF -> BuiltInPresets.FLAT
        EqMode.MANUAL -> resolveManual(preferences, builtIns)
        EqMode.AUTO -> resolveAuto(track, byGenre)
    }

    private fun resolveManual(
        preferences: EqPreferences,
        builtIns: List<EqPreset>,
    ): EqPreset {
        val basePreset = builtIns.firstOrNull { it.name == preferences.manualPresetName }
            ?: BuiltInPresets.FLAT
        // If the user has dragged any slider this session, those overrides
        // win — we don't mutate the named preset, we render a synthetic one.
        return if (preferences.manualGains.isEmpty()) {
            basePreset
        } else {
            basePreset.copy(gainsMillibels = preferences.manualGains)
        }
    }

    private fun resolveAuto(
        track: Track?,
        byGenre: Map<CanonicalGenre, EqPreset>,
    ): EqPreset {
        val genre = track?.canonicalGenre ?: CanonicalGenre.DEFAULT
        return byGenre[genre] ?: BuiltInPresets.FLAT
    }
}
