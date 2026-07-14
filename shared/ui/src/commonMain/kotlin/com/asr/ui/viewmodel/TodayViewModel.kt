package com.asr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asr.core.habit.Habit
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitRepo
import com.asr.core.habit.HabitState
import com.asr.core.habit.habitRecordWithNewState
import com.asr.core.habit.shouldShowToday
import com.asr.core.now
import com.asr.core.tag.Tag
import com.asr.core.tag.TagRepo
import com.asr.core.task.Task
import com.asr.core.task.TaskRepo
import com.asr.ui.app.FilterState
import com.asr.ui.app.Filters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
class TodayViewModel(
    @Provided private val taskRepo: TaskRepo,
    @Provided private val habitRepo: HabitRepo,
    @Provided private val tagRepo: TagRepo,
) : ViewModel() {
    private val today = LocalDate.now()
    private val _filter = MutableStateFlow(FilterState())

    private val _state: StateFlow<TodayState> = combine(
        taskRepo.getUndoneTasksFlow(),
        habitRepo.getHabitsFlow(),
        habitRepo.getRecordsForDateFlow(today),
        tagRepo.getTagsFlow(),
        combine(_filter, tagRepo.getTaskTagMappingsFlow(), tagRepo.getHabitTagMappingsFlow()) { f, ttm, htm ->
            FilterWithMappings(f, ttm, htm)
        },
    ) { tasks, habits, records, tags, (filter, ttm, htm) ->
        val baseTasks = tasks.filter { val due = it.dueDate; due == null || due <= today }
        val baseHabits = habits.filter { it.shouldShowToday(today) }
        TodayState(
            tasks = Filters.tasks(baseTasks, ttm, filter.searchQuery, filter.selectedTagIds, null),
            habits = Filters.habits(baseHabits, htm, filter.searchQuery, filter.selectedTagIds, null),
            tags = tags,
            habitRecords = records.associateBy { it.habitId },
            filter = filter,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayState(),
    )

    val state = _state

    private data class FilterWithMappings(
        val filter: FilterState,
        val taskTagMappings: Map<Long, List<Long>>,
        val habitTagMappings: Map<Long, List<Long>>,
    )

    sealed interface Action {
        data class ToggleTask(val taskId: Long) : Action
        data class ToggleHabit(val habitId: Long, val newState: HabitState) : Action
        data object DeleteDoneTasks : Action
        data class SetSearchQuery(val query: String) : Action
        data class ToggleTag(val tagId: Long) : Action
        data object ClearTagFilter : Action
        data object ToggleFilterSheet : Action
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.ToggleTask -> viewModelScope.launch {
                taskRepo.toggleTask(action.taskId)
            }
            is Action.ToggleHabit -> viewModelScope.launch {
                val existing = habitRepo.getRecordForDate(action.habitId, today)
                val habit = habitRepo.getHabitById(action.habitId) ?: return@launch
                habitRepo.upsertRecord(habitRecordWithNewState(existing, habit, today, action.newState))
            }
            is Action.DeleteDoneTasks -> viewModelScope.launch {
                taskRepo.deleteDoneTasks()
            }
            is Action.SetSearchQuery -> _filter.value = _filter.value.copy(searchQuery = action.query)
            is Action.ToggleTag -> {
                val ids = _filter.value.selectedTagIds
                _filter.value = _filter.value.copy(
                    selectedTagIds = if (action.tagId in ids) ids - action.tagId else ids + action.tagId
                )
            }
            is Action.ClearTagFilter -> _filter.value = _filter.value.copy(selectedTagIds = emptySet())
            is Action.ToggleFilterSheet -> _filter.value = _filter.value.copy(showFilterSheet = !_filter.value.showFilterSheet)
        }
    }
}

data class TodayState(
    val tasks: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val habitRecords: Map<Long, HabitRecord> = emptyMap(),
    val filter: FilterState = FilterState(),
    val isLoading: Boolean = true,
)
