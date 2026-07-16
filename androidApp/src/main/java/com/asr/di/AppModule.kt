package com.asr.di

import android.content.Context
import androidx.room3.Room
import com.asr.app.util.AndroidSoundPlayer
import com.asr.core.interfaces.SoundPlayer
import com.asr.data.database.AppDatabase
import com.asr.data.database.HabitDao
import com.asr.data.database.TagDao
import com.asr.data.database.TaskDao
import com.asr.ui.di.UIModules
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [UIModules::class])
@ComponentScan("com.asr")
class AppModule {
    @Single
    fun provideDatabase(context: Context): AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        AppDatabase.DB_NAME,
    ).addMigrations(AppDatabase.MIGRATION_4_5).build()

    @Single
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Single
    fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()

    @Single
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Single
    fun provideSoundPlayer(context: Context): SoundPlayer = AndroidSoundPlayer(context)
}
