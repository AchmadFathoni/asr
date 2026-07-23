package com.asr.ui.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.asr.core.tag.Tag
import com.asr.core.task.Task
import com.asr.ui.tagColorForValue

@Composable
fun TaskItemCard(
    task: Task,
    depth: Int,
    isParent: Boolean,
    showChevron: Boolean = false,
    isExpanded: Boolean = false,
    progress: Pair<Int, Int>? = null,
    tags: List<Tag> = emptyList(),
    soundPlayer: SoundPlayer,
    onToggle: () -> Unit,
    onToggleExpand: (() -> Unit)? = null,
    onTogglePin: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onAddSub: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
) {
    val scale by animateFloatAsState(
        targetValue = if (task.isDone) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
    )

    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(start = (depth * 40).dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clipToBounds()
            .clickable {
                if (!isParent) {
                    if (!task.isDone) soundPlayer.play()
                    onToggle()
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (task.isDone) 0.5f else 0.3f,
            ),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isParent) {
                Box(
                    modifier = Modifier.size(48.dp)
                        .then(
                            if (showChevron) Modifier.clickable { onToggleExpand?.invoke() }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showChevron) {
                        Text(if (isExpanded) "\u25BE" else "\u25B8")
                    }
                }
            } else {
                SparkleCheck(isDone = task.isDone, onToggle = {
                    if (!task.isDone) soundPlayer.play()
                    onToggle()
                })
            }
            Text(
                text = task.title,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                ),
            )
            tags.forEach { tag ->
                tag.color?.let {
                    Spacer(Modifier.width(2.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(tagColorForValue(it)))
                }
            }
            if (tags.isNotEmpty() && progress != null && progress.second > 0) Spacer(Modifier.width(8.dp))
            if (progress != null && progress.second > 0) {
                Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress.first.toFloat() / progress.second },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 3.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        "${progress.first}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Box {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }, modifier = Modifier.semantics { contentDescription = "More options" }) {
                    Text("\u22EE", fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    onTogglePin?.let {
                        DropdownMenuItem(
                            text = { Text(if (task.isPinned) "Unpin" else "Pin") },
                            onClick = { expanded = false; it() },
                        )
                    }
                    onAddSub?.let {
                        DropdownMenuItem(
                            text = { Text("Add sub-task") },
                            onClick = { expanded = false; it() },
                        )
                    }
                    onEdit?.let {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { expanded = false; it() },
                        )
                    }
                    onDelete?.let {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { expanded = false; it() },
                        )
                    }
                }
            }
        }
    }
}

fun countProgress(
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
