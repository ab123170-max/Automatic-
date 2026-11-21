package com.autoswipe.ai.studio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoswipe.ai.studio.data.UsageStatsSummary
import com.autoswipe.ai.studio.ui.components.NavyCard
import com.autoswipe.ai.studio.ui.components.formatDurationMs
import com.autoswipe.ai.studio.ui.theme.AutoSwipeColors

@Composable
fun UsageStatisticsScreen(
    stats: UsageStatsSummary,
    onResetStats: () -> Unit
) {
    var confirmReset by remember { mutableStateOf(false) }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset Local Statistics?") },
            text = { Text("This permanently clears all recorded swipe counts, sessions, and active time stored on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    onResetStats()
                    confirmReset = false
                }) {
                    Text("Reset", color = AutoSwipeColors.CoralRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatBox("Total Swipes", stats.totalSwipes.toString(), Modifier.weight(1f))
            StatBox("Total Sessions", stats.totalSessions.toString(), Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatBox("Total Active Time", formatDurationMs(stats.totalActiveTimeMs), Modifier.weight(1f))
            StatBox(
                "Avg Video Interval",
                if (stats.averageVideoIntervalMs > 0L) "${stats.averageVideoIntervalMs / 1000.0}s" else "—",
                Modifier.weight(1f)
            )
        }

        NavyCard {
            Text("Reliability & Failure Log", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            StatLine("Detection Timeouts", stats.totalDetectionFailures.toString())
            Spacer(modifier = Modifier.height(4.dp))
            StatLine("Gesture Dispatch Failures", stats.totalGestureFailures.toString())
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "All metrics are tracked locally via Room database — no analytics servers or accounts.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        NavyCard {
            Text("Swipes by Application", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            if (stats.perAppStats.isEmpty()) {
                Text(
                    "No automatic swipes recorded yet. Start AutoSwipe AI and open a supported short-video app.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val maxSwipes = stats.perAppStats.maxOf { it.swipeCount }.coerceAtLeast(1)
                stats.perAppStats.forEach { app ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(app.displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${app.swipeCount} swipes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AutoSwipeColors.ElectricCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { app.swipeCount / maxSwipes.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = AutoSwipeColors.ElectricBlue,
                            trackColor = AutoSwipeColors.NavyElevated
                        )
                        if (app.detectionFailureCount > 0 || app.gestureFailureCount > 0) {
                            Text(
                                "Timeouts: ${app.detectionFailureCount} • Gesture failures: ${app.gestureFailureCount}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { confirmReset = true },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AutoSwipeColors.CoralRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Local Usage Statistics")
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    NavyCard(modifier = modifier, containerColor = AutoSwipeColors.NavyElevated) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AutoSwipeColors.ElectricCyan
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
