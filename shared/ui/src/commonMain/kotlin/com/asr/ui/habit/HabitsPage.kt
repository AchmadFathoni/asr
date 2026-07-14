package com.asr.ui.habit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.unit.dp
import com.asr.core.interfaces.SoundPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlinx.datetime.LocalDate
import asr.shared.ui.generated.resources.*
import com.asr.core.habit.Habit
import com.asr.core.habit.HabitFrequency
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitState
import com.asr.core.habit.daysInMonth
import com.asr.core.habit.shouldShowToday
import com.asr.core.now
import com.asr.core.tag.Tag
import com.asr.ui.LIGHT_CHECK_COLORS
import com.asr.ui.TAG_COLORS
import com.asr.ui.app.EmptyState
import com.asr.ui.app.FilterBottomSheet
import com.asr.ui.viewmodel.HabitsViewModel
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitsPage(viewModel: HabitsViewModel) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newHabitTitle by remember { mutableStateOf("") }
    var newHabitDescription by remember { mutableStateOf("") }
    var newHabitReminder by remember { mutableStateOf("") }
    var newFreq by remember { mutableStateOf(HabitFrequency.DAILY) }
    val newDaysOfWeek = remember { mutableStateListOf<Int>() }
    val newDaysOfMonth = remember { mutableStateListOf<Int>() }
    val selectedYearlyDates = remember { mutableStateListOf<Int>() }
    var activeYearlyMonth by remember { mutableStateOf(1) }
    var showTimePicker by remember { mutableStateOf(false) }
    var periodExpanded by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    var selectedTagIds by remember { mutableStateOf(setOf<Long>()) }
    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember { mutableStateOf<Long?>(null) }
    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            val isEmpty = state.habits.isEmpty() && !state.isLoading
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val filterActive = state.filter.searchQuery.isNotBlank() || state.filter.selectedTagIds.isNotEmpty() || state.filter.filterDate != null
                Box {
                    IconButton(onClick = { viewModel.onAction(HabitsViewModel.Action.ToggleFilterSheet) }) {
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
                IconButton(
                    onClick = {
                        newHabitTitle = ""; newHabitDescription = ""; newHabitReminder = ""
                        newFreq = HabitFrequency.DAILY
                        newDaysOfWeek.clear(); newDaysOfMonth.clear(); selectedYearlyDates.clear(); activeYearlyMonth = 1
                        selectedTagIds = emptySet(); newTagName = ""; newTagColor = null
                        editingHabit = null
                        showAddDialog = true
                    },
                    modifier = if (isEmpty) Modifier.graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                               else Modifier,
                ) {
                    Icon(imageVector = vectorResource(Res.drawable.add), contentDescription = "Add habit")
                }
            }
                Spacer(Modifier.height(8.dp))

                val filteredHabits = state.habits

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(48.dp),
                    )
                }

                var localHabits by remember(filteredHabits) { mutableStateOf(filteredHabits) }
                val lazyListState = rememberLazyListState()
                val reorderableState = rememberReorderableLazyListState(
                    lazyListState = lazyListState,
                    onMove = { from, to ->
                        localHabits = localHabits.toMutableList().apply {
                            add(to.index, removeAt(from.index))
                        }
                    },
                )
                LaunchedEffect(filteredHabits) {
                    if (!reorderableState.isAnyItemDragging) localHabits = filteredHabits
                }
                LaunchedEffect(localHabits) {
                    if (!reorderableState.isAnyItemDragging &&
                        localHabits.map { it.id } != filteredHabits.map { it.id }
                    ) {
                        viewModel.onAction(HabitsViewModel.Action.ReorderHabits(localHabits.map { it.id }))
                    }
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f),
                ) {
                    items(localHabits, key = { it.id }) { habit ->
                        val record = state.todayRecords[habit.id]
                        ReorderableItem(state = reorderableState, key = habit.id) { isDragging ->
                            HabitItem(
                                habit = habit,
                                record = record,
                                onSetState = { s ->
                                    viewModel.onAction(HabitsViewModel.Action.SetRecordState(habit.id, s))
                                },
                                showDoneSnackbar = { undo ->
                                    scope.launch {
                                        if (snackbarHostState.showSnackbar("Completed", "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed)
                                            undo()
                                    }
                                },
                                onViewHistory = { viewModel.onAction(HabitsViewModel.Action.ViewHabitHistory(habit.id)) },
                                onEdit = {
                                    editingHabit = habit
                                    newHabitTitle = habit.title
                                    newHabitDescription = habit.description
                                    newHabitReminder = habit.reminderTime ?: ""
                                    newFreq = habit.frequencyType
                                    newDaysOfWeek.clear(); newDaysOfWeek.addAll(habit.daysOfWeek)
                                    newDaysOfMonth.clear(); newDaysOfMonth.addAll(habit.daysOfMonth)
                                    selectedYearlyDates.clear(); selectedYearlyDates.addAll(habit.yearlyDates); activeYearlyMonth = 1
                                    selectedTagIds = emptySet(); newTagName = ""; newTagColor = null
                                    showAddDialog = true
                                },
                                streak = state.streaks[habit.id] ?: 0,
                            onDelete = { habitToDelete = habit },
                            tags = state.tags.filter { state.habitTagMappings[habit.id]?.contains(it.id) == true },
                            showDragHandle = true,
                            dragHandleModifier = Modifier.draggableHandle(),
                        )
                        }
                        HorizontalDivider()
                    }
                    if (filteredHabits.isEmpty() && !state.isLoading) {
                        item {
                            EmptyState(
                                icon = Res.drawable.repeat,
                                text = "No habits. Tap + to add one.",
                            )
                        }
                    }
                }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (editingHabit != null) "Edit Habit" else "New Habit") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = newHabitTitle, onValueChange = { newHabitTitle = it },
                        label = { Text("Title") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newHabitDescription,
                        onValueChange = { newHabitDescription = it },
                        label = { Text("Description (optional)") },
                        maxLines = 3,
                    )
                    Spacer(Modifier.height(12.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Schedule", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { periodExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("${newFreq.name.lowercase().replaceFirstChar { it.uppercase() }} ▼")
                                }
                                DropdownMenu(expanded = periodExpanded, onDismissRequest = { periodExpanded = false }) {
                                    HabitFrequency.entries.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                            onClick = { newFreq = f; periodExpanded = false },
                                        )
                                    }
                                }
                            }

                            if (newFreq == HabitFrequency.WEEKLY) {
                                Spacer(Modifier.height(8.dp))
                                Text("On:", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth()) {
                                    dayNames.forEachIndexed { i, name ->
                                        val dayNum = i + 1
                                        val selected = dayNum in newDaysOfWeek
                                        Box(
                                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                                                .clip(CircleShape)
                                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable { if (selected) newDaysOfWeek.remove(dayNum) else newDaysOfWeek.add(dayNum) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(name, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                                if (newDaysOfWeek.isEmpty()) Text("Select at least one day", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            if (newFreq == HabitFrequency.MONTHLY) {
                                Spacer(Modifier.height(8.dp))
                                Text("On days:", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                DayGrid(
                                    selectedDays = newDaysOfMonth.toSet(),
                                    onToggleDay = { day -> if (day in newDaysOfMonth) newDaysOfMonth.remove(day) else newDaysOfMonth.add(day) },
                                )
                            }
                            if (newFreq == HabitFrequency.YEARLY) {
                                Spacer(Modifier.height(8.dp))
                                Text("On dates:", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                FlowRow {
                                    monthNames.forEachIndexed { i, name ->
                                        val m = i + 1
                                        FilterChip(
                                            selected = activeYearlyMonth == m,
                                            onClick = { activeYearlyMonth = m },
                                            label = { Text(name) },
                                        )
                                    }
                                }
                                DayGrid(
                                    selectedDays = selectedYearlyDates.filter { it / 100 == activeYearlyMonth }.map { it % 100 }.toSet(),
                                    onToggleDay = { day ->
                                        val encoded = activeYearlyMonth * 100 + day
                                        if (encoded in selectedYearlyDates) selectedYearlyDates.remove(encoded) else selectedYearlyDates.add(encoded)
                                    },
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { showTimePicker = true }) {
                                Text(if (newHabitReminder.isNotBlank()) "⏰ $newHabitReminder" else "Set reminder")
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
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
                                        viewModel.onAction(HabitsViewModel.Action.CreateTag(newTagName.trim(), newTagColor))
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
                        FlowRow {
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
                TextButton(
                    onClick = {
                        if (newHabitTitle.isNotBlank()) {
                            val habit = editingHabit?.copy(
                                title = newHabitTitle,
                                description = newHabitDescription,
                                frequencyType = newFreq,
                                frequencyCount = 1,
                                daysOfWeek = newDaysOfWeek.toSet(),
                                daysOfMonth = newDaysOfMonth.toSet(),
                                yearlyDates = selectedYearlyDates.toSet(),
                                reminderTime = newHabitReminder.ifBlank { null },
                            ) ?: Habit(
                                title = newHabitTitle,
                                frequencyType = newFreq,
                                frequencyCount = 1,
                                daysOfWeek = newDaysOfWeek.toSet(),
                                daysOfMonth = newDaysOfMonth.toSet(),
                                yearlyDates = selectedYearlyDates.toSet(),
                                description = newHabitDescription,
                                reminderTime = newHabitReminder.ifBlank { null },
                            )
                            viewModel.onAction(HabitsViewModel.Action.UpsertHabit(habit, selectedTagIds.toList()))
                            showAddDialog = false
                        }
                    },
                    enabled = newHabitTitle.isNotBlank() && (newFreq != HabitFrequency.WEEKLY || newDaysOfWeek.isNotEmpty()) && (newFreq != HabitFrequency.MONTHLY || newDaysOfMonth.isNotEmpty()) && (newFreq != HabitFrequency.YEARLY || selectedYearlyDates.isNotEmpty()),
                ) { Text(if (editingHabit != null) "Save" else "Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
        )
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
                    newHabitReminder = "${timePickerState.hour.toString().padStart(2, '0')}:${timePickerState.minute.toString().padStart(2, '0')}"
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

    state.selectedHabitId?.let { habitId ->
        val habit = state.habits.find { it.id == habitId }
        val dismissHistory = { viewModel.onAction(HabitsViewModel.Action.ViewHabitHistory(habitId)) }
        AlertDialog(
            onDismissRequest = dismissHistory,
            title = { Text("History") },
            text = {
                Column {
                    if (habit != null) {
                        val today = LocalDate.now()
                        val monthStart = LocalDate(today.year, today.month, 1)
                        val monthLen = daysInMonth(today.year, today.month.ordinal + 1)
                        val startOffset = monthStart.dayOfWeek.ordinal
                        val recordsByDate = state.selectedHabitHistory.filter { it.date.year == today.year && it.date.month == today.month }.associateBy { it.date }
                        Column {
                            Row {
                                listOf("M","T","W","T","F","S","S").forEach { Text(it, modifier = Modifier.weight(1f).padding(2.dp)) }
                            }
                            var day = 1
                            while (day <= monthLen) {
            FlowRow {
                                    for (col in 0..6) {
                                        if ((day == 1 && col < startOffset) || day > monthLen) {
                                            Box(modifier = Modifier.weight(1f).padding(2.dp))
                                        } else {
                                            val date = LocalDate(today.year, today.month, day)
                                            val record = recordsByDate[date]
                                val bg = when (record?.state) {
                                    HabitState.DONE -> MaterialTheme.colorScheme.primary
                                    HabitState.SKIPPED -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val fc = if (habit.shouldShowToday(date)) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            Box(modifier = Modifier.weight(1f).padding(1.dp).background(bg, CircleShape).padding(4.dp), contentAlignment = Alignment.Center) {
                                                Text("$day", color = fc)
                                            }
                                            day++
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    Text("All records:", style = MaterialTheme.typography.titleSmall)
                    state.selectedHabitHistory.sortedByDescending { it.date }.forEach { record ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(record.date.toString(), modifier = Modifier.weight(1f))
                            Text(record.state.name.lowercase().replaceFirstChar { it.uppercase() })
                            if (record.count > 1) Text(" (${record.count}x)")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = dismissHistory) { Text("Close") } },
        )
    }

    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Delete habit?") },
            text = { Text(habit.title) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(HabitsViewModel.Action.DeleteHabit(habit.id))
                    habitToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { habitToDelete = null }) { Text("Cancel") } },
        )
    }

    FilterBottomSheet(
        show = state.filter.showFilterSheet,
        searchQuery = state.filter.searchQuery,
        onSearchQueryChange = { viewModel.onAction(HabitsViewModel.Action.SetSearchQuery(it)) },
        tags = state.tags,
        selectedTagIds = state.filter.selectedTagIds,
        onTagToggle = { viewModel.onAction(HabitsViewModel.Action.ToggleTag(it)) },
        filterDate = state.filter.filterDate,
        showDateFilter = true,
        onDateChange = { viewModel.onAction(HabitsViewModel.Action.SetFilterDate(it)) },
        onReset = {
            viewModel.onAction(HabitsViewModel.Action.SetSearchQuery(""))
            viewModel.onAction(HabitsViewModel.Action.ClearTagFilter)
            viewModel.onAction(HabitsViewModel.Action.SetFilterDate(null))
        },
        onDismiss = { viewModel.onAction(HabitsViewModel.Action.ToggleFilterSheet) },
    )
}

@Composable
fun HabitItem(
    habit: Habit,
    record: HabitRecord?,
    onSetState: (HabitState) -> Unit,
    showDoneSnackbar: ((undo: () -> Unit) -> Unit)? = null,
    onViewHistory: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    streak: Int = 0,
    tags: List<Tag> = emptyList(),
    dragHandleModifier: Modifier = Modifier,
    showDragHandle: Boolean = false,
) {
    val soundPlayer = koinInject<SoundPlayer>()
    val currentState = record?.state ?: HabitState.NOT_DONE
    val currentCount = record?.count ?: 0
    val isDone = currentState == HabitState.DONE
    val isSkipped = currentState == HabitState.SKIPPED

    val scale by animateFloatAsState(
        targetValue = if (isDone) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
    )
    val sparkleScale by animateFloatAsState(
        targetValue = if (isDone) 1.0f else 0.8f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
    )
    val sparkleRotation by animateFloatAsState(
        targetValue = if (isDone) 360f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        colors = CardDefaults.cardColors(
            containerColor = when (isDone || isSkipped) {
                isSkipped -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                isDone -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            },
        ),
    ) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (showDragHandle) {
                Text(
                    "≡",
                    modifier = dragHandleModifier.padding(end = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.title,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isDone) TextDecoration.LineThrough else null))
                if (tags.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        tags.forEach { tag ->
                            tag.color?.let { c ->
                                Spacer(Modifier.width(3.dp))
                                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(c)))
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
                LinearProgressIndicator(
                    progress = { currentCount.toFloat() / habit.frequencyCount.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text("$currentCount / ${habit.frequencyCount} ${habit.frequencyType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (streak > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.fire),
                        contentDescription = "Streak",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(" $streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {
                        if (!isDone) {
                            soundPlayer.play(1f + (streak.coerceAtMost(10) * 0.05f))
                            onSetState(HabitState.DONE)
                            showDoneSnackbar?.invoke { onSetState(HabitState.NOT_DONE) }
                        }
                    },
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.sparkle),
                        contentDescription = if (isDone) "Done" else "Mark done",
                        modifier = Modifier.size(20.dp).graphicsLayer {
                            scaleX = sparkleScale; scaleY = sparkleScale; rotationZ = sparkleRotation
                        },
                        tint = if (isDone) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
                if (!isSkipped) {
                    TextButton(onClick = { onSetState(HabitState.SKIPPED) }) { Text("Skip") }
                }
                if (onViewHistory != null || onEdit != null || onDelete != null) {
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Text("⋮", fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            onViewHistory?.let {
                                DropdownMenuItem(text = { Text("Log") }, onClick = { expanded = false; it() })
                            }
                            onEdit?.let {
                                DropdownMenuItem(text = { Text("Edit") }, onClick = { expanded = false; it() })
                            }
                            onDelete?.let {
                                DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; it() })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayGrid(
    selectedDays: Set<Int>,
    onToggleDay: (Int) -> Unit,
) {
    for (rowStart in listOf(1, 8, 15, 22, 29)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        ) {
            for (day in rowStart..(rowStart + 6).coerceAtMost(31)) {
                val selected = day in selectedDays
                Box(
                    modifier = Modifier.weight(1f).aspectRatio(1f)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onToggleDay(day) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(day.toString(), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (rowStart == 29) repeat(4) { Spacer(Modifier.weight(1f)) }
        }
    }
}
