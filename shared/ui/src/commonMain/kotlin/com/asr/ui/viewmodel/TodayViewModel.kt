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
import com.asr.core.task.Task
import com.asr.core.task.TaskRepo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
class TodayViewModel(
    @Provided private val taskRepo: TaskRepo,
    @Provided private val habitRepo: HabitRepo,
) : ViewModel() {
    private val today = LocalDate.now()

    private val _state: StateFlow<TodayState> = combine(
        taskRepo.getUndoneTasksFlow(),
        habitRepo.getHabitsFlow(),
        habitRepo.getRecordsForDateFlow(today),
    ) { tasks, habits, records ->
        TodayState(
            tasks = tasks.filter { val due = it.dueDate; due == null || due <= today },
            habits = habits.filter { it.shouldShowToday(today) },
            habitRecords = records.associateBy { it.habitId },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayState(),
    )

    val state = _state

    sealed interface Action {
        data class ToggleTask(val taskId: Long) : Action
        data class ToggleHabit(val habitId: Long, val newState: HabitState) : Action
        data object DeleteDoneTasks : Action
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.ToggleTask -> viewModelScope.launch {
                taskRepo.toggleTask(action.taskId)
            }
            is Action.ToggleHabit -> viewModelScope.launch {
                val existing = habitRepo.getRecordForDate(action.habitId, today)
                val habit = habitRepo.getHabitById(action.habitId) ?: return@launch
                habitRepo.upsertRecord(habitRecordWithNewState(existing, habit, today, action.newState, LocalDateTime.now()))
            }
            is Action.DeleteDoneTasks -> viewModelScope.launch {
                taskRepo.deleteDoneTasks()
            }
        }
    }
}

data class TodayState(
    val tasks: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitRecords: Map<Long, HabitRecord> = emptyMap(),
    val isLoading: Boolean = true,
)
