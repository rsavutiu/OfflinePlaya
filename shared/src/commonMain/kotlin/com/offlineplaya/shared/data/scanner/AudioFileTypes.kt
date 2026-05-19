package com.offlineplaya.shared.data.scanner

/**
 * Decides whether a SAF document is audio. Pure Kotlin — no Android imports —
 * so it's testable in `commonTest`. The rule:
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
        if (mimeType?.startsWith("audio/") == true) return true
        if (mimeType != null && isKnownNonAudio(mimeType)) return false
        return hasAudioExtension(fileName)
    }

    private fun isKnownNonAudio(mimeType: String): Boolean =
        NON_AUDIO_MIME_PREFIXES.any { mimeType.startsWith(it) } ||
            mimeType in NON_AUDIO_APPLICATION_MIMES

    private fun hasAudioExtension(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return ext in AUDIO_EXTENSIONS
    }
}
