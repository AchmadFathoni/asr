package com.asr.app

import android.app.Application
import com.asr.di.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.plugin.module.dsl.startKoin

class AsrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin<AppModule> {
            androidLogger()
            androidContext(this@AsrApplication)
        }
    }
}
