package com.asr.data.repository

import com.asr.core.habit.Habit
import com.asr.core.habit.HabitFrequency
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitRepo
import com.asr.core.habit.HabitState
import com.asr.data.database.Converters
import com.asr.data.database.HabitDao
import com.asr.data.database.HabitEntity
import com.asr.data.database.HabitRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single

@Single(binds = [HabitRepo::class])
class HabitRepository(private val habitDao: HabitDao) : HabitRepo {

    override fun getHabitsFlow(): Flow<List<Habit>> =
        habitDao.getAllHabitsFlow().map { it.map { e -> e.toDomain() } }

    override fun getRecordsFlow(): Flow<List<HabitRecord>> =
        habitDao.getAllRecordsFlow().map { it.map { e -> e.toDomain() } }

    override fun getRecordsForDateFlow(date: LocalDate): Flow<List<HabitRecord>> =
            habitDao.getRecordsForDateFlow(date.toEpochDays())
            .map { it.map { e -> e.toDomain() } }

    override suspend fun getHabitById(id: Long): Habit? =
        habitDao.getHabitById(id)?.toDomain()

    override suspend fun getRecordForDate(habitId: Long, date: LocalDate): HabitRecord? =
            habitDao.getRecordForDate(habitId, date.toEpochDays())?.toDomain()

    override suspend fun upsertHabit(habit: Habit): Long =
        habitDao.upsertHabit(habit.toEntity())

    override suspend fun deleteHabit(habitId: Long) =
        habitDao.deleteHabit(habitId)

    override suspend fun upsertRecord(record: HabitRecord) =
        habitDao.upsertRecord(record.toEntity())

    override suspend fun deleteRecord(habitId: Long, date: LocalDate) =
            habitDao.deleteRecord(habitId, date.toEpochDays())

    override suspend fun getRecordsForHabit(habitId: Long): List<HabitRecord> =
        habitDao.getRecordsForHabit(habitId).map { it.toDomain() }

    override suspend fun getCompletionCountInPeriod(
        habitId: Long,
        start: LocalDate,
        end: LocalDate,
    ): Int =
        habitDao.getCompletionCount(
            habitId,
            start.toEpochDays(),
            end.toEpochDays(),
        ) ?: 0

    override suspend fun insertAll(habits: List<Habit>, records: List<HabitRecord>) {
        habitDao.deleteAllHabits()
        habitDao.deleteAllRecords()
        habitDao.insertAllHabits(habits.map { it.toEntity() })
        habitDao.insertAllRecords(records.map { it.toEntity() })
    }

    private fun HabitEntity.toDomain() = Habit(
        id = id,
        title = title,
        description = description,
        frequencyType = HabitFrequency.valueOf(frequencyType),
        frequencyCount = frequencyCount,
        daysOfWeek = if (daysOfWeek.isNotBlank()) daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }.toSet() else emptySet(),
        dayOfMonth = dayOfMonth,
        monthOfYear = monthOfYear,
        order = order,
        reminderTime = reminderTime,
    )

    private fun Habit.toEntity() = HabitEntity(
        id = id,
        title = title,
        description = description,
        frequencyType = frequencyType.name,
        frequencyCount = frequencyCount,
        dayOfWeek = daysOfWeek.firstOrNull(),
        daysOfWeek = daysOfWeek.joinToString(","),
        dayOfMonth = dayOfMonth,
        monthOfYear = monthOfYear,
        order = order,
        reminderTime = reminderTime,
    )

    private fun HabitRecordEntity.toDomain() = HabitRecord(
        id = id,
        habitId = habitId,
        date = Converters.dateFromTimestamp(date),
        state = HabitState.valueOf(state),
        count = count,
    )

    private fun HabitRecord.toEntity() = HabitRecordEntity(
        id = id,
        habitId = habitId,
        date = Converters.dateToTimestamp(date),
        state = state.name,
        count = count,
    )
}
