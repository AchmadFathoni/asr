package com.asr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asr.core.backup.ExportRepo
import com.asr.core.backup.ExportState
import com.asr.core.backup.RestoreRepo
import com.asr.core.backup.RestoreResult
import com.asr.core.backup.RestoreState
import com.asr.core.settings.SettingsRepo
import com.asr.core.tag.Tag
import com.asr.core.tag.TagRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
class SettingsViewModel(
    @Provided private val exportRepo: ExportRepo,
    @Provided private val restoreRepo: RestoreRepo,
    @Provided private val tagRepo: TagRepo,
    @Provided private val settingsRepo: SettingsRepo,
) : ViewModel() {
    private val _exportState = MutableStateFlow(ExportState.IDLE)
    private val _restoreState = MutableStateFlow(RestoreState.IDLE)
    private val _newTagName = MutableStateFlow("")
    private val _newTagColor = MutableStateFlow<Long?>(null)
    private val _isDarkMode = MutableStateFlow(settingsRepo.isDarkMode())

    val state: StateFlow<SettingsState> = combine(
        _exportState,
        _restoreState,
        tagRepo.getTagsFlow(),
        combine(_newTagName, _newTagColor) { n, c -> n to c },
        _isDarkMode,
    ) { exportState, restoreState, tags, (newName, newColor), isDark ->
        SettingsState(
            exportState = exportState,
            restoreState = restoreState,
            tags = tags,
            newTagName = newName,
            newTagColor = newColor,
            isDarkMode = isDark,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState(),
    )

    sealed interface Action {
        data object Export : Action
        data object Restore : Action
        data class SetNewTagName(val name: String) : Action
        data class SetNewTagColor(val color: Long?) : Action
        data object CreateTag : Action
        data class DeleteTag(val id: Long) : Action
        data class SetTagColor(val tagId: Long, val color: Long?) : Action
        data class SetDarkMode(val isDark: Boolean?) : Action
    }

    fun onAction(action: Action) {
        when (action) {
            Action.Export -> viewModelScope.launch {
                _exportState.value = ExportState.EXPORTING
                exportRepo.exportToJson()
                _exportState.value = ExportState.EXPORTED
            }
            Action.Restore -> viewModelScope.launch {
                _restoreState.value = RestoreState.RESTORING
                when (val result = restoreRepo.restoreData()) {
                    is RestoreResult.Success ->
                        _restoreState.value = RestoreState.RESTORED
                    is RestoreResult.Failure ->
                        _restoreState.value = RestoreState.FAILURE
                }
            }
            is Action.SetNewTagName -> _newTagName.value = action.name
            is Action.SetNewTagColor -> _newTagColor.value = action.color
            Action.CreateTag -> viewModelScope.launch {
                val name = _newTagName.value.trim()
                if (name.isNotBlank()) {
                    tagRepo.upsertTag(Tag(name = name, color = _newTagColor.value))
                    _newTagName.value = ""
                    _newTagColor.value = null
                }
            }
            is Action.DeleteTag -> viewModelScope.launch {
                tagRepo.deleteTag(action.id)
            }
            is Action.SetTagColor -> viewModelScope.launch {
                val existing = tagRepo.getTagById(action.tagId) ?: return@launch
                tagRepo.upsertTag(existing.copy(color = action.color))
            }
            is Action.SetDarkMode -> {
                _isDarkMode.value = action.isDark
                settingsRepo.setDarkMode(action.isDark)
            }
        }
    }
}

data class SettingsState(
    val exportState: ExportState = ExportState.IDLE,
    val restoreState: RestoreState = RestoreState.IDLE,
    val tags: List<Tag> = emptyList(),
    val newTagName: String = "",
    val newTagColor: Long? = null,
    val isDarkMode: Boolean? = null,
)
