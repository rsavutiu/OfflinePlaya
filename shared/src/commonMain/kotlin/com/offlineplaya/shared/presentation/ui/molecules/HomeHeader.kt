package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import com.offlineplaya.shared.util.currentHourOfDay
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.home_greeting_afternoon
import offlineplaya.shared.generated.resources.home_greeting_evening
import offlineplaya.shared.generated.resources.home_greeting_morning
import offlineplaya.shared.generated.resources.now_playing_search
import offlineplaya.shared.generated.resources.now_playing_settings
import org.jetbrains.compose.resources.stringResource

/**
 * Home-page header: the time-of-day greeting as the hero title on the left,
 * search and settings icons on the right. Replaces an [AppTopBar] for the
 * landing surface so the title carries more weight than a system bar.
 */
@Composable
fun HomeHeader(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            // The greeting is now the hero title (the redundant "Library" line
            // was dropped). It wears the album accent so the page's biggest
            // text visibly reacts to the cover.
            Text(
                text = greetingText(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row {
            IconButton(onClick = onOpenSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(Res.string.now_playing_search),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(Res.string.now_playing_settings),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun greetingText(): String {
    val res = when (currentHourOfDay()) {
        in 5..11 -> Res.string.home_greeting_morning
        in 12..17 -> Res.string.home_greeting_afternoon
        else -> Res.string.home_greeting_evening
    }
    return stringResource(res)
}

@PreviewScreenSizes
@Composable
private fun HomeHeaderPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            HomeHeader(onOpenSearch = {}, onOpenSettings = {})
        }
    }
}
