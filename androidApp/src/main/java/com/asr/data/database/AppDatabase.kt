package com.asr.data.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

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
        const val SCHEMA_VERSION = 5

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.prepare("ALTER TABLE tasks DROP COLUMN order_index").step()
                connection.prepare("ALTER TABLE tasks ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0").step()
                connection.prepare("ALTER TABLE habits DROP COLUMN order_index").step()
                connection.prepare("ALTER TABLE habits ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0").step()
            }
        }
    }
}
