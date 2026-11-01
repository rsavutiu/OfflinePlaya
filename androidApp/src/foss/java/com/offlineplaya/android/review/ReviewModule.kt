package com.offlineplaya.android.review

import com.offlineplaya.shared.domain.review.ReviewPrompter
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * FOSS-flavour binding for [ReviewPrompter] — a no-op, because this variant
 * links against no Google Play libraries (that is the whole point: it is the
 * build shipped to F-Droid / IzzyOnDroid and attached to GitHub releases).
 *
 * Mirrors `src/play/.../ReviewModule.kt`; both declare the same `reviewModule`
 * symbol so the Koin wiring in `OfflinePlayaApp` is flavour-agnostic.
 */
val reviewModule: Module = module {
    single<ReviewPrompter> { NoopReviewPrompter(logger = get()) }
}
