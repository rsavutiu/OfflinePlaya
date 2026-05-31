package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.cd_play
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlayButton(onPlay: () -> Unit) {
    IconButton(onClick = onPlay) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = stringResource(Res.string.cd_play),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@PreviewScreenSizes
@Composable
fun PlayButtonPreview() {
    PlayButton {  }
}