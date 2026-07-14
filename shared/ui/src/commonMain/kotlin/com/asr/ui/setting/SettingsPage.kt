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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asr.core.backup.ExportState
import com.asr.core.backup.RestoreState
import com.asr.core.tag.Tag
import com.asr.ui.LIGHT_CHECK_COLORS
import com.asr.ui.TAG_COLORS
import com.asr.core.settings.ThemeOption
import com.asr.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsPage(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsState()

    val tagToDelete = remember { mutableStateOf<Tag?>(null) }
    val editingColorTagId = remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
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
                Text("Theme", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                ThemeDropdown(
                    selected = state.theme,
                    onSelected = { viewModel.onAction(SettingsViewModel.Action.SetTheme(it)) },
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

        Spacer(Modifier.height(24.dp))

        SectionHeader("About")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "ASR — Todo & Habit App",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "\"By the time, indeed mankind is in loss, except for those who believe and do righteous deeds and advise each other to truth and advise each other to patience.\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "— Surah Al-Asr (103)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Made by Achmad Fathoni with DeepSeek",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
private fun ThemeDropdown(selected: ThemeOption, onSelected: (ThemeOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            text = when (selected) {
                ThemeOption.SYSTEM -> "System"
                ThemeOption.LIGHT -> "Light"
                ThemeOption.DARK -> "Dark"
            },
            modifier = Modifier.clickable { expanded = true },
            style = MaterialTheme.typography.bodyLarge,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThemeOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (option) {
                                ThemeOption.SYSTEM -> "System"
                                ThemeOption.LIGHT -> "Light"
                                ThemeOption.DARK -> "Dark"
                            }
                        )
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
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
