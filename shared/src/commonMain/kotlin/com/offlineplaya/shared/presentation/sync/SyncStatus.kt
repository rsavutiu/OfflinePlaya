package com.offlineplaya.shared.presentation.sync

import com.offlineplaya.shared.domain.usecase.SyncReport

/**
 * View-facing state for the library-sync feature. Owned by
 * [LibrarySyncCoordinator]; consumed by the home page composable.
 */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data class Scanning(val treeUri: String) : SyncStatus
    data class Completed(val report: SyncReport) : SyncStatus
    data class AlreadyAdded(val treeUri: String, val displayName: String) : SyncStatus
    data class Failed(val message: String) : SyncStatus
}
