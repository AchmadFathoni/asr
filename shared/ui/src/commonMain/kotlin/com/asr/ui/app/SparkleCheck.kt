package com.asr.ui.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import asr.shared.ui.generated.resources.Res
import asr.shared.ui.generated.resources.sparkle
import org.jetbrains.compose.resources.vectorResource

@Composable
fun SparkleCheck(
    isDone: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isDone) 1.0f else 0.75f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
    )
    Box(
        modifier = modifier
            .size(30.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(
                color = if (isDone) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
            )
            .border(
                width = 2.dp,
                color = if (isDone) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                shape = CircleShape,
            )
            .clickable(role = Role.Checkbox) { if (!isDone) onToggle() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.sparkle),
            contentDescription = if (isDone) "Done" else "Mark done",
            modifier = Modifier.size(16.dp),
            tint = if (isDone) MaterialTheme.colorScheme.onPrimary
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        )
    }
}
