package com.asr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asr.core.tag.Tag
import com.asr.core.tag.TagRepo
import com.asr.core.interfaces.AlarmScheduler
import com.asr.core.task.Task
import com.asr.core.task.TaskRepo
import com.asr.ui.app.TagFilterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
class TasksViewModel(
    @Provided private val taskRepo: TaskRepo,
    @Provided private val tagRepo: TagRepo,
    @Provided private val alarmScheduler: AlarmScheduler,
) : ViewModel() {
    private val _filter = MutableStateFlow(TaskFilter.ACTIVE)
    private val _expandedIds = MutableStateFlow<Set<Long>>(emptySet())

    private val _state: StateFlow<TasksState> = combine(
        taskRepo.getTasksFlow(),
        tagRepo.getTagsFlow(),
        _filter,
        _expandedIds,
        combine(tagRepo.getTaskTagMappingsFlow(), TagFilterState.selectedTagIds) { m, ids -> m to ids },
    ) { all, tags, filter, expandedIds, (tagMappings, filterTagIds) ->
        val base = when (filter) {
            TaskFilter.ALL -> all
            TaskFilter.ACTIVE -> all.filter { !it.isDone }
            TaskFilter.DONE -> all.filter { it.isDone }
        }
        val tasks = if (filterTagIds.isEmpty()) base
            else base.filter { tagMappings[it.id]?.any { t -> t in filterTagIds } == true }

        TasksState(
            tasks = tasks,
            filter = filter,
            tags = tags,
            expandedTaskIds = expandedIds,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksState(),
    )

    val state = _state

    sealed interface Action {
        data class UpsertTask(val task: Task, val tagIds: List<Long> = emptyList()) : Action
        data class DeleteTask(val task: Task) : Action
        data class ToggleTask(val taskId: Long) : Action
        data class ToggleExpand(val taskId: Long) : Action
        data class SetFilter(val filter: TaskFilter) : Action
        data class CreateTag(val name: String, val color: Long? = null) : Action
        data object DeleteDoneTasks : Action
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.UpsertTask -> viewModelScope.launch {
                val id = taskRepo.upsertTask(action.task)
                action.task.parentId?.let { parentId ->
                    _expandedIds.value = _expandedIds.value + parentId
                }
                alarmScheduler.schedule(action.task.copy(id = id))
                if (action.tagIds.isNotEmpty()) {
                    tagRepo.setTagsForTask(id, action.tagIds)
                }
            }
            is Action.DeleteTask -> viewModelScope.launch {
                alarmScheduler.cancel(action.task)
                taskRepo.deleteTask(action.task)
            }
            is Action.ToggleTask -> viewModelScope.launch {
                taskRepo.toggleTask(action.taskId)
            }
            is Action.ToggleExpand -> {
                val id = action.taskId
                _expandedIds.value = if (id in _expandedIds.value)
                    _expandedIds.value - id
                else _expandedIds.value + id
            }
            is Action.SetFilter -> _filter.value = action.filter
            is Action.CreateTag -> viewModelScope.launch {
                if (action.name.isNotBlank()) tagRepo.upsertTag(Tag(name = action.name, color = action.color))
            }
            is Action.DeleteDoneTasks -> viewModelScope.launch {
                taskRepo.deleteDoneTasks()
            }
        }
    }
}

enum class TaskFilter { ALL, ACTIVE, DONE }

data class TasksState(
    val tasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.ACTIVE,
    val tags: List<Tag> = emptyList(),
    val expandedTaskIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
)
