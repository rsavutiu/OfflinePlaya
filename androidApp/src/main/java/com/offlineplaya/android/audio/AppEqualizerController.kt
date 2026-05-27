package com.offlineplaya.android.audio

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import androidx.media3.common.C
import com.offlineplaya.shared.domain.model.EqMode
import com.offlineplaya.shared.domain.model.EqPreset
import com.offlineplaya.shared.presentation.eq.EqualizerStateHolder
import com.offlineplaya.shared.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Owns the [android.media.audiofx.Equalizer] instance bound to the playback
 * service's [androidx.media3.exoplayer.ExoPlayer] audio session.
 *
 * Two responsibilities:
 *
 *  1. Translate the resolved [EqPreset] from [EqualizerStateHolder] into
 *     per-band `Equalizer.setBandLevel` calls. The preset is a template (5
 *     gains by convention); we remap onto whatever band count the platform
 *     reports — typically 5, occasionally fewer on cheap hardware, rarely more.
 *
 *  2. **Mutually exclude** our EQ with the system audio-effects broadcast
 *     (manufacturer EQ / Dolby Atmos / Spatial Audio). When the user's mode
 *     is [EqMode.OFF] we broadcast `ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION`
 *     so OEM effects engage; when mode is MANUAL or AUTO we close that
 *     handoff and own the session ourselves. The opposite would have both
 *     stacks compounding gains — same band shifted twice — and audio sounds
 *     wrong on a per-device basis.
 *
 * The controller is constructed by [PlaybackService] after the ExoPlayer
 * exists and is released with the service. Lifecycle is strictly bounded:
 * the [Equalizer] AIDL handle is released in [release], and the observer
 * coroutine is cancelled with [scope].
 */
class AppEqualizerController(
    private val context: Context,
    private val audioSessionId: Int,
    private val stateHolder: EqualizerStateHolder,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {

    private var equalizer: Equalizer? = null
    private var systemEffectsOpen: Boolean = false
    private var collectJob: Job? = null

    /**
     * Snapshot of the platform's reported band layout, captured the first
     * time we open the equalizer. We freeze it because the band layout
     * doesn't change at runtime and re-querying per preset change is
     * needless AIDL work.
     */
    private var bandLayout: BandLayout? = null

    fun start() {
        if (audioSessionId == AudioEffect.ERROR_BAD_VALUE ||
            audioSessionId == C.AUDIO_SESSION_ID_UNSET
        ) {
            logger.w(TAG, "Cannot start equalizer: invalid audio session id")
            return
        }

        collectJob = scope.launch {
            // Combine preferences + active preset so we react to both mode
            // changes (toggle system EQ broadcast) and preset changes (push
            // new band gains). `distinctUntilChanged` prevents redundant
            // AIDL calls when the upstream emits the same pair twice.
            combine(stateHolder.preferences, stateHolder.activePreset) { prefs, preset ->
                prefs.mode to preset
            }.distinctUntilChanged().collect { (mode, preset) ->
                apply(mode, preset)
            }
        }
    }

    /**
     * Decide which of the two exclusive worlds is active for [mode], and
     * push [preset] into the AudioFx instance when our EQ is engaged.
     */
    private fun apply(mode: EqMode, preset: EqPreset) {
        when (mode) {
            EqMode.OFF -> {
                // User wants the OEM effect pipeline. Close our AudioFx
                // instance (if any) and broadcast the handoff so manufacturer
                // EQ / Dolby / Spatial Audio engage.
                releaseEqualizer()
                openSystemEffects()
            }
            EqMode.MANUAL, EqMode.AUTO -> {
                // App-owned EQ. Close the system handoff (if open) and push
                // gains onto our Equalizer instance.
                closeSystemEffects()
                applyPresetToEqualizer(preset)
            }
        }
    }

    private fun applyPresetToEqualizer(preset: EqPreset) {
        val eq = ensureEqualizer() ?: return
        val layout = bandLayout ?: return
        val gains = remapGainsToBands(preset.gainsMillibels, layout)
        for ((index, gainMillibels) in gains.withIndex()) {
            val clamped = gainMillibels.coerceIn(layout.minMillibels, layout.maxMillibels)
            try {
                eq.setBandLevel(index.toShort(), clamped.toShort())
            } catch (t: Throwable) {
                // A band-level write can fail on a flaky AudioFx instance —
                // log once, keep applying the rest. The next preset change
                // will retry the failed band naturally.
                logger.e(TAG, "Failed to set band $index level to $clamped: ${t.message}", t)
            }
        }
    }

    /**
     * Lazily create the [Equalizer] AIDL instance the first time we need to
     * apply a preset. Releasing + recreating across mode flips is fine — the
     * AIDL handshake is cheap relative to setting band levels — but we keep
     * the instance around within MANUAL/AUTO sessions to avoid thrash on
     * per-track preset changes in AUTO.
     */
    private fun ensureEqualizer(): Equalizer? {
        equalizer?.let { return it }
        return try {
            val eq = Equalizer(EQ_PRIORITY, audioSessionId).apply { enabled = true }
            val bandCount = eq.numberOfBands.toInt()
            val (minLevel, maxLevel) = eq.bandLevelRange.let { it[0].toInt() to it[1].toInt() }
            bandLayout = BandLayout(
                numberOfBands = bandCount,
                minMillibels = minLevel,
                maxMillibels = maxLevel,
            )
            logger.i(
                TAG,
                "Equalizer ready: $bandCount bands, range [$minLevel .. $maxLevel] mB",
            )
            equalizer = eq
            eq
        } catch (t: Throwable) {
            // Some emulators and a handful of OEM ROMs return null/throw from
            // the Equalizer constructor — log once and bail. The user just
            // gets a no-op EQ until they switch tracks (which retries).
            logger.e(TAG, "Failed to construct Equalizer: ${t.message}", t)
            null
        }
    }

    private fun releaseEqualizer() {
        equalizer?.let {
            it.enabled = false
            try {
                it.release()
            } catch (t: Throwable) {
                logger.w(TAG, "Equalizer release threw: ${t.message}")
            }
        }
        equalizer = null
        bandLayout = null
    }

    private fun openSystemEffects() {
        if (systemEffectsOpen) return
        context.sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
                .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC),
        )
        systemEffectsOpen = true
    }

    private fun closeSystemEffects() {
        if (!systemEffectsOpen) return
        context.sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
                .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName),
        )
        systemEffectsOpen = false
    }

    fun release() {
        collectJob?.cancel()
        collectJob = null
        releaseEqualizer()
        closeSystemEffects()
    }

    private companion object {
        const val TAG = "AppEqualizerController"

        /**
         * Priority passed to the [Equalizer] constructor. Higher than the
         * default of 0 so other simultaneously-created effects (rarely a
         * concern in this app, but good hygiene) yield to us.
         */
        const val EQ_PRIORITY = 1

        /**
         * Project a preset's gain vector onto the platform's actual band
         * count: truncate when the platform has fewer bands, repeat the last
         * value when it has more. Better than dropping gains silently —
         * users get *something* even on weird hardware.
         */
        fun remapGainsToBands(template: List<Int>, layout: BandLayout): List<Int> {
            val n = layout.numberOfBands
            return when {
                template.isEmpty() -> List(n) { 0 }
                template.size == n -> template
                template.size > n -> template.take(n)
                else -> template + List(n - template.size) { template.last() }
            }
        }
    }

    private data class BandLayout(
        val numberOfBands: Int,
        val minMillibels: Int,
        val maxMillibels: Int,
    )
}
