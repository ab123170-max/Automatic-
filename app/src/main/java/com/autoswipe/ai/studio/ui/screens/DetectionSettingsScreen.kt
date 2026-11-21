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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autoswipe.ai.studio.model.SwipeSettings
import com.autoswipe.ai.studio.ui.components.NavyCard
import com.autoswipe.ai.studio.ui.theme.AutoSwipeColors
import kotlin.math.roundToInt

@Composable
fun DetectionSettingsScreen(
    settings: SwipeSettings,
    onConfidenceChange: (Float) -> Unit,
    onTimeoutChange: (Long) -> Unit,
    onToggleSignals: (replayText: Boolean?, overlayControls: Boolean?, progressCompletion: Boolean?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NavyCard {
            Text("Detection Signals", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "AutoSwipe AI combines multiple accessibility signals instead of relying on a single phrase or blind timers.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            SignalToggleRow(
                title = "Replay & End-Screen Text",
                subtitle = "Detects 'Watch again', 'Replay', 'Replay video', 'Play again', 'Rewatch', 'Video ended'.",
                checked = settings.detectReplayTextSignals,
                onCheckedChange = { onToggleSignals(it, null, null) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SignalToggleRow(
                title = "UI Controls & End Overlays",
                subtitle = "Detects replay button resource IDs, end-of-video cards, and completed player controls.",
                checked = settings.detectOverlayControls,
                onCheckedChange = { onToggleSignals(null, it, null) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SignalToggleRow(
                title = "Playback Progress & Loop Wrap",
                subtitle = "Monitors SeekBar/ProgressBar completion (>= 97%) and loop resets when exposed by the app.",
                checked = settings.detectProgressCompletion,
                onCheckedChange = { onToggleSignals(null, null, it) }
            )
        }

        NavyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Confidence Threshold", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${(settings.confidenceThreshold * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = AutoSwipeColors.ElectricCyan
                )
            }
            Text(
                "Minimum signal confidence required before scheduling a swipe. Higher values prevent accidental early swipes.",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = settings.confidenceThreshold,
                onValueChange = onConfidenceChange,
                valueRange = 0.55f..0.95f,
                steps = 7
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Detection Timeout", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${settings.detectionTimeoutMs / 1000}s",
                    style = MaterialTheme.typography.titleMedium,
                    color = AutoSwipeColors.ElectricCyan
                )
            }
            Text(
                "If a looping video exposes zero end-of-video cues within this window, AutoSwipe AI logs a detection timeout instead of guessing.",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = (settings.detectionTimeoutMs / 1000f),
                onValueChange = { onTimeoutChange((it.roundToInt() * 1000L)) },
                valueRange = 20f..180f,
                steps = 15
            )
        }
    }
}

@Composable
private fun SignalToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
