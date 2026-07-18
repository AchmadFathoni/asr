package com.asr.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.asr.core.tag.Tag
import com.asr.ui.TagColorPicker
import com.asr.ui.tagColorForValue
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import asr.shared.ui.generated.resources.Res
import asr.shared.ui.generated.resources.add
import asr.shared.ui.generated.resources.filter

@Composable
fun <T : Enum<T>> StatusFilterChips(
    entries: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
            )
        }
    }
}

@Composable
fun TopActionRow(
    onToggleFilter: () -> Unit,
    filterActive: Boolean,
    onAdd: () -> Unit,
    addContentDescription: String,
    isEmpty: Boolean,
    pulseScale: Float,
    modifier: Modifier = Modifier,
    filterChips: @Composable RowScope.() -> Unit = {},
    centerActions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        filterChips()
        centerActions()
        Spacer(Modifier.weight(1f))
        Box {
            IconButton(onClick = onToggleFilter) {
                Icon(imageVector = vectorResource(Res.drawable.filter), contentDescription = "Filter")
            }
            if (filterActive) Box(
                Modifier.align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 1.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        IconButton(
            onClick = onAdd,
            modifier = if (isEmpty) Modifier.graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                       else Modifier,
        ) {
            Icon(imageVector = vectorResource(Res.drawable.add), contentDescription = addContentDescription)
        }
    }
}

@Composable
fun EmptyState(icon: DrawableResource, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = vectorResource(icon),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun <T> PinnedItemDivider(
    items: List<T>,
    item: T,
    lastPinnedIdx: Int,
    hasUnpinnedAfter: Boolean,
    showThinDividers: Boolean = true,
) {
    val idx = items.indexOf(item)
    if (lastPinnedIdx >= 0 && idx == lastPinnedIdx && hasUnpinnedAfter) {
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    } else if (showThinDividers && idx < items.size - 1) {
        HorizontalDivider()
    }
}

@Composable
fun UndoDeleteSnackbarEffect(
    pendingDeletedTasks: List<*>?,
    snackbarHostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(pendingDeletedTasks) {
        val tasks = pendingDeletedTasks ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "${tasks.size} tasks cleared",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) onUndo() else onDismiss()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateTagRow(
    tagName: String,
    onTagNameChange: (String) -> Unit,
    tagColor: Long?,
    onTagColorChange: (Long?) -> Unit,
    onCreate: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = tagName,
                onValueChange = onTagNameChange,
                label = { Text("New tag name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onCreate,
                enabled = tagName.isNotBlank(),
            ) { Text("Add") }
        }
        if (tagName.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            TagColorPicker(
                selectedColor = tagColor,
                onColorSelected = onTagColorChange,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFilterRow(
    tags: List<Tag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit,
    useFlowRow: Boolean = false,
) {
    if (tags.isEmpty()) return
    Spacer(Modifier.height(4.dp))
    if (useFlowRow) {
        FlowRow {
            tags.forEach { tag -> TagChip(tag, tag.id in selectedTagIds, onTagToggle) }
        }
    } else {
        Row {
            tags.forEach { tag -> TagChip(tag, tag.id in selectedTagIds, onTagToggle) }
        }
    }
}

@Composable
private fun TagChip(tag: Tag, selected: Boolean, onToggle: (Long) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(tag.id) },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                tag.color?.let {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(tagColorForValue(it)))
                    Spacer(Modifier.width(4.dp))
                }
                Text(tag.name)
            }
        },
    )
}
