package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * Themed top app bar atom. Pass [onBack] to show a back arrow, omit it for a
 * top-level screen with no back affordance. [actions] are right-aligned
 * trailing IconButtons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    // Container uses `surfaceContainer` rather than `surface` so the bar
    // visually separates from the page content beneath it instead of bleeding
    // into the same plane. M3 specifies this as the resting top-bar color.
    TopAppBar(
        modifier = modifier,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Preview
@Composable
private fun AppTopBarRootPreview() {
    PreviewTheme {
        Surface { AppTopBar(title = "Home") }
    }
}

@Preview
@Composable
private fun AppTopBarWithBackPreview() {
    PreviewTheme {
        Surface { AppTopBar(title = "Settings", onBack = {}) }
    }
}
