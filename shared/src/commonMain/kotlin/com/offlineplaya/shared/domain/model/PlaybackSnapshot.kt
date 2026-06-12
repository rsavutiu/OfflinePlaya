package com.offlineplaya.shared.domain.model

/**
 * The transport state persisted alongside the queue so a killed process can
 * come back exactly where playback stopped: which queue slot was current,
 * how far into it, and the shuffle/repeat modes the user had set.
 */
data class PlaybackSnapshot(
    val queueIndex: Int,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode,
)
