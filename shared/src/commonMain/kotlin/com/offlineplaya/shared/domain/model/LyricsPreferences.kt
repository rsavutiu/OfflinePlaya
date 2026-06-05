package com.offlineplaya.shared.domain.model

/**
 * User-controlled lyrics behaviour. Persisted in the Setting table.
 *
 * @property downloadRemoteLyrics — when `true`, the lyrics resolver falls
 *   back to LRCLIB whenever no embedded tag and no `.lrc`/`.txt` sidecar
 *   is found. Default `true`, mirroring [ArtworkPreferences.downloadRemoteArt].
 * @property saveLyricsAsSidecar — when `true`, lyrics fetched from LRCLIB
 *   are written out as `<basename>.lrc` (or `.txt` for unsynced) next to
 *   the audio file using the SAF tree. Lets the sidecar survive a cache
 *   wipe / reinstall and become visible to other players. Gated on
 *   [downloadRemoteLyrics] being on too — there's nothing to save
 *   otherwise. Default `true`, mirroring [ArtworkPreferences.embedDownloadedArt].
 */
data class LyricsPreferences(
    val downloadRemoteLyrics: Boolean,
    val saveLyricsAsSidecar: Boolean,
) {
    companion object {
        val Default = LyricsPreferences(
            downloadRemoteLyrics = true,
            saveLyricsAsSidecar = true,
        )
    }
}
