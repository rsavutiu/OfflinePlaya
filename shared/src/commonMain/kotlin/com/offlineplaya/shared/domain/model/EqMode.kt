package com.offlineplaya.shared.domain.model

/**
 * Equalizer engagement mode.
 *
 * - [OFF] — our EQ is disabled; the system-EQ broadcast handoff is restored,
 *   so OEM effects (Dolby, Spatial Audio, manufacturer EQ) take over again.
 * - [MANUAL] — user picked a named preset (or tuned the sliders themselves).
 * - [AUTO] — preset is picked from the currently-playing track's
 *   [CanonicalGenre]; flips in real time on track change.
 */
enum class EqMode(val dbValue: String) {
    OFF("off"),
    MANUAL("manual"),
    AUTO("auto");

    companion object {
        fun fromDbValue(raw: String?): EqMode =
            entries.firstOrNull { it.dbValue == raw } ?: OFF
    }
}
