package com.offlineplaya.shared.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigatorTest {

    @Test
    fun `new navigator starts at the given initial destination`() {
        val nav = AppNavigator(AppDestination.Home)
        assertEquals(AppDestination.Home, nav.current)
        assertFalse(nav.canGoBack)
    }

    @Test
    fun `push moves current and enables back`() {
        val nav = AppNavigator()
        nav.push(AppDestination.Settings)
        assertEquals(AppDestination.Settings, nav.current)
        assertTrue(nav.canGoBack)
    }

    @Test
    fun `pop returns true when a level is removed and false at the root`() {
        val nav = AppNavigator()
        nav.push(AppDestination.Settings)

        assertTrue(nav.pop())
        assertEquals(AppDestination.Home, nav.current)
        assertFalse(nav.canGoBack)

        // already at root
        assertFalse(nav.pop())
    }

    @Test
    fun `replaceWith collapses the stack to a single destination`() {
        val nav = AppNavigator()
        nav.push(AppDestination.LibraryArtists)
        nav.push(AppDestination.LibraryArtistDetail(7L))
        assertTrue(nav.canGoBack)

        nav.replaceWith(AppDestination.Home)

        assertEquals(AppDestination.Home, nav.current)
        assertFalse(nav.canGoBack)
    }

    @Test
    fun `data class destinations are equal by value`() {
        val a = AppDestination.LibraryArtistDetail(1L)
        val b = AppDestination.LibraryArtistDetail(1L)
        val c = AppDestination.LibraryArtistDetail(2L)
        assertEquals(a, b)
        assertEquals(false, a == c)
    }
}
