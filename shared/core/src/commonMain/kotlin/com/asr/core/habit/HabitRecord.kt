package com.asr.core.habit

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

fun habitRecordWithNewState(
    existing: HabitRecord?,
    habit: Habit,
    date: LocalDate,
    targetState: HabitState,
): HabitRecord {
    if (targetState == HabitState.DONE) {
        val existingState = existing?.state
        val newCount = if (existingState == HabitState.DONE) 0 else (existing?.count ?: 0) + 1
        val state = if (newCount >= habit.frequencyCount) HabitState.DONE else HabitState.NOT_DONE
        return HabitRecord(
            id = existing?.id ?: 0,
            habitId = habit.id,
            date = date,
            state = state,
            count = newCount,
        )
    }
    return HabitRecord(
        id = existing?.id ?: 0,
        habitId = habit.id,
        date = date,
        state = targetState,
    )
}

@Serializable
data class HabitRecord(
    val id: Long = 0,
    val habitId: Long,
    val date: LocalDate,
    val state: HabitState = HabitState.NOT_DONE,
    val count: Int = 0,
)

@Serializable
enum class HabitState {
    NOT_DONE,
    DONE,
    SKIPPED,
}
