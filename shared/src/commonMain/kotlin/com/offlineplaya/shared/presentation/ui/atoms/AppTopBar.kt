package com.offlineplaya.shared.presentation.ui.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.cd_back
import offlineplaya.shared.generated.resources.cd_settings
import org.jetbrains.compose.resources.stringResource

/**
 * App-wide "open settings" handler. [App] provides this once; every [AppTopBar]
 * reads it and renders a trailing settings gear when it's non-null — that's how
 * Settings stays reachable from every screen without threading a callback
 * through each page. Provided as `null` while *on* the Settings screen so the
 * gear doesn't appear there.
 */
val LocalOpenSettings = compositionLocalOf<(() -> Unit)?> { null }

/**
 * Themed top app bar atom. Compact 48dp content height (Material's 64dp wastes
 * vertical space on phones). Pass [onBack] to show a back arrow, omit it for a
 * top-level screen with no back affordance. [actions] are right-aligned
 * trailing IconButtons.
 *
 * A settings-gear action is appended automatically (after [actions]) whenever
 * [LocalOpenSettings] is non-null, giving every screen a path to Settings.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val openSettings = LocalOpenSettings.current
    Column(modifier = modifier.fillMaxWidth()) {
        // Status-bar inset preserved so the bar sits below the system UI on
        // edge-to-edge surfaces; content row is compact 48dp.
        Box(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.cd_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                Box(modifier = Modifier.size(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (onBack != null) 4.dp else 8.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                actions()
                if (openSettings != null) {
                    IconButton(onClick = openSettings, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(Res.string.cd_settings),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun AppTopBarRootPreview() {
    PreviewTheme {
        Surface { AppTopBar(title = "Home") }
    }
}

@PreviewScreenSizes
@Composable
private fun AppTopBarWithBackPreview() {
    PreviewTheme {
        Surface { AppTopBar(title = "Settings", onBack = {}) }
    }
}
