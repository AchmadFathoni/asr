package com.asr.ui.habit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asr.core.habit.Habit
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitState
import com.asr.core.interfaces.SoundPlayer
import com.asr.ui.app.SparkleCheck
import asr.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject

@Composable
fun HabitItem(
    habit: Habit,
    record: HabitRecord?,
    onSetState: (HabitState) -> Unit,
    onViewHistory: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    streak: Int = 0,
    onTogglePin: (() -> Unit)? = null,
    periodCount: Int = 0,
) {
    val soundPlayer = koinInject<SoundPlayer>()
    val currentState = record?.state ?: HabitState.NOT_DONE
    val currentCount = periodCount
    val isDone = currentState == HabitState.DONE
    val isSkipped = currentState == HabitState.SKIPPED

    val scale by animateFloatAsState(
        targetValue = if (isDone) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f),
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clipToBounds(),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.title,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isDone) TextDecoration.LineThrough else null))
                if (habit.frequencyCount > 1) {
                    LinearProgressIndicator(
                        progress = { currentCount.toFloat() / habit.frequencyCount.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text("$currentCount / ${habit.frequencyCount} ${habit.frequencyType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (streak > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.fire),
                        contentDescription = "Streak",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Unspecified,
                    )
                    Text(" $streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SparkleCheck(
                    isDone = isDone,
                    onToggle = {
                        soundPlayer.play(1f + (streak.coerceAtMost(10) * 0.05f))
                        onSetState(HabitState.DONE)
                    },
                    doneContainerColor = MaterialTheme.colorScheme.tertiary,
                    doneContentColor = MaterialTheme.colorScheme.onTertiary,
                )
                if (!isSkipped) {
                    TextButton(onClick = { onSetState(HabitState.SKIPPED) }) { Text("Skip") }
                }
                if (onTogglePin != null || onViewHistory != null || onEdit != null || onDelete != null || onDuplicate != null) {
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }, modifier = Modifier.semantics { contentDescription = "More options" }) {
                            Text("⋮", fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            onTogglePin?.let {
                                DropdownMenuItem(
                                    text = { Text(if (habit.isPinned) "Unpin" else "Pin") },
                                    onClick = { expanded = false; it() },
                                )
                            }
                            onViewHistory?.let {
                                DropdownMenuItem(text = { Text("Log") }, onClick = { expanded = false; it() })
                            }
                            onEdit?.let {
                                DropdownMenuItem(text = { Text("Edit") }, onClick = { expanded = false; it() })
                            }
                            onDuplicate?.let {
                                DropdownMenuItem(text = { Text("Duplicate") }, onClick = { expanded = false; it() })
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
