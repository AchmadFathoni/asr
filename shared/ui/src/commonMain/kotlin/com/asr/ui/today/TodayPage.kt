package com.asr.ui.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.asr.core.interfaces.SoundPlayer
import org.koin.compose.koinInject
import com.asr.core.task.Task
import com.asr.ui.app.EmptyState
import com.asr.ui.app.FilterBottomSheet
import com.asr.ui.habit.HabitItem
import com.asr.ui.viewmodel.TodayViewModel
import asr.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.vectorResource

@Composable
fun TodayPage(viewModel: TodayViewModel) {
    val state by viewModel.state.collectAsState()
    val soundPlayer = koinInject<SoundPlayer>()

    if (state.isLoading) {
        Column(modifier = Modifier.fillMaxSize().padding(48.dp)) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { viewModel.onAction(TodayViewModel.Action.ToggleFilterSheet) }) {
                    Icon(imageVector = vectorResource(Res.drawable.filter), contentDescription = "Filter")
                }
            }
        }

        // Tasks section
        if (state.tasks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tasks", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.onAction(TodayViewModel.Action.DeleteDoneTasks) }) {
                        Text("Clear done", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(state.tasks) { task ->
                val scale by animateFloatAsState(
                    targetValue = if (task.isDone) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                )
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clickable {
                            if (!task.isDone) soundPlayer.play()
                            viewModel.onAction(TodayViewModel.Action.ToggleTask(task.id))
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = if (task.isDone) 0.5f else 0.3f
                        ),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = task.isDone,
                            onCheckedChange = {
                                if (!task.isDone) soundPlayer.play()
                                viewModel.onAction(TodayViewModel.Action.ToggleTask(task.id))
                            },
                        )
                        Text(
                            text = task.title,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                            ),
                        )
                    }
                }
            }
        }

        // Habits section
        if (state.habits.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("Habits", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            items(state.habits) { habit ->
                HabitItem(
                    habit = habit,
                    record = state.habitRecords[habit.id],
                    onSetState = { viewModel.onAction(TodayViewModel.Action.ToggleHabit(habit.id, it)) },
                )
            }
        }

        if (state.tasks.isEmpty() && state.habits.isEmpty()) {
            item {
                EmptyState(
                    icon = Res.drawable.calendar_month,
                    text = "Nothing for today.\nAdd tasks or habits in their tabs.",
                )
            }
        }
    }

    FilterBottomSheet(
        show = state.filter.showFilterSheet,
        searchQuery = state.filter.searchQuery,
        onSearchQueryChange = { viewModel.onAction(TodayViewModel.Action.SetSearchQuery(it)) },
        tags = state.tags,
        selectedTagIds = state.filter.selectedTagIds,
        onTagToggle = { viewModel.onAction(TodayViewModel.Action.ToggleTag(it)) },
        filterDate = null,
        showDateFilter = false,
        onDateChange = {},
        onReset = {
            viewModel.onAction(TodayViewModel.Action.SetSearchQuery(""))
            viewModel.onAction(TodayViewModel.Action.ClearTagFilter)
        },
        onDismiss = { viewModel.onAction(TodayViewModel.Action.ToggleFilterSheet) },
    )
}
