package com.offlineplaya.shared.testsupport

import com.offlineplaya.shared.domain.scanner.AudioMetadata
import com.offlineplaya.shared.domain.scanner.MetadataReader

/**
 * Scripted [MetadataReader] for tests. Returns the metadata mapped to a URI,
 * or `null` if the URI is in the `failingUris` set (simulating an unreadable
 * file). URIs absent from both sets fall back to [AudioMetadata.Empty].
 */
class FakeMetadataReader(
    private val scripted: Map<String, AudioMetadata> = emptyMap(),
    private val failingUris: Set<String> = emptySet(),
) : MetadataReader {

    override suspend fun read(documentUri: String): AudioMetadata? =
        when {
            documentUri in failingUris -> null
            else -> scripted[documentUri] ?: AudioMetadata.Empty
        }
}
