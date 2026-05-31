package com.offlineplaya.shared.presentation.ui.templates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme
import com.offlineplaya.shared.presentation.ui.preview.Preview

/**
 * Template: a Scaffold whose body centers a single content slot. Used by
 * placeholder/empty/error pages where there's nothing scrollable to show.
 */
@Composable
fun CenteredScaffoldTemplate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@PreviewScreenSizes
@Composable
private fun CenteredScaffoldTemplatePreview() {
    OfflinePlayaTheme {
        CenteredScaffoldTemplate {
            Text("Slotted content")
        }
    }
}
