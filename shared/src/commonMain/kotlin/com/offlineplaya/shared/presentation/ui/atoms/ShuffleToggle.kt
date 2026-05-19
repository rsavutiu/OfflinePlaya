package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Single icon button that toggles shuffle on/off. Tints itself with the
 * primary color when active to read at a glance.
 */
@Composable
fun ShuffleToggle(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (enabled) Icons.Default.ShuffleOn else Icons.Default.Shuffle,
            contentDescription = if (enabled) "Shuffle on" else "Shuffle off",
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Preview
@Composable
private fun ShuffleToggleOffPreview() {
    PreviewTheme {
        Surface { ShuffleToggle(enabled = false, onToggle = {}) }
    }
}

@Preview
@Composable
private fun ShuffleToggleOnPreview() {
    PreviewTheme {
        Surface { ShuffleToggle(enabled = true, onToggle = {}) }
    }
}
