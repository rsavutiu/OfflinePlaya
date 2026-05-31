package com.offlineplaya.shared.presentation.metadata

import com.offlineplaya.shared.domain.usecase.BurnMetadataUseCase
import com.offlineplaya.shared.domain.usecase.EmbedReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Stateful coordinator for the unified "Burn Metadata" pass.
 * Exposes a hot [report] flow so the UI can show a progress bar and
 * disable buttons while work is in flight.
 */
class BurnMetadataCoordinator(
    private val useCase: BurnMetadataUseCase,
    private val scope: CoroutineScope,
) {
    private val _report = MutableStateFlow<EmbedReport>(EmbedReport.Idle)

    /** Current status of the burn pass. */
    val report: StateFlow<EmbedReport> = _report.asStateFlow()

    /**
     * Start a burn pass. 
     * @param treeUriFilter null for whole library, or a specific tree URI.
     */
    fun start(treeUriFilter: String? = null): Job? {
        if (_report.value is EmbedReport.Running) return null

        return scope.launch {
            _report.value = EmbedReport.Running(0, 0, 0, 0)
            try {
                val result = useCase(
                    onProgress = { _report.value = it },
                    treeUriFilter = treeUriFilter
                )
                _report.value = result
            } catch (t: Throwable) {
                _report.value = EmbedReport.Failed(t.message ?: "Unknown error")
            }
        }
    }

    /** Clear a finished report and return to Idle. */
    fun acknowledge() {
        _report.value = EmbedReport.Idle
    }

    companion object {
        const val KEY_PROCESSED = "processed"
        const val KEY_EMBEDDED = "embedded"
        const val KEY_FAILED = "failed"
    }
}
