package com.offlineplaya.shared.data.repository

import com.offlineplaya.shared.domain.model.ColorMode
import com.offlineplaya.shared.domain.model.LyricsPreferences
import com.offlineplaya.shared.domain.model.PlaybackPreferences
import com.offlineplaya.shared.domain.model.ThemePreferences
import com.offlineplaya.shared.testsupport.createInMemoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlSettingsRepositoryTest {

    private fun newRepository() =
        SqlSettingsRepository(createInMemoryDatabase(), Dispatchers.Unconfined)

    @Test
    fun `empty store returns the default theme preferences`() = runTest {
        val repo = newRepository()
        assertEquals(ThemePreferences.Default, repo.getThemePreferences())
        assertEquals(ThemePreferences.Default, repo.observeThemePreferences().first())
    }

    @Test
    fun `setThemePreferences round-trips all fields`() = runTest {
        val repo = newRepository()
        // All three flipped away from their defaults so the round-trip proves
        // each one is actually persisted and read back, not just defaulted.
        val target = ThemePreferences(
            colorMode = ColorMode.DARK,
            useDynamicColor = true,
            useAlbumArtColor = false,
        )

        repo.setThemePreferences(target)

        assertEquals(target, repo.getThemePreferences())
    }

    @Test
    fun `setThemePreferences overwrites previous values`() = runTest {
        val repo = newRepository()
        repo.setThemePreferences(
            ThemePreferences(ColorMode.DARK, useDynamicColor = false, useAlbumArtColor = true),
        )
        repo.setThemePreferences(
            ThemePreferences(ColorMode.LIGHT, useDynamicColor = true, useAlbumArtColor = false),
        )

        val stored = repo.getThemePreferences()
        assertEquals(ColorMode.LIGHT, stored.colorMode)
        assertEquals(true, stored.useDynamicColor)
        assertEquals(false, stored.useAlbumArtColor)
    }

    @Test
    fun `each ColorMode enum value round-trips through db storage`() = runTest {
        val repo = newRepository()
        for (mode in ColorMode.entries) {
            repo.setThemePreferences(
                ThemePreferences(mode, useDynamicColor = true, useAlbumArtColor = true),
            )
            assertEquals(mode, repo.getThemePreferences().colorMode)
        }
    }

    @Test
    fun `empty store returns the default playback preferences`() = runTest {
        val repo = newRepository()
        assertEquals(PlaybackPreferences.Default, repo.getPlaybackPreferences())
        assertEquals(PlaybackPreferences.Default, repo.observePlaybackPreferences().first())
    }

    @Test
    fun `setPlaybackPreferences round-trips both fields`() = runTest {
        val repo = newRepository()
        // Flipped away from the default (enabled, 6s) so the round-trip proves
        // each field is actually persisted, not just defaulted.
        val target = PlaybackPreferences(crossfadeEnabled = false, crossfadeDurationSeconds = 11)

        repo.setPlaybackPreferences(target)

        assertEquals(target, repo.getPlaybackPreferences())
    }

    @Test
    fun `playback duration is clamped to the supported range on read`() = runTest {
        val repo = newRepository()
        // The setter clamps too, but a value persisted by an older/newer build
        // (or a hand-edited row) must still read back in-range.
        repo.setPlaybackPreferences(
            PlaybackPreferences(crossfadeEnabled = true, crossfadeDurationSeconds = 999),
        )
        val stored = repo.getPlaybackPreferences()
        assertEquals(PlaybackPreferences.MAX_DURATION_SECONDS, stored.crossfadeDurationSeconds)
    }

    @Test
    fun `empty store returns the default lyrics preferences`() = runTest {
        val repo = newRepository()
        assertEquals(LyricsPreferences.Default, repo.getLyricsPreferences())
        assertEquals(LyricsPreferences.Default, repo.observeLyricsPreferences().first())
    }

    @Test
    fun `setLyricsPreferences round-trips the download toggle`() = runTest {
        val repo = newRepository()
        // Default is ON; flip OFF so the round-trip proves the key/parse
        // path actually persists the value rather than just defaulting.
        repo.setLyricsPreferences(LyricsPreferences(downloadRemoteLyrics = false))
        assertEquals(false, repo.getLyricsPreferences().downloadRemoteLyrics)

        repo.setLyricsPreferences(LyricsPreferences(downloadRemoteLyrics = true))
        assertEquals(true, repo.getLyricsPreferences().downloadRemoteLyrics)
    }

    @Test
    fun `last seed color persists and clears`() = runTest {
        val repo = newRepository()
        assertEquals(null, repo.getLastSeedColor(), "absent by default")

        repo.setLastSeedColor(0x7C5CBF)
        assertEquals(0x7C5CBF, repo.getLastSeedColor())

        repo.setLastSeedColor(null)
        assertEquals(null, repo.getLastSeedColor(), "null wipes the stored seed")
    }
}
