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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.autoswipe.ai.studio.model.EngineRuntimeState
import com.autoswipe.ai.studio.model.PermissionStatus
import com.autoswipe.ai.studio.model.SwipeSettings
import com.autoswipe.ai.studio.service.PermissionHelper
import com.autoswipe.ai.studio.ui.components.NavyCard
import com.autoswipe.ai.studio.ui.components.StatusBadge
import com.autoswipe.ai.studio.ui.theme.AutoSwipeColors
import kotlin.math.roundToInt

@Composable
fun FloatingControlsSettingsScreen(
    settings: SwipeSettings,
    runtime: EngineRuntimeState,
    permissions: PermissionStatus,
    onToggleFloatingEnabled: (Boolean) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onExpandedByDefaultChange: (Boolean) -> Unit,
    onShowFloatingPanel: () -> Unit,
    onHideFloatingPanel: () -> Unit
) {
    val context = LocalContext.current

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Overlay Permission", style = MaterialTheme.typography.titleMedium)
                StatusBadge(
                    label = if (permissions.isOverlayGranted) "Granted" else "Required",
                    color = if (permissions.isOverlayGranted) AutoSwipeColors.Emerald else AutoSwipeColors.Amber
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Android requires 'Display over other apps' so the floating Start / Pause / Stop controller can remain reachable above TikTok, Reels, or YouTube Shorts.",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!permissions.isOverlayGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        context.startActivity(
                            PermissionHelper.openOverlaySettingsIntent(context.packageName)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Overlay Permission")
                }
            }
        }

        NavyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Enable Floating Control Panel", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Shows a movable, semi-transparent control overlay when the engine or accessibility service is active.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = settings.floatingPanelEnabled,
                    onCheckedChange = onToggleFloatingEnabled
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Expanded By Default", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Tap the panel header anytime to collapse it into a tiny status pill.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = settings.floatingPanelExpandedByDefault,
                    onCheckedChange = onExpandedByDefaultChange
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Panel Opacity", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${(settings.floatingPanelAlpha * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = AutoSwipeColors.ElectricCyan
                )
            }
            Slider(
                value = settings.floatingPanelAlpha,
                onValueChange = onAlphaChange,
                valueRange = 0.45f..1.0f
            )
        }

        if (permissions.isOverlayGranted && settings.floatingPanelEnabled) {
            NavyCard {
                Text("Visibility Control", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "You can hide the floating panel at any time without stopping automatic scrolling. Restore it here or from the persistent notification.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onShowFloatingPanel,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Show Panel")
                    }
                    OutlinedButton(
                        onClick = onHideFloatingPanel,
                        enabled = runtime.isFloatingOverlayVisible,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hide Panel")
                    }
                }
            }
        }
    }
}
