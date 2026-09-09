package com.offlineplaya.shared.presentation.onboarding

import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tracks whether the user has completed (or skipped) first-run onboarding.
 *
 * [completed] is a **tri-state**: `null` means "not resolved yet" — the DB
 * read (and one-time migration) is still in flight — and the host should render
 * neither the wizard nor the main app until a real value arrives. Seeding
 * `null` rather than `false` is what stops the onboarding wizard from flashing
 * on every cold start (same reasoning as `LibraryStateHolder.isLibraryEmpty`).
 *
 * **Upgrade migration:** the `onboarding.completed` flag is new, so an existing
 * user who already has an indexed library would otherwise read `false` and be
 * shown the wizard on the update that introduces it. To avoid that, on first
 * resolve we treat "the library already has tracks" as "already onboarded" and
 * persist the flag. The decision is made **once** at init — not a live combine
 * with the track count — so a fresh user who indexes their first track *during*
 * the wizard's Add-music step doesn't get the wizard yanked out from under them.
 */
class OnboardingStateHolder(
    private val settings: SettingsRepository,
    private val tracks: TrackRepository,
    private val scope: CoroutineScope,
) {
    private val _completed = MutableStateFlow<Boolean?>(null)
    val completed: StateFlow<Boolean?> = _completed.asStateFlow()

    init {
        scope.launch {
            // One-time upgrade migration: a non-empty library means this user
            // predates the flag — mark them onboarded so the wizard never shows.
            if (!settings.isOnboardingCompleted() && tracks.count() > 0) {
                settings.setOnboardingCompleted(true)
            }
            // The persisted flag is now authoritative; mirror it, including the
            // flip from complete(). Never re-consults the live track count.
            settings.observeOnboardingCompleted().collect { _completed.value = it }
        }
    }

    /** Mark onboarding finished or skipped; flips [completed] to `true`. */
    fun complete() {
        scope.launch { settings.setOnboardingCompleted(true) }
    }
}
