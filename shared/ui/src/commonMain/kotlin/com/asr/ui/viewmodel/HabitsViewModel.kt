package com.asr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asr.core.habit.Habit
import com.asr.core.habit.HabitFrequency
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitRepo
import com.asr.core.habit.HabitState
import com.asr.core.habit.computeStreak
import com.asr.core.habit.habitRecordWithNewState
import com.asr.core.habit.periodStart
import com.asr.core.interfaces.AlarmScheduler
import com.asr.core.currentDateFlow
import com.asr.core.now
import com.asr.core.hideAfter
import com.asr.core.sortedByPinAndTime
import com.asr.core.StatusFilter
import com.asr.core.tag.Tag
import com.asr.core.tag.TagRepo
import com.asr.ui.app.FilterState
import com.asr.ui.app.Filters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided



@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class HabitsViewModel(
    @Provided private val habitRepo: HabitRepo,
    @Provided private val tagRepo: TagRepo,
    @Provided private val alarmScheduler: AlarmScheduler,
) : ViewModel() {
    private val todayFlow = currentDateFlow()
    private var currentToday: LocalDate = LocalDate.now()

    init { viewModelScope.launch { todayFlow.collect { currentToday = it } } }

    private val _selected = MutableStateFlow(SelectedHabit())
    private val _filter = MutableStateFlow(FilterState())
    private val _habitFilter = MutableStateFlow(StatusFilter.DUE)
    private val _completingHabitIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _createdTagId = MutableStateFlow<Long?>(null)
    val createdTagId: StateFlow<Long?> = _createdTagId.asStateFlow()

    private val recordsForDate = todayFlow.flatMapLatest { today ->
        habitRepo.getRecordsForDateFlow(today).map { records -> today to records }
    }

    private val _state: StateFlow<HabitsState> = combine(
        habitRepo.getHabitsFlow(),
        habitRepo.getRecordsFlow(),
        recordsForDate,
        tagRepo.getTagsFlow(),
        combine(_filter, _selected, _habitFilter, tagRepo.getHabitTagMappingsFlow(), _completingHabitIds) { f, s, hf, m, c -> FilterStateWithHistory(f, s, hf, m, c) },
    ) { habits, allRecords, (today, records), tags, (filter, selected, habitFilter, tagMappings, completingHabitIds) ->
        val base = when (habitFilter) {
            StatusFilter.ALL -> habits
            StatusFilter.DUE -> habits.filter { !it.isDoneInPeriod(today, allRecords) || it.id in completingHabitIds }
            StatusFilter.DONE -> habits.filter { it.isDoneInPeriod(today, allRecords) }
        }
        val periodCounts = habits.associate { h ->
            h.id to allRecords.filter { it.habitId == h.id && it.date >= h.periodStart(today) && it.date <= today }.sumOf { it.count }
        }
        HabitsState(
            habits = Filters.habits(base.sortedByPinAndTime(), tagMappings, filter.searchQuery, filter.selectedTagIds, filter.filterDate),
            allRecords = allRecords,
            todayRecords = records.associateBy { it.habitId },
            periodCounts = periodCounts,
            streaks = habits.associate { it.id to it.computeStreak(allRecords, today, requireToday = false) },
            habitFilter = habitFilter,
            tags = tags,
            filter = filter,
            selectedHabitId = selected.habitId,
            selectedHabitHistory = selected.history,
            habitTagMappings = tagMappings,
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
        data class DuplicateHabit(val habitId: Long) : Action
        data class DeleteHabit(val habitId: Long) : Action
        data class SetRecordState(val habitId: Long, val state: HabitState) : Action
        data class ViewHabitHistory(val habitId: Long) : Action
        data class ToggleLogDate(val habitId: Long, val date: LocalDate) : Action
        data class CreateTag(val name: String, val color: Long? = null) : Action
        data class TogglePinHabit(val habitId: Long) : Action
        data class SetSearchQuery(val query: String) : Action
        data class ToggleTag(val tagId: Long) : Action
        data object ClearTagFilter : Action
        data object ConsumeCreatedTag : Action
        data class SetFilterDate(val date: LocalDate?) : Action
        data object ToggleFilterSheet : Action
        data class SetHabitFilter(val filter: StatusFilter) : Action
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.UpsertHabit -> viewModelScope.launch {
                val id = habitRepo.upsertHabit(action.habit)
                val effectiveId = if (id <= 0) action.habit.id else id
                if (effectiveId <= 0) return@launch
                tagRepo.setTagsForHabit(effectiveId, action.tagIds)
                alarmScheduler.schedule(action.habit.copy(id = effectiveId))
            }
            is Action.DuplicateHabit -> viewModelScope.launch {
                val original = habitRepo.getHabitById(action.habitId) ?: return@launch
                val tagIds = tagRepo.getTagsForHabit(action.habitId)
                val newId = habitRepo.upsertHabit(original.copy(id = 0, title = "${original.title} (Copy)"))
                if (tagIds.isNotEmpty()) tagRepo.setTagsForHabit(newId, tagIds.map { it.id })
                if (original.reminderTime != null) alarmScheduler.schedule(original.copy(id = newId))
            }
            is Action.DeleteHabit -> viewModelScope.launch {
                val habit = habitRepo.getHabitById(action.habitId)
                if (habit != null) {
                    alarmScheduler.cancel(habit)
                    habitRepo.deleteHabit(habit.id)
                }
            }
            is Action.SetRecordState -> viewModelScope.launch {
                val d = currentToday
                val existing = habitRepo.getRecordForDate(action.habitId, d)
                val habit = habitRepo.getHabitById(action.habitId) ?: return@launch
                val wasNotDone = existing == null || existing.state == HabitState.NOT_DONE
                val completing = wasNotDone && action.state == HabitState.DONE
                if (completing) _completingHabitIds.value = _completingHabitIds.value + action.habitId
                val periodTotal = habitRepo.getRecordsForHabit(action.habitId)
                    .filter { it.date >= habit.periodStart(d) && it.date <= d }
                    .sumOf { it.count }
                habitRepo.upsertRecord(habitRecordWithNewState(existing, habit, d, action.state, periodTotal))
                if (action.state != HabitState.NOT_DONE) {
                    alarmScheduler.cancel(habit)
                } else {
                    alarmScheduler.schedule(habit)
                }
                if (completing) _completingHabitIds.hideAfter(500, action.habitId)
            }
            is Action.CreateTag -> viewModelScope.launch {
                if (action.name.isNotBlank()) {
                    val id = tagRepo.upsertTag(Tag(name = action.name, color = action.color))
                    _createdTagId.value = id
                }
            }
            is Action.ConsumeCreatedTag -> _createdTagId.value = null
            is Action.TogglePinHabit -> viewModelScope.launch {
                val habit = habitRepo.getHabitById(action.habitId) ?: return@launch
                habitRepo.upsertHabit(habit.copy(isPinned = !habit.isPinned))
            }
            is Action.ViewHabitHistory -> viewModelScope.launch {
                if (_selected.value.habitId == action.habitId) {
                    _selected.value = SelectedHabit()
                } else {
                    val history = habitRepo.getRecordsForHabit(action.habitId)
                    _selected.value = SelectedHabit(action.habitId, history)
                }
            }
            is Action.ToggleLogDate -> viewModelScope.launch {
                val habit = habitRepo.getHabitById(action.habitId) ?: return@launch
                val existing = habitRepo.getRecordForDate(action.habitId, action.date)
                val periodTotal = habitRepo.getRecordsForHabit(action.habitId)
                    .filter { it.date >= habit.periodStart(action.date) && it.date <= action.date }
                    .sumOf { it.count }
                habitRepo.upsertRecord(habitRecordWithNewState(existing, habit, action.date, HabitState.DONE, periodTotal))
                val history = habitRepo.getRecordsForHabit(action.habitId)
                _selected.value = SelectedHabit(action.habitId, history)
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
            is Action.SetHabitFilter -> _habitFilter.value = action.filter
        }
    }
}

private data class SelectedHabit(
    val habitId: Long? = null,
    val history: List<HabitRecord> = emptyList(),
)

private data class FilterStateWithHistory(
    val filter: FilterState,
    val selected: SelectedHabit,
    val habitFilter: StatusFilter,
    val tagMappings: Map<Long, List<Long>>,
    val completingHabitIds: Set<Long> = emptySet(),
)

data class HabitsState(
    val habits: List<Habit> = emptyList(),
    val allRecords: List<HabitRecord> = emptyList(),
    val todayRecords: Map<Long, HabitRecord> = emptyMap(),
    val periodCounts: Map<Long, Int> = emptyMap(),
    val streaks: Map<Long, Int> = emptyMap(),
    val tags: List<Tag> = emptyList(),
    val filter: FilterState = FilterState(),
    val habitFilter: StatusFilter = StatusFilter.DUE,
    val selectedHabitId: Long? = null,
    val selectedHabitHistory: List<HabitRecord> = emptyList(),
    val habitTagMappings: Map<Long, List<Long>> = emptyMap(),
    val isLoading: Boolean = true,
)

private fun Habit.isDoneInPeriod(today: LocalDate, allRecords: List<HabitRecord>): Boolean {
    val pStart = periodStart(today)
    return allRecords.any { it.habitId == id && it.state == HabitState.DONE && it.date >= pStart }
}
