package com.offlineplaya.shared.domain.model

/**
 * User-controlled artwork behaviour. Persisted in the Setting table.
 *
 * @property downloadRemoteArt — when `true`, the Coil chain falls back to
 *   MusicBrainz + Cover Art Archive whenever a track has no embedded art.
 *   Default `true`.
 * @property embedDownloadedArt — when `true`, art fetched from the network
 *   is written back into the audio file (requires write access to the SAF
 *   tree). Default `true` and gated on [downloadRemoteArt] being on too,
 *   because there's nothing to embed otherwise.
 */
data class ArtworkPreferences(
    val downloadRemoteArt: Boolean,
    val embedDownloadedArt: Boolean,
) {
    companion object {
        val Default = ArtworkPreferences(
            downloadRemoteArt = true,
            embedDownloadedArt = true,
        )
    }
}
