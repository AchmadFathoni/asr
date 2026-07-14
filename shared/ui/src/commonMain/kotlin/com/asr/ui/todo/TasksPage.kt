package com.asr.ui.todo

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import com.asr.core.interfaces.SoundPlayer
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import asr.shared.ui.generated.resources.*
import com.asr.core.now
import com.asr.core.task.Task
import com.asr.ui.LIGHT_CHECK_COLORS
import com.asr.ui.TAG_COLORS
import com.asr.ui.app.EmptyState
import com.asr.ui.app.FilterBottomSheet
import com.asr.ui.viewmodel.TaskFilter
import com.asr.ui.viewmodel.TasksViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import org.jetbrains.compose.resources.vectorResource

@Composable
fun TasksPage(viewModel: TasksViewModel) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDescription by remember { mutableStateOf("") }
    var newTaskReminder by remember { mutableStateOf("") }
    var newDueDate by remember { mutableStateOf<LocalDate?>(null) }
    var newTaskParentId by remember { mutableStateOf<Long?>(null) }
    var selectedTagIds by remember { mutableStateOf(setOf<Long>()) }
    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember { mutableStateOf<Long?>(null) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val subTaskMap = remember(state.tasks) {
        state.tasks.filter { it.parentId != null }.groupBy { it.parentId!! }
    }

    val flatTasks = remember(state.tasks, state.expandedTaskIds) {
        buildFlatList(state.tasks, state.expandedTaskIds)
    }

    val taskToDeleteHasChildren = taskToDelete?.let { subTaskMap.containsKey(it.id) } ?: false
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                newTaskTitle = ""; newTaskDescription = ""; newTaskReminder = ""
                newDueDate = null; newTaskParentId = null
                selectedTagIds = emptySet(); newTagName = ""; newTagColor = null
                editingTask = null; showAddDialog = true
            }) {
                Icon(imageVector = vectorResource(Res.drawable.add), contentDescription = "Add task")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { viewModel.onAction(TasksViewModel.Action.ToggleFilterSheet) }) {
                    Icon(imageVector = vectorResource(Res.drawable.filter), contentDescription = "Filter")
                }
            }
            Spacer(Modifier.height(8.dp))

            Row {
                TaskFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.onAction(TasksViewModel.Action.SetFilter(filter)) },
                        label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
                if (state.filter == TaskFilter.DONE) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.onAction(TasksViewModel.Action.DeleteDoneTasks) }) {
                        Text("Clear done", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(flatTasks) { (task, depth) ->
                    val hasChildren = subTaskMap.containsKey(task.id)
                    val progress = if (hasChildren) countProgress(task.id, subTaskMap) else null
                    TaskRow(
                        task = task,
                        depth = depth,
                        hasChildren = hasChildren,
                        isExpanded = task.id in state.expandedTaskIds,
                        progress = progress,
                        onToggle = { viewModel.onAction(TasksViewModel.Action.ToggleTask(task.id)) },
                        showUndoSnackbar = { undo ->
                            scope.launch {
                                if (snackbarHostState.showSnackbar("Completed", "Undo") == SnackbarResult.ActionPerformed)
                                    undo()
                            }
                        },
                        onToggleExpand = { viewModel.onAction(TasksViewModel.Action.ToggleExpand(task.id)) },
                        onDelete = { taskToDelete = task },
                        onAddSub = {
                            newTaskTitle = ""
                            newTaskDescription = ""
                            newTaskReminder = ""
                            newDueDate = null
                            newTagName = ""; newTagColor = null
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
                            newTagName = ""; newTagColor = null
                            newTaskParentId = null
                            showAddDialog = true
                        },
                    )
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
                Column {
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
                                "${it.month}/${it.day}/${it.year}"
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

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newTagName,
                                    onValueChange = { newTagName = it },
                                    label = { Text("New tag name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = {
                                        if (newTagName.isNotBlank()) {
                                            viewModel.onAction(TasksViewModel.Action.CreateTag(newTagName.trim(), newTagColor))
                                            newTagName = ""; newTagColor = null
                                        }
                                    },
                                    enabled = newTagName.isNotBlank(),
                                ) { Text("Add") }
                            }
                            if (newTagName.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Column {
                                    TAG_COLORS.chunked(8).forEach { row ->
                                        Row(Modifier.fillMaxWidth()) {
                                            row.forEach { (c, _) ->
                                                val selected = newTagColor == c
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .padding(2.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(c))
                                                        .clickable { newTagColor = if (selected) null else c },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (selected) {
                                                        Text(
                                                            "✓",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (c in LIGHT_CHECK_COLORS) Color.Black else Color.White,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    if (state.tags.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row {
                            state.tags.forEach { tag ->
                                FilterChip(
                                    selected = tag.id in selectedTagIds,
                                    onClick = {
                                        selectedTagIds = if (tag.id in selectedTagIds)
                                            selectedTagIds - tag.id
                                        else selectedTagIds + tag.id
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val color = tag.color
                                            if (color != null) {
                                                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(color)))
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(tag.name)
                                        }
                                    },
                                )
                            }
                        }
                    }
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
                            .toLocalDateTime(TimeZone.UTC).date
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

@Composable
fun TaskRow(
    task: Task,
    depth: Int,
    hasChildren: Boolean,
    isExpanded: Boolean,
    progress: Pair<Int, Int>?,
    onToggle: () -> Unit,
    showUndoSnackbar: ((undo: () -> Unit) -> Unit)? = null,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
    onAddSub: () -> Unit,
    onEdit: () -> Unit,
) {
    val soundPlayer = koinInject<SoundPlayer>()
    val scale by animateFloatAsState(
        targetValue = if (task.isDone) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
    )

    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(start = (depth * 24).dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable {
                if (!task.isDone) {
                    soundPlayer.play()
                    onToggle()
                    showUndoSnackbar?.invoke { onToggle() }
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (task.isDone) 0.5f else 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = {
                if (!task.isDone) {
                    soundPlayer.play()
                    onToggle()
                    showUndoSnackbar?.invoke { onToggle() }
                }
            })
            if (hasChildren) {
                TextButton(onClick = onToggleExpand, modifier = Modifier.padding(0.dp)) {
                    Text(if (isExpanded) "\u25BE" else "\u25B8")
                }
            }
            Text(
                text = task.title,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                ),
            )
            if (progress != null && progress.second > 0) {
                Text(
                    "${progress.first}/${progress.second}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onAddSub) { Text("+") }
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = onDelete) { Text("Del") }
        }
    }
}

private fun buildFlatList(
    tasks: List<Task>,
    expandedIds: Set<Long>,
): List<Pair<Task, Int>> {
    val subTaskMap = tasks.filter { it.parentId != null }.groupBy { it.parentId!! }
    fun recurse(items: List<Task>, depth: Int): List<Pair<Task, Int>> {
        val result = mutableListOf<Pair<Task, Int>>()
        for (task in items) {
            result.add(task to depth)
            if (task.id in expandedIds) {
                result.addAll(recurse(subTaskMap[task.id].orEmpty(), depth + 1))
            }
        }
        return result
    }
    return recurse(tasks.filter { it.parentId == null }, 0)
}

private fun countProgress(
    taskId: Long,
    subTaskMap: Map<Long, List<Task>>,
): Pair<Int, Int> {
    val subs = subTaskMap[taskId] ?: return 0 to 0
    var done = subs.count { it.isDone }
    var total = subs.size
    for (sub in subs) {
        val (d, t) = countProgress(sub.id, subTaskMap)
        done += d
        total += t
    }
    return done to total
}
