package com.offlineplaya.shared.domain.model

/**
 * A named equalizer preset: a list of per-band gains, in millibels (1/100 dB).
 *
 * The preset is template-shaped — it carries 10 gains by convention (a
 * standard 10-band graphic-EQ layout: 31 / 62 / 125 / 250 / 500 Hz /
 * 1 / 2 / 4 / 8 / 16 kHz), which gives the user far finer control than the
 * old 5-band curve. The number of *physical* bands the OS exposes is
 * independent of this template: the controller **resamples** the curve onto
 * whatever band count `Equalizer.getNumberOfBands()` reports (commonly 5),
 * so the audible shape is preserved regardless of hardware.
 *
 * Millibels rather than dB to match the underlying [android.media.audiofx.Equalizer] API
 * exactly — no rounding at the boundary.
 */
data class EqPreset(
    val name: String,
    val gainsMillibels: List<Int>,
) {
    init {
        require(gainsMillibels.isNotEmpty()) { "Preset must have at least one band gain" }
    }
}

/**
 * Built-in presets keyed by canonical genre. Auto mode picks from this map;
 * Manual mode lets the user override into any of these (or any flat slice).
 *
 * Numbers are conservative — peaks of ~+5 dB, dips of ~-2 dB. Stronger presets
 * would clip on devices with restrictive `getBandLevelRange()` limits, and
 * subtle is the safer default for an "auto" mode the user didn't ask for
 * explicitly.
 */
object BuiltInPresets {

    // 10-band curves. Index → centre frequency:
    //  0:31  1:62  2:125  3:250  4:500 Hz | 5:1k  6:2k  7:4k  8:8k  9:16k Hz

    val FLAT = EqPreset(
        name = "Default",
        gainsMillibels = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    )

    val ROCK = EqPreset(
        name = "Rock",
        gainsMillibels = listOf(400, 300, 150, 0, -100, -100, 0, 150, 300, 400),
    )

    val POP = EqPreset(
        name = "Pop",
        gainsMillibels = listOf(-100, 0, 100, 250, 400, 400, 250, 150, 200, 300),
    )

    val ELECTRONIC = EqPreset(
        name = "Electronic",
        gainsMillibels = listOf(500, 400, 300, 100, 0, -100, 0, 200, 400, 500),
    )

    val JAZZ = EqPreset(
        name = "Jazz",
        gainsMillibels = listOf(300, 250, 150, 100, -50, -100, -50, 100, 200, 300),
    )

    val CLASSICAL = EqPreset(
        name = "Classical",
        gainsMillibels = listOf(400, 300, 200, 0, -100, -100, -50, 150, 300, 400),
    )

    val HIPHOP = EqPreset(
        name = "Hip-Hop",
        gainsMillibels = listOf(500, 500, 400, 200, 100, -100, 0, 150, 250, 350),
    )

    /** Lookup used by [com.offlineplaya.shared.domain.usecase.PickPresetForTrack]. */
    val byGenre: Map<CanonicalGenre, EqPreset> = mapOf(
        CanonicalGenre.ROCK to ROCK,
        CanonicalGenre.POP to POP,
        CanonicalGenre.ELECTRONIC to ELECTRONIC,
        CanonicalGenre.JAZZ to JAZZ,
        CanonicalGenre.CLASSICAL to CLASSICAL,
        CanonicalGenre.HIPHOP to HIPHOP,
        CanonicalGenre.DEFAULT to FLAT,
    )

    /** Stable iteration order for the UI preset chooser. */
    val all: List<EqPreset> = listOf(FLAT, ROCK, POP, ELECTRONIC, JAZZ, CLASSICAL, HIPHOP)
}
