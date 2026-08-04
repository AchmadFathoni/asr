package com.asr.data.storage

import com.asr.core.habit.Habit
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitStorage
import com.asr.data.database.HabitDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class RoomHabitStorage(private val habitDao: HabitDao) : HabitStorage {
    override fun observeHabits(): Flow<List<Habit>> =
        habitDao.getAllHabitsFlow().map { it.map { e -> e.toDomain() } }

    override fun observeRecords(): Flow<List<HabitRecord>> =
        habitDao.getAllRecordsFlow().map { it.map { e -> e.toDomain() } }

    override fun observeRecordsForDate(date: LocalDate): Flow<List<HabitRecord>> =
        habitDao.getRecordsForDateFlow(date.toEpochDays()).map { it.map { e -> e.toDomain() } }

    override suspend fun getHabitById(id: Long): Habit? =
        habitDao.getHabitById(id)?.toDomain()

    override suspend fun getRecordForDate(habitId: Long, date: LocalDate): HabitRecord? =
        habitDao.getRecordForDate(habitId, date.toEpochDays())?.toDomain()

    override suspend fun upsertHabit(habit: Habit): Long =
        habitDao.upsertHabit(habit.toEntity())

    override suspend fun deleteHabit(habitId: Long) =
        habitDao.deleteHabit(habitId)

    override suspend fun upsertRecord(record: HabitRecord) {
        val entity = record.toEntity()
        if (entity.id == 0L) {
            habitDao.upsertRecordForDate(entity.habitId, entity.date, entity.state, entity.count)
        } else {
            habitDao.upsertRecord(entity)
        }
    }

    override suspend fun deleteRecord(habitId: Long, date: LocalDate) =
        habitDao.deleteRecord(habitId, date.toEpochDays())

    override suspend fun getRecordsForHabit(habitId: Long): List<HabitRecord> =
        habitDao.getRecordsForHabit(habitId).map { it.toDomain() }

    override suspend fun getCompletionCountInPeriod(habitId: Long, start: LocalDate, end: LocalDate): Int =
        habitDao.getPeriodTotalCount(habitId, start.toEpochDays(), end.toEpochDays()) ?: 0

    override suspend fun replaceAll(habits: List<Habit>, records: List<HabitRecord>) {
        habitDao.deleteAllHabits()
        habitDao.deleteAllRecords()
        habitDao.insertAllHabits(habits.map { it.toEntity() })
        habitDao.insertAllRecords(records.map { it.toEntity() })
    }
}
