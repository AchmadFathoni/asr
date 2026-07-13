package com.asr.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.asr.core.tag.Tag

@Composable
fun TagFilterDropdown(tags: List<Tag>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    val selectedIds by TagFilterState.selectedTagIds.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val hasFilter = selectedIds.isNotEmpty()

    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(if (hasFilter) "Filter (${selectedIds.size})" else "Filter")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tags.forEach { tag ->
                val selected = tag.id in selectedIds
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val color = tag.color
                            if (color != null) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(color)))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(tag.name, modifier = Modifier.weight(1f))
                            if (selected) Text("\u2713")
                        }
                    },
                    onClick = { TagFilterState.toggle(tag.id) },
                )
            }
            if (hasFilter) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Clear filter") },
                    onClick = { TagFilterState.clear(); expanded = false },
                )
            }
        }
    }
}
