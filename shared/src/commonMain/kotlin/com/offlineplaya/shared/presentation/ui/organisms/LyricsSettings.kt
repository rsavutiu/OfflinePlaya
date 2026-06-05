package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.LyricsPreferences
import com.offlineplaya.shared.presentation.ui.molecules.SettingsSection
import com.offlineplaya.shared.presentation.ui.molecules.SwitchRow
import com.offlineplaya.shared.presentation.ui.preview.Preview
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.lyrics_download_remote
import offlineplaya.shared.generated.resources.lyrics_download_remote_subtitle
import offlineplaya.shared.generated.resources.settings_section_lyrics
import org.jetbrains.compose.resources.stringResource

/**
 * Single-toggle settings card for lyrics behaviour. Sits between
 * [BurnMetadataSettings] and [PlaybackSettings] on the Settings page —
 * lyrics aren't tag-burnable (embedded USLT is virtually never written
 * by other apps), so the only knob is the remote lookup.
 */
@Composable
fun LyricsSettings(
    preferences: LyricsPreferences,
    onDownloadRemoteLyricsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(
        title = stringResource(Res.string.settings_section_lyrics),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            SwitchRow(
                title = stringResource(Res.string.lyrics_download_remote),
                subtitle = stringResource(Res.string.lyrics_download_remote_subtitle),
                checked = preferences.downloadRemoteLyrics,
                onCheckedChange = onDownloadRemoteLyricsChange,
            )
        }
    }
}

@Preview
@Composable
private fun LyricsSettingsOnPreview() {
    PreviewTheme {
        LyricsSettings(
            preferences = LyricsPreferences(downloadRemoteLyrics = true),
            onDownloadRemoteLyricsChange = {},
        )
    }
}

@Preview
@Composable
private fun LyricsSettingsOffPreview() {
    PreviewTheme(darkTheme = true) {
        LyricsSettings(
            preferences = LyricsPreferences(downloadRemoteLyrics = false),
            onDownloadRemoteLyricsChange = {},
        )
    }
}
