package com.offlineplaya.shared.presentation.ui.templates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Default cap for a single "readable" content column.
 *
 * Sized so phones (portrait *and* landscape) and foldables fill the width
 * edge-to-edge — the old 720dp cap left fat empty gutters on a landscape
 * phone, which read as wasted space. Only genuinely wide surfaces (large
 * tablets, car displays) exceed this and get a centered column instead of
 * sparse full-bleed lines.
 */
val ReadableContentWidth: Dp = 1100.dp

/**
 * Centers [content] horizontally and caps its width at [maxWidth] once the
 * available width exceeds that cap. Below the cap it behaves like a plain
 * full-width container, so portrait phone layouts are unchanged.
 *
 * Wrap a page's scrollable body (LazyColumn / scrolling Column) in this so the
 * same code reads well on a phone and doesn't look broken in landscape.
 */
@Composable
fun ResponsiveContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ReadableContentWidth,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable () -> Unit,
) {
    val align = when (horizontalAlignment) {
        Alignment.Start -> Alignment.TopStart
        Alignment.End -> Alignment.TopEnd
        else -> Alignment.TopCenter
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = align,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxSize(),
        ) {
            content()
        }
    }
}
