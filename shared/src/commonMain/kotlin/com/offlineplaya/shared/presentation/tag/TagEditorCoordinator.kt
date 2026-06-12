package com.offlineplaya.shared.presentation.tag

import com.offlineplaya.shared.domain.model.Track
import com.offlineplaya.shared.domain.model.TrackTagEdits
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.domain.usecase.EditTrackTagsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the tag-editor screen: loads the track being edited and runs the
 * save through [EditTrackTagsUseCase], exposing a single hot [state] the page
 * observes (mirrors `BurnMetadataCoordinator`).
 */
class TagEditorCoordinator(
    private val tracks: TrackRepository,
    private val editUseCase: EditTrackTagsUseCase,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(TagEditorUiState())
    val state: StateFlow<TagEditorUiState> = _state.asStateFlow()

    /** Load the track to edit. Call once when the editor opens. */
    fun load(trackId: Long) {
        scope.launch {
            val track = tracks.findById(trackId)
            _state.value = TagEditorUiState(
                track = track,
                status = if (track == null) TagEditorStatus.NotFound else TagEditorStatus.Editing,
            )
        }
    }

    /** Write [edits] to [track]'s file and update the library. */
    fun save(track: Track, edits: TrackTagEdits) {
        scope.launch {
            _state.value = _state.value.copy(status = TagEditorStatus.Saving, errorMessage = null)
            val result = editUseCase.edit(track, edits)
            _state.value = _state.value.copy(
                status = if (result.isSuccess) TagEditorStatus.Saved else TagEditorStatus.Error,
                errorMessage = result.exceptionOrNull()?.message,
            )
        }
    }

    /** Reset to a clean editing state after the UI has reacted to Saved/Error. */
    fun consumeResult() {
        _state.value = _state.value.copy(status = TagEditorStatus.Editing, errorMessage = null)
    }
}

data class TagEditorUiState(
    val track: Track? = null,
    val status: TagEditorStatus = TagEditorStatus.Loading,
    val errorMessage: String? = null,
)

enum class TagEditorStatus { Loading, Editing, Saving, Saved, Error, NotFound }
