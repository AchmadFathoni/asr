package com.asr.ui.setting

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asr.core.backup.ExportState
import com.asr.core.backup.RestoreState
import com.asr.core.tag.Tag
import com.asr.ui.LIGHT_CHECK_COLORS
import com.asr.ui.TAG_COLORS
import com.asr.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsPage(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsState()

    val tagToDelete = remember { mutableStateOf<Tag?>(null) }
    val editingColorTagId = remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        SectionHeader("Appearance")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Dark mode", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(
                    checked = state.isDarkMode == true,
                    onCheckedChange = { viewModel.onAction(SettingsViewModel.Action.SetDarkMode(it)) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        SectionHeader("Data")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                OutlinedButton(
                    onClick = { viewModel.onAction(SettingsViewModel.Action.Export) },
                    enabled = state.exportState != ExportState.EXPORTING,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when (state.exportState) {
                            ExportState.IDLE -> "Export"
                            ExportState.EXPORTING -> "Exporting..."
                            ExportState.EXPORTED -> "Exported"
                        }
                    )
                }
                Spacer(Modifier.padding(8.dp))
                OutlinedButton(
                    onClick = { viewModel.onAction(SettingsViewModel.Action.Restore) },
                    enabled = state.restoreState != RestoreState.RESTORING,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when (state.restoreState) {
                            RestoreState.IDLE -> "Import"
                            RestoreState.RESTORING -> "Restoring..."
                            RestoreState.RESTORED -> "Restored"
                            RestoreState.FAILURE -> "Import"
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SectionHeader("Tags")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.newTagName,
                        onValueChange = { viewModel.onAction(SettingsViewModel.Action.SetNewTagName(it)) },
                        label = { Text("New tag name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { viewModel.onAction(SettingsViewModel.Action.CreateTag) },
                        enabled = state.newTagName.isNotBlank(),
                    ) { Text("Create") }
                }
                Spacer(Modifier.height(4.dp))
                Column {
                    TAG_COLORS.chunked(8).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { (color, _) ->
                                val selected = state.newTagColor == color
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .clickable {
                                            viewModel.onAction(SettingsViewModel.Action.SetNewTagColor(if (selected) null else color))
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) {
                                        Text(
                                            "✓",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (color == 0xFFFFFAC8L || color == 0xFFF0E442L || color == 0xFFBFEF45L || color == 0xFF98FB98L || color == 0xFFDCBEFFL) Color.Black else Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (state.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    state.tags.forEach { tag ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val color = tag.color
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (color != null) Color(color) else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { editingColorTagId.value = if (editingColorTagId.value == tag.id) null else tag.id },
                                )
                                Text(
                                    text = tag.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                TextButton(
                                    onClick = { tagToDelete.value = tag },
                                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                            }
                            if (editingColorTagId.value == tag.id) {
                                Spacer(Modifier.height(4.dp))
                                Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp)) {
                                    TAG_COLORS.chunked(8).forEach { row ->
                                        Row(Modifier.fillMaxWidth()) {
                                            row.forEach { (c, _) ->
                                                val selected = tag.color == c
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .padding(2.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(c))
                                                        .clickable {
                                                            viewModel.onAction(SettingsViewModel.Action.SetTagColor(tag.id, if (selected) null else c))
                                                            editingColorTagId.value = null
                                                        },
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
                    }
                }
            }
        }
    }

    tagToDelete.value?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete.value = null },
            title = { Text("Delete tag?") },
            text = { Text(tag.name) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(SettingsViewModel.Action.DeleteTag(tag.id))
                    tagToDelete.value = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { tagToDelete.value = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
}
