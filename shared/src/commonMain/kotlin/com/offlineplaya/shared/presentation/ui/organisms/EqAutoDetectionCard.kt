package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.CanonicalGenre
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.eq_auto_explainer
import offlineplaya.shared.generated.resources.eq_detected_genre
import offlineplaya.shared.generated.resources.eq_fallback_no_tag
import offlineplaya.shared.generated.resources.eq_fallback_unknown_tag
import offlineplaya.shared.generated.resources.eq_label_applied_preset
import offlineplaya.shared.generated.resources.eq_label_from_tag
import offlineplaya.shared.generated.resources.eq_label_now_playing
import offlineplaya.shared.generated.resources.eq_no_genre_tag
import offlineplaya.shared.generated.resources.eq_nothing_playing
import org.jetbrains.compose.resources.stringResource

/**
 * Makes Auto-mode EQ legible: shows what the app read off the track and what it
 * did with it. Genre inference is tag-based — it reads the file's embedded
 * genre tag and buckets it (e.g. "EDM"/"house" → Electronic). When a track has
 * no tag there's nothing to infer, so it stays on Default (flat) and we say so
 * outright rather than leaving the user wondering why nothing changed.
 */
@Composable
fun EqAutoDetectionCard(
    nowPlayingTitle: String?,
    rawGenreTag: String?,
    detectedGenreLabel: String?,
    appliedPresetName: String,
    modifier: Modifier = Modifier,
) {
    val detected = detectedGenreLabel ?: DEFAULT_GENRE_LABEL
    val isFlatFallback = detectedGenreLabel == null || detectedGenreLabel == DEFAULT_GENRE_LABEL

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppSpacing.md),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(modifier = Modifier.padding(AppSpacing.lg)) {
            if (nowPlayingTitle == null) {
                NothingPlayingHint()
                return@Column
            }

            Text(
                text = stringResource(Res.string.eq_detected_genre),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(AppSpacing.xs))
            Text(
                text = detected,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isFlatFallback) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Spacer(Modifier.height(AppSpacing.sm))

            DetailRow(stringResource(Res.string.eq_label_applied_preset), appliedPresetName)
            DetailRow(
                stringResource(Res.string.eq_label_from_tag),
                rawGenreTag?.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.eq_no_genre_tag),
            )
            DetailRow(stringResource(Res.string.eq_label_now_playing), nowPlayingTitle)

            if (isFlatFallback) {
                Spacer(Modifier.height(AppSpacing.sm))
                Text(
                    text = if (rawGenreTag.isNullOrBlank()) {
                        stringResource(Res.string.eq_fallback_no_tag)
                    } else {
                        stringResource(Res.string.eq_fallback_unknown_tag, rawGenreTag)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NothingPlayingHint() {
    Text(
        text = stringResource(Res.string.eq_nothing_playing),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(AppSpacing.xs))
    Text(
        text = stringResource(Res.string.eq_auto_explainer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs / 2),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Human label for the catch-all "no inference" canonical genre. */
private const val DEFAULT_GENRE_LABEL = "Default"

/** Convert a canonical genre to a human label. */
fun CanonicalGenre.userLabel(): String = when (this) {
    CanonicalGenre.ROCK -> "Rock"
    CanonicalGenre.POP -> "Pop"
    CanonicalGenre.ELECTRONIC -> "Electronic"
    CanonicalGenre.JAZZ -> "Jazz"
    CanonicalGenre.CLASSICAL -> "Classical"
    CanonicalGenre.HIPHOP -> "Hip-Hop"
    CanonicalGenre.DEFAULT -> DEFAULT_GENRE_LABEL
}

@PreviewScreenSizes
@Composable
private fun EqAutoDetectionCardPreview() {
    PreviewTheme {
        EqAutoDetectionCard(
            nowPlayingTitle = "Strobe",
            rawGenreTag = "EDM",
            detectedGenreLabel = "Electronic",
            appliedPresetName = "Electronic",
        )
    }
}

@PreviewScreenSizes
@Composable
private fun EqAutoDetectionCardNoTagPreview() {
    PreviewTheme {
        EqAutoDetectionCard(
            nowPlayingTitle = "Untitled Recording",
            rawGenreTag = null,
            detectedGenreLabel = null,
            appliedPresetName = "Default",
        )
    }
}

@PreviewScreenSizes
@Composable
private fun EqAutoDetectionCardNothingPlayingPreview() {
    PreviewTheme {
        EqAutoDetectionCard(
            nowPlayingTitle = null,
            rawGenreTag = null,
            detectedGenreLabel = null,
            appliedPresetName = "Default",
        )
    }
}
