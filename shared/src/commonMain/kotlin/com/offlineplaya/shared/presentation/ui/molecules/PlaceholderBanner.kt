package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.atoms.AppCaption
import com.offlineplaya.shared.presentation.ui.atoms.AppHeadline
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Vertical pair of headline + caption used for empty/placeholder screens.
 * A molecule because it groups two atoms with shared layout intent.
 */
@Composable
fun PlaceholderBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppHeadline(text = title)
        AppCaption(text = subtitle)
    }
}

@PreviewScreenSizes
@Composable
private fun PlaceholderBannerPreview() {
    OfflinePlayaTheme {
        Surface {
            PlaceholderBanner(
                title = "OfflinePlaya",
                subtitle = "Phase 1 scaffold — point me at a folder when scanning lands.",
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun PlaceholderBannerDarkPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            PlaceholderBanner(
                title = "OfflinePlaya",
                subtitle = "Phase 1 scaffold — point me at a folder when scanning lands.",
            )
        }
    }
}
