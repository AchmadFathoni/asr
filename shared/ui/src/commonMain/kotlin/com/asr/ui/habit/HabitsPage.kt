package com.asr.ui.habit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.asr.core.interfaces.SoundPlayer
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
import com.asr.ui.LIGHT_CHECK_COLORS
import com.asr.ui.TAG_COLORS
import com.asr.ui.viewmodel.HabitsViewModel
import com.asr.ui.app.EmptyState
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
    var newCount by remember { mutableStateOf("1") }
    val newDaysOfWeek = remember { mutableStateListOf<Int>() }
    var newDayOfMonth by remember { mutableStateOf<Int?>(null) }
    var newMonthOfYear by remember { mutableStateOf<Int?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showFreqPicker by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    var selectedTagIds by remember { mutableStateOf(setOf<Long>()) }
    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                newHabitTitle = ""; newHabitDescription = ""; newHabitReminder = ""
                newFreq = HabitFrequency.DAILY; newCount = "1"
                newDaysOfWeek.clear(); newDayOfMonth = null; newMonthOfYear = null
                selectedTagIds = emptySet(); newTagName = ""; newTagColor = null
                editingHabit = null
                showAddDialog = true
            }) {
                Icon(imageVector = vectorResource(Res.drawable.add), contentDescription = "Add habit")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // Main habits list
            Text("Habits", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search habits") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                val filteredHabits = if (searchQuery.isBlank()) state.habits
                    else state.habits.filter { it.title.contains(searchQuery, ignoreCase = true) }

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(48.dp),
                    )
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredHabits) { habit ->
                        val record = state.todayRecords[habit.id]
                        HabitItem(
                            habit = habit,
                            record = record,
                            onSetState = { s ->
                                viewModel.onAction(HabitsViewModel.Action.SetRecordState(habit.id, s))
                            },
                            onViewHistory = { viewModel.onAction(HabitsViewModel.Action.ViewHabitHistory(habit.id)) },
                            onEdit = {
                                editingHabit = habit
                                newHabitTitle = habit.title
                                newHabitDescription = habit.description
                                newHabitReminder = habit.reminderTime ?: ""
                                newFreq = habit.frequencyType
                                newCount = habit.frequencyCount.toString()
                                newDaysOfWeek.clear(); newDaysOfWeek.addAll(habit.daysOfWeek)
                                newDayOfMonth = habit.dayOfMonth
                                newMonthOfYear = habit.monthOfYear
                                selectedTagIds = emptySet(); newTagName = ""; newTagColor = null
                                showAddDialog = true
                            },
                            onMoveUp = if (filteredHabits.firstOrNull() != habit) {{ viewModel.onAction(HabitsViewModel.Action.MoveHabit(habit.id, -1)) }} else null,
                            onMoveDown = if (filteredHabits.lastOrNull() != habit) {{ viewModel.onAction(HabitsViewModel.Action.MoveHabit(habit.id, 1)) }} else null,
                            streak = state.streaks[habit.id] ?: 0,
                            onDelete = { habitToDelete = habit },
                        )
                        HorizontalDivider()
                    }
                    if (filteredHabits.isEmpty() && !state.isLoading) {
                        item {
                            EmptyState(
                                icon = Res.drawable.check_circle,
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
                Column {
                    OutlinedTextField(value = newHabitTitle, onValueChange = { newHabitTitle = it },
                        label = { Text("Title") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showFreqPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Period: ${newFreq.name.lowercase().replaceFirstChar { it.uppercase() }}")
                    }
                    if (newFreq == HabitFrequency.WEEKLY) {
                        Spacer(Modifier.height(4.dp))
                        FlowRow {
                            dayNames.forEachIndexed { i, name ->
                                val dayNum = i + 1
                                FilterChip(
                                    selected = dayNum in newDaysOfWeek,
                                    onClick = { if (dayNum in newDaysOfWeek) newDaysOfWeek.remove(dayNum) else newDaysOfWeek.add(dayNum) },
                                    label = { Text(name) },
                                )
                            }
                        }
                    }
                    if (newFreq == HabitFrequency.MONTHLY) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = newDayOfMonth?.toString() ?: "",
                            onValueChange = {
                                val n = it.toIntOrNull()
                                if (it.isEmpty() || (n != null && n in 1..31)) newDayOfMonth = n
                            },
                            label = { Text("Day of month (1-31)") },
                            singleLine = true,
                        )
                    }
                    if (newFreq == HabitFrequency.YEARLY) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { showMonthPicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(monthNames.getOrElse((newMonthOfYear ?: 1) - 1) { "Select month" })
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = newDayOfMonth?.toString() ?: "",
                            onValueChange = {
                                val n = it.toIntOrNull()
                                if (it.isEmpty() || (n != null && n in 1..31)) newDayOfMonth = n
                            },
                            label = { Text("Day of month (1-31)") },
                            singleLine = true,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newCount,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) newCount = it },
                        label = { Text("Times per period") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newHabitDescription,
                        onValueChange = { newHabitDescription = it },
                        label = { Text("Description (optional)") },
                        maxLines = 3,
                    )
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
                                        viewModel.onAction(HabitsViewModel.Action.CreateTag(newTagName.trim(), newTagColor))
                                        newTagName = ""; newTagColor = null
                                    }
                                },
                                enabled = newTagName.isNotBlank(),
                            ) { Text("Add") }
                        }
                        if (newTagName.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            FlowRow {
                                TAG_COLORS.forEach { (c, _) ->
                                    val selected = newTagColor == c
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .size(16.dp)
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
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showTimePicker = true }) {
                        Text(if (newHabitReminder.isNotBlank()) "⏰ $newHabitReminder" else "Set reminder")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val count = newCount.toIntOrNull() ?: 1
                    if (newHabitTitle.isNotBlank() && count > 0) {
                        val habit = editingHabit?.copy(
                            title = newHabitTitle,
                            description = newHabitDescription,
                            frequencyType = newFreq,
                            frequencyCount = count,
                            daysOfWeek = newDaysOfWeek.toSet(),
                            dayOfMonth = newDayOfMonth,
                            monthOfYear = newMonthOfYear,
                            reminderTime = newHabitReminder.ifBlank { null },
                        ) ?: Habit(
                            title = newHabitTitle,
                            frequencyType = newFreq,
                            frequencyCount = count,
                            daysOfWeek = newDaysOfWeek.toSet(),
                            dayOfMonth = newDayOfMonth,
                            monthOfYear = newMonthOfYear,
                            description = newHabitDescription,
                            reminderTime = newHabitReminder.ifBlank { null },
                        )
                        viewModel.onAction(HabitsViewModel.Action.UpsertHabit(habit, selectedTagIds.toList()))
                        showAddDialog = false
                    }
                }) { Text(if (editingHabit != null) "Save" else "Add") }
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

    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = { Text("Select month") },
            text = {
                Column {
                    monthNames.forEachIndexed { i, name ->
                        TextButton(
                            onClick = {
                                newMonthOfYear = i + 1
                                showMonthPicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(name) }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showMonthPicker = false }) { Text("Cancel") } },
            confirmButton = {},
        )
    }

    if (showFreqPicker) {
        AlertDialog(
            onDismissRequest = { showFreqPicker = false },
            title = { Text("Select period") },
            text = {
                Column {
                    HabitFrequency.entries.forEach { f ->
                        TextButton(
                            onClick = {
                                newFreq = f
                                showFreqPicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showFreqPicker = false }) { Text("Cancel") } },
            confirmButton = {},
        )
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
                                Row {
                                    for (col in 0..6) {
                                        if ((day == 1 && col < startOffset) || day > monthLen) {
                                            Box(modifier = Modifier.weight(1f).padding(2.dp))
                                        } else {
                                            val date = LocalDate(today.year, today.month, day)
                                            val record = recordsByDate[date]
                                            val bg = when (record?.state) {
                                                HabitState.DONE -> Color(0xFF4CAF50)
                                                HabitState.SKIPPED -> Color(0xFFFF9800)
                                                else -> Color.Gray.copy(alpha = 0.15f)
                                            }
                                            val fc = if (habit.shouldShowToday(date)) Color.Black else Color.Gray
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
}

@Composable
fun HabitItem(
    habit: Habit,
    record: HabitRecord?,
    onSetState: (HabitState) -> Unit,
    onViewHistory: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    streak: Int = 0,
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

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = when {
                    isSkipped -> 0.2f
                    isDone -> 0.5f
                    else -> 0.3f
                }
            ),
        ),
    ) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isDone) TextDecoration.LineThrough else null))
                LinearProgressIndicator(
                    progress = { currentCount.toFloat() / habit.frequencyCount.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text("$currentCount / ${habit.frequencyCount} ${habit.frequencyType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (streak > 0) Text("🔥 $streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Row {
                OutlinedButton(
                    onClick = {
                        if (!isDone) soundPlayer.play()
                        onSetState(HabitState.DONE)
                    },
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                ) { Text(if (isDone) "✓" else "+") }
                if (!isSkipped) {
                    TextButton(onClick = { onSetState(HabitState.SKIPPED) }) { Text("Skip") }
                }
                onMoveUp?.let { TextButton(onClick = it) { Text("↑") } }
                onMoveDown?.let { TextButton(onClick = it) { Text("↓") } }
                onViewHistory?.let { TextButton(onClick = it) { Text("Log") } }
                onEdit?.let { TextButton(onClick = it) { Text("Edit") } }
                onDelete?.let { TextButton(onClick = it) { Text("Del") } }
            }
        }
    }
}
