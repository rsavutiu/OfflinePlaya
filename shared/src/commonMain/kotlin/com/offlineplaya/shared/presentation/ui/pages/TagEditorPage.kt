package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.TrackTagEdits
import com.offlineplaya.shared.presentation.tag.TagEditorStatus
import com.offlineplaya.shared.presentation.tag.TagEditorUiState
import com.offlineplaya.shared.presentation.ui.atoms.AppTopBar
import com.offlineplaya.shared.presentation.ui.organisms.TagEditorForm
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import com.offlineplaya.shared.domain.model.ScanStatus
import com.offlineplaya.shared.domain.model.Track
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.tag_editor_error
import offlineplaya.shared.generated.resources.tag_editor_not_found
import offlineplaya.shared.generated.resources.tag_editor_title
import org.jetbrains.compose.resources.stringResource

/**
 * Manual tag-editor screen. Renders the editable form for the loaded track and
 * surfaces save progress / errors. Navigation away on success is handled by the
 * caller observing [TagEditorStatus.Saved].
 */
@Composable
fun TagEditorPage(
    state: TagEditorUiState,
    onSave: (Track, TrackTagEdits) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { AppTopBar(title = stringResource(Res.string.tag_editor_title), onBack = onBack) },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val track = state.track
            when {
                state.status == TagEditorStatus.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                track == null ->
                    Text(
                        text = stringResource(Res.string.tag_editor_not_found),
                        modifier = Modifier.align(Alignment.Center),
                    )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = track.fileName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    if (state.status == TagEditorStatus.Error) {
                        Text(
                            text = stringResource(Res.string.tag_editor_error, state.errorMessage ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    TagEditorForm(
                        initial = TrackTagEdits.from(track),
                        onSave = { edits -> onSave(track, edits) },
                        saving = state.status == TagEditorStatus.Saving,
                    )
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun TagEditorPagePreview() {
    PreviewTheme {
        TagEditorPage(
            state = TagEditorUiState(
                track = Track(
                    id = 1, documentUri = "u", treeUri = "t", relativePath = "Queen/x.mp3",
                    fileName = "11. Bohemian Rhapsody.mp3", title = "Bohemian Rhapsody",
                    artistName = "Queen", albumArtistName = "Queen", albumName = "A Night at the Opera",
                    genre = "Rock", year = 1975, trackNumber = 11, discNumber = 1, durationMs = 354_000L,
                    bitrate = 320_000, sampleRate = 44_100, channels = 2, codec = "mp3",
                    artistId = 1L, albumId = 1L, folderId = 1L, scanStatus = ScanStatus.SCANNED,
                ),
                status = TagEditorStatus.Editing,
            ),
            onSave = { _, _ -> },
            onBack = {},
        )
    }
}
