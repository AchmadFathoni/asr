package com.asr.data.storage

import com.asr.core.habit.Habit
import com.asr.core.habit.HabitFrequency
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitState
import com.asr.core.task.Task
import com.asr.data.database.Converters
import com.asr.data.database.HabitEntity
import com.asr.data.database.HabitRecordEntity
import com.asr.data.database.TaskEntity

internal fun TaskEntity.toDomain() = Task(
    id = id,
    title = title,
    description = description,
    isDone = isDone,
    dueDate = dueDate?.let { Converters.dateFromTimestamp(it) },
    parentId = parentId,
    isPinned = isPinned,
    reminderTime = reminderTime,
)

internal fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    isDone = isDone,
    dueDate = dueDate?.let { Converters.dateToTimestamp(it) },
    parentId = parentId,
    isPinned = isPinned,
    reminderTime = reminderTime,
)

internal fun HabitEntity.toDomain() = Habit(
    id = id,
    title = title,
    description = description,
    frequencyType = HabitFrequency.valueOf(frequencyType),
    frequencyCount = frequencyCount,
    daysOfWeek = if (daysOfWeek.isNotBlank()) daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }.toSet() else emptySet(),
    daysOfMonth = if (daysOfMonth.isNotBlank()) daysOfMonth.split(",").mapNotNull { it.toIntOrNull() }.toSet() else emptySet(),
    yearlyDates = if (yearlyDates.isNotBlank()) yearlyDates.split(",").mapNotNull { it.toIntOrNull() }.toSet() else emptySet(),
    isPinned = isPinned,
    reminderTime = reminderTime,
)

internal fun Habit.toEntity() = HabitEntity(
    id = id,
    title = title,
    description = description,
    frequencyType = frequencyType.name,
    frequencyCount = frequencyCount,
    dayOfWeek = daysOfWeek.firstOrNull(),
    daysOfWeek = daysOfWeek.joinToString(","),
    daysOfMonth = daysOfMonth.joinToString(","),
    yearlyDates = yearlyDates.joinToString(","),
    isPinned = isPinned,
    reminderTime = reminderTime,
)

internal fun HabitRecordEntity.toDomain() = HabitRecord(
    id = id,
    habitId = habitId,
    date = Converters.dateFromTimestamp(date),
    state = HabitState.valueOf(state),
    count = count,
)

internal fun HabitRecord.toEntity() = HabitRecordEntity(
    id = id,
    habitId = habitId,
    date = Converters.dateToTimestamp(date),
    state = state.name,
    count = count,
)
