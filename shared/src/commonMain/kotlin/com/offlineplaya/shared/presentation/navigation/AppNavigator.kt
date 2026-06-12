package com.offlineplaya.shared.presentation.navigation

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
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
    private val _stack = MutableStateFlow<PersistentList<AppDestination>>(persistentListOf(initial))

    /** Read-only view of the current stack (root first). */
    val stack: StateFlow<PersistentList<AppDestination>> = _stack.asStateFlow()

    /** The destination on top of the stack. */
    val current: AppDestination get() = _stack.value.last()

    /** `true` when there is something to pop back to. */
    val canGoBack: Boolean get() = _stack.value.size > 1

    /** Direction of the last navigation action, for transition animation. */
    var direction: Direction = Direction.FORWARD
        private set

    enum class Direction { FORWARD, BACKWARD, SWAP }

    fun push(destination: AppDestination) {
        direction = Direction.FORWARD
        _stack.update { it.add(destination) }
    }

    /** Pop the top destination. Returns `false` if already at the root. */
    fun pop(): Boolean {
        if (!canGoBack) return false
        direction = Direction.BACKWARD
        _stack.update { it.removeAt(it.lastIndex) }
        return true
    }

    /** Reset the stack to a single destination — used for top-level switches. */
    fun replaceWith(destination: AppDestination) {
        direction = Direction.FORWARD
        _stack.value = persistentListOf(destination)
    }

    /**
     * Replace just the top destination without touching the rest of the stack.
     * Used by tab strips: switching tabs swaps the current top-level view, but
     * Back should still pop to whatever was below (e.g. Home).
     */
    fun swapTop(destination: AppDestination) {
        direction = Direction.SWAP
        _stack.update { it.set(it.lastIndex, destination) }
    }
}
