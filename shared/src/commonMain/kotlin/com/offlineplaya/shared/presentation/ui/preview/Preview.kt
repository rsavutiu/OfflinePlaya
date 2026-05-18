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
