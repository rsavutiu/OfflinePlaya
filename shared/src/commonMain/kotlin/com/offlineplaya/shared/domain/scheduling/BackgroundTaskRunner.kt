package com.offlineplaya.shared.domain.scheduling

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic gateway for running long-lived work that needs to
 * survive UI lifecycle events. Implementations:
 *
 *  - **Android** ([com.offlineplaya.shared.data.scheduling.WorkManagerTaskRunner])
 *    enqueues a foreground `CoroutineWorker` so the OS shows a progress
 *    notification and re-launches the worker if the process is killed.
 *  - **Desktop / iOS** will add their own implementations (plain coroutine
 *    on Desktop because the JVM keeps running; `BGProcessingTaskRequest`
 *    on iOS). The commonMain code never sees the difference.
 *
 * Deliberately narrow: callers identify *what* to run via
 * [BackgroundTaskKind] and observe the lifecycle via [BackgroundTaskStatus].
 * The runner's contract says nothing about *when* the OS chooses to schedule
 * the work — that's necessarily platform-shaped.
 */
interface BackgroundTaskRunner {

    /**
     * Enqueue work of the given [kind]. Returns a stable task id the caller
     * can pass to [observe] and [cancel].
     *
     * Multiple enqueues of the same kind may be deduped by the platform
     * (Android's `KEEP` work policy collapses concurrent embeds, for
     * example) — callers shouldn't assume each call starts a fresh run.
     */
    suspend fun enqueue(kind: BackgroundTaskKind): String

    /** Hot flow of status updates for [taskId]. Completes (cold) once the
     *  task reaches a terminal state. */
    fun observe(taskId: String): Flow<BackgroundTaskStatus>

    /** Best-effort cancellation. Some platforms may not honour it immediately. */
    suspend fun cancel(taskId: String)
}
