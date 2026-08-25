package com.offlineplaya.shared.domain.lyrics

/**
 * Strips reissue / edition "noise" from track titles and album names so a
 * remote lyrics lookup can fall back to the canonical name a crowdsourced
 * database (LRCLIB) actually stores the lyrics under.
 *
 * The motivating case: a file tagged `Hey Jude (2014 Remaster)` on the album
 * `1 (2015 Version)` will never exact-match LRCLIB's `Hey Jude` / `1`. We keep
 * the original tags for the *first* lookup (they're occasionally the more
 * precise match) and only reach for the cleaned form when that misses.
 *
 * What counts as noise: a trailing bracketed group — `(...)` or `[...]` — or a
 * trailing ` - ...` dash suffix whose text contains a remaster/edition/version
 * keyword. We intentionally do NOT strip groups without a keyword (e.g.
 * `(feat. Someone)`, `(Reprise)`, `(Part 1)`) — those can be load-bearing parts
 * of the real title.
 */
object LyricsTitleNormalizer {

    // Keywords that mark a bracket/dash group as a reissue tag rather than part
    // of the song's actual name. Matched case-insensitively as whole words.
    private val NOISE = Regex(
        "(?i)\\b(" +
            "re-?master(ed)?|" +
            "remaster|" +
            "mono|stereo|" +
            "deluxe|expanded|super deluxe|" +
            "anniversary|reissue|" +
            "bonus(\\s+track)?|" +
            "single version|album version|" +
            "radio edit|radio version|" +
            "mono version|stereo version|" +
            "digital remaster|" +
            "re-?record(ed|ing)?|" +
            "version|edition" +
            ")\\b",
    )

    // A bracketed group hanging off the end of the string: "... (2014 Remaster)".
    private val TRAILING_BRACKET = Regex("\\s*[(\\[][^()\\[\\]]*[)\\]]\\s*$")

    // A dash-delimited suffix hanging off the end: "... - 2014 Remaster".
    // Accepts hyphen, en dash, and em dash as the delimiter.
    private val TRAILING_DASH = Regex("\\s+[-–—]\\s+[^-–—]+$")

    // A full URL or bare "handle.tld/path" spam token — the calling card of
    // scene rips, e.g. "Fire Rides vk.com/xclusives_zone" or "Song https://t.me/x".
    // TLD list is deliberately narrow (common spam hosts) so we don't clip a
    // legitimate title that happens to contain a dotted word.
    private val URL_SPAM = Regex("(?i)\\s*(?:https?://|www\\.)\\S+")
    private val DOMAIN_SPAM = Regex(
        "(?i)\\s*[\\w-]+\\.(?:com|net|org|ru|io|me|co|info|biz|tv|fm|xyz|to|cc|club|online|site)\\b(?:/\\S*)?",
    )

    // Any trailing bracket group, keyword or not — the desperate last pass only.
    private val ANY_TRAILING_BRACKET = Regex("\\s*[(\\[][^()\\[\\]]*[)\\]]\\s*$")

    // A leading track-number prefix: "01. ", "06 - ", "3) ". Requires a real
    // separator (. - )) followed by whitespace so we don't clip titles that
    // genuinely start with a number ("99 Luftballons", "7 rings", "3.14").
    private val TRACK_NUMBER_PREFIX = Regex("^\\s*\\d{1,3}\\s*[-.)]\\s+")

    // A trailing featuring credit, anchored on an explicit feat./ft./featuring
    // marker so band names with "&"/"," survive. Mirrors LibrarySyncUseCase.
    private val FEAT_CREDIT = Regex(
        "(?i)[\\s(\\[]+(?:feat\\.?|ft\\.?|featuring)\\s.*",
    )

    /**
     * Returns [raw] with trailing reissue/edition noise removed. If nothing was
     * noise the input is returned trimmed but otherwise unchanged. Never returns
     * an empty string — if stripping would empty the value, the trimmed original
     * is returned instead (a title made *entirely* of a bracket group is almost
     * certainly not noise).
     */
    fun normalize(raw: String): String {
        var s = raw.trim()

        // Peel trailing bracket groups first, repeatedly — some tags stack them,
        // e.g. "Song (Live) (Remastered)".
        while (true) {
            val match = TRAILING_BRACKET.find(s) ?: break
            if (!NOISE.containsMatchIn(match.value)) break
            val stripped = s.removeRange(match.range).trim()
            if (stripped.isEmpty()) break
            s = stripped
        }

        // Then a single trailing dash suffix, if it reads as a reissue tag.
        val dash = TRAILING_DASH.find(s)
        if (dash != null && NOISE.containsMatchIn(dash.value)) {
            val stripped = s.removeRange(dash.range).trim()
            if (stripped.isNotEmpty()) s = stripped
        }

        return s.ifEmpty { raw.trim() }
    }

    /**
     * Remove URL / domain spam tokens anywhere in [raw] (scene-rip calling
     * cards like `vk.com/xclusives_zone`, `https://t.me/x`). Collapses the
     * whitespace left behind. Falls back to the trimmed original if stripping
     * would empty the string.
     */
    fun stripSpam(raw: String): String {
        val s = raw
            .replace(URL_SPAM, " ")
            .replace(DOMAIN_SPAM, " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
        return s.ifEmpty { raw.trim() }
    }

    /**
     * Strip a leading track-number prefix: `06 - The Beautiful American` →
     * `The Beautiful American`, `01. Fire Rides` → `Fire Rides`. Only fires
     * with a real separator so numeric titles ("99 Luftballons", "7 rings")
     * are left alone. Falls back to the trimmed original if it would empty out.
     */
    fun stripLeadingTrackNumber(raw: String): String {
        val s = raw.trim().replaceFirst(TRACK_NUMBER_PREFIX, "").trim()
        return s.ifEmpty { raw.trim() }
    }

    /**
     * Remove a trailing featuring credit: `Ms. Jackson (feat. Killer Mike)` →
     * `Ms. Jackson`. Only used in the aggressive fallback chain — the primary
     * lookup keeps the credit, since LRCLIB sometimes files lyrics under it.
     */
    fun stripCredits(raw: String): String {
        val s = raw.replace(FEAT_CREDIT, "").trim()
        return s.ifEmpty { raw.trim() }
    }

    /**
     * Most aggressive title reduction: peel every trailing bracket group and a
     * trailing dash suffix, keyword or not — `Song (Live) (Remix) - 2011` →
     * `Song`. The last resort, guarded downstream by LRCLIB's duration filter
     * so a wrong recording still gets rejected.
     */
    fun core(raw: String): String {
        var s = raw.trim()
        while (true) {
            val m = ANY_TRAILING_BRACKET.find(s) ?: break
            val stripped = s.removeRange(m.range).trim()
            if (stripped.isEmpty()) break
            s = stripped
        }
        val dash = TRAILING_DASH.find(s)
        if (dash != null) {
            val stripped = s.removeRange(dash.range).trim()
            if (stripped.isNotEmpty()) s = stripped
        }
        return s.ifEmpty { raw.trim() }
    }

    /**
     * Ordered, de-duplicated list of `(album, title)` query pairs to try, from
     * the most faithful to the most aggressive. Escalation only *costs*
     * anything on a miss — the caller returns on the first hit, and a clean
     * title collapses the whole chain down to one or two pairs.
     *
     * The steps, in order:
     *  1. raw tags (occasionally the most precise match);
     *  2. reissue/edition de-noised ([normalize]);
     *  3. + URL/domain spam removed ([stripSpam]);
     *  4. + featuring credit removed ([stripCredits]);
     *  5. album dropped entirely — an album-tag mismatch is a top reason the
     *     exact `/get` 404s;
     *  6. album dropped + title reduced to its [core].
     *
     * Empty-title pairs are never emitted.
     */
    fun variants(album: String, title: String): List<Pair<String, String>> {
        val rawA = album.trim()
        val rawT = title.trim()

        val denA = normalize(rawA)
        val denT = normalize(rawT)
        val spamA = stripSpam(denA)
        // Also drop a leading track number ("06 - Title") — it's never part of
        // the name a lyrics DB files the song under.
        val spamT = stripLeadingTrackNumber(stripSpam(denT))
        val featT = stripCredits(spamT)
        val coreT = core(featT)

        val ordered = listOf(
            rawA to rawT,
            denA to denT,
            spamA to spamT,
            spamA to featT,
            "" to featT,
            "" to coreT,
        )
        // LinkedHashSet dedups while preserving first-seen order.
        return ordered.filter { it.second.isNotEmpty() }.toCollection(LinkedHashSet()).toList()
    }
}
