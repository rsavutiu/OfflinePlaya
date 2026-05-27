package com.offlineplaya.shared.presentation.eq

import com.offlineplaya.shared.domain.model.BuiltInPresets
import com.offlineplaya.shared.domain.model.EqMode
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.domain.model.EqPreset
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.domain.usecase.PickPresetForTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Combines [SettingsRepository]'s [EqPreferences] flow with the currently-playing
 * [Track] and emits the resolved [EqPreset] the audio-effect driver should apply.
 *
 * Holds the projection in a hot [StateFlow] so the UI and the
 * [com.offlineplaya.android.audio.AppEqualizerController] (or any future
 * cross-platform equivalent) can both observe without recomputing.
 *
 * Mutator methods are tiny — every change goes through
 * [SettingsRepository.setEqPreferences] so persistence is automatic and
 * single-sourced.
 */
class EqualizerStateHolder(
    private val settings: SettingsRepository,
    private val musicPlayer: MusicPlayer,
    private val scope: CoroutineScope,
) {

    /** Live equalizer preferences, defaulted when unset. */
    val preferences: StateFlow<EqPreferences> = settings.observeEqPreferences()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = EqPreferences.Default,
        )

    /**
     * Resolved [EqPreset] given the active mode and the currently-playing
     * track. Re-emits on track change (so AUTO mode picks the right preset)
     * and on every preference change.
     */
    val activePreset: StateFlow<EqPreset> = combine(
        preferences,
        musicPlayer.playbackState.map { it.currentTrack },
    ) { prefs, track ->
        PickPresetForTrack.pick(track, prefs)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = BuiltInPresets.FLAT,
    )

    fun setMode(mode: EqMode) {
        scope.launch {
            val current = preferences.value
            settings.setEqPreferences(current.copy(mode = mode))
        }
    }

    fun setManualPreset(name: String) {
        scope.launch {
            val current = preferences.value
            settings.setEqPreferences(
                current.copy(
                    mode = EqMode.MANUAL,
                    manualPresetName = name,
                    // Picking a fresh preset clears slider-overrides — switching
                    // to "Rock" should give the user the published Rock curve,
                    // not the dragged-Jazz curve still hanging around.
                    manualGains = emptyList(),
                ),
            )
        }
    }

    /**
     * Override a single band's gain. Promotes the mode to MANUAL if needed —
     * dragging a slider in OFF or AUTO mode is interpreted as "I want manual
     * control now". The full gain vector is persisted so re-renders see the
     * complete state, not just the one band that moved.
     */
    fun setBandGain(bandIndex: Int, millibels: Int, fullGains: List<Int>) {
        scope.launch {
            val current = preferences.value
            val newGains = if (bandIndex in fullGains.indices) {
                fullGains.toMutableList().also { it[bandIndex] = millibels }
            } else {
                fullGains
            }
            settings.setEqPreferences(
                current.copy(mode = EqMode.MANUAL, manualGains = newGains),
            )
        }
    }

    /** Reset slider-overrides back to the current preset's published gains. */
    fun resetManualOverrides() {
        scope.launch {
            val current = preferences.value
            settings.setEqPreferences(current.copy(manualGains = emptyList()))
        }
    }
}
