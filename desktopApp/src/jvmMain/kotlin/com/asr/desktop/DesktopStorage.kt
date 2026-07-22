package com.asr.desktop

import com.asr.core.habit.Habit
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitStorage
import com.asr.core.settings.SettingsStorage
import com.asr.core.settings.ThemeOption
import com.asr.core.tag.Tag
import com.asr.core.tag.TagStorage
import com.asr.core.task.Task
import com.asr.core.task.TaskStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class JsonTaskStorage : TaskStorage {
    private val _tasks = MutableStateFlow(DataStore.data.tasks)

    override fun observeAll(): Flow<List<Task>> = _tasks

    override suspend fun getById(id: Long): Task? = _tasks.value.find { it.id == id }

    override suspend fun getByParent(parentId: Long): List<Task> = _tasks.value.filter { it.parentId == parentId }

    override suspend fun upsert(task: Task): Long {
        val id = if (task.id == 0L) (_tasks.value.maxOfOrNull { it.id } ?: 0) + 1 else task.id
        _tasks.value = _tasks.value.filter { it.id != id } + task.copy(id = id)
        save(); return id
    }

    override suspend fun delete(id: Long) { _tasks.value = _tasks.value.filter { it.id != id }; save() }

    override suspend fun deleteDone() { _tasks.value = _tasks.value.filter { !it.isDone }; save() }

    override suspend fun replaceAll(tasks: List<Task>) { _tasks.value = tasks; save() }

    private fun save() = DataStore.update { it.copy(tasks = _tasks.value) }
}

class JsonHabitStorage : HabitStorage {
    private val _habits = MutableStateFlow(DataStore.data.habits)
    private val _records = MutableStateFlow(DataStore.data.records)

    private fun save() = DataStore.update { it.copy(habits = _habits.value, records = _records.value) }

    override fun observeHabits(): Flow<List<Habit>> = _habits
    override fun observeRecords(): Flow<List<HabitRecord>> = _records
    override fun observeRecordsForDate(date: LocalDate): Flow<List<HabitRecord>> = _records.map { it.filter { r -> r.date == date } }

    override suspend fun getHabitById(id: Long): Habit? = _habits.value.find { it.id == id }
    override suspend fun getRecordForDate(habitId: Long, date: LocalDate): HabitRecord? = _records.value.find { it.habitId == habitId && it.date == date }

    override suspend fun upsertHabit(habit: Habit): Long {
        val id = if (habit.id == 0L) (_habits.value.maxOfOrNull { it.id } ?: 0) + 1 else habit.id
        _habits.value = _habits.value.filter { it.id != id } + habit.copy(id = id)
        save(); return id
    }

    override suspend fun deleteHabit(habitId: Long) {
        _habits.value = _habits.value.filter { it.id != habitId }
        _records.value = _records.value.filter { it.habitId != habitId }
        save()
    }

    override suspend fun upsertRecord(record: HabitRecord) {
        val id = if (record.id == 0L) (_records.value.maxOfOrNull { it.id } ?: 0) + 1 else record.id
        _records.value = _records.value.filter { it.id != id } + record.copy(id = id)
        save()
    }

    override suspend fun deleteRecord(habitId: Long, date: LocalDate) {
        _records.value = _records.value.filter { !(it.habitId == habitId && it.date == date) }
        save()
    }

    override suspend fun getRecordsForHabit(habitId: Long): List<HabitRecord> = _records.value.filter { it.habitId == habitId }

    override suspend fun getCompletionCountInPeriod(habitId: Long, start: LocalDate, end: LocalDate): Int =
        _records.value.filter { it.habitId == habitId && it.date >= start && it.date <= end }.sumOf { it.count }

    override suspend fun replaceAll(habits: List<Habit>, records: List<HabitRecord>) {
        _habits.value = habits; _records.value = records; save()
    }
}

class JsonTagStorage : TagStorage {
    private val _tags = MutableStateFlow(DataStore.data.tags)
    private val _taskTags = MutableStateFlow(DataStore.data.taskTags)
    private val _habitTags = MutableStateFlow(DataStore.data.habitTags)

    private fun save() = DataStore.update { it.copy(tags = _tags.value, taskTags = _taskTags.value, habitTags = _habitTags.value) }

    override fun observeTags(): Flow<List<Tag>> = _tags
    override suspend fun getTags(): List<Tag> = _tags.value
    override suspend fun getTagById(id: Long): Tag? = _tags.value.find { it.id == id }

    override suspend fun upsertTag(tag: Tag): Long {
        val id = if (tag.id == 0L) (_tags.value.maxOfOrNull { it.id } ?: 0) + 1 else tag.id
        _tags.value = _tags.value.filter { it.id != id } + tag.copy(id = id)
        save(); return id
    }

    override suspend fun deleteTag(tagId: Long) {
        _tags.value = _tags.value.filter { it.id != tagId }
        _taskTags.value = _taskTags.value.mapValues { (_, v) -> v.filter { it != tagId } }
        _habitTags.value = _habitTags.value.mapValues { (_, v) -> v.filter { it != tagId } }
        save()
    }

    override suspend fun getTagsForTask(taskId: Long): List<Tag> =
        _taskTags.value[taskId].orEmpty().mapNotNull { id -> _tags.value.find { it.id == id } }

    override suspend fun getTagsForHabit(habitId: Long): List<Tag> =
        _habitTags.value[habitId].orEmpty().mapNotNull { id -> _tags.value.find { it.id == id } }

    override fun observeTaskTagMappings(): Flow<Map<Long, List<Long>>> = _taskTags
    override fun observeHabitTagMappings(): Flow<Map<Long, List<Long>>> = _habitTags

    override suspend fun setTagsForTask(taskId: Long, tagIds: List<Long>) {
        _taskTags.value = _taskTags.value + (taskId to tagIds)
        save()
    }

    override suspend fun setTagsForHabit(habitId: Long, tagIds: List<Long>) {
        _habitTags.value = _habitTags.value + (habitId to tagIds)
        save()
    }

    override suspend fun replaceAll(tags: List<Tag>) { _tags.value = tags; save() }
}

class JsonSettingsStorage : SettingsStorage {
    private var theme: ThemeOption = runCatching { ThemeOption.valueOf(DataStore.data.theme ?: "SYSTEM") }.getOrDefault(ThemeOption.SYSTEM)
    private var punishmentDate: String? = DataStore.data.punishmentDate
    override fun getTheme(): ThemeOption = theme
    override fun setTheme(theme: ThemeOption) {
        this.theme = theme
        DataStore.update { it.copy(theme = theme.name) }
    }

    override fun getPunishmentAcknowledgedDate(): String? = punishmentDate

    override fun setPunishmentAcknowledgedDate(date: String?) {
        punishmentDate = date
        DataStore.update { it.copy(punishmentDate = date) }
    }
}
