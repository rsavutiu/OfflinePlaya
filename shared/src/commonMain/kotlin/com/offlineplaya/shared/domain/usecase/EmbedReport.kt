package com.offlineplaya.shared.domain.usecase

/**
 * View-facing status for the "embed missing album art" pass.
 *
 *  - [Idle] — never started, or finished and the user dismissed the report.
 *  - [Running] — pass is in progress; numerator/denominator describe overall
 *    track count being walked. `embedded` / `failed` accumulate as we go.
 *  - [Completed] — pass finished cleanly.
 *  - [Failed] — pass aborted with a top-level error (e.g. permission lost
 *    mid-flight). Individual per-track write failures are counted into
 *    `failed`, not surfaced as this state.
 */
sealed interface EmbedReport {
    data object Idle : EmbedReport
    data class Running(
        val processed: Int,
        val total: Int,
        val embedded: Int,
        val failed: Int,
    ) : EmbedReport
    data class Completed(
        val processed: Int,
        val embedded: Int,
        val failed: Int,
    ) : EmbedReport
    data class Failed(val message: String) : EmbedReport
}
