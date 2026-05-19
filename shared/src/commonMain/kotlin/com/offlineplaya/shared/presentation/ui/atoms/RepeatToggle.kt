package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.domain.model.RepeatMode
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Icon button that cycles through repeat modes: OFF → ALL → ONE → OFF.
 * Tints itself with the primary color when active.
 */
@Composable
fun RepeatToggle(
    mode: RepeatMode,
    onCycle: (RepeatMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val next = when (mode) {
        RepeatMode.OFF -> RepeatMode.ALL
        RepeatMode.ALL -> RepeatMode.ONE
        RepeatMode.ONE -> RepeatMode.OFF
    }
    val icon = when (mode) {
        RepeatMode.OFF -> Icons.Default.Repeat
        RepeatMode.ALL -> Icons.Default.RepeatOn
        RepeatMode.ONE -> Icons.Default.RepeatOneOn
    }
    val tint = if (mode == RepeatMode.OFF) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.primary
    }
    IconButton(onClick = { onCycle(next) }, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = "Repeat: $mode", tint = tint)
    }
}

@Preview
@Composable
private fun RepeatToggleOffPreview() {
    PreviewTheme {
        Surface { RepeatToggle(mode = RepeatMode.OFF, onCycle = {}) }
    }
}

@Preview
@Composable
private fun RepeatToggleAllPreview() {
    PreviewTheme {
        Surface { RepeatToggle(mode = RepeatMode.ALL, onCycle = {}) }
    }
}

@Preview
@Composable
private fun RepeatToggleOnePreview() {
    PreviewTheme {
        Surface { RepeatToggle(mode = RepeatMode.ONE, onCycle = {}) }
    }
}
