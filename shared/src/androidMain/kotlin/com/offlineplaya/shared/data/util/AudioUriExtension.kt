package com.offlineplaya.shared.data.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap

/**
 * Resolve the file extension for an audio document URI so Jaudiotagger's
 * `AudioFileIO.read()` can dispatch to the right format reader.
 *
 * `AudioFileIO` picks the reader by extension. If we hand it a `.mp3`
 * temp file containing FLAC bytes, it will try the MP3 reader and fail
 * with `CannotReadException`. SAF makes this non-trivial because:
 *
 *  - `ContentResolver.getType()` returns `application/octet-stream` or
 *    `null` for many user-stored FLAC / OGG files, depending on the
 *    DocumentsProvider's MIME database.
 *  - `Uri.lastPathSegment` for a `content://.../document/<encoded-doc-id>`
 *    URI returns the (decoded) doc id, which usually contains the file
 *    name but can be missing the extension after some encoding round-trips.
 *
 * Resolution order, first hit wins:
 *
 *  1. SAF `DISPLAY_NAME` column — always present, always the real file
 *     name including extension.
 *  2. URI last-path-segment after stripping query/fragment.
 *  3. Explicit MIME → extension table for the audio types we care about
 *     (covers `application/octet-stream` cases where the user has set the
 *     MIME by hand).
 *  4. Android's [MimeTypeMap] (works for well-known types on modern API).
 *  5. `"mp3"` as a last resort — the same default that triggered the bug,
 *     kept so callers don't have to handle null.
 */
internal fun resolveAudioExtension(context: Context, uri: Uri): String {
    val resolver = context.contentResolver

    extensionFromDisplayName(resolver, uri)?.let { return it }
    extensionFromUriPath(uri)?.let { return it }

    val mime = runCatching { resolver.getType(uri) }.getOrNull()?.lowercase()
    extensionFromKnownMime(mime)?.let { return it }
    mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }?.let { return it }

    return "mp3"
}

private fun extensionFromDisplayName(resolver: ContentResolver, uri: Uri): String? =
    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()
        ?.substringAfterLast('.', "")
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }

private fun extensionFromUriPath(uri: Uri): String? =
    uri.lastPathSegment
        ?.substringAfterLast('/')        // SAF doc IDs can embed a path
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?.substringAfterLast('.', "")
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }

private fun extensionFromKnownMime(mime: String?): String? = when (mime) {
    "audio/flac", "audio/x-flac" -> "flac"
    "audio/mpeg", "audio/mp3" -> "mp3"
    "audio/mp4", "audio/m4a", "audio/x-m4a", "audio/aac", "audio/aacp" -> "m4a"
    "audio/ogg", "audio/vorbis", "application/ogg" -> "ogg"
    "audio/opus" -> "opus"
    "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
    "audio/x-ms-wma" -> "wma"
    else -> null
}
