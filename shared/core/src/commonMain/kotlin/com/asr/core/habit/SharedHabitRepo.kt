package com.asr.core.habit

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class SharedHabitRepo(private val storage: HabitStorage) : HabitRepo {
    override fun getHabitsFlow(): Flow<List<Habit>> = storage.observeHabits()
    override fun getRecordsFlow(): Flow<List<HabitRecord>> = storage.observeRecords()
    override fun getRecordsForDateFlow(date: LocalDate): Flow<List<HabitRecord>> = storage.observeRecordsForDate(date)
    override suspend fun getHabitById(id: Long): Habit? = storage.getHabitById(id)
    override suspend fun getRecordForDate(habitId: Long, date: LocalDate): HabitRecord? = storage.getRecordForDate(habitId, date)
    override suspend fun upsertHabit(habit: Habit): Long = storage.upsertHabit(habit)
    override suspend fun deleteHabit(habitId: Long) = storage.deleteHabit(habitId)
    override suspend fun upsertRecord(record: HabitRecord) = storage.upsertRecord(record)
    override suspend fun deleteRecord(habitId: Long, date: LocalDate) = storage.deleteRecord(habitId, date)
    override suspend fun getRecordsForHabit(habitId: Long): List<HabitRecord> = storage.getRecordsForHabit(habitId)
    override suspend fun getCompletionCountInPeriod(habitId: Long, start: LocalDate, end: LocalDate): Int =
        storage.getCompletionCountInPeriod(habitId, start, end)
    override suspend fun insertAll(habits: List<Habit>, records: List<HabitRecord>) = storage.replaceAll(habits, records)
}
