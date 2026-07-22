package com.asr.ui.today


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.asr.core.interfaces.SoundPlayer
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject
import com.asr.ui.app.EmptyState
import com.asr.ui.app.FilterBottomSheet
import com.asr.ui.app.PinnedItemDivider
import com.asr.ui.app.SparkleCheck
import com.asr.ui.app.UndoDeleteSnackbarEffect
import com.asr.ui.habit.HabitItem
import com.asr.ui.tagColorForValue
import com.asr.ui.viewmodel.TodayViewModel
import asr.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.vectorResource

@Composable
fun TodayPage(viewModel: TodayViewModel) {
    val state by viewModel.state.collectAsState()
    val soundPlayer = koinInject<SoundPlayer>()
    val snackbarHostState = remember { SnackbarHostState() }

    UndoDeleteSnackbarEffect(
        pendingDeletedTasks = state.pendingDeletedTasks,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.onAction(TodayViewModel.Action.UndoDeleteDoneTasks) },
        onDismiss = { viewModel.onAction(TodayViewModel.Action.DismissDeletedTasks) },
    )

    var previousAllDone by remember { mutableStateOf(false) }
    LaunchedEffect(state.allDone) {
        if (state.allDone && !previousAllDone) {
            snapshotFlow { snackbarHostState.currentSnackbarData }
                .first { it == null }
            snackbarHostState.showSnackbar("All done for today!", duration = SnackbarDuration.Short)
        }
        previousAllDone = state.allDone
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
    if (state.isLoading) {
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(48.dp)) {
            CircularProgressIndicator()
        }
    } else {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            val filterActive = state.filter.searchQuery.isNotBlank() || state.filter.selectedTagIds.isNotEmpty()
            Box {
                IconButton(onClick = { viewModel.onAction(TodayViewModel.Action.ToggleFilterSheet) }) {
                    Icon(imageVector = vectorResource(Res.drawable.filter), contentDescription = "Filter")
                }
                if (filterActive) Box(
                    Modifier.align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val listState = rememberLazyListState()
        LazyColumn(modifier = Modifier.weight(1f), state = listState) {
            // Tasks section
            if (state.tasks.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Tasks", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                val lastTaskPinIdx = state.tasks.indexOfLast { it.isPinned }
                val hasTaskUnpinnedAfter = lastTaskPinIdx >= 0 && lastTaskPinIdx < state.tasks.size - 1
                items(state.tasks, key = { it.id }) { task ->
                    val isParent = task.id in state.parentTaskIds
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clipToBounds()
                            .clickable {
                                if (!isParent) {
                                    if (!task.isDone) soundPlayer.play()
                                    viewModel.onAction(TodayViewModel.Action.ToggleTask(task.id))
                                }
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
                            if (isParent) {
                                Spacer(Modifier.width(30.dp))
                            } else {
                                SparkleCheck(
                                    isDone = task.isDone,
                                    onToggle = {
                                        if (!task.isDone) soundPlayer.play()
                                        viewModel.onAction(TodayViewModel.Action.ToggleTask(task.id))
                                    },
                                )
                            }
                            Text(
                                text = task.title,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                                ),
                            )
                            state.taskTagMappings[task.id]?.forEach { tagId ->
                                state.tags.find { it.id == tagId }?.color?.let {
                                    Spacer(Modifier.width(2.dp))
                                    Box(
                                        Modifier.size(8.dp).clip(CircleShape).background(tagColorForValue(it))
                                    )
                                }
                            }
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(onClick = { expanded = true }, modifier = Modifier.semantics { contentDescription = "More options" }) {
                                    Text("\u22EE", fontWeight = FontWeight.Bold)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text(if (task.isPinned) "Unpin" else "Pin") },
                                        onClick = { expanded = false; viewModel.onAction(TodayViewModel.Action.TogglePinTask(task.id)) },
                                    )
                                }
                            }
                        }
                    }
                    PinnedItemDivider(state.tasks, task, lastTaskPinIdx, hasTaskUnpinnedAfter, showThinDividers = false)
                }
            }

            // Habits section
            if (state.habits.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Habits", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                val lastHabitPinIdx = state.habits.indexOfLast { it.isPinned }
                val hasHabitUnpinnedAfter = lastHabitPinIdx >= 0 && lastHabitPinIdx < state.habits.size - 1
                items(state.habits, key = { it.id }) { habit ->
                    HabitItem(
                        habit = habit,
                        record = state.habitRecords[habit.id],
                        onSetState = { viewModel.onAction(TodayViewModel.Action.ToggleHabit(habit.id, it)) },
                        periodCount = state.periodCounts[habit.id] ?: 0,
                        onTogglePin = { viewModel.onAction(TodayViewModel.Action.TogglePinHabit(habit.id)) },
                        tags = state.tags.filter { state.habitTagMappings[habit.id]?.contains(it.id) == true },
                    )
                    PinnedItemDivider(state.habits, habit, lastHabitPinIdx, hasHabitUnpinnedAfter)
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

    if (state.showPunishmentDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(TodayViewModel.Action.DismissPunishmentDialog(false)) },
            text = {
                Column {
                    Text("Unfortunately, the clock is ticking, the hours are going by.")
                    Spacer(Modifier.height(4.dp))
                    Text("The past increases, the future recedes.")
                    Spacer(Modifier.height(4.dp))
                    Text("Possibilities decreasing, regrets mounting")
                    Spacer(Modifier.height(16.dp))
                    Text("Do you understand?", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(TodayViewModel.Action.DismissPunishmentDialog(true)) }) {
                    Text("I understand")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(TodayViewModel.Action.DismissPunishmentDialog(false)) }) {
                    Text("Remain ignorant")
                }
            },
        )
    }
}
