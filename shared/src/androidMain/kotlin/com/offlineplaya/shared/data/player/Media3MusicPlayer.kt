package com.offlineplaya.shared.data.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.MusicPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * In-process [MusicPlayer] backed by a [MediaController] bound to
 * [com.offlineplaya.android.service.PlaybackService].
 *
 * Connecting the controller is async — until the binding completes,
 * [playbackState] sits at [PlaybackState.Empty] and transport methods are
 * no-ops. After connection, a [Player.Listener] keeps the state flow in
 * sync, and a background tick polls position every 500 ms while playing.
 *
 * The class keeps a private list of [Track] objects parallel to the
 * controller's `MediaItem` timeline so the UI can map back without
 * round-tripping through the database.
 */
internal class Media3MusicPlayer(
    private val context: Context,
    private val sessionServiceComponent: ComponentName,
    private val scope: CoroutineScope,
) : MusicPlayer {

    private val _playbackState = MutableStateFlow(PlaybackState.Empty)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    /** Parallel to controller.mediaItems; same indices. */
    private var queueTracks: List<Track> = emptyList()
    private var controller: MediaController? = null
    private var positionTicker: Job? = null

    /**
     * Main-thread bound. Media3 controller callbacks fire on the Looper that
     * built the controller; we use the main looper.
     */
    private val mainDispatcher = android.os.Looper.getMainLooper().let { looper ->
        android.os.Handler(looper).asCoroutineDispatcher("PlayerMain")
    }

    init {
        scope.launch { connect() }
    }

    private suspend fun connect() = withContext(mainDispatcher) {
        val token = SessionToken(context, sessionServiceComponent)
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            { onControllerReady(future.get()) },
            { runnable -> android.os.Handler(android.os.Looper.getMainLooper()).post(runnable) },
        )
    }

    private fun onControllerReady(ctrl: MediaController) {
        controller = ctrl
        ctrl.addListener(playerListener)
        refreshState()
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refreshState()
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                if (player.isPlaying) startPositionTicker() else stopPositionTicker()
            }
        }
    }

    // --- transport ---

    override fun play() = onMain {
        if (controller?.mediaItemCount == 0) return@onMain
        controller?.play()
    }

    override fun pause() = onMain { controller?.pause() }
    override fun stop() = onMain { controller?.stop() }
    override fun seekTo(positionMs: Long) = onMain {
        controller?.seekTo(positionMs)
        refreshState()
    }
    override fun skipToNext() = onMain { controller?.seekToNext() }
    override fun skipToPrevious() = onMain { controller?.seekToPrevious() }

    override fun seekToIndex(index: Int) = onMain {
        val ctrl = controller ?: return@onMain
        if (index !in 0 until ctrl.mediaItemCount) return@onMain
        ctrl.seekTo(index, /* positionMs = */ 0L)
        ctrl.playWhenReady = true
    }

    // --- queue ---

    override fun setQueue(tracks: List<Track>, startIndex: Int) {
        // Building the MediaItem list is heavy: every track triggers
        // file.exists() + mkdirs() + six grantUriPermission() binder calls
        // inside artworkUriFor. For a 1300-track queue that's ~8 000 binder
        // round-trips, which froze the UI for several seconds when the
        // mapping ran on the main thread. Compute the items off-main and
        // submit only the cheap controller calls inside [onMain].
        scope.launch {
            val items = withContext(Dispatchers.IO) {
                tracks.map { TrackMediaItemMapper.toMediaItem(it, artworkUriFor(it)) }
            }
            withContext(mainDispatcher) {
                val ctrl = controller ?: return@withContext
                queueTracks = tracks
                val safeIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
                ctrl.setMediaItems(items, safeIndex, /* startPositionMs = */ 0L)
                ctrl.prepare()
                ctrl.playWhenReady = true
            }
        }
    }

    override fun addToQueue(track: Track) = onMain {
        val ctrl = controller ?: return@onMain
        queueTracks = queueTracks + track
        ctrl.addMediaItem(TrackMediaItemMapper.toMediaItem(track, artworkUriFor(track)))
    }

    override fun addNext(track: Track) = onMain {
        val ctrl = controller ?: return@onMain
        val insertAt = (ctrl.currentMediaItemIndex + 1).coerceAtLeast(0)
        queueTracks = queueTracks.toMutableList().also { it.add(insertAt, track) }
        ctrl.addMediaItem(insertAt, TrackMediaItemMapper.toMediaItem(track, artworkUriFor(track)))
    }

    /**
     * `content://` URI for [track]'s cached art, routed through
     * [com.offlineplaya.shared.data.image.TrackArtCache] so the cache-key
     * convention stays consistent with the Coil fetcher and the Auto
     * FileProvider. Null when the file isn't on disk yet; downstream
     * surfaces (lock screen, Auto, Bluetooth) render placeholders.
     */
    private fun artworkUriFor(track: Track): android.net.Uri? =
        com.offlineplaya.shared.data.image.TrackArtCache.uriForTrack(context, track)

    override fun removeFromQueue(index: Int) = onMain {
        val ctrl = controller ?: return@onMain
        if (index !in queueTracks.indices) return@onMain
        queueTracks = queueTracks.toMutableList().also { it.removeAt(index) }
        ctrl.removeMediaItem(index)
    }

    override fun moveInQueue(from: Int, to: Int) = onMain {
        val ctrl = controller ?: return@onMain
        if (from !in queueTracks.indices || to !in queueTracks.indices) return@onMain
        queueTracks = queueTracks.toMutableList().also {
            val item = it.removeAt(from)
            it.add(to, item)
        }
        ctrl.moveMediaItem(from, to)
    }

    override fun clearQueue() = onMain {
        controller?.clearMediaItems()
        queueTracks = emptyList()
    }

    // --- mode ---

    override fun setShuffleEnabled(enabled: Boolean) = onMain {
        controller?.shuffleModeEnabled = enabled
    }

    override fun setRepeatMode(mode: RepeatMode) = onMain {
        controller?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    override fun setVolume(volume: Float) = onMain {
        controller?.volume = volume.coerceIn(0f, 1f)
    }

    // --- state plumbing ---

    private fun refreshState() {
        val ctrl = controller ?: return
        val index = ctrl.currentMediaItemIndex
        val currentTrack = queueTracks.getOrNull(index)
        val mappedRepeat = when (ctrl.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }
        // Drive the UI play/pause icon from play *intent*, not raw isPlaying.
        // ExoPlayer.isPlaying is false during STATE_BUFFERING — so skipping to
        // the next track (which re-buffers for 1-2s) would otherwise flip the
        // toggle play -> pause -> play. playWhenReady stays true across the
        // buffer, so the icon holds steady; only a genuine pause or the end of
        // the queue (no repeat) clears it.
        val playIntent = ctrl.playWhenReady &&
            ctrl.playbackState != Player.STATE_ENDED &&
            ctrl.playbackState != Player.STATE_IDLE
        _playbackState.value = PlaybackState(
            currentTrack = currentTrack,
            isPlaying = playIntent,
            positionMs = ctrl.currentPosition.coerceAtLeast(0L),
            durationMs = ctrl.duration.takeIf { it > 0 } ?: (currentTrack?.durationMs ?: 0L),
            shuffleEnabled = ctrl.shuffleModeEnabled,
            repeatMode = mappedRepeat,
            queue = queueTracks,
            queueIndex = index.takeIf { queueTracks.isNotEmpty() } ?: -1,
            volume = ctrl.volume,
        )
    }

    private fun startPositionTicker() {
        stopPositionTicker()
        positionTicker = scope.launch(mainDispatcher) {
            while (true) {
                refreshState()
                delay(500L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTicker?.cancel()
        positionTicker = null
    }

    private fun onMain(block: () -> Unit) {
        scope.launch(mainDispatcher) { block() }
    }
}
