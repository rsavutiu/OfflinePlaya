package com.offlineplaya.shared.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.TestTags
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
        // The greeting is the hero title, led by an animated time-of-day
        // icon: the sun rises into place in the morning, sits high at noon,
        // sinks at dusk, and the moon drifts in at night — then everything
        // keeps a slow floating bob. All in the album accent.
        Row(verticalAlignment = Alignment.CenterVertically) {
            GreetingIcon(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(26.dp),
            )
            Text(
                text = greetingText(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row {
            IconButton(onClick = onOpenSearch, modifier = Modifier.testTag(TestTags.Home.SEARCH)) {
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

/** Coarse day phases driving the greeting icon and its entry motion. */
private enum class DayPhase { MORNING, NOON, EVENING, NIGHT }

private fun dayPhase() = when (currentHourOfDay()) {
    in 5..11 -> DayPhase.MORNING
    in 12..17 -> DayPhase.NOON
    in 18..21 -> DayPhase.EVENING
    else -> DayPhase.NIGHT
}

/**
 * Time-of-day icon with two layered motions:
 *  - a one-shot entry: the sun *rises* from below in the morning / noon,
 *    *sinks* from above at dusk, and the moon fades in at night;
 *  - a perpetual slow bob (±1.5dp over ~4.5s) so the sky never feels frozen.
 */
@Composable
private fun GreetingIcon(modifier: Modifier = Modifier) {
    val phase = remember { dayPhase() }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    // Where the icon starts relative to its resting spot: below for a
    // sunrise, above for a sunset, in place (fade only) for the moon.
    val startOffset: Dp = when (phase) {
        DayPhase.MORNING -> 14.dp
        DayPhase.NOON -> 7.dp
        DayPhase.EVENING -> (-14).dp
        DayPhase.NIGHT -> 0.dp
    }
    val entryOffset by animateDpAsState(
        targetValue = if (entered) 0.dp else startOffset,
        animationSpec = tween(durationMillis = 1400),
        label = "greetingIconEntry",
    )
    val entryAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 1400),
        label = "greetingIconAlpha",
    )

    val bobTransition = rememberInfiniteTransition(label = "greetingIconBob")
    val bob by bobTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "greetingIconBobValue",
    )

    val icon = when (phase) {
        DayPhase.MORNING -> Icons.Outlined.WbSunny
        DayPhase.NOON -> Icons.Outlined.LightMode
        DayPhase.EVENING -> Icons.Outlined.WbTwilight
        DayPhase.NIGHT -> Icons.Outlined.DarkMode
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .offset(y = entryOffset + bob.dp)
            .alpha(entryAlpha),
    )
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
