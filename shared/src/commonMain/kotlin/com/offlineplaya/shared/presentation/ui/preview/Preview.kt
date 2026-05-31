package com.offlineplaya.shared.presentation.ui.preview

/**
 * Multiplatform `@Preview` shim. On Android this aliases to
 * `androidx.compose.ui.tooling.preview.Preview` so Android Studio's preview
 * pane renders annotated composables. On other targets (future iOS/Desktop)
 * this becomes a no-op — keep authoring previews in `commonMain`, they will
 * always render where a real preview surface exists.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
expect annotation class Preview()

/**
 * Multiplatform shim for `@PreviewScreenSizes`. On Android, aliases to the
 * Android-only annotation so Studio renders the composable at every common
 * phone / tablet / foldable size. On other targets this becomes a no-op so
 * pages authoring multi-size previews in `commonMain` still compile.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
expect annotation class PreviewScreenSizes()
