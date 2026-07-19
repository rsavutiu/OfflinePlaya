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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.cd_shuffle
import offlineplaya.shared.generated.resources.state_off
import offlineplaya.shared.generated.resources.state_on
import org.jetbrains.compose.resources.stringResource

/**
 * Single icon button that toggles shuffle on/off. Tints itself with the
 * primary color when active to read at a glance. TalkBack announces the
 * localized label plus an On/Off state ("Shuffle, On") rather than baking
 * the state into the description.
 */
@Composable
fun ShuffleToggle(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = stringResource(if (enabled) Res.string.state_on else Res.string.state_off)
    IconButton(
        onClick = onToggle,
        modifier = modifier.semantics { stateDescription = state },
    ) {
        Icon(
            imageVector = if (enabled) Icons.Default.ShuffleOn else Icons.Default.Shuffle,
            contentDescription = stringResource(Res.string.cd_shuffle),
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@PreviewScreenSizes
@Composable
private fun ShuffleToggleOffPreview() {
    PreviewTheme {
        Surface { ShuffleToggle(enabled = false, onToggle = {}) }
    }
}

@PreviewScreenSizes
@Composable
private fun ShuffleToggleOnPreview() {
    PreviewTheme {
        Surface { ShuffleToggle(enabled = true, onToggle = {}) }
    }
}
