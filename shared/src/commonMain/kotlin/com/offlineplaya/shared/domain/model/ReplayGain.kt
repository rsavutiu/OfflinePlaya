package com.offlineplaya.shared.domain.model

import kotlin.math.abs

/** Volume-normalisation mode driven by embedded ReplayGain tags. */
enum class ReplayGainMode(val dbValue: String) {
    OFF("off"),
    TRACK("track"),
    ALBUM("album"),
    ;

    companion object {
        fun fromDbValue(value: String?): ReplayGainMode =
            entries.firstOrNull { it.dbValue == value } ?: OFF
    }
}

/** ReplayGain values read from a file's tags, in dB. Null = tag absent. */
data class ReplayGainTags(
    val trackGainDb: Float? = null,
    val albumGainDb: Float? = null,
) {
    companion object {
        /** Opus R128 tags are relative to -23 LUFS; ReplayGain's reference is -18. */
        private const val R128_TO_REPLAYGAIN_DB = 5f

        /**
         * Collects a tag from raw (key, value) pairs; keys are matched
         * case-insensitively, so `REPLAYGAIN_TRACK_GAIN` / `replaygain_track_gain` both work.
         */
        fun fromTags(entries: List<Pair<String, String>>): ReplayGainTags {
            fun find(key: String): String? =
                entries.firstOrNull { it.first.equals(key, ignoreCase = true) }?.second

            fun r128(key: String): Float? =
                find(key)?.trim()?.toIntOrNull()?.let { it / 256f + R128_TO_REPLAYGAIN_DB }

            return ReplayGainTags(
                trackGainDb = parseGainDb(find("replaygain_track_gain")) ?: r128("r128_track_gain"),
                albumGainDb = parseGainDb(find("replaygain_album_gain")) ?: r128("r128_album_gain"),
            )
        }

        /** Parses "-6.48 dB", "+3.2dB" or "-6.48"; null if not a finite number. */
        fun parseGainDb(raw: String?): Float? {
            val cleaned = raw?.trim()?.removeSuffix("dB")?.removeSuffix("DB")?.trim()
                ?: return null
            val value = cleaned.toFloatOrNull() ?: return null
            return value.takeIf { it.isFinite() }
        }
    }
}

object ReplayGain {
    /** Ignore absurd tag values (broken taggers) beyond this magnitude. */
    const val MAX_ABS_GAIN_DB = 24f

    /**
     * The gain to apply, in millibels, for [mode]. Track mode falls back to the
     * album gain and vice versa; no tag at all means no change (0).
     */
    fun gainMillibels(mode: ReplayGainMode, tags: ReplayGainTags?): Int {
        if (tags == null) return 0
        val db = when (mode) {
            ReplayGainMode.OFF -> return 0
            ReplayGainMode.TRACK -> tags.trackGainDb ?: tags.albumGainDb
            ReplayGainMode.ALBUM -> tags.albumGainDb ?: tags.trackGainDb
        } ?: return 0
        if (abs(db) > MAX_ABS_GAIN_DB) return 0
        return (db * 100f).toInt()
    }
}
