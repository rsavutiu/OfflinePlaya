package com.offlineplaya.shared.presentation.eq

import com.offlineplaya.shared.domain.model.BuiltInPresets
import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.domain.model.EqMode
import com.offlineplaya.shared.domain.model.EqPreferences
import com.offlineplaya.shared.domain.model.EqPreset
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.MusicPlayer
import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.domain.usecase.BurnMetadataUseCase
import com.offlineplaya.shared.domain.usecase.PickPresetForTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Combines [SettingsRepository]'s [EqPreferences] flow with the currently-playing
 * [Track] and emits the resolved [EqPreset] the audio-effect driver should apply.
 *
 * Holds the projection in a hot [StateFlow] so the UI and the
 * [com.offlineplaya.android.audio.AppEqualizerController] (or any future
 * cross-platform equivalent) can both observe without recomputing.
 *
 * Mutator methods are tiny — every change goes through
 * [SettingsRepository.setEqPreferences] so persistence is automatic and
 * single-sourced.
 */
class EqualizerStateHolder(
    private val settings: SettingsRepository,
    private val musicPlayer: MusicPlayer,
    private val tracks: TrackRepository,
    private val burnMetadata: BurnMetadataUseCase,
    private val scope: CoroutineScope,
) {

    /**
     * Albums we've already tried (or are currently trying) to lookup-and-tag
     * during this process. Keeps EQ Auto from kicking a fresh MusicBrainz
     * request every time the user skips back to the same untagged album.
     * Cleared on process death — a restart re-tries any miss.
     */
    private val attemptedAlbums = mutableSetOf<Pair<String, String>>()
    private val attemptedLock = Mutex()

    /** Live equalizer preferences, defaulted when unset. */
    val preferences: StateFlow<EqPreferences> = settings.observeEqPreferences()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = EqPreferences.Default,
        )

    /**
     * Resolved [EqPreset] given the active mode and the currently-playing
     * track. Re-emits on track change (so AUTO mode picks the right preset)
     * and on every preference change.
     */
    val activePreset: StateFlow<EqPreset> = combine(
        preferences,
        musicPlayer.playbackState.map { it.currentTrack },
    ) { prefs, track ->
        PickPresetForTrack.pick(track, prefs)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = BuiltInPresets.FLAT,
    )

    init {
        // Opportunistic auto-tag while in AUTO mode: when a track lacks a
        // genre tag and we'd otherwise fall back to the flat preset, look up
        // the (artist, album) on MusicBrainz, write the tag into every track
        // on that album, and update the DB. The PickPresetForTrack flow then
        // re-evaluates on the next track-state emission (the DB update kicks
        // observeAll() via SQLDelight, which propagates into the queue) so
        // the user gets the matching curve without manual intervention.
        scope.launch {
            // collectLatest cancels the in-flight tag job whenever the user
            // skips tracks faster than the network — no point finishing a
            // lookup whose result we'll immediately overwrite.
            combine(
                preferences,
                musicPlayer.playbackState.map { it.currentTrack }) { p, t -> p to t }
                .distinctUntilChanged()
                .collectLatest { (prefs, track) -> maybeAutoTag(prefs, track) }
        }
    }

    private suspend fun maybeAutoTag(prefs: EqPreferences, track: Track?) {
        if (prefs.mode != EqMode.AUTO) return
        if (track == null) return
        if (!track.genre.isNullOrBlank()) return
        if (track.canonicalGenre != null && track.canonicalGenre != CanonicalGenre.DEFAULT) return
        val artist = track.albumArtistName ?: track.artistName
        val album = track.albumName
        val key = artist to album
        attemptedLock.withLock {
            if (key in attemptedAlbums) return
            attemptedAlbums.add(key)
        }
        // Fetch the current album's tracks so the use case can write to all
        // of them at once — same MB hit costs every track on the album its
        // tag, not just the one currently playing.
        val albumId = track.albumId
        val tracksOnAlbum = if (albumId != null) {
            runCatching { tracks.observeByAlbum(albumId).first() }.getOrElse { listOf(track) }
        } else {
            listOf(track)
        }
        burnMetadata.tagAlbumOpportunistically(artist, album, tracksOnAlbum)
        // Result reaches the UI via the DB → observeAll → preset flow.
        // Nothing to do here on success; on miss we've recorded the attempt
        // and won't re-fire for the same album this process.
    }

    fun setMode(mode: EqMode) {
        scope.launch {
            val current = preferences.value
            settings.setEqPreferences(current.copy(mode = mode))
        }
    }

    fun setManualPreset(name: String) {
        scope.launch {
            val current = preferences.value
            settings.setEqPreferences(
                current.copy(
                    mode = EqMode.MANUAL,
                    manualPresetName = name,
                    // Picking a fresh preset clears slider-overrides — switching
                    // to "Rock" should give the user the published Rock curve,
                    // not the dragged-Jazz curve still hanging around.
                    manualGains = emptyList(),
                ),
            )
        }
    }

    /**
     * Override a single band's gain. Promotes the mode to MANUAL if needed —
     * dragging a slider in OFF or AUTO mode is interpreted as "I want manual
     * control now". The full gain vector is persisted so re-renders see the
     * complete state, not just the one band that moved.
     */
    fun setBandGain(bandIndex: Int, millibels: Int, fullGains: List<Int>) {
        scope.launch {
            val current = preferences.value
            val newGains = if (bandIndex in fullGains.indices) {
                fullGains.toMutableList().also { it[bandIndex] = millibels }
            } else {
                fullGains
            }
            settings.setEqPreferences(
                current.copy(mode = EqMode.MANUAL, manualGains = newGains),
            )
        }
    }

    /** Reset slider-overrides back to the current preset's published gains. */
    fun resetManualOverrides() {
        scope.launch {
            val current = preferences.value
            settings.setEqPreferences(current.copy(manualGains = emptyList()))
        }
    }
}
