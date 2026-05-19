package com.offlineplaya.shared.presentation.sync

import com.offlineplaya.shared.domain.repository.ManagedTreeRootRepository
import com.offlineplaya.shared.domain.usecase.LibrarySyncUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Glue between the UI layer and the [LibrarySyncUseCase]. Holds a single
 * [StateFlow] of [SyncStatus] so the home page can observe progress, and
 * funnels picker results into [ManagedTreeRootRepository] + the use case.
 *
 * Lives in `commonMain` so any future iOS/Desktop UI can reuse it as-is.
 * The platform layer supplies the URI (via SAF on Android) and a coroutine
 * [scope] tied to the application lifetime.
 */
class LibrarySyncCoordinator(
    private val syncUseCase: LibrarySyncUseCase,
    private val managedRoots: ManagedTreeRootRepository,
    private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /**
     * Register a newly-picked tree URI as a managed root and immediately sync
     * it. The launched [Job] is returned so tests (and future UI cancellation
     * logic) can await completion; production callers can ignore it.
     */
    fun addAndSync(treeUri: String, displayName: String): Job = scope.launch {
        try {
            _status.value = SyncStatus.Scanning(treeUri)
            managedRoots.add(treeUri, displayName)
            val report = syncUseCase.syncOne(treeUri)
            _status.value = SyncStatus.Completed(report)
        } catch (t: Throwable) {
            _status.value = SyncStatus.Failed(t.message ?: "Unknown error")
        }
    }

    /** Trigger a re-scan of every already-registered managed root. */
    fun resyncAll(): Job = scope.launch {
        try {
            _status.value = SyncStatus.Scanning(treeUri = "<all>")
            val report = syncUseCase.syncAll()
            _status.value = SyncStatus.Completed(report)
        } catch (t: Throwable) {
            _status.value = SyncStatus.Failed(t.message ?: "Unknown error")
        }
    }
}
