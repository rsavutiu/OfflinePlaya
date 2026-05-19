package com.offlineplaya.shared.domain.scanner

/**
 * Reads tag metadata from a single audio file given its SAF document URI.
 *
 * Implementations:
 *  - `androidMain`: Media3 `MediaMetadataRetriever` primary; jaudiotagger
 *    fallback for formats with extended tags (APE, WMA, etc).
 *  - Tests: [FakeMetadataReader].
 *
 * Implementations MUST NOT throw on missing files or malformed metadata;
 * return `null` instead so the use case can mark the track as errored.
 */
interface MetadataReader {
    suspend fun read(documentUri: String): AudioMetadata?
}
