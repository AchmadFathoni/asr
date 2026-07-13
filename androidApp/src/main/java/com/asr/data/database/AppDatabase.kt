package com.asr.data.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitRecordEntity::class,
        TagEntity::class,
        TaskTagEntity::class,
        HabitTagEntity::class,
    ],
    version = AppDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
@ColumnTypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun tagDao(): TagDao

    companion object {
        const val DB_NAME = "asr_database"
        const val SCHEMA_VERSION = 3
    }
}
