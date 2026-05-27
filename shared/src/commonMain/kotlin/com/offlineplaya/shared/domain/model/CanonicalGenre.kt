package com.offlineplaya.shared.domain.model

/**
 * Small fixed bucket of genres that the equalizer's Auto mode reasons over.
 * Inferred from the freeform tag string by [com.offlineplaya.shared.domain.usecase.GenreClassifier].
 *
 * The set is deliberately tiny — every additional bucket needs a hand-tuned
 * preset, and most tag strings collapse to one of these without losing
 * audible distinction.
 */
enum class CanonicalGenre {
    ROCK,
    POP,
    ELECTRONIC,
    JAZZ,
    CLASSICAL,
    HIPHOP,

    /** Catch-all: unknown / untagged / "Other" → flat preset. */
    DEFAULT;

    companion object {
        fun fromDbValue(raw: String?): CanonicalGenre =
            raw?.let { runCatching { valueOf(it) }.getOrNull() } ?: DEFAULT
    }
}
