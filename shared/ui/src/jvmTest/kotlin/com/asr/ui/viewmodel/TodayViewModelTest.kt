package com.asr.ui.viewmodel

import com.asr.core.habit.Habit
import com.asr.core.habit.HabitFrequency
import com.asr.core.now
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitRepo
import com.asr.core.habit.HabitState
import com.asr.core.interfaces.AlarmScheduler
import com.asr.core.interfaces.SoundPlayer
import com.asr.core.settings.SettingsRepo
import com.asr.core.tag.Tag
import com.asr.core.tag.TagRepo
import com.asr.core.task.Task
import com.asr.core.task.TaskRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    private val today = LocalDate.now()
    private val weekStart = LocalDate.fromEpochDays(today.toEpochDays() - today.dayOfWeek.ordinal)

    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private val habits = MutableStateFlow<List<Habit>>(emptyList())
    private val records = MutableStateFlow<List<HabitRecord>>(emptyList())
    private val tags = MutableStateFlow<List<Tag>>(emptyList())
    private val habitTagMappings = MutableStateFlow<Map<Long, List<Long>>>(emptyMap())
    private var acknowledgedDate: String? = null

    private lateinit var viewModel: TodayViewModel

    private val taskRepo = object : TaskRepo {
        override fun getTasksFlow(): Flow<List<Task>> = tasks
        override fun getUndoneTasksFlow(): Flow<List<Task>> = tasks.map { it.filter { !it.isDone } }
        override fun getDoneTasksFlow(): Flow<List<Task>> = tasks.map { it.filter { it.isDone } }
        override suspend fun getTaskById(id: Long): Task? = tasks.value.find { it.id == id }
        override suspend fun getSubTasks(parentId: Long): List<Task> = tasks.value.filter { it.parentId == parentId }
        override suspend fun upsertTask(task: Task): Long {
            tasks.value = tasks.value.filter { it.id != task.id } + task
            return task.id
        }
        override suspend fun toggleTask(id: Long) {
            tasks.value = tasks.value.map { if (it.id == id) it.copy(isDone = !it.isDone) else it }
        }
        override suspend fun deleteTask(task: Task) { tasks.value = tasks.value.filter { it.id != task.id } }
        override suspend fun deleteDoneTasks() { tasks.value = tasks.value.filter { !it.isDone } }
        override suspend fun insertAll(tasks: List<Task>) {}
    }

    private val habitRepo = object : HabitRepo {
        override fun getHabitsFlow(): Flow<List<Habit>> = habits
        override fun getRecordsFlow(): Flow<List<HabitRecord>> = records
        override fun getRecordsForDateFlow(date: LocalDate): Flow<List<HabitRecord>> =
            records.map { it.filter { r -> r.date == date } }
        override suspend fun getHabitById(id: Long): Habit? = habits.value.find { it.id == id }
        override suspend fun getRecordForDate(habitId: Long, date: LocalDate): HabitRecord? =
            records.value.find { it.habitId == habitId && it.date == date }
        override suspend fun upsertHabit(habit: Habit): Long {
            habits.value = habits.value.filter { it.id != habit.id } + habit
            return habit.id
        }
        override suspend fun deleteHabit(habitId: Long) {
            habits.value = habits.value.filter { it.id != habitId }
            records.value = records.value.filter { it.habitId != habitId }
        }
        override suspend fun upsertRecord(record: HabitRecord) {
            records.value = records.value.filter { it.habitId != record.habitId || it.date != record.date } + record
        }
        override suspend fun deleteRecord(habitId: Long, date: LocalDate) {
            records.value = records.value.filter { it.habitId != habitId || it.date != date }
        }
        override suspend fun getRecordsForHabit(habitId: Long): List<HabitRecord> =
            records.value.filter { it.habitId == habitId }
        override suspend fun getCompletionCountInPeriod(habitId: Long, start: LocalDate, end: LocalDate): Int =
            records.value.filter { it.habitId == habitId && it.date >= start && it.date <= end }.sumOf { it.count }
        override suspend fun insertAll(habits: List<Habit>, records: List<HabitRecord>) {}
    }

    private val tagRepo = object : TagRepo {
        override fun getTagsFlow(): Flow<List<Tag>> = tags
        override suspend fun getTags(): List<Tag> = tags.value
        override suspend fun getTagById(id: Long): Tag? = tags.value.find { it.id == id }
        override suspend fun upsertTag(tag: Tag): Long {
            val id = if (tag.id == 0L) 1L else tag.id
            tags.value = tags.value.filter { it.id != id } + tag.copy(id = id)
            return id
        }
        override suspend fun deleteTag(tagId: Long) { tags.value = tags.value.filter { it.id != tagId } }
        override suspend fun getTagsForTask(taskId: Long): List<Tag> = emptyList()
        override suspend fun getTagsForHabit(habitId: Long): List<Tag> = emptyList()
        override fun getTaskTagMappingsFlow(): Flow<Map<Long, List<Long>>> = flowOf(emptyMap())
        override fun getHabitTagMappingsFlow(): Flow<Map<Long, List<Long>>> = habitTagMappings
        override suspend fun setTagsForTask(taskId: Long, tagIds: List<Long>) {}
        override suspend fun setTagsForHabit(habitId: Long, tagIds: List<Long>) {}
        override suspend fun insertAll(tags: List<Tag>) {}
    }

    private val alarmScheduler = object : AlarmScheduler {
        override fun schedule(habit: Habit) {}
        override fun schedule(task: Task) {}
        override fun cancel(habit: Habit) {}
        override fun cancel(task: Task) {}
        override fun cancelAll() {}
    }

    private val settingsRepo = object : SettingsRepo {
        override fun getTheme() = com.asr.core.settings.ThemeOption.SYSTEM
        override fun setTheme(theme: com.asr.core.settings.ThemeOption) {}
        override fun getPunishmentAcknowledgedDate(): String? = acknowledgedDate
        override fun setPunishmentAcknowledgedDate(date: String?) { acknowledgedDate = date }
    }

    @Before
    fun setup() {
        stopKoin()
        startKoin {
            modules(module {
                single<SoundPlayer> { object : SoundPlayer { override fun play(pitch: Float) {} } }
            })
        }
        Dispatchers.setMain(Dispatchers.Unconfined)
        tasks.value = emptyList()
        habits.value = emptyList()
        records.value = emptyList()
        tags.value = emptyList()
        habitTagMappings.value = emptyMap()
        acknowledgedDate = null
        viewModel = TodayViewModel(taskRepo, habitRepo, tagRepo, alarmScheduler, settingsRepo)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun waitForState() = runBlocking { viewModel.state.first { !it.isLoading } }

    // ── Daily habits ──────────────────────────────────────────────

    @Test fun `daily target1 with no record shows in Today`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.DAILY, frequencyCount = 1))
        val s = waitForState()
        assertContains(s.habits.map { it.id }, 1L)
    }

    @Test fun `daily target1 with DONE record hides from Today`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.DAILY, frequencyCount = 1))
        records.value = listOf(HabitRecord(habitId = 1, date = today, state = HabitState.DONE, count = 1))
        val s = waitForState()
        assertTrue(s.habits.none { it.id == 1L })
    }

    @Test fun `daily target1 with SKIPPED record hides from Today`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.DAILY, frequencyCount = 1))
        records.value = listOf(HabitRecord(habitId = 1, date = today, state = HabitState.SKIPPED))
        val s = waitForState()
        assertTrue(s.habits.none { it.id == 1L })
    }

    @Test fun `daily target3 with partial count stays visible`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.DAILY, frequencyCount = 3))
        records.value = listOf(HabitRecord(habitId = 1, date = today, state = HabitState.NOT_DONE, count = 1))
        val s = waitForState()
        assertContains(s.habits.map { it.id }, 1L)
    }

    @Test fun `daily target3 with full DONE hides from Today`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.DAILY, frequencyCount = 3))
        records.value = listOf(HabitRecord(habitId = 1, date = today, state = HabitState.DONE, count = 3))
        val s = waitForState()
        assertTrue(s.habits.none { it.id == 1L })
    }

    @Test fun `daily target3 with partial and SKIPPED hides from Today`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.DAILY, frequencyCount = 3))
        records.value = listOf(HabitRecord(habitId = 1, date = today, state = HabitState.SKIPPED, count = 1))
        val s = waitForState()
        assertTrue(s.habits.none { it.id == 1L })
    }

    @Test fun `daily target1 completed yesterday shows today`() = runBlocking {
        val yesterday = LocalDate.fromEpochDays(today.toEpochDays() - 1)
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.DAILY, frequencyCount = 1))
        records.value = listOf(HabitRecord(habitId = 1, date = yesterday, state = HabitState.DONE, count = 1))
        val s = waitForState()
        assertContains(s.habits.map { it.id }, 1L)
    }

    // ── Weekly habits (frequencyCount = 1) ────────────────────────

    @Test fun `weekly target1 with no record shows in Today`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.WEEKLY, frequencyCount = 1))
        val s = waitForState()
        assertContains(s.habits.map { it.id }, 1L)
    }

    @Test fun `weekly target1 with DONE today hides from Today`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.WEEKLY, frequencyCount = 1))
        records.value = listOf(HabitRecord(habitId = 1, date = today, state = HabitState.DONE, count = 1))
        val s = waitForState()
        assertTrue(s.habits.none { it.id == 1L })
    }

    @Test fun `weekly target1 done yesterday same week still shows today`() = runBlocking {
        val yesterday = LocalDate.fromEpochDays(today.toEpochDays() - 1)
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.WEEKLY, frequencyCount = 1))
        records.value = listOf(HabitRecord(habitId = 1, date = yesterday, state = HabitState.DONE, count = 1))
        val s = waitForState()
        assertContains(s.habits.map { it.id }, 1L)
    }

    @Test fun `weekly target1 with SKIPPED today hides`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.WEEKLY, frequencyCount = 1))
        records.value = listOf(HabitRecord(habitId = 1, date = today, state = HabitState.SKIPPED))
        val s = waitForState()
        assertTrue(s.habits.none { it.id == 1L })
    }

    // ── Weekly habits (frequencyCount > 1) ────────────────────────

    @Test fun `weekly target3 tapped once today hides for the day`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.WEEKLY, frequencyCount = 3))
        records.value = listOf(HabitRecord(habitId = 1, date = today, state = HabitState.NOT_DONE, count = 1))
        val s = waitForState()
        assertTrue(s.habits.none { it.id == 1L })
    }

    @Test fun `weekly target3 fully done on prev day hides today`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.WEEKLY, frequencyCount = 3))
        records.value = listOf(HabitRecord(habitId = 1, date = weekStart, state = HabitState.DONE, count = 3))
        val s = waitForState()
        assertTrue(s.habits.none { it.id == 1L })
    }

    @Test fun `weekly target3 partial on prev day still shows today`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.WEEKLY, frequencyCount = 3))
        records.value = listOf(HabitRecord(habitId = 1, date = weekStart, state = HabitState.NOT_DONE, count = 1))
        val s = waitForState()
        assertContains(s.habits.map { it.id }, 1L)
    }

    @Test fun `weekly target3 skipped on prev day hides today`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.WEEKLY, frequencyCount = 3))
        records.value = listOf(HabitRecord(habitId = 1, date = weekStart, state = HabitState.SKIPPED))
        val s = waitForState()
        assertTrue(s.habits.none { it.id == 1L })
    }

    // ── Mixed habits ──────────────────────────────────────────────

    @Test fun `done habit hides but undone habit still shows`() = runBlocking {
        habits.value = listOf(
            Habit(id = 1, title = "Done", frequencyType = HabitFrequency.DAILY, frequencyCount = 1),
            Habit(id = 2, title = "Undone", frequencyType = HabitFrequency.DAILY, frequencyCount = 1),
        )
        records.value = listOf(HabitRecord(habitId = 1, date = today, state = HabitState.DONE, count = 1))
        val s = waitForState()
        assertTrue(s.habits.none { it.id == 1L })
        assertContains(s.habits.map { it.id }, 2L)
    }

    // ── Tasks ─────────────────────────────────────────────────────

    @Test fun `undone task with no due date shows in Today`() = runBlocking {
        tasks.value = listOf(Task(id = 1, title = "T", isDone = false))
        val s = waitForState()
        assertContains(s.tasks.map { it.id }, 1L)
    }

    @Test fun `done task hides from Today`() = runBlocking {
        tasks.value = listOf(Task(id = 1, title = "T", isDone = true))
        val s = waitForState()
        assertTrue(s.tasks.none { it.id == 1L })
    }

    @Test fun `undone task due today shows`() = runBlocking {
        tasks.value = listOf(Task(id = 1, title = "T", isDone = false, dueDate = today))
        val s = waitForState()
        assertContains(s.tasks.map { it.id }, 1L)
    }

    @Test fun `done task due today hides`() = runBlocking {
        tasks.value = listOf(Task(id = 1, title = "T", isDone = true, dueDate = today))
        val s = waitForState()
        assertTrue(s.tasks.none { it.id == 1L })
    }

    // ── periodCounts ──────────────────────────────────────────────

    @Test fun `periodCounts sums across period not just today`() = runBlocking {
        val habitId = 1L
        habits.value = listOf(Habit(id = habitId, title = "H", frequencyType = HabitFrequency.WEEKLY, frequencyCount = 3))
        records.value = listOf(
            HabitRecord(habitId = habitId, date = weekStart, state = HabitState.NOT_DONE, count = 1),
            HabitRecord(habitId = habitId, date = LocalDate.fromEpochDays(weekStart.toEpochDays() + 1), state = HabitState.NOT_DONE, count = 1),
        )
        val s = waitForState()
        assertEquals(2, s.periodCounts[habitId])
    }

    @Test fun `periodCounts is zero for undone habit without records`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.DAILY, frequencyCount = 1))
        val s = waitForState()
        assertEquals(0, s.periodCounts[1])
    }

    // ── allDone ────────────────────────────────────────────────────

    @Test fun `allDone true when no items`() = runBlocking {
        val s = waitForState()
        assertFalse(s.allDone)
    }

    @Test fun `allDone true when all habits and tasks done`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.DAILY, frequencyCount = 1))
        tasks.value = listOf(Task(id = 1, title = "T", isDone = true))
        records.value = listOf(HabitRecord(habitId = 1, date = today, state = HabitState.DONE, count = 1))
        val s = waitForState()
        assertTrue(s.allDone)
    }

    @Test fun `allDone false when habit undone`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.DAILY, frequencyCount = 1))
        val s = waitForState()
        assertFalse(s.allDone)
    }

    @Test fun `allDone true for weekly target3 done prev day`() = runBlocking {
        habits.value = listOf(Habit(id = 1, title = "H", frequencyType = HabitFrequency.WEEKLY, frequencyCount = 3))
        records.value = listOf(HabitRecord(habitId = 1, date = weekStart, state = HabitState.DONE, count = 3))
        val s = waitForState()
        assertTrue(s.allDone)
    }
}
