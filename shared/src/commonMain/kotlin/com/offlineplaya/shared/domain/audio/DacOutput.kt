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
     * into a wider-or-equal integer container (zero-padded LSBs), float into float.
     * Integer → float is deliberately rejected: the DAC must receive the integer
     * words untouched for the path to stay bit-perfect.
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
     * Bit-perfect path to the DAC. The player MUST write exactly [format] and
     * MUST NOT apply software volume, EQ, preamp, crossfade or any other DSP.
     */
    data class BitPerfect(val format: PcmFormat) : OutputMode
}

enum class FallbackReason {
    /** Platform has no bit-perfect API (Android < 14). */
    API_TOO_OLD,

    /** The audio HAL exposes no bit-perfect mixer for this DAC. */
    DEVICE_NOT_SUPPORTED,

    /** The DAC can't take this sample rate / depth bit-perfectly. */
    FORMAT_NOT_SUPPORTED,

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

/** Badge text for the signal path, e.g. "FiiO K11 · 96 kHz / 24-bit · Bit-perfect". */
fun DacOutputState.signalPathLabel(): String? {
    val dev = device ?: return null
    return when (val m = mode) {
        is OutputMode.BitPerfect -> {
            val depth = if (m.format.encoding.isFloat) {
                "32-bit float"
            } else {
                "${m.format.encoding.bitDepth}-bit"
            }
            "${dev.name} · ${formatKhz(m.format.sampleRate)} kHz / $depth · Bit-perfect"
        }
        OutputMode.Standard -> "${dev.name} · System mixer"
    }
}

/** 44100 → "44.1", 96000 → "96" (KMP-safe; no String.format in commonMain). */
internal fun formatKhz(sampleRate: Int): String {
    val whole = sampleRate / 1000
    val tenths = (sampleRate % 1000) / 100
    return if (tenths == 0) "$whole" else "$whole.$tenths"
}

/**
 * Picks the bit-perfect output format for [source] out of what the DAC offers.
 *
 * Exact sample rate and channel count are required — no resampling is the whole
 * point. Prefers the exact encoding; otherwise the smallest container that holds
 * the source losslessly (e.g. a 16-bit track into a 24-bit-only DAC).
 */
object BitPerfectFormatSelector {
    fun pick(offered: List<PcmFormat>, source: PcmFormat): PcmFormat? =
        offered
            .filter { it.sampleRate == source.sampleRate && it.channelCount == source.channelCount }
            .filter { it.encoding.canCarryLosslessly(source.encoding) }
            .minByOrNull { if (it.encoding == source.encoding) 0 else it.encoding.bitDepth }
}
