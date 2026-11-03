package com.asr.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asr.core.habit.Habit
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitState
import com.asr.core.interfaces.SoundPlayer
import com.asr.core.task.Task
import com.asr.ui.app.TaskItemCard
import com.asr.ui.habit.HabitItem
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YesterdaySheet(
    show: Boolean,
    tasks: List<Task>,
    habits: List<Habit>,
    records: Map<Long, HabitRecord>,
    periodCounts: Map<Long, Int>,
    periodTargets: Map<Long, Int>,
    onToggleTask: (Long) -> Unit,
    onSetHabitState: (Long, HabitState) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    val soundPlayer = koinInject<SoundPlayer>()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Yesterday", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            if (tasks.isEmpty() && habits.isEmpty()) {
                Text(
                    "Nothing was scheduled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (tasks.isNotEmpty()) {
                Text("Tasks", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                tasks.forEach { task ->
                    TaskItemCard(
                        task = task,
                        depth = 0,
                        isParent = false,
                        soundPlayer = soundPlayer,
                        onToggle = { onToggleTask(task.id) },
                    )
                }
            }

            if (habits.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Habits", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                habits.forEach { habit ->
                    HabitItem(
                        habit = habit,
                        record = records[habit.id],
                        onSetState = { onSetHabitState(habit.id, it) },
                        periodCount = periodCounts[habit.id] ?: 0,
                        periodTarget = periodTargets[habit.id] ?: 0,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
