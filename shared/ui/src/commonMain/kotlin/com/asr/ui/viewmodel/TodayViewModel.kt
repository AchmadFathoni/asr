package com.asr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asr.core.habit.Habit
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitRepo
import com.asr.core.habit.HabitState
import com.asr.core.habit.habitRecordWithNewState
import com.asr.core.habit.periodStart
import com.asr.core.habit.shouldShowToday
import com.asr.core.interfaces.AlarmScheduler
import com.asr.core.currentDateFlow
import com.asr.core.now
import com.asr.core.settings.SettingsRepo
import com.asr.core.sortedByPinAndDate
import com.asr.core.sortedByPinAndTime
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
class TodayViewModel(
    @Provided private val taskRepo: TaskRepo,
    @Provided private val habitRepo: HabitRepo,
    @Provided private val tagRepo: TagRepo,
    @Provided private val alarmScheduler: AlarmScheduler,
    @Provided private val settingsRepo: SettingsRepo,
) : ViewModel() {
    private val todayFlow = currentDateFlow()
    private var currentToday: LocalDate = LocalDate.now()

    init {
        viewModelScope.launch {
            todayFlow.collect { newToday ->
                if (newToday != currentToday) _punishmentDismissed.value = false
                currentToday = newToday
            }
        }
    }

    private val _filter = MutableStateFlow(FilterState())
    private val _pendingDeleted = MutableStateFlow<List<Task>?>(null)
    private val _punishmentDismissed = MutableStateFlow(false)

    private val recordsWithDate = todayFlow.flatMapLatest { today ->
        val yesterday = LocalDate.fromEpochDays(today.toEpochDays() - 1)
        combine(
            habitRepo.getRecordsForDateFlow(today),
            habitRepo.getRecordsForDateFlow(yesterday)
        ) { todayRecs, yesterdayRecs ->
            Triple(today, todayRecs, yesterdayRecs)
        }
    }

    private val _state: StateFlow<TodayState> = combine(
        taskRepo.getTasksFlow(),
        habitRepo.getHabitsFlow(),
        recordsWithDate,
        tagRepo.getTagsFlow(),
        combine(_filter, tagRepo.getTaskTagMappingsFlow(), tagRepo.getHabitTagMappingsFlow(), _pendingDeleted, _punishmentDismissed) { f, ttm, htm, p, d ->
            FilterWithMappings(f, ttm, htm, p, d)
        },
    ) { tasks, habits, (today, records, yRecs), tags, (filter, ttm, htm, pendingDeleted, punishmentDismissed) ->
        val parentTaskIds = tasks.mapNotNull { it.parentId }.toSet()
        val undoneTasks = tasks.filter { !it.isDone }
        val baseTasks = undoneTasks.filter { val due = it.dueDate; due == null || due <= today }
        val todayHabits = habits.filter { it.shouldShowToday(today) }
        val baseHabits = todayHabits.filter { h ->
            val rec = records.firstOrNull { it.habitId == h.id }
            rec == null || rec.state == HabitState.NOT_DONE
        }

        val todayTasks = tasks.filter { val due = it.dueDate; due == null || due <= today }
        val hasItems = todayTasks.isNotEmpty() || todayHabits.isNotEmpty()
        val noFilter = filter.searchQuery.isBlank() && filter.selectedTagIds.isEmpty()
        val allDone = noFilter && hasItems &&
            todayTasks.all { it.isDone } &&
            todayHabits.all { h ->
                val rec = records.firstOrNull { it.habitId == h.id }
                rec != null && rec.state != HabitState.NOT_DONE
            }

        val periodCounts = todayHabits.associate { h ->
            h.id to records.filter { it.habitId == h.id && it.date >= h.periodStart(today) && it.date <= today }.sumOf { it.count }
        }

        val yesterdayDate = LocalDate.fromEpochDays(today.toEpochDays() - 1)
        val yesterdayTasks = tasks.filter { it.dueDate == yesterdayDate }
        val yesterdayHabits = habits.filter { it.shouldShowToday(yesterdayDate) }
        val totalYesterday = yesterdayTasks.size + yesterdayHabits.size
        val undoneYesterday =
            yesterdayTasks.count { !it.isDone } +
            yesterdayHabits.count { h ->
                val rec = yRecs.firstOrNull { it.habitId == h.id }
                rec == null || rec.state != HabitState.DONE
            }
        val acknowledgedDate = settingsRepo.getPunishmentAcknowledgedDate()
        val alreadyAcknowledged = acknowledgedDate == yesterdayDate.toString()
        val showPunishment = totalYesterday > 0 &&
            undoneYesterday > totalYesterday / 2 &&
            !alreadyAcknowledged &&
            !punishmentDismissed

        TodayState(
            tasks = Filters.tasks(baseTasks.sortedByPinAndDate(), ttm, filter.searchQuery, filter.selectedTagIds, null),
            habits = Filters.habits(baseHabits.sortedByPinAndTime(), htm, filter.searchQuery, filter.selectedTagIds, null),
            tags = tags,
            habitRecords = records.associateBy { it.habitId },
            periodCounts = periodCounts,
            filter = filter,
            pendingDeletedTasks = pendingDeleted,
            taskTagMappings = ttm,
            habitTagMappings = htm,
            parentTaskIds = parentTaskIds,
            allDone = allDone,
            showPunishmentDialog = showPunishment,
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
        val pendingDeleted: List<Task>? = null,
        val punishmentDismissed: Boolean = false,
    )

    sealed interface Action {
        data class ToggleTask(val taskId: Long) : Action
        data class ToggleHabit(val habitId: Long, val newState: HabitState) : Action
        data class TogglePinTask(val taskId: Long) : Action
        data class TogglePinHabit(val habitId: Long) : Action
        data object DeleteDoneTasks : Action
        data object UndoDeleteDoneTasks : Action
        data object DismissDeletedTasks : Action
        data class SetSearchQuery(val query: String) : Action
        data class ToggleTag(val tagId: Long) : Action
        data object ClearTagFilter : Action
        data object ToggleFilterSheet : Action
        data class DismissPunishmentDialog(val acknowledged: Boolean) : Action
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.ToggleTask -> viewModelScope.launch {
                taskRepo.toggleTask(action.taskId)
            }
            is Action.ToggleHabit -> viewModelScope.launch {
                val d = currentToday
                val existing = habitRepo.getRecordForDate(action.habitId, d)
                val habit = habitRepo.getHabitById(action.habitId) ?: return@launch
                println("ASR_BUGTRACE ToggleHabit habitId=${action.habitId} newState=${action.newState} today=$d existingRecord=${existing?.state} habitDaysOfMonth=${habit.daysOfMonth} habitFreq=${habit.frequencyType}")
                val periodTotal = habitRepo.getRecordsForHabit(action.habitId)
                    .filter { it.date >= habit.periodStart(d) && it.date <= d }
                    .sumOf { it.count }
                habitRepo.upsertRecord(habitRecordWithNewState(existing, habit, d, action.newState, periodTotal))
                if (action.newState != HabitState.NOT_DONE) {
                    println("ASR_BUGTRACE ToggleHabit habitId=${action.habitId} newState=${action.newState} → CANCEL alarm")
                    alarmScheduler.cancel(habit)
                } else {
                    println("ASR_BUGTRACE ToggleHabit habitId=${action.habitId} newState=NOT_DONE → RESCHEDULE alarm")
                    alarmScheduler.schedule(habit)
                }
            }
            is Action.TogglePinTask -> viewModelScope.launch {
                val task = taskRepo.getTaskById(action.taskId) ?: return@launch
                taskRepo.upsertTask(task.copy(isPinned = !task.isPinned))
            }
            is Action.TogglePinHabit -> viewModelScope.launch {
                val habit = habitRepo.getHabitById(action.habitId) ?: return@launch
                habitRepo.upsertHabit(habit.copy(isPinned = !habit.isPinned))
            }
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
            is Action.ToggleFilterSheet -> _filter.value = _filter.value.copy(showFilterSheet = !_filter.value.showFilterSheet)
            is Action.DismissPunishmentDialog -> {
                _punishmentDismissed.value = true
                if (action.acknowledged) {
                    val yesterday = LocalDate.fromEpochDays(currentToday.toEpochDays() - 1)
                    settingsRepo.setPunishmentAcknowledgedDate(yesterday.toString())
                }
            }
        }
    }
}

data class TodayState(
    val tasks: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val habitRecords: Map<Long, HabitRecord> = emptyMap(),
    val periodCounts: Map<Long, Int> = emptyMap(),
    val filter: FilterState = FilterState(),
    val pendingDeletedTasks: List<Task>? = null,
    val taskTagMappings: Map<Long, List<Long>> = emptyMap(),
    val habitTagMappings: Map<Long, List<Long>> = emptyMap(),
    val parentTaskIds: Set<Long> = emptySet(),
    val allDone: Boolean = false,
    val showPunishmentDialog: Boolean = false,
    val isLoading: Boolean = true,
)
