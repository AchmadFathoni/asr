package com.asr.core.habit

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Habit(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val frequencyType: HabitFrequency = HabitFrequency.DAILY,
    val frequencyCount: Int = 1,
    val daysOfWeek: Set<Int> = emptySet(),
    val dayOfMonth: Int? = null,
    val monthOfYear: Int? = null,
    val order: Int = 0,
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
        val dom = dayOfMonth ?: return true
        today.dayOfMonth == kotlin.math.min(dom, daysInMonth(today.year, today.monthNumber))
    }
    HabitFrequency.YEARLY -> {
        val moy = monthOfYear ?: return true
        val dom = dayOfMonth ?: return true
        today.monthNumber == moy && today.dayOfMonth == kotlin.math.min(dom, daysInMonth(today.year, moy))
    }
}

fun Habit.computeStreak(records: List<HabitRecord>, today: LocalDate): Int {
    val doneDates = records.filter { it.state == HabitState.DONE }.map { it.date }.toSet()
    var streak = 0
    var date = today
    val step = when (frequencyType) {
        HabitFrequency.DAILY -> 1
        HabitFrequency.WEEKLY -> 7
        HabitFrequency.MONTHLY -> 30
        HabitFrequency.YEARLY -> 365
    }
    while (date in doneDates) {
        streak++
        date = LocalDate.fromEpochDays(date.toEpochDays() - step)
    }
    return streak
}

fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 30
}
