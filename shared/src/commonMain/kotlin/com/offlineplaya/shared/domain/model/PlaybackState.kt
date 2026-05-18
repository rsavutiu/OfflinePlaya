package com.offlineplaya.shared.domain.model

data class PlaybackState(
    val currentTrack: Track?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode,
    val queue: List<Track>,
    val queueIndex: Int,
    val volume: Float,
) {
    companion object {
        val Empty = PlaybackState(
            currentTrack = null,
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L,
            shuffleEnabled = false,
            repeatMode = RepeatMode.OFF,
            queue = emptyList(),
            queueIndex = -1,
            volume = 1f,
        )
    }
}

enum class RepeatMode { OFF, ONE, ALL }
