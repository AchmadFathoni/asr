package com.asr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class TagColorRole(val label: String) {
    PRIMARY("Primary"),
    SECONDARY("Secondary"),
    TERTIARY("Tertiary"),
    PRIMARY_CONTAINER("Primary Ctnr"),
    SECONDARY_CONTAINER("Secondary Ctnr"),
    TERTIARY_CONTAINER("Tertiary Ctnr"),
    SURFACE_VARIANT("Surface"),
    OUTLINE("Outline"),
    INVERSE_SURFACE("Inverse Surf"),
    INVERSE_PRIMARY("Inverse Prim"),
}

@Composable
fun resolveTagColor(role: TagColorRole): Color = when (role) {
    TagColorRole.PRIMARY -> MaterialTheme.colorScheme.primary
    TagColorRole.SECONDARY -> MaterialTheme.colorScheme.secondary
    TagColorRole.TERTIARY -> MaterialTheme.colorScheme.tertiary
    TagColorRole.PRIMARY_CONTAINER -> MaterialTheme.colorScheme.primaryContainer
    TagColorRole.SECONDARY_CONTAINER -> MaterialTheme.colorScheme.secondaryContainer
    TagColorRole.TERTIARY_CONTAINER -> MaterialTheme.colorScheme.tertiaryContainer
    TagColorRole.SURFACE_VARIANT -> MaterialTheme.colorScheme.surfaceVariant
    TagColorRole.OUTLINE -> MaterialTheme.colorScheme.outline
    TagColorRole.INVERSE_SURFACE -> MaterialTheme.colorScheme.inverseSurface
    TagColorRole.INVERSE_PRIMARY -> MaterialTheme.colorScheme.inversePrimary
}

@Composable
fun tagColorForValue(value: Long?): Color {
    if (value == null) return MaterialTheme.colorScheme.surfaceVariant
    return if (value in 0..<TagColorRole.entries.size.toLong()) {
        resolveTagColor(TagColorRole.entries[value.toInt()])
    } else {
        Color(value)
    }
}

fun tagColorLabel(value: Long?): String? {
    if (value == null) return null
    if (value in 0..<TagColorRole.entries.size.toLong()) {
        return TagColorRole.entries[value.toInt()].label
    }
    return null
}

private fun checkmarkColor(bg: Color): Color {
    val l = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return if (l > 0.5f) Color.Black else Color.White
}

@Composable
fun TagColorPicker(
    selectedColor: Long?,
    onColorSelected: (Long?) -> Unit,
) {
    Column {
        TagColorRole.entries.chunked(5).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                row.forEach { role ->
                    val ordinal = role.ordinal.toLong()
                    val selected = selectedColor == ordinal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                        modifier = Modifier
                            .fillMaxSize(0.92f)
                            .clip(CircleShape)
                            .background(resolveTagColor(role))
                            .clickable {
                                onColorSelected(if (selected) null else ordinal)
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Text(
                                    "✓",
                                    color = checkmarkColor(resolveTagColor(role)),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(0.dp))
        }
    }
}
