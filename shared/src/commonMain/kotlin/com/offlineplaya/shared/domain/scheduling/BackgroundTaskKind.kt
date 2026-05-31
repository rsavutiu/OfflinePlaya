package com.offlineplaya.shared.domain.scheduling

/**
 * Identifier for a kind of background work the app knows how to perform.
 * Adding a new long-running job means extending this sealed hierarchy AND
 * teaching the platform-specific [BackgroundTaskRunner] how to dispatch it.
 *
 * Sealed (not an enum) so future kinds can carry parameters — e.g.
 * `data class SyncOneRoot(val treeUri: String)` for a scoped re-scan.
 */
sealed interface BackgroundTaskKind {

    /** Stable string id used as a wire-format key (WorkManager input data,
     *  serialized scheduling state, telemetry). One per concrete kind. */
    val id: String

    /** Walk all tracks with missing embedded art or genre tags, fetch them,
     *  and write them back into the audio files. */
    data object BurnMetadata : BackgroundTaskKind {
        override val id: String = "burn_metadata"
    }

    companion object {
        /** Parse a [BackgroundTaskKind] from its [id], or `null` if unknown. */
        fun fromId(raw: String?): BackgroundTaskKind? = when (raw) {
            BurnMetadata.id -> BurnMetadata
            else -> null
        }
    }
}
