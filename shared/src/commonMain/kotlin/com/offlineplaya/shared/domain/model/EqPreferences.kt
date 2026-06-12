package com.offlineplaya.shared.domain.model

/**
 * Persisted equalizer state.
 *
 * @property mode currently engaged mode — see [EqMode].
 * @property manualPresetName name of the last preset the user explicitly picked
 *   in [EqMode.MANUAL]. Remembered while in OFF/AUTO so flipping back to
 *   MANUAL restores their choice.
 * @property manualGains user's per-band overrides for the current MANUAL
 *   session, in millibels. Empty means "use the preset as-is"; non-empty
 *   means "the user dragged a slider, ignore the preset name's gains".
 * @property preampPercent loudness boost beyond 100% stream volume, as a
 *   percentage of perceived amplitude: 0 = off, 100 = roughly double (+6 dB).
 *   Applied via a platform loudness effect, independent of [mode] — capped at
 *   [MAX_PREAMP_PERCENT] because boosting further clips badly on most files.
 */
data class EqPreferences(
    val mode: EqMode,
    val manualPresetName: String,
    val manualGains: List<Int>,
    val preampPercent: Int = 0,
) {
    companion object {
        val Default = EqPreferences(
            mode = EqMode.OFF,
            manualPresetName = BuiltInPresets.FLAT.name,
            manualGains = emptyList(),
            preampPercent = 0,
        )

        /** Hard cap: +100% ≈ +6 dB. More than this clips audibly. */
        const val MAX_PREAMP_PERCENT = 100

        /** Per-volume-key-press increment. 10 presses from 0 to the cap. */
        const val PREAMP_STEP_PERCENT = 10
    }
}
