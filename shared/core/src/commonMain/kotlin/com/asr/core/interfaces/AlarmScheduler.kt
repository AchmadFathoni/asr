package com.asr.core.interfaces

import com.asr.core.habit.Habit
import com.asr.core.task.Task

interface AlarmScheduler {
    fun schedule(habit: Habit)
    fun schedule(task: Task)
    fun cancel(habit: Habit)
    fun cancel(task: Task)
    fun cancelAll()
}
