package com.offlineplaya.android.audio

import androidx.media3.common.Player
import androidx.media3.common.Tracks
import com.offlineplaya.shared.domain.model.ReplayGainTags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Publishes the ReplayGain tags of whatever the [player] is currently playing.
 * [AppEqualizerController] combines this with the user's mode to set the gain.
 * Must be constructed on the player's application thread.
 */
class ReplayGainTracker(private val player: Player) {

    private val _tags = MutableStateFlow<ReplayGainTags?>(null)
    val tags: StateFlow<ReplayGainTags?> = _tags

    private val listener = object : Player.Listener {
        override fun onTracksChanged(tracks: Tracks) {
            _tags.value = ReplayGainTagReader.read(tracks)
        }
    }

    init {
        player.addListener(listener)
        _tags.value = ReplayGainTagReader.read(player.currentTracks)
    }

    fun release() {
        player.removeListener(listener)
    }
}
