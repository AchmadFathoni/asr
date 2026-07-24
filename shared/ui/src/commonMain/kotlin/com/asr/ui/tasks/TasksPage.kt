package com.asr.ui.tasks

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.asr.core.interfaces.SoundPlayer
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import asr.shared.ui.generated.resources.*
import com.asr.core.now
import com.asr.core.tag.Tag
import com.asr.core.task.Task
import com.asr.ui.app.CreateTagRow
import com.asr.ui.app.EmptyState
import com.asr.ui.app.FilterBottomSheet
import com.asr.ui.app.PinnedItemDivider
import com.asr.ui.app.SparkleCheck
import com.asr.ui.app.StatusFilterChips
import com.asr.ui.app.TagFilterRow
import com.asr.ui.app.TaskItemCard
import com.asr.ui.app.TopActionRow
import com.asr.ui.app.countProgress
import com.asr.ui.app.UndoDeleteSnackbarEffect

import com.asr.core.StatusFilter
import com.asr.ui.viewmodel.TasksViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import org.jetbrains.compose.resources.vectorResource

@Composable
fun TasksPage(viewModel: TasksViewModel) {
    val state by viewModel.state.collectAsState()
    val soundPlayer = koinInject<SoundPlayer>()
    var showAddDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDescription by remember { mutableStateOf("") }
    var newTaskReminder by remember { mutableStateOf("") }
    var newDueDate by remember { mutableStateOf<LocalDate?>(null) }
    var newTaskParentId by remember { mutableStateOf<Long?>(null) }
    var selectedTagIds by remember { mutableStateOf(setOf<Long>()) }
    var newTagName by remember { mutableStateOf("") }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    val createdTagId by viewModel.createdTagId.collectAsState()
    LaunchedEffect(createdTagId) {
        createdTagId?.let { id ->
            selectedTagIds = selectedTagIds + id
            viewModel.onAction(TasksViewModel.Action.ConsumeCreatedTag)
        }
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val subTaskMap = remember(state.tasks) {
        state.tasks.mapNotNull { t -> t.parentId?.let { pid -> pid to t } }
            .groupBy({ it.first }, { it.second })
    }

    val flatTasks = remember(state.tasks, state.expandedTaskIds) {
        buildFlatList(state.tasks, state.expandedTaskIds)
    }

    val taskToDeleteHasChildren = taskToDelete?.let { subTaskMap.containsKey(it.id) } ?: false
    val snackbarHostState = remember { SnackbarHostState() }

    UndoDeleteSnackbarEffect(
        pendingDeletedTasks = state.pendingDeletedTasks,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.onAction(TasksViewModel.Action.UndoDeleteDoneTasks) },
        onDismiss = { viewModel.onAction(TasksViewModel.Action.DismissDeletedTasks) },
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            val isEmpty = flatTasks.isEmpty() && !state.isLoading
            var pulseUp by remember { mutableStateOf(false) }
            val pulseScale by animateFloatAsState(
                targetValue = if (pulseUp) 1.2f else 1f,
                animationSpec = tween(700, easing = FastOutSlowInEasing),
            )
            if (isEmpty) {
                LaunchedEffect(Unit) {
                    while (true) {
                        pulseUp = !pulseUp
                        delay(700)
                    }
                }
            }
            TopActionRow(
                onToggleFilter = { viewModel.onAction(TasksViewModel.Action.ToggleFilterSheet) },
                filterActive = state.filterState.searchQuery.isNotBlank() || state.filterState.selectedTagIds.isNotEmpty() || state.filterState.filterDate != null,
                onAdd = {
                    newTaskTitle = ""
                    newTaskDescription = ""
                    newTaskReminder = ""
                    newDueDate = null
                    newTaskParentId = null
                    selectedTagIds = emptySet()
                    newTagName = ""
                    editingTask = null
                    showAddDialog = true
                },
                addContentDescription = "Add task",
                isEmpty = isEmpty,
                pulseScale = pulseScale,
                filterChips = {
                    StatusFilterChips(
                        entries = StatusFilter.entries,
                        selected = state.filter,
                        onSelect = { viewModel.onAction(TasksViewModel.Action.SetFilter(it)) },
                    )
                },
                centerActions = {
                    if (state.filter == StatusFilter.DONE) {
                        TextButton(onClick = { viewModel.onAction(TasksViewModel.Action.DeleteDoneTasks) }) {
                            Text("Clear done", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
            )
            Spacer(Modifier.height(8.dp))

            val listState = rememberLazyListState()
            LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                val lastPinnedIdx = flatTasks.indexOfLast { (task, _) -> task.isPinned }
                val hasUnpinnedAfter = lastPinnedIdx >= 0 && lastPinnedIdx < flatTasks.size - 1
                items(flatTasks, key = { (task, _) -> task.id }) { (task, depth) ->
                    val hasChildren = subTaskMap.containsKey(task.id)
                    val progress = if (hasChildren && state.filter == StatusFilter.ALL) countProgress(task.id, subTaskMap) else null
                    TaskItemCard(
                        task = task,
                        depth = depth,
                        isParent = hasChildren,
                        showChevron = hasChildren,
                        isExpanded = task.id in state.expandedTaskIds,
                        progress = progress,
                        soundPlayer = soundPlayer,
                        onToggle = { viewModel.onAction(TasksViewModel.Action.ToggleTask(task.id)) },
                        onToggleExpand = { viewModel.onAction(TasksViewModel.Action.ToggleExpand(task.id)) },
                        onTogglePin = { viewModel.onAction(TasksViewModel.Action.TogglePinTask(task.id)) },
                        onDelete = { taskToDelete = task },
                        onAddSub = {
                            newTaskTitle = ""
                            newTaskDescription = ""
                            newTaskReminder = ""
                            newDueDate = null
                            newTagName = ""
                            editingTask = null
                            newTaskParentId = task.id
                            showAddDialog = true
                        },
                        onEdit = {
                            editingTask = task
                            newTaskTitle = task.title
                            newTaskDescription = task.description
                            newTaskReminder = task.reminderTime ?: ""
                            newDueDate = task.dueDate
                            selectedTagIds = emptySet()
                            newTagName = ""
                            newTaskParentId = null
                            showAddDialog = true
                        },
                    )
                    PinnedItemDivider(flatTasks, task to depth, lastPinnedIdx, hasUnpinnedAfter, showThinDividers = false)
                }

                if (state.isLoading) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                        )
                    }
                } else if (flatTasks.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Res.drawable.check_list,
                            text = "No tasks. Tap + to add one.",
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    when {
                        editingTask != null -> "Edit Task"
                        newTaskParentId != null -> "New Sub-task"
                        else -> "New Task"
                    }
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTaskDescription,
                        onValueChange = { newTaskDescription = it },
                        label = { Text("Description (optional)") },
                        maxLines = 3,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(newDueDate?.let {
                                "${it.year}-${(it.month.ordinal + 1).toString().padStart(2, '0')}-${it.day.toString().padStart(2, '0')}"
                            } ?: "Set date")
                        }
                        if (newDueDate != null) {
                            Text("  at  ", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { showTimePicker = true }) {
                                Text(if (newTaskReminder.isNotBlank()) newTaskReminder else "set time")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Tags", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))

                        CreateTagRow(
                            tagName = newTagName,
                            onTagNameChange = { newTagName = it },
                            onCreate = {
                                if (newTagName.isNotBlank()) {
                                    viewModel.onAction(TasksViewModel.Action.CreateTag(newTagName.trim()))
                                    newTagName = ""
                                }
                            },
                        )

                    TagFilterRow(
                        tags = state.tags,
                        selectedTagIds = selectedTagIds,
                        onTagToggle = { id ->
                            selectedTagIds = if (id in selectedTagIds) selectedTagIds - id else selectedTagIds + id
                        },
                        useFlowRow = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTaskTitle.isNotBlank()) {
                        val task = editingTask?.copy(
                            title = newTaskTitle,
                            description = newTaskDescription,
                            dueDate = newDueDate,
                            reminderTime = newTaskReminder.ifBlank { null },
                        ) ?: Task(
                            title = newTaskTitle,
                            parentId = newTaskParentId,
                            description = newTaskDescription,
                            dueDate = newDueDate,
                            reminderTime = newTaskReminder.ifBlank { null },
                        )
                        viewModel.onAction(
                            TasksViewModel.Action.UpsertTask(task, selectedTagIds.toList())
                        )
                        showAddDialog = false
                    }
                }) { Text(if (editingTask != null) "Save" else "Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        newDueDate = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = kotlinx.datetime.LocalTime.now().hour,
            initialMinute = kotlinx.datetime.LocalTime.now().minute,
            is24Hour = true,
        )
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    newTaskReminder = "${timePickerState.hour.toString().padStart(2, '0')}:${timePickerState.minute.toString().padStart(2, '0')}"
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            title = { Text("Set reminder time") },
        ) {
            TimePicker(state = timePickerState)
        }
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text(if (taskToDeleteHasChildren) "Delete task and subtasks?" else "Delete task?") },
            text = {
                Column {
                    Text(task.title)
                    if (taskToDeleteHasChildren) {
                        Spacer(Modifier.height(4.dp))
                        Text("All subtasks will also be deleted.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(TasksViewModel.Action.DeleteTask(task))
                    taskToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { taskToDelete = null }) { Text("Cancel") } },
        )
    }

    FilterBottomSheet(
        show = state.filterState.showFilterSheet,
        searchQuery = state.filterState.searchQuery,
        onSearchQueryChange = { viewModel.onAction(TasksViewModel.Action.SetSearchQuery(it)) },
        tags = state.tags,
        selectedTagIds = state.filterState.selectedTagIds,
        onTagToggle = { viewModel.onAction(TasksViewModel.Action.ToggleTag(it)) },
        filterDate = state.filterState.filterDate,
        showDateFilter = true,
        onDateChange = { viewModel.onAction(TasksViewModel.Action.SetFilterDate(it)) },
        onReset = {
            viewModel.onAction(TasksViewModel.Action.SetSearchQuery(""))
            viewModel.onAction(TasksViewModel.Action.ClearTagFilter)
            viewModel.onAction(TasksViewModel.Action.SetFilterDate(null))
        },
        onDismiss = { viewModel.onAction(TasksViewModel.Action.ToggleFilterSheet) },
    )
}

