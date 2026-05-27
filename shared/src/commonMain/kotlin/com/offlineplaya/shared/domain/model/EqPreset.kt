package com.offlineplaya.shared.domain.model

/**
 * A named equalizer preset: a list of per-band gains, in millibels (1/100 dB).
 *
 * The preset is template-shaped — it carries 5 gains by convention because
 * most Android devices report `Equalizer.getNumberOfBands() == 5`, but the
 * controller remaps the values onto whatever band layout the platform
 * actually exposes. Devices with fewer bands take a truncated slice; devices
 * with more bands repeat the last value.
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

    val FLAT = EqPreset(
        name = "Default",
        gainsMillibels = listOf(0, 0, 0, 0, 0),
    )

    val ROCK = EqPreset(
        name = "Rock",
        gainsMillibels = listOf(300, 200, -100, 200, 400),
    )

    val POP = EqPreset(
        name = "Pop",
        gainsMillibels = listOf(200, 400, 200, 300, 200),
    )

    val ELECTRONIC = EqPreset(
        name = "Electronic",
        gainsMillibels = listOf(500, 300, 0, 200, 400),
    )

    val JAZZ = EqPreset(
        name = "Jazz",
        gainsMillibels = listOf(300, 200, -100, 200, 300),
    )

    val CLASSICAL = EqPreset(
        name = "Classical",
        gainsMillibels = listOf(400, 300, -200, 300, 400),
    )

    val HIPHOP = EqPreset(
        name = "Hip-Hop",
        gainsMillibels = listOf(500, 400, 100, -100, 200),
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
