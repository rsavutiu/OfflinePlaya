package com.offlineplaya.shared.presentation.ui

/**
 * Stable `Modifier.testTag` identifiers for UI tests.
 *
 * Centralised so tests key off a single source of truth instead of string
 * literals, and so each page exposes a known **root** tag — navigation
 * (push/pop) assertions just check which root is present. Deliberately
 * harness-agnostic: the same tags are used whether screen tests run via
 * `commonTest` `runComposeUiTest` or `androidApp/androidTest`.
 *
 * Tagging is added per page as that page's tests are written (see
 * docs/instrumentation-test-plan.md), not all at once.
 */
object TestTags {
    object Search {
        /** Page root. */
        const val ROOT = "search_page"

        /** The query text field. */
        const val FIELD = "search_field"

        /** "Type to search" prompt — query shorter than the 2-char minimum. */
        const val PROMPT = "search_prompt"

        /** "No results" state — a valid query that matched nothing. */
        const val NO_RESULTS = "search_no_results"

        /** The results list (present only when there are matches). */
        const val RESULTS = "search_results"
    }
}
