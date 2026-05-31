package com.offlineplaya.shared.presentation.artwork

import com.offlineplaya.shared.domain.usecase.EmbedMissingArtUseCase
import com.offlineplaya.shared.domain.usecase.EmbedReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Glue between the Settings UI and the per-folder "burn cover art into the
 * audio files" pass.
 *
 * Stateless on purpose — the user-facing flow is "tap → pick → done →
 * snackbar". No persistent `Idle/Running/Completed/Failed` state to bind
 * the UI to, no progress bar to babysit. Completion (or failure) shows up
 * as a single [Event] on [events], which the host composable surfaces via
 * a SnackbarHost.
 *
 * The pass runs in-process on [scope] (no WorkManager) so the snackbar
 * fires the moment the work finishes, in the same process as the UI.
 */
class EmbedArtCoordinator(
    private val useCase: EmbedMissingArtUseCase,
    private val scope: CoroutineScope,
) {

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)

    /** One-shot completion events. Host UI collects this to drive snackbars. */
    val events: SharedFlow<Event> = _events.asSharedFlow()

    /**
     * Run the embed pass against every track whose `treeUri` equals
     * [treeUri]. The pass:
     *  1. Skips tracks that already have embedded art.
     *  2. Tries the folder's sidecar cover first ([SafFolderArtSource]).
     *  3. Falls back to the remote source.
     *  4. Writes results into the audio files via the AlbumArtWriter.
     *
     * Fires exactly one [Event] when finished. Cancelling the returned
     * [Job] aborts the pass without emitting anything.
     */
    fun embedFolder(treeUri: String): Job = scope.launch {
        try {
            val report = useCase(
                onProgress = { /* discarded — snackbar-only UX */ },
                treeUriFilter = treeUri,
            )
            val event = when (report) {
                is EmbedReport.Completed ->
                    if (report.processed == 0) Event.NoTracks(treeUri)
                    else Event.Done(
                        embedded = report.embedded,
                        failed = report.failed,
                        processed = report.processed,
                    )

                is EmbedReport.Failed -> Event.Error(report.message)
                EmbedReport.Idle, is EmbedReport.Running ->
                    Event.Error("Embed pass produced no result")
            }
            _events.emit(event)
        } catch (t: Throwable) {
            _events.emit(Event.Error(t.message ?: "Embed pass failed"))
        }
    }

    sealed interface Event {
        /** Pass completed; counters describe what happened. */
        data class Done(val embedded: Int, val failed: Int, val processed: Int) : Event

        /** The picked folder didn't contain any tracks the library knows about. */
        data class NoTracks(val treeUri: String) : Event

        /** Something blew up before completion. */
        data class Error(val message: String) : Event
    }

    companion object {
        // Wire-format keys for the (still-wired but no-longer-UI-triggered)
        // WorkManager EmbedMissingArt pipeline. Output-Data payloads use
        // these so the runner can decode the SyncReport-equivalent fields.
        // Kept here so the worker, the runner, and any future scheduler
        // share one source of truth.
        const val KEY_PROCESSED = "processed"
        const val KEY_EMBEDDED = "embedded"
        const val KEY_FAILED = "failed"
    }
}
