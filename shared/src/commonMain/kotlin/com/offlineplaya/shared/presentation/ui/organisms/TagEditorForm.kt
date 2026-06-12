package com.offlineplaya.shared.presentation.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.domain.model.TrackTagEdits
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.tag_editor_field_album
import offlineplaya.shared.generated.resources.tag_editor_field_album_artist
import offlineplaya.shared.generated.resources.tag_editor_field_artist
import offlineplaya.shared.generated.resources.tag_editor_field_disc
import offlineplaya.shared.generated.resources.tag_editor_field_genre
import offlineplaya.shared.generated.resources.tag_editor_field_title
import offlineplaya.shared.generated.resources.tag_editor_field_track
import offlineplaya.shared.generated.resources.tag_editor_field_year
import offlineplaya.shared.generated.resources.tag_editor_save
import offlineplaya.shared.generated.resources.tag_editor_saving
import offlineplaya.shared.generated.resources.tag_editor_write_hint
import org.jetbrains.compose.resources.stringResource

/**
 * Editable form for a track's tags. Owns its field state (seeded from
 * [initial]); on Save it assembles a [TrackTagEdits] — blank text fields and
 * unparseable numbers become `null` (= clear the tag) — and hands it to [onSave].
 */
@Composable
fun TagEditorForm(
    initial: TrackTagEdits,
    onSave: (TrackTagEdits) -> Unit,
    modifier: Modifier = Modifier,
    saving: Boolean = false,
) {
    // Keyed on initial so loading a different track reseeds the fields.
    var title by rememberSaveable(initial) { mutableStateOf(initial.title.orEmpty()) }
    var artist by rememberSaveable(initial) { mutableStateOf(initial.artist.orEmpty()) }
    var albumArtist by rememberSaveable(initial) { mutableStateOf(initial.albumArtist.orEmpty()) }
    var album by rememberSaveable(initial) { mutableStateOf(initial.album.orEmpty()) }
    var genre by rememberSaveable(initial) { mutableStateOf(initial.genre.orEmpty()) }
    var year by rememberSaveable(initial) { mutableStateOf(initial.year?.toString().orEmpty()) }
    var trackNo by rememberSaveable(initial) { mutableStateOf(initial.trackNumber?.toString().orEmpty()) }
    var discNo by rememberSaveable(initial) { mutableStateOf(initial.discNumber?.toString().orEmpty()) }

    val numberKeyboard = remember { KeyboardOptions(keyboardType = KeyboardType.Number) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Field(title, { title = it }, stringResource(Res.string.tag_editor_field_title))
        Field(artist, { artist = it }, stringResource(Res.string.tag_editor_field_artist))
        Field(albumArtist, { albumArtist = it }, stringResource(Res.string.tag_editor_field_album_artist))
        Field(album, { album = it }, stringResource(Res.string.tag_editor_field_album))
        Field(genre, { genre = it }, stringResource(Res.string.tag_editor_field_genre))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(year, { year = it }, stringResource(Res.string.tag_editor_field_year), numberKeyboard, Modifier.weight(1f))
            Field(trackNo, { trackNo = it }, stringResource(Res.string.tag_editor_field_track), numberKeyboard, Modifier.weight(1f))
            Field(discNo, { discNo = it }, stringResource(Res.string.tag_editor_field_disc), numberKeyboard, Modifier.weight(1f))
        }

        Text(
            text = stringResource(Res.string.tag_editor_write_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Button(
            onClick = {
                onSave(
                    TrackTagEdits(
                        title = title.trim().ifBlank { null },
                        artist = artist.trim().ifBlank { null },
                        albumArtist = albumArtist.trim().ifBlank { null },
                        album = album.trim().ifBlank { null },
                        genre = genre.trim().ifBlank { null },
                        year = year.trim().toIntOrNull(),
                        trackNumber = trackNo.trim().toIntOrNull(),
                        discNumber = discNo.trim().toIntOrNull(),
                    ),
                )
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(
                if (saving) stringResource(Res.string.tag_editor_saving)
                else stringResource(Res.string.tag_editor_save),
            )
        }
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        modifier = modifier,
    )
}

@PreviewScreenSizes
@Composable
private fun TagEditorFormPreview() {
    PreviewTheme {
        TagEditorForm(
            initial = TrackTagEdits(
                title = "Bohemian Rhapsody", artist = "Queen", albumArtist = "Queen",
                album = "A Night at the Opera", genre = "Rock",
                year = 1975, trackNumber = 11, discNumber = 1,
            ),
            onSave = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
