package com.offlineplaya.shared.data.repository

import com.offlineplaya.shared.domain.model.ColorMode
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
    fun `setThemePreferences round-trips both fields`() = runTest {
        val repo = newRepository()
        val target = ThemePreferences(
            colorMode = ColorMode.DARK,
            useDynamicColor = false,
        )

        repo.setThemePreferences(target)

        assertEquals(target, repo.getThemePreferences())
    }

    @Test
    fun `setThemePreferences overwrites previous values`() = runTest {
        val repo = newRepository()
        repo.setThemePreferences(ThemePreferences(ColorMode.DARK, useDynamicColor = false))
        repo.setThemePreferences(ThemePreferences(ColorMode.LIGHT, useDynamicColor = true))

        val stored = repo.getThemePreferences()
        assertEquals(ColorMode.LIGHT, stored.colorMode)
        assertEquals(true, stored.useDynamicColor)
    }

    @Test
    fun `each ColorMode enum value round-trips through db storage`() = runTest {
        val repo = newRepository()
        for (mode in ColorMode.entries) {
            repo.setThemePreferences(ThemePreferences(mode, useDynamicColor = true))
            assertEquals(mode, repo.getThemePreferences().colorMode)
        }
    }
}
