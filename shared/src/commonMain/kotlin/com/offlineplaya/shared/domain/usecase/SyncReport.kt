package com.offlineplaya.shared.domain.usecase

/**
 * Summary returned by [LibrarySyncUseCase] for a single sync invocation.
 */
data class SyncReport(
    val foldersUpserted: Int,
    val tracksDiscovered: Int,
    val tracksScanned: Int,
    val tracksFailed: Int,
) {
    companion object {
        val Empty = SyncReport(
            foldersUpserted = 0,
            tracksDiscovered = 0,
            tracksScanned = 0,
            tracksFailed = 0,
        )
    }

    operator fun plus(other: SyncReport) = SyncReport(
        foldersUpserted = foldersUpserted + other.foldersUpserted,
        tracksDiscovered = tracksDiscovered + other.tracksDiscovered,
        tracksScanned = tracksScanned + other.tracksScanned,
        tracksFailed = tracksFailed + other.tracksFailed,
    )
}
