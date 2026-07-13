package com.asr.desktop

import com.asr.ui.di.UIModules
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [UIModules::class])
@ComponentScan("com.asr.desktop")
class DesktopAppModule
