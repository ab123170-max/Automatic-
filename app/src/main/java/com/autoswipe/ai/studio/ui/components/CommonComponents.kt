package com.autoswipe.ai.studio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autoswipe.ai.studio.model.EnginePhase
import com.autoswipe.ai.studio.model.EngineRuntimeState
import com.autoswipe.ai.studio.ui.theme.AutoSwipeColors

@Composable
fun AutoSwipeLogo(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.075f

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF102447), Color(0xFF0A1428))
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.26f, w * 0.26f)
        )

        drawRoundRect(
            color = AutoSwipeColors.ElectricBlue,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(w - stroke, h - stroke),
            cornerRadius = CornerRadius(w * 0.24f, w * 0.24f),
            style = Stroke(width = stroke * 0.65f)
        )

        val topChevron = Path().apply {
            moveTo(w * 0.30f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.28f)
            lineTo(w * 0.70f, h * 0.50f)
        }
        drawPath(
            path = topChevron,
            color = AutoSwipeColors.ElectricCyan,
            style = Stroke(width = stroke * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val lowerChevron = Path().apply {
            moveTo(w * 0.35f, h * 0.70f)
            lineTo(w * 0.50f, h * 0.53f)
            lineTo(w * 0.65f, h * 0.70f)
        }
        drawPath(
            path = lowerChevron,
            color = AutoSwipeColors.ElectricBlue,
            style = Stroke(width = stroke * 0.95f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun NavyCard(
    modifier: Modifier = Modifier,
    borderColor: Color = AutoSwipeColors.NavyOutline,
    containerColor: Color = AutoSwipeColors.NavySurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatusBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PersistentEmergencyBar(
    runtime: EngineRuntimeState,
    autoScrollEnabled: Boolean,
    onPauseResume: () -> Unit,
    onEmergencyStop: () -> Unit,
    onDismissError: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!runtime.lastError.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                color = AutoSwipeColors.RedContainer,
                border = BorderStroke(1.dp, AutoSwipeColors.CoralRed.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = runtime.lastError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissError, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss error",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (autoScrollEnabled) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = AutoSwipeColors.NavyElevated,
                border = BorderStroke(1.dp, AutoSwipeColors.ElectricBlue.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (runtime.isPaused) "Engine Paused" else "AutoSwipe Active • ${runtime.phase.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (runtime.isPaused) AutoSwipeColors.Amber else AutoSwipeColors.ElectricCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = runtime.activeApp?.displayName ?: "Waiting for supported app",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onPauseResume,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AutoSwipeColors.ElectricCyan.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (runtime.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (runtime.isPaused) "Resume" else "Pause")
                    }
                    Button(
                        onClick = onEmergencyStop,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AutoSwipeColors.CoralRed,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
            }
        }
    }
}

fun phaseColor(phase: EnginePhase): Color {
    return when (phase) {
        EnginePhase.MONITORING, EnginePhase.VIDEO_END_DETECTED, EnginePhase.SWIPING -> AutoSwipeColors.Emerald
        EnginePhase.WAITING_DELAY, EnginePhase.COOLDOWN, EnginePhase.WAITING_SUPPORTED_APP -> AutoSwipeColors.ElectricCyan
        EnginePhase.PAUSED, EnginePhase.UNSUPPORTED_APP -> AutoSwipeColors.Amber
        EnginePhase.ERROR, EnginePhase.WAITING_PERMISSION -> AutoSwipeColors.CoralRed
        EnginePhase.OFF -> AutoSwipeColors.TextSecondary
    }
}

fun formatDurationMs(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> String.format("%dh %02dm %02ds", hours, minutes, seconds)
        minutes > 0L -> String.format("%02dm %02ds", minutes, seconds)
        else -> String.format("%02ds", seconds)
    }
}
