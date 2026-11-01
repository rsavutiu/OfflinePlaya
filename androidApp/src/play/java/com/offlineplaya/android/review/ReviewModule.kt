package com.offlineplaya.android.review

import com.offlineplaya.shared.data.review.CurrentActivityHolder
import com.offlineplaya.shared.domain.review.ReviewPrompter
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Play-flavour binding for [ReviewPrompter] — the real Google Play in-app
 * review flow.
 *
 * This lives in the `play` flavour (and the `play-review-ktx` dependency with
 * it) so the `foss` variant links against no Google Play libraries at all; see
 * the matching file in `src/foss/`. Both flavours declare the same
 * `reviewModule` symbol, so the Koin wiring in `OfflinePlayaApp` is
 * flavour-agnostic.
 */
val reviewModule: Module = module {
    single<ReviewPrompter> {
        PlayReviewPrompter(
            activityProvider = { get<CurrentActivityHolder>().current },
            logger = get(),
        )
    }
}
