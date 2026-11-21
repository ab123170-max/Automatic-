package com.autoswipe.ai.studio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autoswipe.ai.studio.model.SwipeSensitivity
import com.autoswipe.ai.studio.model.SwipeSettings
import com.autoswipe.ai.studio.ui.components.NavyCard
import com.autoswipe.ai.studio.ui.theme.AutoSwipeColors
import kotlin.math.roundToInt

@Composable
fun SwipeSettingsScreen(
    settings: SwipeSettings,
    onSwipeDelayChange: (Long) -> Unit,
    onSwipeDurationChange: (Long) -> Unit,
    onSensitivityChange: (SwipeSensitivity) -> Unit,
    onCooldownChange: (Long) -> Unit,
    onPauseOnScreenOffChange: (Boolean) -> Unit,
    onResetDefaults: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NavyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Swipe Delay After End", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${settings.swipeDelayMs} ms",
                    style = MaterialTheme.typography.titleMedium,
                    color = AutoSwipeColors.ElectricCyan
                )
            }
            Text(
                "Wait duration after video end is confirmed before performing the upward swipe (default 800ms).",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = settings.swipeDelayMs.toFloat(),
                onValueChange = { onSwipeDelayChange((it / 50f).roundToInt() * 50L) },
                valueRange = SwipeSettings.MIN_SWIPE_DELAY_MS.toFloat()..SwipeSettings.MAX_SWIPE_DELAY_MS.toFloat()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Swipe Gesture Duration", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${settings.swipeDurationMs} ms",
                    style = MaterialTheme.typography.titleMedium,
                    color = AutoSwipeColors.ElectricCyan
                )
            }
            Text(
                "Speed of the upward Accessibility gesture stroke (default 350ms). Shorter = faster fling.",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = settings.swipeDurationMs.toFloat(),
                onValueChange = { onSwipeDurationChange((it / 25f).roundToInt() * 25L) },
                valueRange = SwipeSettings.MIN_SWIPE_DURATION_MS.toFloat()..SwipeSettings.MAX_SWIPE_DURATION_MS.toFloat()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Cooldown Between Swipes", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${settings.cooldownMs} ms",
                    style = MaterialTheme.typography.titleMedium,
                    color = AutoSwipeColors.ElectricCyan
                )
            }
            Text(
                "Minimum safety pause after a swipe completes to prevent double-skipping.",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = settings.cooldownMs.toFloat(),
                onValueChange = { onCooldownChange((it / 100f).roundToInt() * 100L) },
                valueRange = SwipeSettings.MIN_COOLDOWN_MS.toFloat()..SwipeSettings.MAX_COOLDOWN_MS.toFloat()
            )
        }

        NavyCard {
            Text("Swipe Sensitivity", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            SwipeSensitivity.entries.forEach { option ->
                val selected = settings.swipeSensitivity == option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSensitivityChange(option) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) AutoSwipeColors.NavyElevated else AutoSwipeColors.NavySurface,
                    border = BorderStroke(
                        1.dp,
                        if (selected) AutoSwipeColors.ElectricBlue else AutoSwipeColors.NavyOutline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSensitivityChange(option) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.label, style = MaterialTheme.typography.bodyLarge)
                            Text(option.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        NavyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Pause When Screen Turns Off", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Automatically pauses timers and resets pending gestures when locking your phone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = settings.pauseOnScreenOff,
                    onCheckedChange = onPauseOnScreenOffChange
                )
            }
        }

        OutlinedButton(
            onClick = onResetDefaults,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Restore Default Swipe Settings (800ms / 350ms)")
        }
    }
}
