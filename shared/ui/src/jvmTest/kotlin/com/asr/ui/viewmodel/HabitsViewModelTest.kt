package com.asr.ui.viewmodel

import com.asr.core.habit.Habit
import com.asr.core.habit.HabitFrequency
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitRepo
import com.asr.core.interfaces.AlarmScheduler
import com.asr.core.interfaces.SoundPlayer
import com.asr.core.now
import com.asr.core.tag.Tag
import com.asr.core.tag.TagRepo
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
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HabitsViewModelTest {
    private val today = LocalDate.now()
    private var nextHabitId = 100L
    private var nextTagId = 100L

    private val habits = MutableStateFlow<List<Habit>>(emptyList())
    private val records = MutableStateFlow<List<HabitRecord>>(emptyList())
    private val tags = MutableStateFlow<List<Tag>>(emptyList())
    private val habitTagMappings = MutableStateFlow<Map<Long, List<Long>>>(emptyMap())

    private var lastSetTagHabitId: Long? = null
    private var lastSetTagIds: List<Long>? = null

    private lateinit var viewModel: HabitsViewModel

    private val habitRepo = object : HabitRepo {
        override fun getHabitsFlow(): Flow<List<Habit>> = habits
        override fun getRecordsFlow(): Flow<List<HabitRecord>> = records
        override fun getRecordsForDateFlow(date: LocalDate): Flow<List<HabitRecord>> =
            records.map { it.filter { r -> r.date == date } }
        override suspend fun getHabitById(id: Long): Habit? = habits.value.find { it.id == id }
        override suspend fun getRecordForDate(habitId: Long, date: LocalDate): HabitRecord? = null
        override suspend fun upsertHabit(habit: Habit): Long {
            val id = if (habit.id == 0L) nextHabitId++ else habit.id
            habits.value = habits.value.filter { it.id != id } + habit.copy(id = id)
            return id
        }
        override suspend fun deleteHabit(habitId: Long) {
            habits.value = habits.value.filter { it.id != habitId }
            records.value = records.value.filter { it.habitId != habitId }
        }
        override suspend fun upsertRecord(record: HabitRecord) {}
        override suspend fun deleteRecord(habitId: Long, date: LocalDate) {}
        override suspend fun getRecordsForHabit(habitId: Long): List<HabitRecord> = emptyList()
        override suspend fun getCompletionCountInPeriod(habitId: Long, start: LocalDate, end: LocalDate): Int = 0
        override suspend fun insertAll(habits: List<Habit>, records: List<HabitRecord>) {}
    }

    private val tagRepo = object : TagRepo {
        override fun getTagsFlow(): Flow<List<Tag>> = tags
        override suspend fun getTags(): List<Tag> = tags.value
        override suspend fun getTagById(id: Long): Tag? = tags.value.find { it.id == id }
        override suspend fun upsertTag(tag: Tag): Long {
            val id = if (tag.id == 0L) nextTagId++ else tag.id
            tags.value = tags.value.filter { it.id != id } + tag.copy(id = id)
            return id
        }
        override suspend fun deleteTag(tagId: Long) {
            tags.value = tags.value.filter { it.id != tagId }
        }
        override suspend fun getTagsForTask(taskId: Long): List<Tag> = emptyList()
        override suspend fun getTagsForHabit(habitId: Long): List<Tag> =
            habitTagMappings.value[habitId].orEmpty().mapNotNull { id -> tags.value.find { it.id == id } }
        override fun getTaskTagMappingsFlow(): Flow<Map<Long, List<Long>>> = flowOf(emptyMap())
        override fun getHabitTagMappingsFlow(): Flow<Map<Long, List<Long>>> = habitTagMappings
        override suspend fun setTagsForTask(taskId: Long, tagIds: List<Long>) {}
        override suspend fun setTagsForHabit(habitId: Long, tagIds: List<Long>) {
            lastSetTagHabitId = habitId
            lastSetTagIds = tagIds
            habitTagMappings.value = habitTagMappings.value + (habitId to tagIds)
        }
        override suspend fun insertAll(tags: List<Tag>) {}
    }

    private val alarmScheduler = object : AlarmScheduler {
        override fun schedule(habit: Habit) {}
        override fun schedule(task: com.asr.core.task.Task) {}
        override fun cancel(habit: Habit) {}
        override fun cancel(task: com.asr.core.task.Task) {}
        override fun cancelAll() {}
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
        nextHabitId = 100L
        nextTagId = 100L
        habits.value = emptyList()
        records.value = emptyList()
        tags.value = emptyList()
        habitTagMappings.value = emptyMap()
        lastSetTagHabitId = null
        lastSetTagIds = null
        viewModel = HabitsViewModel(habitRepo, tagRepo, alarmScheduler)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `duplicate adds copy suffix to title`() = runBlocking {
        habits.value = listOf(Habit(id = 1L, title = "Morning run", frequencyType = HabitFrequency.DAILY))
        viewModel.state.first { !it.isLoading }

        viewModel.onAction(HabitsViewModel.Action.DuplicateHabit(1L))
        viewModel.state.first { it.habits.any { h -> h.id != 1L } }

        val duplicate = viewModel.state.value.habits.find { it.id != 1L }
        assertEquals("Morning run (Copy)", duplicate?.title)
    }

    @Test
    fun `duplicate transfers tags`() = runBlocking {
        val tag = Tag(id = 1L, name = "fitness")
        tags.value = listOf(tag)
        habits.value = listOf(Habit(id = 1L, title = "Run", frequencyType = HabitFrequency.DAILY))
        habitTagMappings.value = mapOf(1L to listOf(1L))
        viewModel.state.first { !it.isLoading }

        viewModel.onAction(HabitsViewModel.Action.DuplicateHabit(1L))
        viewModel.state.first { it.habits.any { h -> h.id != 1L } }

        val duplicateId = viewModel.state.value.habits.find { it.id != 1L }?.id
        assertEquals(listOf(1L), viewModel.state.value.habitTagMappings[duplicateId])
    }

    @Test
    fun `upsert with empty tagIds clears existing tags`() = runBlocking {
        val tag = Tag(id = 1L, name = "fitness")
        tags.value = listOf(tag)
        habits.value = listOf(Habit(id = 1L, title = "Run", frequencyType = HabitFrequency.DAILY))
        habitTagMappings.value = mapOf(1L to listOf(1L))
        viewModel.state.first { !it.isLoading }

        val updated = habits.value.first().copy(title = "Jog")
        viewModel.onAction(HabitsViewModel.Action.UpsertHabit(updated, emptyList()))

        assertEquals(1L, lastSetTagHabitId)
        assertEquals(emptyList<Long>(), lastSetTagIds)
        assertEquals(emptyList(), viewModel.state.value.habitTagMappings[1L])
    }

    @Test
    fun `duplicate with no tags creates habit without tags`() = runBlocking {
        habits.value = listOf(Habit(id = 1L, title = "Read", frequencyType = HabitFrequency.DAILY))
        viewModel.state.first { !it.isLoading }

        viewModel.onAction(HabitsViewModel.Action.DuplicateHabit(1L))

        val duplicate = viewModel.state.first { it.habits.size == 2 }.habits.find { it.id != 1L }!!
        assertTrue(duplicate.title.contains("(Copy)"))
        assertEquals(null, habitTagMappings.value[duplicate.id])
    }
}
