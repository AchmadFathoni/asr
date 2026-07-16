package com.asr.di

import android.content.Context
import androidx.room3.Room
import com.asr.app.util.AndroidSoundPlayer
import com.asr.core.habit.HabitRepo
import com.asr.core.habit.HabitStorage
import com.asr.core.habit.SharedHabitRepo
import com.asr.core.interfaces.SoundPlayer
import com.asr.core.settings.SettingsRepo
import com.asr.core.settings.SettingsStorage
import com.asr.core.settings.SharedSettingsRepo
import com.asr.core.tag.SharedTagRepo
import com.asr.core.tag.TagRepo
import com.asr.core.tag.TagStorage
import com.asr.core.task.SharedTaskRepo
import com.asr.core.task.TaskRepo
import com.asr.core.task.TaskStorage
import com.asr.data.database.AppDatabase
import com.asr.data.database.HabitDao
import com.asr.data.database.TagDao
import com.asr.data.database.TaskDao
import com.asr.data.storage.PrefsSettingsStorage
import com.asr.data.storage.RoomHabitStorage
import com.asr.data.storage.RoomTagStorage
import com.asr.data.storage.RoomTaskStorage
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

    @Single(binds = [TaskStorage::class])
    fun provideTaskStorage(taskDao: TaskDao): TaskStorage = RoomTaskStorage(taskDao)

    @Single(binds = [HabitStorage::class])
    fun provideHabitStorage(habitDao: HabitDao): HabitStorage = RoomHabitStorage(habitDao)

    @Single(binds = [TagStorage::class])
    fun provideTagStorage(tagDao: TagDao): TagStorage = RoomTagStorage(tagDao)

    @Single(binds = [SettingsStorage::class])
    fun provideSettingsStorage(context: Context): SettingsStorage = PrefsSettingsStorage(context)

    @Single(binds = [TaskRepo::class])
    fun provideTaskRepo(storage: TaskStorage): TaskRepo = SharedTaskRepo(storage)

    @Single(binds = [HabitRepo::class])
    fun provideHabitRepo(storage: HabitStorage): HabitRepo = SharedHabitRepo(storage)

    @Single(binds = [TagRepo::class])
    fun provideTagRepo(storage: TagStorage): TagRepo = SharedTagRepo(storage)

    @Single(binds = [SettingsRepo::class])
    fun provideSettingsRepo(storage: SettingsStorage): SettingsRepo = SharedSettingsRepo(storage)

    @Single
    fun provideSoundPlayer(context: Context): SoundPlayer = AndroidSoundPlayer(context)
}
