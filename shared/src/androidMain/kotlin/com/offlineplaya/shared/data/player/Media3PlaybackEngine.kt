package com.offlineplaya.shared.data.player

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.EngineState
import com.offlineplaya.shared.domain.player.PlaybackEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Android [PlaybackEngine]: a thin pass-through to a [MediaController] bound
 * to [com.offlineplaya.android.service.PlaybackService]. It holds **no** notion
 * of "the current track" beyond the controller's own index — all the coordination
 * that used to live in the old `Media3MusicPlayer` (the parallel track list,
 * optimistic state, command serialization) now lives in the platform-agnostic,
 * unit-tested [CoordinatingMusicPlayer] on top of this.
 *
 * Connecting the controller is async; the `suspend` apply methods `await` the
 * connection so a command issued before binding still lands once it completes.
 * Heavy `MediaItem` mapping for a new queue runs off the main thread; the cheap
 * controller calls hop back to the main looper the controller was built on.
 */
internal class Media3PlaybackEngine(
    private val context: Context,
    private val sessionServiceComponent: ComponentName,
    private val scope: CoroutineScope,
) : PlaybackEngine {

    private val mainDispatcher = Handler(Looper.getMainLooper())
        .asCoroutineDispatcher("PlayerMain")

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    override val changes = _changes

    private val controllerReady = CompletableDeferred<MediaController>()
    private var controller: MediaController? = null
    private var positionTicker: Job? = null

    init {
        scope.launch { connect() }
    }

    private suspend fun connect() = withContext(mainDispatcher) {
        val token = SessionToken(context, sessionServiceComponent)
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            { onControllerReady(future.get()) },
            { runnable -> Handler(Looper.getMainLooper()).post(runnable) },
        )
    }

    private fun onControllerReady(ctrl: MediaController) {
        controller = ctrl
        ctrl.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                _changes.tryEmit(Unit)
                if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                    if (player.isPlaying) startPositionTicker() else stopPositionTicker()
                }
            }
        })
        controllerReady.complete(ctrl)
        _changes.tryEmit(Unit)
    }

    override fun readState(): EngineState {
        val c = controller ?: return EngineState.Empty
        return EngineState(
            index = c.currentMediaItemIndex,
            itemCount = c.mediaItemCount,
            playWhenReady = c.playWhenReady,
            isActive = c.playbackState != Player.STATE_IDLE && c.playbackState != Player.STATE_ENDED,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.takeIf { it > 0 } ?: 0L,
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode = c.repeatMode.toDomainRepeat(),
            volume = c.volume,
        )
    }

    override suspend fun applyQueue(tracks: List<Track>, startIndex: Int) {
        // Heavy: each MediaItem triggers file.exists() + grantUriPermission
        // binder calls inside artworkUriFor — keep it off the main thread.
        val items = withContext(Dispatchers.IO) {
            tracks.map { TrackMediaItemMapper.toMediaItem(it, artworkUriFor(it)) }
        }
        val c = controllerReady.await()
        withContext(mainDispatcher) {
            val safeIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
            c.setMediaItems(items, safeIndex, /* startPositionMs = */ 0L)
            c.prepare()
            c.playWhenReady = true
        }
    }

    override suspend fun restoreQueue(tracks: List<Track>, startIndex: Int, positionMs: Long) {
        val items = withContext(Dispatchers.IO) {
            tracks.map { TrackMediaItemMapper.toMediaItem(it, artworkUriFor(it)) }
        }
        val c = controllerReady.await()
        withContext(mainDispatcher) {
            val safeIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
            c.setMediaItems(items, safeIndex, positionMs.coerceAtLeast(0L))
            c.playWhenReady = false
            c.prepare()
        }
    }

    override suspend fun jumpTo(index: Int) = onControllerAwait { c ->
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, /* positionMs = */ 0L)
            c.playWhenReady = true
        }
    }

    override suspend fun next() = onControllerAwait { it.seekToNext() }

    override suspend fun previous() = onControllerAwait { it.seekToPrevious() }

    override suspend fun append(track: Track): Int = onControllerAwait { c ->
        c.addMediaItem(TrackMediaItemMapper.toMediaItem(track, artworkUriFor(track)))
        c.mediaItemCount
    }

    override suspend fun insertAfterCurrent(track: Track): Int = onControllerAwait { c ->
        val at = (c.currentMediaItemIndex + 1).coerceAtLeast(0)
        c.addMediaItem(at, TrackMediaItemMapper.toMediaItem(track, artworkUriFor(track)))
        at
    }

    override suspend fun removeAt(index: Int) = onControllerAwait { c ->
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    override suspend fun move(from: Int, to: Int) = onControllerAwait { c ->
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) {
            c.moveMediaItem(from, to)
        }
    }

    override suspend fun clear() = onControllerAwait { it.clearMediaItems() }

    override fun play() = fireOnMain { it.play() }
    override fun pause() = fireOnMain { it.pause() }
    override fun stop() = fireOnMain { it.stop() }
    override fun seekTo(positionMs: Long) = fireOnMain { it.seekTo(positionMs) }
    override fun setShuffleEnabled(enabled: Boolean) = fireOnMain { it.shuffleModeEnabled = enabled }
    override fun setRepeatMode(mode: RepeatMode) = fireOnMain { it.repeatMode = mode.toPlayerRepeat() }
    override fun setVolume(volume: Float) = fireOnMain { it.volume = volume.coerceIn(0f, 1f) }

    // --- internals ---

    private suspend fun <T> onControllerAwait(block: (MediaController) -> T): T {
        val c = controllerReady.await()
        return withContext(mainDispatcher) { block(c) }
    }

    private fun fireOnMain(block: (MediaController) -> Unit) {
        scope.launch(mainDispatcher) { controller?.let(block) }
    }

    private fun startPositionTicker() {
        stopPositionTicker()
        positionTicker = scope.launch(mainDispatcher) {
            while (true) {
                _changes.tryEmit(Unit)
                delay(500L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTicker?.cancel()
        positionTicker = null
    }

    private fun artworkUriFor(track: Track): android.net.Uri? =
        com.offlineplaya.shared.data.image.TrackArtCache.uriForTrack(context, track)
}

private fun Int.toDomainRepeat(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}

private fun RepeatMode.toPlayerRepeat(): Int = when (this) {
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
}
