package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme
import com.offlineplaya.shared.presentation.ui.preview.Preview

/**
 * The headline text atom: a single line of large, theme-aware copy used at the top
 * of empty/placeholder states and major sections. Dumb — no domain knowledge.
 */
@Composable
fun AppHeadline(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
}

@Preview
@Composable
private fun AppHeadlinePreview() {
    OfflinePlayaTheme {
        Surface {
            AppHeadline(text = "Offline Music, Your Way")
        }
    }
}
