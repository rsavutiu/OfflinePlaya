package com.offlineplaya.shared.domain.scheduling

/**
 * View of a [BackgroundTaskKind] as it moves through its lifecycle, surfaced
 * by [BackgroundTaskRunner.observe].
 *
 * The interface deliberately doesn't promise *when* the work runs — that's
 * a per-platform concern (WorkManager queues, iOS BGTaskScheduler defers,
 * Desktop runs immediately). Callers get a state machine they can reason
 * about, not a real-time guarantee.
 */
sealed interface BackgroundTaskStatus {

    /** Enqueued but not yet started. */
    data object Pending : BackgroundTaskStatus

    /** Currently executing. [current] / [total] are 0 until the task reports
     *  its first progress tick. */
    data class Running(val current: Int, val total: Int) : BackgroundTaskStatus

    /** Finished successfully. The free-form [payload] carries any
     *  task-specific metadata (counts, ids, …). Keys are domain-defined. */
    data class Succeeded(val payload: Map<String, Long>) : BackgroundTaskStatus

    /** Finished with a top-level error. Per-item failures inside the task
     *  body are the task's own concern and don't surface here. */
    data class Failed(val message: String) : BackgroundTaskStatus

    /** Cancelled by [BackgroundTaskRunner.cancel] or platform constraints. */
    data object Cancelled : BackgroundTaskStatus
}
