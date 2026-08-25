package com.offlineplaya.shared.presentation.lyrics

import com.offlineplaya.shared.domain.lyrics.LyricLine
import com.offlineplaya.shared.domain.lyrics.Lyrics
import com.offlineplaya.shared.domain.lyrics.LyricsCandidate
import com.offlineplaya.shared.domain.lyrics.LyricsRepository
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.MusicPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * UI-facing lyrics state. Resolves lyrics for the currently-playing track via
 * [LyricsRepository], then — for synced lyrics — tracks playback position to
 * expose the active line. Mirrors the collector-in-init pattern of
 * [com.offlineplaya.shared.presentation.eq.EqualizerStateHolder].
 *
 * One hot [StateFlow] so both the Lyrics page and the tap-to-flip view on Now
 * Playing observe the same source.
 */
class LyricsStateHolder(
    private val musicPlayer: MusicPlayer,
    private val repository: LyricsRepository,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow<LyricsUiState>(LyricsUiState.None)
    val state: StateFlow<LyricsUiState> = _state.asStateFlow()

    /** The "pick from matches" overlay state (hidden until the user opens it). */
    private val _picker = MutableStateFlow<LyricsPickerState>(LyricsPickerState.Hidden)
    val picker: StateFlow<LyricsPickerState> = _picker.asStateFlow()

    // Latest resolved track, so openPicker/choose have a subject without
    // re-reading the player flow. Written only from the collector below.
    private var currentTrack: Track? = null

    // Bumped by [choose] to force the main collector to re-resolve the current
    // track from cache (which now holds the user's pick) without waiting for a
    // track change.
    private val refresh = MutableStateFlow(0)

    init {
        scope.launch {
            combine(
                musicPlayer.playbackState
                    .map { it.currentTrack }
                    // Only re-resolve when the *track* changes, not on every
                    // position tick (those would otherwise trigger a fresh
                    // embedded-tag read every 500 ms).
                    .distinctUntilChanged { a, b -> a?.id == b?.id },
                refresh,
            ) { track, _ -> track }
                .collectLatest { track ->
                    currentTrack = track
                    if (track == null) {
                        _state.value = LyricsUiState.None
                        return@collectLatest
                    }
                    _state.value = LyricsUiState.Loading
                    // A failed lookup (network error, tag read) must not tear
                    // down the whole collector — fall back to "no lyrics" so the
                    // next track still resolves. Cancellation (track changed
                    // mid-flight) still propagates to honour collectLatest.
                    val lyrics = try {
                        repository.lyricsFor(track)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Lyrics.None
                    }
                    when (lyrics) {
                        Lyrics.None -> _state.value = LyricsUiState.None
                        is Lyrics.Plain -> _state.value = LyricsUiState.Plain(lyrics.text)
                        is Lyrics.Synced -> {
                            // Drive the active line off position. collectLatest
                            // above cancels this inner collect on track change.
                            musicPlayer.playbackState
                                .map { it.positionMs }
                                .distinctUntilChanged()
                                .collect { positionMs ->
                                    _state.value = LyricsUiState.Synced(
                                        lines = lyrics.lines,
                                        activeIndex = activeLineIndex(lyrics.lines, positionMs),
                                    )
                                }
                        }
                    }
                }
        }
    }

    /** Seek playback to the start of [line] (tap-a-line-to-seek). */
    fun seekToLine(line: LyricLine) {
        musicPlayer.seekTo(line.timeMs.coerceAtLeast(0L))
    }

    /**
     * Open the "pick from matches" list for the current track, fetching remote
     * candidates. No-op when nothing is playing. A track change while loading
     * discards the stale result.
     */
    fun openPicker() {
        val track = currentTrack ?: return
        _picker.value = LyricsPickerState.Loading
        scope.launch {
            val candidates = try {
                repository.candidatesFor(track)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            if (currentTrack?.id != track.id) return@launch
            _picker.value = if (candidates.isEmpty()) {
                LyricsPickerState.Empty
            } else {
                LyricsPickerState.Loaded(candidates)
            }
        }
    }

    /**
     * Persist [candidate] as the chosen lyrics for the current track and close
     * the picker. The main lyrics view re-resolves from cache (the pick) via
     * [refresh], so the selection shows immediately.
     */
    fun choose(candidate: LyricsCandidate) {
        val track = currentTrack ?: return
        scope.launch {
            try {
                repository.selectCandidate(track, candidate)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Swallow — the picker still closes; the old lyrics remain.
            }
            _picker.value = LyricsPickerState.Hidden
            refresh.value += 1
        }
    }

    /** Close the picker without changing the current lyrics. */
    fun dismissPicker() {
        _picker.value = LyricsPickerState.Hidden
    }
}

/** State of the "pick from matches" overlay. */
sealed interface LyricsPickerState {
    /** Not shown. */
    data object Hidden : LyricsPickerState

    /** Fetching candidates. */
    data object Loading : LyricsPickerState

    /** Fetch completed with no matches. */
    data object Empty : LyricsPickerState

    /** Candidates ready for the user to choose from (best match first). */
    data class Loaded(val candidates: List<LyricsCandidate>) : LyricsPickerState
}

/** Rendering states for the lyrics surfaces. */
sealed interface LyricsUiState {
    /** Resolving (network or tag read in flight). */
    data object Loading : LyricsUiState

    /** Nothing found anywhere. */
    data object None : LyricsUiState

    /** Unsynced text — shown as a static scroll. */
    data class Plain(val text: String) : LyricsUiState

    /**
     * Synced lyrics. [activeIndex] is the index of the current line, or `-1`
     * before the first line's timestamp.
     */
    data class Synced(val lines: List<LyricLine>, val activeIndex: Int) : LyricsUiState
}

/**
 * Index of the last line whose [LyricLine.timeMs] is `<= positionMs`, or `-1`
 * when [positionMs] precedes the first line. Binary search; [lines] must be
 * sorted ascending by time (the parser guarantees this).
 */
internal fun activeLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
    var lo = 0
    var hi = lines.size - 1
    var ans = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (lines[mid].timeMs <= positionMs) {
            ans = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return ans
}
