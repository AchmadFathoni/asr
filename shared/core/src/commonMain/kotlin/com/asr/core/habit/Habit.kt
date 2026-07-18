package com.asr.core.habit

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Habit(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val frequencyType: HabitFrequency = HabitFrequency.DAILY,
    val frequencyCount: Int = 1, // ponytail: per-period total, not split across daysOfWeek
    val daysOfWeek: Set<Int> = emptySet(),
    val daysOfMonth: Set<Int> = emptySet(),
    val yearlyDates: Set<Int> = emptySet(),
    val isPinned: Boolean = false,
    val reminderTime: String? = null,
)

@Serializable
enum class HabitFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

fun Habit.shouldShowToday(today: LocalDate): Boolean = when (frequencyType) {
    HabitFrequency.DAILY -> true
    HabitFrequency.WEEKLY -> if (daysOfWeek.isEmpty()) true else today.dayOfWeek.ordinal + 1 in daysOfWeek
    HabitFrequency.MONTHLY -> {
        if (daysOfMonth.isEmpty()) return true
        val maxDay = daysInMonth(today.year, today.month.ordinal + 1)
        today.day in daysOfMonth.map { kotlin.math.min(it, maxDay) }
    }
    HabitFrequency.YEARLY -> {
        if (yearlyDates.isEmpty()) return true
        (today.month.ordinal + 1) * 100 + today.day in yearlyDates
    }
}

fun Habit.computeStreak(records: List<HabitRecord>, today: LocalDate, requireToday: Boolean = true): Int {
    fun periodKey(date: LocalDate): Long = when (frequencyType) {
        HabitFrequency.DAILY -> date.toEpochDays()
        HabitFrequency.WEEKLY -> (date.toEpochDays() - 4) / 7 // Monday-aligned
        HabitFrequency.MONTHLY -> date.year.toLong() * 12 + date.month.ordinal
        HabitFrequency.YEARLY -> date.year.toLong()
    }

    val myRecords = records.filter { it.habitId == id }

    val completePeriods = if (frequencyType == HabitFrequency.DAILY || frequencyCount == 1) {
        myRecords.filter { it.state == HabitState.DONE }.map { periodKey(it.date) }.toSet()
    } else {
        myRecords.groupBy { periodKey(it.date) }
            .filter { (_, recs) -> recs.sumOf { it.count } >= frequencyCount }
            .keys
    }

    fun countFrom(start: Long): Int {
        var streak = 0
        var key = start
        while (key in completePeriods) { streak++; key-- }
        return streak
    }

    val todayKey = periodKey(today)
    if (todayKey in completePeriods) return countFrom(todayKey)
    if (requireToday) return 0
    val prevKey = todayKey - 1
    return if (prevKey in completePeriods) countFrom(prevKey) else 0
}

fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 30
}
