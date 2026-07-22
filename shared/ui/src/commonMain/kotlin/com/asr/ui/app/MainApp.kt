package com.asr.ui.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import asr.shared.ui.generated.resources.*
import com.asr.ui.habit.HabitsPage
import com.asr.ui.setting.SettingsPage
import com.asr.ui.today.TodayPage
import com.asr.ui.tasks.TasksPage
import com.asr.ui.viewmodel.HabitsViewModel
import com.asr.ui.viewmodel.SettingsViewModel
import com.asr.ui.viewmodel.TasksViewModel
import com.asr.ui.viewmodel.TodayViewModel
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainApp() {
    val settingsVM = koinViewModel<SettingsViewModel>()
    val settingsState by settingsVM.state.collectAsState()

    ASRTheme(theme = settingsState.theme) {
    var selectedTab by rememberSaveable { mutableStateOf(AppRoute.Today) }

    val todayVM = koinViewModel<TodayViewModel>()
    val tasksVM = koinViewModel<TasksViewModel>()
    val habitsVM = koinViewModel<HabitsViewModel>()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
                NavigationBarItem(
                    selected = selectedTab == AppRoute.Today,
                    onClick = { selectedTab = AppRoute.Today },
                    alwaysShowLabel = true,
                    icon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.calendar_month),
                            contentDescription = "Today",
                        )
                    },
                    label = { Text("Today", maxLines = 1) },
                )
                NavigationBarItem(
                    selected = selectedTab == AppRoute.Tasks,
                    onClick = { selectedTab = AppRoute.Tasks },
                    alwaysShowLabel = true,
                    icon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.check_list),
                            contentDescription = "Tasks",
                        )
                    },
                    label = { Text("Tasks", maxLines = 1) },
                )
                NavigationBarItem(
                    selected = selectedTab == AppRoute.Habit,
                    onClick = { selectedTab = AppRoute.Habit },
                    alwaysShowLabel = true,
                    icon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.repeat),
                            contentDescription = "Habit",
                        )
                    },
                    label = { Text("Habit", maxLines = 1) },
                )
                NavigationBarItem(
                    selected = selectedTab == AppRoute.Settings,
                    onClick = { selectedTab = AppRoute.Settings },
                    alwaysShowLabel = true,
                    icon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.settings),
                            contentDescription = "Settings",
                        )
                    },
                    label = { Text("Settings", maxLines = 1) },
                )
            }
        },
    ) { padding ->
        Surface(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(targetState = selectedTab, transitionSpec = { fadeIn() togetherWith fadeOut() }) { tab ->
                when (tab) {
                    AppRoute.Today -> TodayPage(todayVM)
                    AppRoute.Tasks -> TasksPage(tasksVM)
                    AppRoute.Habit -> HabitsPage(habitsVM)
                    AppRoute.Settings -> SettingsPage(settingsVM)
                }
            }
        }
    }
    }
}
