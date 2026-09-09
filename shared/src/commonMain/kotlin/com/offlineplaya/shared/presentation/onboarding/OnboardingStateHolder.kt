package com.offlineplaya.shared.presentation.onboarding

import com.offlineplaya.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tracks whether the user has completed (or skipped) first-run onboarding.
 *
 * [completed] is a **tri-state**: `null` means "not resolved yet" — the DB
 * read is still in flight — and the host should render neither the wizard nor
 * the main app until a real value arrives. Seeding `null` rather than `false`
 * is what stops the onboarding wizard from flashing for an existing user on
 * every cold start (same reasoning as `LibraryStateHolder.isLibraryEmpty`).
 */
class OnboardingStateHolder(
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    val completed: StateFlow<Boolean?> = settings.observeOnboardingCompleted()
        .map<Boolean, Boolean?> { it }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    /** Mark onboarding finished or skipped; flips [completed] to `true`. */
    fun complete() {
        scope.launch { settings.setOnboardingCompleted(true) }
    }
}
