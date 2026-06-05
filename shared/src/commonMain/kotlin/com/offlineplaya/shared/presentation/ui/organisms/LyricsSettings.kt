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
import offlineplaya.shared.generated.resources.lyrics_save_sidecar
import offlineplaya.shared.generated.resources.lyrics_save_sidecar_subtitle
import offlineplaya.shared.generated.resources.settings_section_lyrics
import org.jetbrains.compose.resources.stringResource

/**
 * Settings card for lyrics behaviour. Sits between [BurnMetadataSettings]
 * and [LibrarySettings] on the Settings page — lyrics aren't tag-burnable
 * (embedded USLT is virtually never written by other apps), so the knobs
 * are the remote lookup and the optional `.lrc`/`.txt` sidecar write.
 *
 * The save-sidecar row disables when downloading is off (cascade rule
 * also enforced in [LyricsPreferencesStateHolder]), since there's nothing
 * to save if we're not fetching from LRCLIB.
 */
@Composable
fun LyricsSettings(
    preferences: LyricsPreferences,
    onDownloadRemoteLyricsChange: (Boolean) -> Unit,
    onSaveLyricsAsSidecarChange: (Boolean) -> Unit,
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
            SwitchRow(
                title = stringResource(Res.string.lyrics_save_sidecar),
                subtitle = stringResource(Res.string.lyrics_save_sidecar_subtitle),
                checked = preferences.saveLyricsAsSidecar,
                onCheckedChange = onSaveLyricsAsSidecarChange,
                enabled = preferences.downloadRemoteLyrics,
            )
        }
    }
}

@Preview
@Composable
private fun LyricsSettingsBothOnPreview() {
    PreviewTheme {
        LyricsSettings(
            preferences = LyricsPreferences(
                downloadRemoteLyrics = true,
                saveLyricsAsSidecar = true,
            ),
            onDownloadRemoteLyricsChange = {},
            onSaveLyricsAsSidecarChange = {},
        )
    }
}

@Preview
@Composable
private fun LyricsSettingsSidecarGatedPreview() {
    PreviewTheme(darkTheme = true) {
        LyricsSettings(
            preferences = LyricsPreferences(
                downloadRemoteLyrics = false,
                saveLyricsAsSidecar = false,
            ),
            onDownloadRemoteLyricsChange = {},
            onSaveLyricsAsSidecarChange = {},
        )
    }
}
