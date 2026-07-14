package com.asr.desktop

import com.asr.core.habit.Habit
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitRepo
import com.asr.core.habit.HabitState
import com.asr.core.tag.Tag
import com.asr.core.tag.TagRepo
import com.asr.core.task.Task
import com.asr.core.task.TaskRepo
import com.asr.core.backup.ExportRepo
import com.asr.core.backup.RestoreRepo
import com.asr.core.backup.RestoreResult
import com.asr.core.interfaces.AlarmScheduler
import com.asr.core.settings.SettingsRepo
import com.asr.core.settings.ThemeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single

@Single(binds = [TaskRepo::class])
class TaskRepoStub : TaskRepo {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L

    override fun getTasksFlow(): Flow<List<Task>> = tasks
    override fun getUndoneTasksFlow(): Flow<List<Task>> = tasks.map { it.filter { t -> !t.isDone } }
    override fun getDoneTasksFlow(): Flow<List<Task>> = tasks.map { it.filter { t -> t.isDone } }
    override suspend fun getTaskById(id: Long): Task? = tasks.value.find { it.id == id }
    override suspend fun getSubTasks(parentId: Long): List<Task> = tasks.value.filter { it.parentId == parentId }
    override suspend fun upsertTask(task: Task): Long {
        val id = if (task.id == 0L) nextId++ else task.id
        tasks.value = tasks.value.filter { it.id != id } + task.copy(id = id)
        return id
    }
    override suspend fun toggleTask(id: Long) {
        tasks.value = tasks.value.map { if (it.id == id) it.copy(isDone = !it.isDone) else it }
    }
    override suspend fun deleteTask(task: Task) {
        tasks.value = tasks.value.filter { it.id != task.id && it.parentId != task.id }
    }
    override suspend fun deleteDoneTasks() {
        tasks.value = tasks.value.filter { !it.isDone }
    }
    override suspend fun insertAll(tasks: List<Task>) {
        this.tasks.value = tasks
        nextId = (tasks.maxOfOrNull { it.id } ?: 0) + 1
    }
}

@Single(binds = [HabitRepo::class])
class HabitRepoStub : HabitRepo {
    private val habits = MutableStateFlow<List<Habit>>(emptyList())
    private val records = MutableStateFlow<List<HabitRecord>>(emptyList())
    private var nextHabitId = 1L
    private var nextRecordId = 1L

    override fun getHabitsFlow(): Flow<List<Habit>> = habits
    override fun getRecordsFlow(): Flow<List<HabitRecord>> = records
    override fun getRecordsForDateFlow(date: LocalDate): Flow<List<HabitRecord>> =
        records.map { it.filter { r -> r.date == date } }
    override suspend fun getHabitById(id: Long): Habit? = habits.value.find { it.id == id }
    override suspend fun getRecordForDate(habitId: Long, date: LocalDate): HabitRecord? =
        records.value.find { it.habitId == habitId && it.date == date }
    override suspend fun upsertHabit(habit: Habit): Long {
        val id = if (habit.id == 0L) nextHabitId++ else habit.id
        habits.value = habits.value.filter { it.id != id } + habit.copy(id = id)
        return id
    }
    override suspend fun deleteHabit(habitId: Long) {
        habits.value = habits.value.filter { it.id != habitId }
        records.value = records.value.filter { it.habitId != habitId }
    }
    override suspend fun upsertRecord(record: HabitRecord) {
        val id = if (record.id == 0L) nextRecordId++ else record.id
        records.value = records.value.filter { it.id != id } + record.copy(id = id)
    }
    override suspend fun deleteRecord(habitId: Long, date: LocalDate) {
        records.value = records.value.filter { !(it.habitId == habitId && it.date == date) }
    }
    override suspend fun getRecordsForHabit(habitId: Long): List<HabitRecord> =
        records.value.filter { it.habitId == habitId }
    override suspend fun getCompletionCountInPeriod(habitId: Long, start: LocalDate, end: LocalDate): Int =
        records.value.count { it.habitId == habitId && it.date >= start && it.date <= end }
    override suspend fun insertAll(habits: List<Habit>, records: List<HabitRecord>) {
        this.habits.value = habits
        this.records.value = records
        nextHabitId = (habits.maxOfOrNull { it.id } ?: 0) + 1
        nextRecordId = (records.maxOfOrNull { it.id } ?: 0) + 1
    }
}

@Single(binds = [TagRepo::class])
class TagRepoStub : TagRepo {
    private val tags = MutableStateFlow<List<Tag>>(emptyList())
    private val taskTags = MutableStateFlow<Map<Long, List<Long>>>(emptyMap())
    private val habitTags = MutableStateFlow<Map<Long, List<Long>>>(emptyMap())
    private var nextTagId = 1L

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
    override suspend fun getTagsForTask(taskId: Long): List<Tag> =
        taskTags.value[taskId].orEmpty().mapNotNull { id -> tags.value.find { it.id == id } }
    override suspend fun getTagsForHabit(habitId: Long): List<Tag> =
        habitTags.value[habitId].orEmpty().mapNotNull { id -> tags.value.find { it.id == id } }
    override fun getTaskTagMappingsFlow(): Flow<Map<Long, List<Long>>> = taskTags
    override fun getHabitTagMappingsFlow(): Flow<Map<Long, List<Long>>> = habitTags
    override suspend fun setTagsForTask(taskId: Long, tagIds: List<Long>) {
        taskTags.value = taskTags.value + (taskId to tagIds)
    }
    override suspend fun setTagsForHabit(habitId: Long, tagIds: List<Long>) {
        habitTags.value = habitTags.value + (habitId to tagIds)
    }
    override suspend fun insertAll(tags: List<Tag>) {
        this.tags.value = tags
        nextTagId = (tags.maxOfOrNull { it.id } ?: 0) + 1
    }
}

@Single(binds = [ExportRepo::class])
class ExportRepoStub : ExportRepo {
    override suspend fun exportToJson() {}
}

@Single(binds = [RestoreRepo::class])
class RestoreRepoStub : RestoreRepo {
    override suspend fun restoreData(): RestoreResult = RestoreResult.Success
}

@Single(binds = [SettingsRepo::class])
class SettingsRepoStub : SettingsRepo {
    private var theme: ThemeOption = ThemeOption.SYSTEM

    override fun getTheme(): ThemeOption = theme
    override fun setTheme(t: ThemeOption) { theme = t }
}

@Single(binds = [AlarmScheduler::class])
class AlarmSchedulerStub : AlarmScheduler {
    override fun schedule(habit: Habit) {}
    override fun schedule(task: Task) {}
    override fun cancel(habit: Habit) {}
    override fun cancel(task: Task) {}
    override fun cancelAll() {}
}
