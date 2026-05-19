package com.offlineplaya.shared.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A pure in-memory navigation stack. Push deeper destinations, pop to go
 * back, replace to reset. No external library dependency — works in
 * commonMain and is trivially testable.
 *
 * The Android host wires this into the platform back button via
 * [BackHandler] in the activity.
 */
class AppNavigator(
    initial: AppDestination = AppDestination.Home,
) {
    private val _stack = MutableStateFlow<List<AppDestination>>(listOf(initial))

    /** Read-only view of the current stack (root first). */
    val stack: StateFlow<List<AppDestination>> = _stack.asStateFlow()

    /** The destination on top of the stack. */
    val current: AppDestination get() = _stack.value.last()

    /** `true` when there is something to pop back to. */
    val canGoBack: Boolean get() = _stack.value.size > 1

    fun push(destination: AppDestination) {
        _stack.update { it + destination }
    }

    /** Pop the top destination. Returns `false` if already at the root. */
    fun pop(): Boolean {
        if (!canGoBack) return false
        _stack.update { it.dropLast(1) }
        return true
    }

    /** Reset the stack to a single destination — used for top-level switches. */
    fun replaceWith(destination: AppDestination) {
        _stack.value = listOf(destination)
    }
}
