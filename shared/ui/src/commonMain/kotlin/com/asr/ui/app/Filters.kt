package com.asr.ui.app

import com.asr.core.habit.Habit
import com.asr.core.habit.shouldShowToday
import com.asr.core.task.Task
import kotlinx.datetime.LocalDate

object Filters {
    fun tasks(
        tasks: List<Task>,
        tagMappings: Map<Long, List<Long>>,
        query: String,
        tagIds: Set<Long>,
        date: LocalDate?,
    ): List<Task> {
        val q = query.trim().lowercase()
        val step1 = if (q.isEmpty()) tasks
            else tasks.filter { t -> t.title.lowercase().contains(q) || t.description.lowercase().contains(q) }
        val step2 = if (tagIds.isEmpty()) step1
            else step1.filter { tagMappings[it.id]?.any { t -> t in tagIds } == true }
        return if (date == null) step2
            else step2.filter { it.dueDate == date }
    }

    fun habits(
        habits: List<Habit>,
        tagMappings: Map<Long, List<Long>>,
        query: String,
        tagIds: Set<Long>,
        date: LocalDate?,
    ): List<Habit> {
        var result = habits
        if (date != null) result = result.filter { it.shouldShowToday(date) }
        val q = query.trim().lowercase()
        if (q.isNotEmpty()) result = result.filter { h -> h.title.lowercase().contains(q) || h.description.lowercase().contains(q) }
        if (tagIds.isNotEmpty()) result = result.filter { tagMappings[it.id]?.any { t -> t in tagIds } == true }
        return result
    }
}
