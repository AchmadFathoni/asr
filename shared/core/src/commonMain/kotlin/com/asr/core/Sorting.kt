package com.asr.core

import com.asr.core.habit.Habit
import com.asr.core.task.Task

fun List<Task>.sortedByPinAndDate(): List<Task> = sortedWith(
    compareByDescending<Task> { it.isPinned }
        .thenBy { it.dueDate != null }
        .thenBy { it.dueDate }
        .thenBy { it.reminderTime != null }
        .thenBy { it.reminderTime }
)

fun List<Habit>.sortedByPinAndTime(): List<Habit> = sortedWith(
    compareByDescending<Habit> { it.isPinned }
        .thenBy { it.reminderTime != null }
        .thenBy { it.reminderTime }
)
