package com.offlineplaya.shared.presentation.artwork

import com.offlineplaya.shared.domain.usecase.EmbedMissingArtUseCase
import com.offlineplaya.shared.domain.usecase.EmbedReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Glue between the Settings UI and [EmbedMissingArtUseCase]. Exposes the
 * current [EmbedReport] as a hot flow; the page collects it to drive the
 * progress UI. One pass at a time — calling [start] while already running
 * returns the existing [Job] rather than queueing.
 */
class EmbedArtCoordinator(
    private val useCase: EmbedMissingArtUseCase,
    private val scope: CoroutineScope,
) {
    private val _report = MutableStateFlow<EmbedReport>(EmbedReport.Idle)
    val report: StateFlow<EmbedReport> = _report.asStateFlow()

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
                val result = useCase(
                    onProgress = { running -> _report.value = running },
                    shouldCancel = { false },
                )
                _report.value = result
            } catch (t: Throwable) {
                _report.value = EmbedReport.Failed(t.message ?: "Embed pass failed")
            }
        }
        currentJob = job
        return job
    }

    /** Reset the report back to Idle (the user dismissed the completion banner). */
    fun acknowledge() {
        _report.value = EmbedReport.Idle
    }
}
