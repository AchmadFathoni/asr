package com.asr.desktop

import com.asr.core.habit.HabitRepo
import com.asr.core.habit.HabitStorage
import com.asr.core.habit.SharedHabitRepo
import com.asr.core.interfaces.SoundPlayer
import com.asr.core.interfaces.WidgetUpdater
import com.asr.core.settings.SettingsRepo
import com.asr.core.settings.SettingsStorage
import com.asr.core.settings.SharedSettingsRepo
import com.asr.core.tag.SharedTagRepo
import com.asr.core.tag.TagRepo
import com.asr.core.tag.TagStorage
import com.asr.core.task.SharedTaskRepo
import com.asr.core.task.TaskRepo
import com.asr.core.task.TaskStorage
import com.asr.ui.di.DefaultSoundPlayer
import com.asr.ui.di.DefaultWidgetUpdater
import com.asr.ui.di.UIModules
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [UIModules::class])
@ComponentScan("com.asr.desktop")
class DesktopAppModule {
    @Single(binds = [TaskStorage::class])
    fun provideTaskStorage(): TaskStorage = JsonTaskStorage()

    @Single(binds = [HabitStorage::class])
    fun provideHabitStorage(): HabitStorage = JsonHabitStorage()

    @Single(binds = [TagStorage::class])
    fun provideTagStorage(): TagStorage = JsonTagStorage()

    @Single(binds = [SettingsStorage::class])
    fun provideSettingsStorage(): SettingsStorage = JsonSettingsStorage()

    @Single(binds = [TaskRepo::class])
    fun provideTaskRepo(storage: TaskStorage, widgetUpdater: WidgetUpdater): TaskRepo = SharedTaskRepo(storage, widgetUpdater)

    @Single(binds = [HabitRepo::class])
    fun provideHabitRepo(storage: HabitStorage, widgetUpdater: WidgetUpdater): HabitRepo = SharedHabitRepo(storage, widgetUpdater)

    @Single(binds = [TagRepo::class])
    fun provideTagRepo(storage: TagStorage): TagRepo = SharedTagRepo(storage)

    @Single(binds = [SettingsRepo::class])
    fun provideSettingsRepo(storage: SettingsStorage): SettingsRepo = SharedSettingsRepo(storage)

    @Single(binds = [SoundPlayer::class])
    fun provideSoundPlayer(): SoundPlayer = DefaultSoundPlayer()

    @Single(binds = [WidgetUpdater::class])
    fun provideWidgetUpdater(): WidgetUpdater = DefaultWidgetUpdater()
}
