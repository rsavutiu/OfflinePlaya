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
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.cd_repeat_mode
import org.jetbrains.compose.resources.stringResource

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
        Icon(
            imageVector = icon,
            contentDescription = stringResource(Res.string.cd_repeat_mode, mode.toString()),
            tint = tint
        )
    }
}

@PreviewScreenSizes
@Composable
private fun RepeatToggleOffPreview() {
    PreviewTheme {
        Surface { RepeatToggle(mode = RepeatMode.OFF, onCycle = {}) }
    }
}

@PreviewScreenSizes
@Composable
private fun RepeatToggleAllPreview() {
    PreviewTheme {
        Surface { RepeatToggle(mode = RepeatMode.ALL, onCycle = {}) }
    }
}

@PreviewScreenSizes
@Composable
private fun RepeatToggleOnePreview() {
    PreviewTheme {
        Surface { RepeatToggle(mode = RepeatMode.ONE, onCycle = {}) }
    }
}
