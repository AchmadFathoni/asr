package com.asr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asr.core.habit.Habit
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitRepo
import com.asr.core.habit.HabitState
import com.asr.core.habit.computeStreak
import com.asr.core.habit.habitRecordWithNewState
import com.asr.core.interfaces.AlarmScheduler
import com.asr.core.now
import com.asr.core.tag.Tag
import com.asr.core.tag.TagRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
class HabitsViewModel(
    @Provided private val habitRepo: HabitRepo,
    @Provided private val tagRepo: TagRepo,
    @Provided private val alarmScheduler: AlarmScheduler,
) : ViewModel() {
    private val today = LocalDate.now()

    private val _selected = MutableStateFlow(SelectedHabit())

    private val _state: StateFlow<HabitsState> = combine(
        habitRepo.getHabitsFlow(),
        habitRepo.getRecordsFlow(),
        habitRepo.getRecordsForDateFlow(today),
        tagRepo.getTagsFlow(),
        _selected,
    ) { habits, allRecords, records, tags, selected ->
        HabitsState(
            habits = habits,
            allRecords = allRecords,
            todayRecords = records.associateBy { it.habitId },
            streaks = habits.associate { it.id to it.computeStreak(allRecords, today) },
            tags = tags,
            selectedHabitId = selected.habitId,
            selectedHabitHistory = selected.history,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsState(),
    )

    val state = _state

    sealed interface Action {
        data class UpsertHabit(val habit: Habit, val tagIds: List<Long> = emptyList()) : Action
        data class DeleteHabit(val habitId: Long) : Action
        data class SetRecordState(val habitId: Long, val state: HabitState) : Action
        data class ViewHabitHistory(val habitId: Long) : Action
        data class CreateTag(val name: String) : Action
        data class MoveHabit(val habitId: Long, val direction: Int) : Action
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.UpsertHabit -> viewModelScope.launch {
                val id = habitRepo.upsertHabit(action.habit)
                if (action.tagIds.isNotEmpty()) {
                    tagRepo.setTagsForHabit(id, action.tagIds)
                }
                alarmScheduler.schedule(action.habit.copy(id = id))
            }
            is Action.DeleteHabit -> viewModelScope.launch {
                val habit = habitRepo.getHabitById(action.habitId)
                if (habit != null) {
                    alarmScheduler.cancel(habit)
                    habitRepo.deleteHabit(habit.id)
                }
            }
            is Action.SetRecordState -> viewModelScope.launch {
                val existing = habitRepo.getRecordForDate(action.habitId, today)
                val habit = habitRepo.getHabitById(action.habitId) ?: return@launch
                habitRepo.upsertRecord(habitRecordWithNewState(existing, habit, today, action.state, LocalDateTime.now()))
            }
            is Action.CreateTag -> viewModelScope.launch {
                if (action.name.isNotBlank()) tagRepo.upsertTag(Tag(name = action.name))
            }
            is Action.MoveHabit -> viewModelScope.launch {
                val habits = _state.value.habits.sortedBy { it.order }
                val idx = habits.indexOfFirst { it.id == action.habitId }
                val target = idx + action.direction
                if (idx < 0 || target < 0 || target >= habits.size) return@launch
                habitRepo.upsertHabit(habits[idx].copy(order = habits[target].order))
                habitRepo.upsertHabit(habits[target].copy(order = habits[idx].order))
            }
            is Action.ViewHabitHistory -> viewModelScope.launch {
                if (_selected.value.habitId == action.habitId) {
                    _selected.value = SelectedHabit()
                } else {
                    val history = habitRepo.getRecordsForHabit(action.habitId)
                    _selected.value = SelectedHabit(action.habitId, history)
                }
            }
        }
    }
}

private data class SelectedHabit(
    val habitId: Long? = null,
    val history: List<HabitRecord> = emptyList(),
)

data class HabitsState(
    val habits: List<Habit> = emptyList(),
    val allRecords: List<HabitRecord> = emptyList(),
    val todayRecords: Map<Long, HabitRecord> = emptyMap(),
    val streaks: Map<Long, Int> = emptyMap(),
    val tags: List<Tag> = emptyList(),
    val selectedHabitId: Long? = null,
    val selectedHabitHistory: List<HabitRecord> = emptyList(),
    val isLoading: Boolean = true,
)
