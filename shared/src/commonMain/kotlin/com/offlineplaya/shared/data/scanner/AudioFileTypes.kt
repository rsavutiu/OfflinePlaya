package com.offlineplaya.shared.data.scanner

/**
 * Decides whether a SAF document is audio. Pure Kotlin — no Android imports —
 * so it's testable in `commonTest`. The rule:
 *  - If the extension is a known playlist / cue-sheet format (`.m3u`,
 *    `.pls`, `.cue`, …) reject *unconditionally*, even when MIME says
 *    `audio/x-mpegurl`. These files describe audio but aren't playable on
 *    their own — including them in the library inflates track counts and
 *    surfaces unplayable rows.
 *  - If MIME starts with `audio/`, accept.
 *  - If MIME is clearly non-audio (`image/`, `video/`, `text/`,
 *    `application/pdf` …) reject without consulting the extension. SAF
 *    providers sometimes report `application/octet-stream` for unknown types,
 *    which falls through to the extension check.
 *  - Otherwise check the extension against a known audio set.
 */
internal object AudioFileTypes {

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "ogg", "oga", "opus",
        "m4a", "m4b", "mp4a", "aac",
        "wav", "aif", "aiff",
        "wma", "ape", "wv", "alac",
        "mka", "dsf", "dff",
    )

    /**
     * Playlist / cue-sheet container formats. SAF providers often hand these
     * back with `audio/x-mpegurl` or `audio/x-scpls` MIME types — which is
     * technically correct but useless for a player that wants to enumerate
     * playable files.
     */
    private val PLAYLIST_EXTENSIONS = setOf(
        "m3u", "m3u8", "pls", "cue", "asx", "xspf", "wpl",
    )

    private val NON_AUDIO_MIME_PREFIXES = listOf(
        "image/",
        "video/",
        "text/",
    )

    private val NON_AUDIO_APPLICATION_MIMES = setOf(
        "application/pdf",
        "application/zip",
        "application/x-rar-compressed",
        "application/x-7z-compressed",
        "application/x-tar",
        "application/json",
        "application/xml",
    )

    fun isAudioFile(fileName: String, mimeType: String?): Boolean {
        val ext = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        if (ext in PLAYLIST_EXTENSIONS) return false
        if (mimeType?.startsWith("audio/") == true) return true
        if (mimeType != null && isKnownNonAudio(mimeType)) return false
        return ext in AUDIO_EXTENSIONS
    }

    private fun isKnownNonAudio(mimeType: String): Boolean =
        NON_AUDIO_MIME_PREFIXES.any { mimeType.startsWith(it) } ||
            mimeType in NON_AUDIO_APPLICATION_MIMES
}
