package com.offlineplaya.shared.presentation.artwork

import com.offlineplaya.shared.domain.scheduling.BackgroundTaskKind
import com.offlineplaya.shared.domain.scheduling.BackgroundTaskRunner
import com.offlineplaya.shared.domain.scheduling.BackgroundTaskStatus
import com.offlineplaya.shared.domain.usecase.EmbedReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Glue between the Settings UI and the background "embed missing art" pass.
 *
 * The actual work is dispatched through a [BackgroundTaskRunner], so the
 * platform decides *how* to run it (WorkManager on Android, plain coroutine
 * on Desktop, `BGProcessingTaskRequest` on iOS). This class only deals in
 * [EmbedReport] for the Settings page to bind against.
 *
 * One pass at a time — calling [start] while a task is already enqueued
 * re-uses the existing observation job rather than enqueuing a duplicate.
 */
class EmbedArtCoordinator(
    private val runner: BackgroundTaskRunner,
    private val scope: CoroutineScope,
) {
    private val _report = MutableStateFlow<EmbedReport>(EmbedReport.Idle)
    val report: StateFlow<EmbedReport> = _report.asStateFlow()

    private var currentTaskId: String? = null
    private var currentJob: Job? = null

    fun start(): Job {
        currentJob?.takeIf { it.isActive }?.let { return it }
        val job = scope.launch {
            try {
                _report.value = EmbedReport.Running(
                    processed = 0,
                    total = 0,
                    embedded = 0,
                    failed = 0,
                )
                val taskId = runner.enqueue(BackgroundTaskKind.EmbedMissingArt)
                currentTaskId = taskId
                runner.observe(taskId).collect { status ->
                    _report.value = status.toEmbedReport()
                }
            } catch (t: Throwable) {
                _report.value = EmbedReport.Failed(t.message ?: "Embed pass failed")
            }
        }
        currentJob = job
        return job
    }

    /** Best-effort cancel of the in-flight pass. */
    suspend fun cancel() {
        currentTaskId?.let { runner.cancel(it) }
    }

    /** Reset the report back to Idle (the user dismissed the completion banner). */
    fun acknowledge() {
        _report.value = EmbedReport.Idle
    }

    private fun BackgroundTaskStatus.toEmbedReport(): EmbedReport = when (this) {
        is BackgroundTaskStatus.Pending -> EmbedReport.Running(
            processed = 0,
            total = 0,
            embedded = 0,
            failed = 0,
        )
        is BackgroundTaskStatus.Running -> EmbedReport.Running(
            processed = current,
            total = total,
            embedded = 0,
            failed = 0,
        )
        is BackgroundTaskStatus.Succeeded -> EmbedReport.Completed(
            processed = payload[KEY_PROCESSED]?.toInt() ?: 0,
            embedded = payload[KEY_EMBEDDED]?.toInt() ?: 0,
            failed = payload[KEY_FAILED]?.toInt() ?: 0,
        )
        is BackgroundTaskStatus.Failed -> EmbedReport.Failed(message)
        is BackgroundTaskStatus.Cancelled -> EmbedReport.Idle
    }

    companion object {
        /** Payload keys the runner's success result must carry for the
         *  [EmbedMissingArt][BackgroundTaskKind.EmbedMissingArt] task. */
        const val KEY_PROCESSED = "processed"
        const val KEY_EMBEDDED = "embedded"
        const val KEY_FAILED = "failed"
    }
}
