package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Small all-caps section header used between page sections. Tracked-out,
 * outline-colored — reads as a divider without drawing one.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        ),
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 2.dp),
    )
}

@PreviewScreenSizes
@Composable
private fun SectionLabelPreview() {
    PreviewTheme {
        Surface { SectionLabel(text = "RECENTLY PLAYED") }
    }
}
