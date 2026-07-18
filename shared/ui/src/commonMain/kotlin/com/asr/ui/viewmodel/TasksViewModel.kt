package com.asr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asr.core.tag.Tag
import com.asr.core.tag.TagRepo
import com.asr.core.interfaces.AlarmScheduler
import com.asr.core.task.Task
import com.asr.core.task.TaskRepo
import com.asr.core.sortedByPinAndDate
import com.asr.ui.app.FilterState
import com.asr.ui.app.Filters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
class TasksViewModel(
    @Provided private val taskRepo: TaskRepo,
    @Provided private val tagRepo: TagRepo,
    @Provided private val alarmScheduler: AlarmScheduler,
) : ViewModel() {
    private val _taskFilter = MutableStateFlow(TaskFilter.ACTIVE)
    private val _expandedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _filter = MutableStateFlow(FilterState())
    private val _pendingDeleted = MutableStateFlow<List<Task>?>(null)
    private val _createdTagId = MutableStateFlow<Long?>(null)
    val createdTagId: StateFlow<Long?> = _createdTagId.asStateFlow()

    private val _state: StateFlow<TasksState> = combine(
        taskRepo.getTasksFlow(),
        tagRepo.getTagsFlow(),
        _taskFilter,
        _expandedIds,
        combine(_filter, tagRepo.getTaskTagMappingsFlow(), _pendingDeleted) { f, m, p ->
            FilterWithMappings(f, m, p)
        },
    ) { all, tags, taskFilter, expandedIds, (filter, tagMappings, pendingDeleted) ->
        val parentTaskIds = all.filter { it.parentId != null }.map { it.parentId!! }.toSet()
        val base = when (taskFilter) {
            TaskFilter.ALL -> all
            TaskFilter.ACTIVE -> all.filter { !it.isDone }
            TaskFilter.DONE -> all.filter { it.isDone }
        }

        TasksState(
            tasks = Filters.tasks(base.sortedByPinAndDate(), tagMappings, filter.searchQuery, filter.selectedTagIds, filter.filterDate),
            filter = taskFilter,
            tags = tags,
            expandedTaskIds = expandedIds,
            filterState = filter,
            pendingDeletedTasks = pendingDeleted,
            taskTagMappings = tagMappings,
            parentTaskIds = parentTaskIds,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksState(),
    )

    val state = _state

    private data class FilterWithMappings(
        val filter: FilterState,
        val tagMappings: Map<Long, List<Long>>,
        val pendingDeleted: List<Task>? = null,
    )

    sealed interface Action {
        data class UpsertTask(val task: Task, val tagIds: List<Long> = emptyList()) : Action
        data class DeleteTask(val task: Task) : Action
        data class ToggleTask(val taskId: Long) : Action
        data class ToggleExpand(val taskId: Long) : Action
        data class SetFilter(val filter: TaskFilter) : Action
        data class CreateTag(val name: String, val color: Long? = null) : Action
        data class TogglePinTask(val taskId: Long) : Action
        data object DeleteDoneTasks : Action
        data object UndoDeleteDoneTasks : Action
        data object DismissDeletedTasks : Action
        data class SetSearchQuery(val query: String) : Action
        data class ToggleTag(val tagId: Long) : Action
        data object ClearTagFilter : Action
        data object ConsumeCreatedTag : Action
        data class SetFilterDate(val date: LocalDate?) : Action
        data object ToggleFilterSheet : Action
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
            is Action.SetFilter -> _taskFilter.value = action.filter
            is Action.TogglePinTask -> viewModelScope.launch {
                val task = taskRepo.getTaskById(action.taskId) ?: return@launch
                taskRepo.upsertTask(task.copy(isPinned = !task.isPinned))
            }
            is Action.CreateTag -> viewModelScope.launch {
                if (action.name.isNotBlank()) {
                    val id = tagRepo.upsertTag(Tag(name = action.name, color = action.color))
                    _createdTagId.value = id
                }
            }
            is Action.ConsumeCreatedTag -> _createdTagId.value = null
            is Action.DeleteDoneTasks -> viewModelScope.launch {
                val doneTasks = taskRepo.getDoneTasksFlow().first()
                taskRepo.deleteDoneTasks()
                _pendingDeleted.value = doneTasks
            }
            is Action.UndoDeleteDoneTasks -> viewModelScope.launch {
                val tasks = _pendingDeleted.value ?: return@launch
                tasks.forEach { taskRepo.upsertTask(it) }
                _pendingDeleted.value = null
            }
            is Action.DismissDeletedTasks -> {
                _pendingDeleted.value = null
            }
            is Action.SetSearchQuery -> _filter.value = _filter.value.copy(searchQuery = action.query)
            is Action.ToggleTag -> {
                val ids = _filter.value.selectedTagIds
                _filter.value = _filter.value.copy(
                    selectedTagIds = if (action.tagId in ids) ids - action.tagId else ids + action.tagId
                )
            }
            is Action.ClearTagFilter -> _filter.value = _filter.value.copy(selectedTagIds = emptySet())
            is Action.SetFilterDate -> _filter.value = _filter.value.copy(filterDate = action.date)
            is Action.ToggleFilterSheet -> _filter.value = _filter.value.copy(showFilterSheet = !_filter.value.showFilterSheet)
        }
    }
}

enum class TaskFilter { ALL, ACTIVE, DONE }

data class TasksState(
    val tasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.ACTIVE,
    val tags: List<Tag> = emptyList(),
    val expandedTaskIds: Set<Long> = emptySet(),
    val filterState: FilterState = FilterState(),
    val pendingDeletedTasks: List<Task>? = null,
    val taskTagMappings: Map<Long, List<Long>> = emptyMap(),
    val parentTaskIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
)
