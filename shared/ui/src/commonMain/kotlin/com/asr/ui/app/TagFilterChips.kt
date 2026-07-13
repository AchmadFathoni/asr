package com.asr.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.asr.core.tag.Tag

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFilterChips(tags: List<Tag>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    val selectedIds by TagFilterState.selectedTagIds.collectAsState()
    val hasFilter = selectedIds.isNotEmpty()

    FlowRow(modifier = modifier) {
        tags.forEach { tag ->
            val selected = tag.id in selectedIds
            FilterChip(
                selected = selected,
                onClick = { TagFilterState.toggle(tag.id) },
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
        if (hasFilter) {
            TextButton(onClick = { TagFilterState.clear() }) {
                Text("Clear", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
