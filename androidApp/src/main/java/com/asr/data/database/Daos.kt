package com.asr.data.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY order_index ASC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDone = 0 ORDER BY order_index ASC")
    fun getUndoneTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDone = 1 ORDER BY id DESC")
    fun getDoneTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE parentId = :parentId ORDER BY order_index ASC")
    suspend fun getSubTasks(parentId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Upsert
    suspend fun upsertTask(task: TaskEntity): Long

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("DELETE FROM tasks WHERE isDone = 1")
    suspend fun deleteDoneTasks()

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Upsert
    suspend fun insertAll(tasks: List<TaskEntity>)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY order_index ASC")
    fun getAllHabitsFlow(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY order_index ASC")
    suspend fun getAllHabits(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Query("SELECT * FROM habit_records")
    fun getAllRecordsFlow(): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habit_records")
    suspend fun getAllRecords(): List<HabitRecordEntity>

    @Query("SELECT * FROM habit_records WHERE date = :date")
    fun getRecordsForDateFlow(date: Long): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId AND date = :date")
    suspend fun getRecordForDate(habitId: Long, date: Long): HabitRecordEntity?

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId")
    suspend fun getRecordsForHabit(habitId: Long): List<HabitRecordEntity>

    @Upsert
    suspend fun upsertHabit(habit: HabitEntity): Long

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Upsert
    suspend fun upsertRecord(record: HabitRecordEntity)

    @Query("DELETE FROM habit_records WHERE habitId = :habitId AND date = :date")
    suspend fun deleteRecord(habitId: Long, date: Long)

    @Query(
        """
        SELECT SUM(count) FROM habit_records 
        WHERE habitId = :habitId AND state = 'DONE' 
        AND date >= :start AND date <= :end
        """
    )
    suspend fun getCompletionCount(habitId: Long, start: Long, end: Long): Int?

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("DELETE FROM habit_records")
    suspend fun deleteAllRecords()

    @Upsert
    suspend fun insertAllHabits(habits: List<HabitEntity>)

    @Upsert
    suspend fun insertAllRecords(records: List<HabitRecordEntity>)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTagsFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Long): TagEntity?

    @Upsert
    suspend fun upsertTag(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Query(
        """
        SELECT t.* FROM tags t 
        INNER JOIN task_tags tt ON t.id = tt.tagId 
        WHERE tt.taskId = :taskId
        """
    )
    suspend fun getTagsForTask(taskId: Long): List<TagEntity>

    @Query(
        """
        SELECT t.* FROM tags t 
        INNER JOIN habit_tags ht ON t.id = ht.tagId 
        WHERE ht.habitId = :habitId
        """
    )
    suspend fun getTagsForHabit(habitId: Long): List<TagEntity>

    @Query("DELETE FROM task_tags WHERE taskId = :taskId")
    suspend fun clearTaskTags(taskId: Long)

    @Upsert
    suspend fun addTaskTag(junction: TaskTagEntity)

    @Query("DELETE FROM habit_tags WHERE habitId = :habitId")
    suspend fun clearHabitTags(habitId: Long)

    @Upsert
    suspend fun addHabitTag(junction: HabitTagEntity)

    @Upsert
    suspend fun insertAllTags(tags: List<TagEntity>)

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("DELETE FROM task_tags")
    suspend fun deleteAllTaskTags()

    @Query("DELETE FROM habit_tags")
    suspend fun deleteAllHabitTags()
}
