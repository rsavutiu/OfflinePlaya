package com.offlineplaya.shared.domain.audio

/**
 * PCM sample container as handed to the audio output.
 *
 * @property bitDepth significant bits per sample.
 * @property isFloat `true` for IEEE-754 float samples; integer otherwise.
 */
enum class PcmEncoding(val bitDepth: Int, val isFloat: Boolean = false) {
    PCM_16(16),
    PCM_24(24),
    PCM_32(32),
    PCM_FLOAT(32, isFloat = true);

    /**
     * `true` if samples in [source] fit in this container with no loss — integer
     * into a wider-or-equal integer container, float into float. Integer ↔ float
     * is rejected: the DAC must receive the original integer words untouched.
     */
    fun canCarryLosslessly(source: PcmEncoding): Boolean =
        isFloat == source.isFloat && bitDepth >= source.bitDepth
}

data class PcmFormat(
    val sampleRate: Int,
    val encoding: PcmEncoding,
    val channelCount: Int = 2,
)

/**
 * An external USB DAC currently routed as an output.
 *
 * @property id platform device id — changes on every replug.
 * @property key stable-ish key for per-DAC settings (survives replugging).
 */
data class OutputDevice(
    val id: Int,
    val name: String,
    val key: String,
)

sealed interface OutputMode {
    /** Normal platform mixer: may resample; software volume / EQ / preamp allowed. */
    data object Standard : OutputMode

    /**
     * Bit-perfect path to the DAC. The player writes exactly [format] and MUST
     * NOT apply software volume, EQ, preamp, crossfade or any other DSP.
     */
    data class BitPerfect(val format: PcmFormat) : OutputMode
}

enum class FallbackReason {
    /** Platform has no bit-perfect API (Android < 14). */
    API_TOO_OLD,

    /** The audio HAL exposes no bit-perfect mixer for this DAC. */
    DEVICE_NOT_SUPPORTED,

    /** The DAC offers no bit-perfect mixer at this track's rate / channels. */
    FORMAT_NOT_SUPPORTED,

    /**
     * The track is deeper than what the player can write to the output
     * (e.g. 24-bit FLAC while the player outputs 16-bit), so the bits reaching
     * the DAC would not be the file's bits.
     */
    HI_RES_SOURCE,

    DISABLED_BY_USER,

    /** The platform refused the request, or revoked it later. */
    SYSTEM_REJECTED,
}

data class DacOutputState(
    /** `null` = no USB DAC connected. */
    val device: OutputDevice? = null,
    val mode: OutputMode = OutputMode.Standard,
    val bitPerfectCapable: Boolean = false,
    val bitPerfectEnabled: Boolean = true,
    val fallbackReason: FallbackReason? = null,
) {
    /** EQ, preamp, crossfade and software volume must stay off while this is `false`. */
    val softwareDspAllowed: Boolean get() = mode !is OutputMode.BitPerfect
}

/** 44100 → "44.1", 96000 → "96" (KMP-safe; no String.format in commonMain). */
fun formatKhz(sampleRate: Int): String {
    val whole = sampleRate / 1000
    val tenths = (sampleRate % 1000) / 100
    return if (tenths == 0) "$whole" else "$whole.$tenths"
}

/** Outcome of [BitPerfectPlanner.plan]. */
sealed interface BitPerfectPlan {
    data class Use(val format: PcmFormat) : BitPerfectPlan
    data class Fallback(val reason: FallbackReason) : BitPerfectPlan
}

/**
 * Decides whether a track can go to the DAC bit-perfectly.
 *
 * The platform only keeps a stream bit-perfect when the AudioTrack's format
 * equals the mixer's format, so the request must be for exactly what the player
 * writes: the track's own sample rate and channel count, in [sinkEncoding].
 * A track deeper than [sinkEncoding] can't be bit-perfect at all, because the
 * player has already reduced it before the DAC sees it.
 */
object BitPerfectPlanner {
    fun plan(
        offered: Collection<PcmFormat>,
        source: PcmFormat,
        sinkEncoding: PcmEncoding,
    ): BitPerfectPlan {
        if (offered.isEmpty()) {
            return BitPerfectPlan.Fallback(FallbackReason.DEVICE_NOT_SUPPORTED)
        }
        if (!sinkEncoding.canCarryLosslessly(source.encoding)) {
            return BitPerfectPlan.Fallback(FallbackReason.HI_RES_SOURCE)
        }
        val output = PcmFormat(source.sampleRate, sinkEncoding, source.channelCount)
        return if (output in offered) {
            BitPerfectPlan.Use(output)
        } else {
            BitPerfectPlan.Fallback(FallbackReason.FORMAT_NOT_SUPPORTED)
        }
    }
}
