package com.offlineplaya.shared.presentation.lyrics

import com.offlineplaya.shared.domain.lyrics.LyricLine
import com.offlineplaya.shared.domain.lyrics.Lyrics
import com.offlineplaya.shared.domain.lyrics.LyricsRepository
import com.offlineplaya.shared.domain.model.PlaybackState
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.player.MusicPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LyricsStateHolderTest {

    // ── activeLineIndex (pure) ────────────────────────────────────────

    private val lines = listOf(
        LyricLine(1_000L, "a"),
        LyricLine(2_000L, "b"),
        LyricLine(3_000L, "c"),
    )

    @Test
    fun `active index is -1 before the first line`() {
        assertEquals(-1, activeLineIndex(lines, 0L))
        assertEquals(-1, activeLineIndex(lines, 999L))
    }

    @Test
    fun `active index lands exactly on a line at its timestamp`() {
        assertEquals(0, activeLineIndex(lines, 1_000L))
        assertEquals(1, activeLineIndex(lines, 2_000L))
        assertEquals(2, activeLineIndex(lines, 3_000L))
    }

    @Test
    fun `active index holds the previous line between timestamps`() {
        assertEquals(0, activeLineIndex(lines, 1_500L))
        assertEquals(2, activeLineIndex(lines, 9_999L))
    }

    @Test
    fun `active index on empty list is -1`() {
        assertEquals(-1, activeLineIndex(emptyList(), 5_000L))
    }

    // ── integration ───────────────────────────────────────────────────

    @Test
    fun `synced lyrics expose the active line as position advances`() = runTest {
        // Unconfined so the holder's collectors run eagerly on each emit —
        // deterministic without juggling advanceUntilIdle ordering.
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        try {
            val player = FakePlayer()
            val repo = LyricsRepository { Lyrics.Synced(lines) }
            val holder = LyricsStateHolder(player, repo, scope)

            player.emit(track(), positionMs = 0L)
            assertTrue(holder.state.value is LyricsUiState.Synced)
            assertEquals(-1, (holder.state.value as LyricsUiState.Synced).activeIndex)

            player.emit(track(), positionMs = 2_100L)
            assertEquals(1, (holder.state.value as LyricsUiState.Synced).activeIndex)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `seekToLine forwards to the player`() = runTest {
        val player = FakePlayer()
        val holder = LyricsStateHolder(player, LyricsRepository { Lyrics.None }, backgroundScope)
        holder.seekToLine(LyricLine(42_000L, "x"))
        assertEquals(42_000L, player.lastSeekMs)
    }

    // ── fakes ─────────────────────────────────────────────────────────

    private fun LyricsRepository(block: suspend (Track) -> Lyrics): LyricsRepository =
        object : LyricsRepository {
            override suspend fun lyricsFor(track: Track): Lyrics = block(track)
        }

    private fun track() = Track(
        id = 1, documentUri = "content://t/1", treeUri = "content://tree", relativePath = "b.mp3",
        fileName = "b.mp3", title = "T", artistName = "A", albumArtistName = null, albumName = "Al",
        genre = null, year = null, trackNumber = null, discNumber = null, durationMs = 200_000L,
        bitrate = null, sampleRate = null, channels = null, codec = null, artistId = null,
        albumId = null, folderId = null, scanStatus = ScanStatus.SCANNED,
    )

    private class FakePlayer : MusicPlayer {
        private val _state = MutableStateFlow(PlaybackState.Empty)
        override val playbackState: StateFlow<PlaybackState> = _state
        var lastSeekMs: Long = -1

        fun emit(track: Track, positionMs: Long) {
            _state.value = _state.value.copy(currentTrack = track, positionMs = positionMs)
        }

        override fun seekTo(positionMs: Long) { lastSeekMs = positionMs }

        override fun play() {}
        override fun pause() {}
        override fun stop() {}
        override fun skipToNext() {}
        override fun skipToPrevious() {}
        override fun seekToIndex(index: Int) {}
        override fun setQueue(tracks: List<Track>, startIndex: Int) {}
        override fun restoreQueue(tracks: List<Track>, startIndex: Int, positionMs: Long) {}
        override fun addToQueue(track: Track) {}
        override fun addNext(track: Track) {}
        override fun removeFromQueue(index: Int) {}
        override fun moveInQueue(from: Int, to: Int) {}
        override fun clearQueue() {}
        override fun setShuffleEnabled(enabled: Boolean) {}
        override fun setRepeatMode(mode: RepeatMode) {}
        override fun setVolume(volume: Float) {}
    }
}
