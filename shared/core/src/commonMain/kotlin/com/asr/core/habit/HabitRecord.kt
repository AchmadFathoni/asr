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
        if (existing?.state == HabitState.DONE) {
            return HabitRecord(
                id = existing.id, habitId = habit.id, date = date,
                state = HabitState.NOT_DONE, count = 0,
            )
        }
        return HabitRecord(
            id = existing?.id ?: 0, habitId = habit.id, date = date,
            state = HabitState.DONE, count = 1,
        )
    }
    return HabitRecord(
        id = existing?.id ?: 0, habitId = habit.id, date = date,
        state = targetState, count = 0,
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
