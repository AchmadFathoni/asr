package com.asr.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    show: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    tags: List<Tag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit,
    filterDate: LocalDate?,
    showDateFilter: Boolean,
    onDateChange: (LocalDate?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search title or description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Tags", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow {
                    tags.forEach { tag ->
                        val selected = tag.id in selectedTagIds
                        FilterChip(
                            selected = selected,
                            onClick = { onTagToggle(tag.id) },
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

            if (showDateFilter) {
                Spacer(Modifier.height(16.dp))
                Text("Date", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showDatePicker = true }) {
                    Text(filterDate?.let { "${it.year}-${(it.month.ordinal + 1).toString().padStart(2, '0')}-${it.day.toString().padStart(2, '0')}" } ?: "Select date")
                }
                if (filterDate != null) {
                    TextButton(onClick = { onDateChange(null); showDatePicker = false }) {
                        Text("Clear date")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Reset filters")
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = filterDate?.let { d ->
                d.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateChange(Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date)
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
}
