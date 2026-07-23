package com.asr.core

import com.asr.core.habit.Habit
import com.asr.core.habit.HabitFrequency
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitState
import com.asr.core.habit.isCompleteForPeriod
import com.asr.core.habit.shouldShowToday
import com.asr.core.task.Task
import kotlinx.datetime.LocalDate

object TodayItems {
    fun tasks(
        tasks: List<Task>,
        today: LocalDate,
        completingIds: Set<Long> = emptySet(),
    ): List<Task> =
        tasks.filter { val d = it.dueDate; (d == null || d <= today) && !it.isDone && it.id !in completingIds }

    fun habits(
        habits: List<Habit>,
        today: LocalDate,
        allRecords: List<HabitRecord>,
        todayRecords: List<HabitRecord>,
        completingIds: Set<Long> = emptySet(),
    ): List<Habit> {
        val todayRecMap = todayRecords.associateBy { it.habitId }
        return habits.filter { h ->
            h.shouldShowToday(today) && (!isCompleteForToday(h, today, allRecords, todayRecMap) || h.id in completingIds)
        }
    }

    private fun isCompleteForToday(
        habit: Habit,
        today: LocalDate,
        allRecords: List<HabitRecord>,
        todayRecMap: Map<Long, HabitRecord>,
    ): Boolean {
        val rec = todayRecMap[habit.id]
        return when {
            habit.frequencyType == HabitFrequency.DAILY || habit.frequencyCount == 1 ->
                rec != null && rec.state != HabitState.NOT_DONE
            else -> rec != null || habit.isCompleteForPeriod(today, allRecords)
        }
    }
}
