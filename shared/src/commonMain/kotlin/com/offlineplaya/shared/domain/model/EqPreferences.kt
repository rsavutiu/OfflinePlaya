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
 */
data class EqPreferences(
    val mode: EqMode,
    val manualPresetName: String,
    val manualGains: List<Int>,
) {
    companion object {
        val Default = EqPreferences(
            mode = EqMode.OFF,
            manualPresetName = BuiltInPresets.FLAT.name,
            manualGains = emptyList(),
        )
    }
}
