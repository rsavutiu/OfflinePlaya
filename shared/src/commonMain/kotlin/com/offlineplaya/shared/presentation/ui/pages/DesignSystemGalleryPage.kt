package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.templates.ResponsiveContent
import com.offlineplaya.shared.presentation.ui.theme.AppElevation
import com.offlineplaya.shared.presentation.ui.theme.AppShapes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.OfflinePlayaTheme
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme

/**
 * One-screen catalog of every design token: colors, type, shapes, spacing,
 * elevation. Read it like a style guide — if you can't find a token that
 * matches what a design needs, the answer is "add it to the system", not
 * "hard-code a one-off".
 *
 * Used by the Preview MCP for visual regression: each token block has its
 * own @PreviewScreenSizes, so changes to the design system show up as obvious image
 * diffs rather than getting lost across 30 page screenshots.
 */
@Composable
fun DesignSystemGalleryPage(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = "Design system", onBack = onBack) },
    ) { inner ->
        ResponsiveContent(modifier = Modifier.padding(inner)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
            ) {
                ColorsBlock()
                TypographyBlock()
                ShapesBlock()
                SpacingBlock()
                ElevationBlock()
            }
        }
    }
}

// --- Color block ---

@Composable
private fun ColorsBlock() {
    SectionHeader("Colors")
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        ColorRow("primary / onPrimary", scheme.primary, scheme.onPrimary)
        ColorRow(
            "primaryContainer / onPrimaryContainer",
            scheme.primaryContainer,
            scheme.onPrimaryContainer,
        )
        ColorRow("secondary / onSecondary", scheme.secondary, scheme.onSecondary)
        ColorRow(
            "secondaryContainer / onSecondaryContainer",
            scheme.secondaryContainer,
            scheme.onSecondaryContainer,
        )
        ColorRow("tertiary / onTertiary", scheme.tertiary, scheme.onTertiary)
        ColorRow(
            "tertiaryContainer / onTertiaryContainer",
            scheme.tertiaryContainer,
            scheme.onTertiaryContainer,
        )
        ColorRow("surface / onSurface", scheme.surface, scheme.onSurface)
        ColorRow(
            "surfaceVariant / onSurfaceVariant",
            scheme.surfaceVariant,
            scheme.onSurfaceVariant,
        )
        ColorRow("error / onError", scheme.error, scheme.onError)
    }
}

@Composable
private fun ColorRow(name: String, bg: Color, fg: Color) {
    Surface(
        color = bg,
        contentColor = fg,
        shape = AppShapes.card,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(AppSpacing.md),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

// --- Typography block ---

@Composable
private fun TypographyBlock() {
    SectionHeader("Typography")
    val t = MaterialTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        TypeSpecimen("displayLarge", t.displayLarge.fontSize.value)
        TypeSpecimen("headlineLarge", t.headlineLarge.fontSize.value)
        TypeSpecimen("headlineMedium", t.headlineMedium.fontSize.value)
        TypeSpecimen("titleLarge", t.titleLarge.fontSize.value)
        TypeSpecimen("titleMedium", t.titleMedium.fontSize.value)
        TypeSpecimen("bodyLarge", t.bodyLarge.fontSize.value)
        TypeSpecimen("bodyMedium", t.bodyMedium.fontSize.value)
        TypeSpecimen("labelLarge", t.labelLarge.fontSize.value)
        TypeSpecimen("labelSmall", t.labelSmall.fontSize.value)
    }
}

@Composable
private fun TypeSpecimen(name: String, sizeSp: Float) {
    val style = when (name) {
        "displayLarge" -> MaterialTheme.typography.displayLarge
        "headlineLarge" -> MaterialTheme.typography.headlineLarge
        "headlineMedium" -> MaterialTheme.typography.headlineMedium
        "titleLarge" -> MaterialTheme.typography.titleLarge
        "titleMedium" -> MaterialTheme.typography.titleMedium
        "bodyLarge" -> MaterialTheme.typography.bodyLarge
        "bodyMedium" -> MaterialTheme.typography.bodyMedium
        "labelLarge" -> MaterialTheme.typography.labelLarge
        "labelSmall" -> MaterialTheme.typography.labelSmall
        else -> MaterialTheme.typography.bodyMedium
    }
    Column {
        Text(text = "$name · ${sizeSp.toInt()}sp", style = MaterialTheme.typography.labelSmall)
        Text(text = "The quick brown fox jumps over the lazy dog.", style = style)
    }
}

// --- Shapes block ---

@Composable
private fun ShapesBlock() {
    SectionHeader("Shapes")
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        ShapeChip("AppShapes.tile", AppShapes.tile)
        ShapeChip("AppShapes.card", AppShapes.card)
        ShapeChip("AppShapes.floating", AppShapes.floating)
        ShapeChip("AppShapes.dialog", AppShapes.dialog)
        ShapeChip("AppShapes.pill", AppShapes.pill)
    }
}

@Composable
private fun ShapeChip(name: String, shape: androidx.compose.ui.graphics.Shape) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(width = 96.dp, height = 56.dp),
        ) {}
        Text(name, style = MaterialTheme.typography.labelLarge)
    }
}

// --- Spacing block ---

@Composable
private fun SpacingBlock() {
    SectionHeader("Spacing")
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        SpacingRow("xxs", AppSpacing.xxs)
        SpacingRow("xs", AppSpacing.xs)
        SpacingRow("sm", AppSpacing.sm)
        SpacingRow("md", AppSpacing.md)
        SpacingRow("lg", AppSpacing.lg)
        SpacingRow("xl", AppSpacing.xl)
        SpacingRow("xxl", AppSpacing.xxl)
        SpacingRow("xxxl", AppSpacing.xxxl)
    }
}

@Composable
private fun SpacingRow(name: String, size: Dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .width(size)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.primary, AppShapes.tile),
        )
        Text("$name · $size", style = MaterialTheme.typography.labelLarge)
    }
}

// --- Elevation block ---

@Composable
private fun ElevationBlock() {
    SectionHeader("Elevation")
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        ElevationCard("level0", AppElevation.level0)
        ElevationCard("level1", AppElevation.level1)
        ElevationCard("level2", AppElevation.level2)
        ElevationCard("level3", AppElevation.level3)
        ElevationCard("level4", AppElevation.level4)
        ElevationCard("level5", AppElevation.level5)
    }
}

@Composable
private fun ElevationCard(name: String, elevation: Dp) {
    Surface(
        tonalElevation = elevation,
        shadowElevation = elevation,
        shape = AppShapes.card,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "$name · $elevation",
            modifier = Modifier.padding(AppSpacing.md),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

// --- Shared helpers ---

@Composable
private fun SectionHeader(text: String) {
    Column {
        Text(text, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(AppSpacing.xs))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)),
        )
    }
}

// --- Previews — these are the visual-regression entry points ---

@PreviewScreenSizes
@Composable
private fun DesignSystemGalleryLightPreview() {
    PreviewTheme(darkTheme = false) {
        Surface {
            DesignSystemGalleryPage()
        }
    }
}

@PreviewScreenSizes
@Composable
private fun DesignSystemGalleryDarkPreview() {
    PreviewTheme(darkTheme = true) {
        Surface {
            DesignSystemGalleryPage()
        }
    }
}

@PreviewScreenSizes
@Composable
private fun DesignSystemColorsPreview() {
    OfflinePlayaTheme {
        Surface { Column(Modifier.padding(AppSpacing.lg)) { ColorsBlock() } }
    }
}

@PreviewScreenSizes
@Composable
private fun DesignSystemTypographyPreview() {
    OfflinePlayaTheme {
        Surface { Column(Modifier.padding(AppSpacing.lg)) { TypographyBlock() } }
    }
}

@PreviewScreenSizes
@Composable
private fun DesignSystemShapesPreview() {
    OfflinePlayaTheme {
        Surface { Column(Modifier.padding(AppSpacing.lg)) { ShapesBlock() } }
    }
}

@PreviewScreenSizes
@Composable
private fun DesignSystemSpacingPreview() {
    OfflinePlayaTheme {
        Surface { Column(Modifier.padding(AppSpacing.lg)) { SpacingBlock() } }
    }
}

@PreviewScreenSizes
@Composable
private fun DesignSystemElevationPreview() {
    OfflinePlayaTheme {
        Surface { Column(Modifier.padding(AppSpacing.lg)) { ElevationBlock() } }
    }
}
