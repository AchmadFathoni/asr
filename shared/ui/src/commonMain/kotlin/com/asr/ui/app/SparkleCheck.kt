package com.asr.ui.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
        targetValue = if (isDone) 1.0f else 0.8f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
    )
    val rotation by animateFloatAsState(
        targetValue = if (isDone) 360f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
    )
    Icon(
        imageVector = vectorResource(Res.drawable.sparkle),
        contentDescription = if (isDone) "Done" else "Mark done",
        modifier = modifier
            .size(32.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation }
            .clickable(role = Role.Checkbox) { if (!isDone) onToggle() },
        tint = if (isDone) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
    )
}
