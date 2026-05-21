package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.Composable
import com.offlineplaya.shared.presentation.ui.preview.Preview

@Composable
fun PlayButton(onPlay: () -> Unit) {
    IconButton(onClick = onPlay) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview
@Composable
fun PlayButtonPreview() {
    PlayButton {  }
}