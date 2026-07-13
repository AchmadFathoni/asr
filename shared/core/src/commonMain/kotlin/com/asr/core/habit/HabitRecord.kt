package com.asr.core.habit

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

fun habitRecordWithNewState(
    existing: HabitRecord?,
    habit: Habit,
    date: LocalDate,
    targetState: HabitState,
    now: LocalDateTime,
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
            doneAt = if (state == HabitState.DONE) now else null,
        )
    }
    return HabitRecord(
        id = existing?.id ?: 0,
        habitId = habit.id,
        date = date,
        state = targetState,
        doneAt = if (targetState == HabitState.DONE) now else null,
    )
}

@Serializable
data class HabitRecord(
    val id: Long = 0,
    val habitId: Long,
    val date: LocalDate,
    val state: HabitState = HabitState.NOT_DONE,
    val count: Int = 0,
    val doneAt: LocalDateTime? = null,
)

@Serializable
enum class HabitState {
    NOT_DONE,
    DONE,
    SKIPPED,
}
