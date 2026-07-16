package com.asr.core.habit

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface HabitStorage {
    fun observeHabits(): Flow<List<Habit>>
    fun observeRecords(): Flow<List<HabitRecord>>
    fun observeRecordsForDate(date: LocalDate): Flow<List<HabitRecord>>
    suspend fun getHabitById(id: Long): Habit?
    suspend fun getRecordForDate(habitId: Long, date: LocalDate): HabitRecord?
    suspend fun upsertHabit(habit: Habit): Long
    suspend fun deleteHabit(habitId: Long)
    suspend fun upsertRecord(record: HabitRecord)
    suspend fun deleteRecord(habitId: Long, date: LocalDate)
    suspend fun getRecordsForHabit(habitId: Long): List<HabitRecord>
    suspend fun getCompletionCountInPeriod(habitId: Long, start: LocalDate, end: LocalDate): Int
    suspend fun replaceAll(habits: List<Habit>, records: List<HabitRecord>)
}
