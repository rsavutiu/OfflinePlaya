package com.offlineplaya.shared.domain.usecase

import com.offlineplaya.shared.domain.model.CanonicalGenre

/**
 * Pure function that buckets a freeform genre tag string into one of the
 * fixed [CanonicalGenre] values. The bucket selection drives equalizer
 * Auto mode and (eventually) anything else that wants to reason about genre
 * without coping with hundreds of spelling variants.
 *
 * Rules (in priority order):
 *  1. Numeric ID3v1 codes — `(17)`, `17`, `(17)Rock` — get expanded via
 *     [ID3V1_GENRES] before keyword matching. Common in old MP3s.
 *  2. The raw string is lowercased and stripped of separators. The FIRST
 *     keyword that matches as a substring wins — order matters in
 *     [KEYWORDS] (e.g. "rock and roll" must match ROCK, not be missed
 *     because "roll" isn't a keyword).
 *  3. Multi-genre tags ("Rock; Pop", "Electronic / Dance") are split on
 *     `;`, `/`, `,`, `|` and the first non-blank segment is classified.
 *     First-wins keeps the rule trivial and matches the user's tagging
 *     intent — primary genre is usually first.
 *  4. No match → [CanonicalGenre.DEFAULT].
 */
object GenreClassifier {

    fun classify(raw: String?): CanonicalGenre {
        if (raw.isNullOrBlank()) return CanonicalGenre.DEFAULT

        // Take only the first segment when the tag is multi-genre.
        val firstSegment = raw.split(';', '/', ',', '|').firstOrNull { it.isNotBlank() }
            ?: return CanonicalGenre.DEFAULT

        // Expand numeric ID3v1 codes like "(17)" or "17" before keyword search.
        val expanded = expandId3v1(firstSegment.trim()) ?: firstSegment

        val needle = expanded.lowercase().replace(Regex("[^a-z0-9 &+-]"), " ")

        for ((keyword, genre) in KEYWORDS) {
            if (needle.contains(keyword)) return genre
        }
        return CanonicalGenre.DEFAULT
    }

    /**
     * Parse "(17)" / "17" / "(17)Rock" → ID3v1 name lookup → "Rock". Returns
     * null if the input isn't a recognised numeric code, leaving the caller to
     * keyword-match the original string.
     */
    private fun expandId3v1(segment: String): String? {
        val trimmed = segment.trim()
        // Bare "17" form.
        trimmed.toIntOrNull()?.let { return ID3V1_GENRES.getOrNull(it) }
        // "(17)" or "(17)Rock" form.
        if (trimmed.startsWith("(")) {
            val close = trimmed.indexOf(')')
            if (close > 1) {
                val inner = trimmed.substring(1, close).toIntOrNull()
                if (inner != null) return ID3V1_GENRES.getOrNull(inner)
            }
        }
        return null
    }

    /**
     * Keyword → bucket table, scanned in order. More-specific patterns first
     * so they aren't shadowed by broader ones ("synthwave" → ELECTRONIC, not
     * captured by anything else).
     *
     * "Blues" buckets into JAZZ for v1 since we don't have a dedicated blues
     * preset — the same gentle low-mid emphasis works for both.
     */
    private val KEYWORDS: List<Pair<String, CanonicalGenre>> = listOf(
        // Compound POP terms that overlap with ELECTRONIC keywords — checked
        // first so e.g. "Dance Pop" doesn't get caught by the bare "dance"
        // keyword further down.
        "dance pop" to CanonicalGenre.POP,

        // ELECTRONIC family
        "synthwave" to CanonicalGenre.ELECTRONIC,
        "drum and bass" to CanonicalGenre.ELECTRONIC,
        "drum & bass" to CanonicalGenre.ELECTRONIC,
        "dnb" to CanonicalGenre.ELECTRONIC,
        "dubstep" to CanonicalGenre.ELECTRONIC,
        "techno" to CanonicalGenre.ELECTRONIC,
        "trance" to CanonicalGenre.ELECTRONIC,
        "house" to CanonicalGenre.ELECTRONIC,
        "edm" to CanonicalGenre.ELECTRONIC,
        "idm" to CanonicalGenre.ELECTRONIC,
        "ambient" to CanonicalGenre.ELECTRONIC,
        "electronic" to CanonicalGenre.ELECTRONIC,
        "electronica" to CanonicalGenre.ELECTRONIC,
        "dance" to CanonicalGenre.ELECTRONIC,

        // HIPHOP family
        "hip hop" to CanonicalGenre.HIPHOP,
        "hip-hop" to CanonicalGenre.HIPHOP,
        "hiphop" to CanonicalGenre.HIPHOP,
        "rap" to CanonicalGenre.HIPHOP,
        "trap" to CanonicalGenre.HIPHOP,
        "r&b" to CanonicalGenre.HIPHOP,
        "rnb" to CanonicalGenre.HIPHOP,
        "r and b" to CanonicalGenre.HIPHOP,
        "soul" to CanonicalGenre.HIPHOP,
        "funk" to CanonicalGenre.HIPHOP,

        // CLASSICAL family
        "classical" to CanonicalGenre.CLASSICAL,
        "baroque" to CanonicalGenre.CLASSICAL,
        "romantic" to CanonicalGenre.CLASSICAL,
        "orchestra" to CanonicalGenre.CLASSICAL,
        "orchestral" to CanonicalGenre.CLASSICAL,
        "opera" to CanonicalGenre.CLASSICAL,
        "chamber" to CanonicalGenre.CLASSICAL,
        "symphony" to CanonicalGenre.CLASSICAL,

        // JAZZ family
        "bebop" to CanonicalGenre.JAZZ,
        "swing" to CanonicalGenre.JAZZ,
        "fusion" to CanonicalGenre.JAZZ,
        "jazz" to CanonicalGenre.JAZZ,
        "blues" to CanonicalGenre.JAZZ,

        // POP family
        "k-pop" to CanonicalGenre.POP,
        "j-pop" to CanonicalGenre.POP,
        "kpop" to CanonicalGenre.POP,
        "jpop" to CanonicalGenre.POP,
        "synth pop" to CanonicalGenre.POP,
        "synthpop" to CanonicalGenre.POP,
        "pop" to CanonicalGenre.POP,

        // ROCK family — last because "rock" appears in many compound tags
        "metal" to CanonicalGenre.ROCK,
        "punk" to CanonicalGenre.ROCK,
        "grunge" to CanonicalGenre.ROCK,
        "indie" to CanonicalGenre.ROCK,
        "alternative" to CanonicalGenre.ROCK,
        "alt rock" to CanonicalGenre.ROCK,
        "rock" to CanonicalGenre.ROCK,
    )

    /**
     * Standard ID3v1 genre table. Index = numeric code.
     * https://en.wikipedia.org/wiki/List_of_ID3v1_genres — the first 80 are
     * the original ID3v1 set; Winamp extensions go up to 191. Out-of-range
     * indices return null and fall through to keyword classification.
     */
    private val ID3V1_GENRES: List<String> = listOf(
        "Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge",
        "Hip-Hop", "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B",
        "Rap", "Reggae", "Rock", "Techno", "Industrial", "Alternative", "Ska",
        "Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient", "Trip-Hop",
        "Vocal", "Jazz+Funk", "Fusion", "Trance", "Classical", "Instrumental",
        "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise", "Alternative Rock",
        "Bass", "Soul", "Punk", "Space", "Meditative", "Instrumental Pop",
        "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial",
        "Electronic", "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy",
        "Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle",
        "Native American", "Cabaret", "New Wave", "Psychedelic", "Rave", "Showtunes",
        "Trailer", "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz", "Polka", "Retro",
        "Musical", "Rock & Roll", "Hard Rock",
    )
}
