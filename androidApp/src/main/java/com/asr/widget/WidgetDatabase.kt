package com.asr.widget

import android.content.Context
import androidx.room3.Room
import com.asr.data.database.AppDatabase

@Volatile private var dbInstance: AppDatabase? = null

@Synchronized
fun getDatabase(context: Context): AppDatabase {
    return dbInstance ?: Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        AppDatabase.DB_NAME,
    ).addMigrations(AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6).build().also { dbInstance = it }
}
