package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.runtime.Composable
import com.offlineplaya.shared.presentation.ui.molecules.PlaceholderBanner
import com.offlineplaya.shared.presentation.ui.templates.CenteredScaffoldTemplate
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme
import com.offlineplaya.shared.presentation.ui.preview.Preview

private const val TITLE = "OfflinePlaya"
private const val SUBTITLE = "Phase 1 scaffold ready. Folder scanning arrives in Phase 2."

/**
 * Concrete page used while the rest of the app is being built. Composes the
 * centered-scaffold template with the placeholder banner molecule.
 */
@Composable
fun PlaceholderPage() {
    CenteredScaffoldTemplate {
        PlaceholderBanner(title = TITLE, subtitle = SUBTITLE)
    }
}

@Preview
@Composable
private fun PlaceholderPagePreview() {
    OfflinePlayaTheme {
        PlaceholderPage()
    }
}

@Preview
@Composable
private fun PlaceholderPageDarkPreview() {
    OfflinePlayaTheme(darkTheme = true) {
        PlaceholderPage()
    }
}
