package com.offlineplaya.shared.domain.model

/**
 * User-controlled lyrics behaviour. Persisted in the Setting table.
 *
 * @property downloadRemoteLyrics — when `true`, the lyrics resolver falls
 *   back to LRCLIB whenever no embedded tag and no `.lrc`/`.txt` sidecar
 *   is found. Default `true`, mirroring [ArtworkPreferences.downloadRemoteArt].
 */
data class LyricsPreferences(
    val downloadRemoteLyrics: Boolean,
) {
    companion object {
        val Default = LyricsPreferences(
            downloadRemoteLyrics = true,
        )
    }
}
