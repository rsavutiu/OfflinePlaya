package com.offlineplaya.shared.presentation.theme

import com.offlineplaya.shared.domain.image.AlbumArtColorExtractor
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Holds the ARGB seed color that drives the album-art reactive theme.
 *
 * Two inputs feed [seedColor]:
 *  1. On construction, the persisted last-played seed is loaded so the app
 *     opens already tinted to the last song instead of flashing the brand
 *     palette until the first track change of the session.
 *  2. Thereafter it follows [MusicPlayer]'s current track — but only
 *     re-extracts when the *cover* changes (keyed by album, falling back to
 *     track id), so skipping between tracks on one album does no redundant
 *     work, and a position tick never triggers extraction.
 *
 * A successful extraction is persisted immediately, so "last song played"
 * survives process death. Lives in `commonMain`; the platform supplies the
 * [AlbumArtColorExtractor] actual (Android: Palette over the Coil art chain).
 */
class AlbumColorStateHolder(
    private val player: MusicPlayer,
    private val extractor: AlbumArtColorExtractor,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val _seedColor = MutableStateFlow<Int?>(null)

    /** Current seed, or `null` before anything has ever played. */
    val seedColor: StateFlow<Int?> = _seedColor.asStateFlow()

    init {
        // 1. Open tinted to the last-played album.
        scope.launch {
            val persisted = settings.getLastSeedColor()
            // Don't clobber a seed already extracted this session if the player
            // beat us to it (cheap guard against the rare init race).
            if (_seedColor.value == null && persisted != null) {
                _seedColor.value = persisted
            }
        }

        // 2. Follow the current track's cover.
        scope.launch {
            player.playbackState
                .map { it.currentTrack }
                .distinctUntilChanged { old, new -> coverKey(old) == coverKey(new) }
                .collect { track ->
                    if (track == null) return@collect
                    // Extraction (Palette over the art chain) and the persist are
                    // both fallible — a corrupt cover or a DB hiccup must not tear
                    // down the collector and freeze album-art theming for the rest
                    // of the session. Keep the current seed; the next cover change
                    // retries. Cancellation still propagates.
                    try {
                        val seed = extractor.seedColor(track) ?: return@collect
                        _seedColor.value = seed
                        settings.setLastSeedColor(seed)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // swallow — leave the existing tint in place.
                    }
                }
        }
    }

    /**
     * Identity of the *art* for a track: two tracks on the same album share a
     * cover (and therefore a seed), so we key on album id and only fall back
     * to the track id for album-less files. `null` track → `null` key.
     */
    private fun coverKey(track: Track?): Long? = track?.let { it.albumId ?: -it.id }
}
