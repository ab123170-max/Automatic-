package com.autoswipe.ai.studio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoswipe.ai.studio.data.UsageStatsSummary
import com.autoswipe.ai.studio.model.EngineRuntimeState
import com.autoswipe.ai.studio.model.PermissionStatus
import com.autoswipe.ai.studio.model.ScreenRoute
import com.autoswipe.ai.studio.model.SwipeSettings
import com.autoswipe.ai.studio.ui.components.AutoSwipeLogo
import com.autoswipe.ai.studio.ui.components.NavyCard
import com.autoswipe.ai.studio.ui.components.StatusBadge
import com.autoswipe.ai.studio.ui.components.formatDurationMs
import com.autoswipe.ai.studio.ui.components.phaseColor
import com.autoswipe.ai.studio.ui.theme.AutoSwipeColors

@Composable
fun HomeDashboardScreen(
    settings: SwipeSettings,
    runtime: EngineRuntimeState,
    permissions: PermissionStatus,
    stats: UsageStatsSummary,
    onToggleAutoScroll: (Boolean) -> Unit,
    onShowFloatingPanel: () -> Unit,
    onHideFloatingPanel: () -> Unit,
    onNavigate: (ScreenRoute) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Master Control Card
        NavyCard(
            borderColor = if (settings.autoScrollEnabled) {
                AutoSwipeColors.ElectricBlue.copy(alpha = 0.7f)
            } else {
                AutoSwipeColors.NavyOutline
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AutoSwipeLogo(size = 48.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AutoSwipe AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusBadge(
                            label = runtime.phase.label,
                            color = phaseColor(runtime.phase)
                        )
                    }
                }
                Switch(
                    checked = settings.autoScrollEnabled,
                    onCheckedChange = onToggleAutoScroll,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AutoSwipeColors.ElectricBlue,
                        uncheckedThumbColor = AutoSwipeColors.TextSecondary,
                        uncheckedTrackColor = AutoSwipeColors.NavyElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    label = "Total Swipes",
                    value = stats.totalSwipes.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Session Time",
                    value = formatDurationMs(runtime.sessionActiveMs),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live Detection Engine Card
        NavyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Detection Engine", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${(runtime.detectionConfidence * 100).toInt()}% confidence",
                    style = MaterialTheme.typography.labelMedium,
                    color = AutoSwipeColors.ElectricCyan
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { runtime.detectionConfidence.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = AutoSwipeColors.ElectricCyan,
                trackColor = AutoSwipeColors.NavyElevated
            )
            Spacer(modifier = Modifier.height(10.dp))
            InfoPairRow(
                label = "Current Application",
                value = runtime.activeApp?.displayName
                    ?: runtime.activePackageName?.substringAfterLast('.')
                    ?: "None detected"
            )
            Spacer(modifier = Modifier.height(6.dp))
            InfoPairRow(
                label = "Engine Status",
                value = runtime.detectionDetail
            )
            Spacer(modifier = Modifier.height(6.dp))
            InfoPairRow(
                label = "Swipe Timing",
                value = "${settings.swipeDelayMs}ms delay • ${settings.swipeDurationMs}ms swipe"
            )
        }

        // Permissions & Floating Controls Status Card
        NavyCard {
            Text("System Readiness", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))

            StatusActionRow(
                title = "Accessibility Service",
                statusLabel = if (permissions.isAccessibilityEnabled) "Connected" else "Action Needed",
                statusColor = if (permissions.isAccessibilityEnabled) AutoSwipeColors.Emerald else AutoSwipeColors.CoralRed,
                onClick = { onNavigate(ScreenRoute.PERMISSIONS_SETUP) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatusActionRow(
                title = "Overlay Permission",
                statusLabel = if (permissions.isOverlayGranted) "Granted" else "Missing",
                statusColor = if (permissions.isOverlayGranted) AutoSwipeColors.Emerald else AutoSwipeColors.Amber,
                onClick = { onNavigate(ScreenRoute.PERMISSIONS_SETUP) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatusActionRow(
                title = "Floating Control Panel",
                statusLabel = when {
                    !permissions.isOverlayGranted -> "Permission Needed"
                    !settings.floatingPanelEnabled -> "Disabled"
                    settings.floatingPanelHidden -> "Hidden (Engine Running)"
                    runtime.isFloatingOverlayVisible -> "Visible"
                    else -> "Ready"
                },
                statusColor = when {
                    !permissions.isOverlayGranted -> AutoSwipeColors.Amber
                    runtime.isFloatingOverlayVisible -> AutoSwipeColors.ElectricCyan
                    else -> AutoSwipeColors.TextSecondary
                },
                onClick = { onNavigate(ScreenRoute.FLOATING_CONTROLS) }
            )

            if (permissions.isOverlayGranted && settings.floatingPanelEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (settings.floatingPanelHidden || !runtime.isFloatingOverlayVisible) {
                        Button(
                            onClick = onShowFloatingPanel,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Show Floating Panel")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onHideFloatingPanel,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hide Panel Only")
                        }
                    }
                }
            }
        }

        // Quick Navigation Cards (No bottom bar)
        Text("Quick Configuration", style = MaterialTheme.typography.titleMedium)

        QuickLinkRow(
            icon = Icons.Rounded.Apps,
            title = "Supported Applications",
            subtitle = "Manage TikTok, Reels, Shorts & compatibility notes",
            onClick = { onNavigate(ScreenRoute.SUPPORTED_APPS) }
        )
        QuickLinkRow(
            icon = Icons.Rounded.Visibility,
            title = "Detection Settings",
            subtitle = "Replay cues, progress tracking & confidence threshold",
            onClick = { onNavigate(ScreenRoute.DETECTION_SETTINGS) }
        )
        QuickLinkRow(
            icon = Icons.Rounded.Speed,
            title = "Swipe Settings",
            subtitle = "Delay (${settings.swipeDelayMs}ms), duration & sensitivity",
            onClick = { onNavigate(ScreenRoute.SWIPE_SETTINGS) }
        )
        QuickLinkRow(
            icon = Icons.Rounded.BarChart,
            title = "Usage Statistics",
            subtitle = "${stats.totalSessions} sessions • ${formatDurationMs(stats.totalActiveTimeMs)} total active",
            onClick = { onNavigate(ScreenRoute.USAGE_STATISTICS) }
        )
        QuickLinkRow(
            icon = Icons.Rounded.Security,
            title = "Permissions & Reliability",
            subtitle = "Accessibility, overlay, notifications & battery guide",
            onClick = { onNavigate(ScreenRoute.PERMISSIONS_SETUP) }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    NavyCard(
        modifier = modifier,
        containerColor = AutoSwipeColors.NavyElevated,
        borderColor = AutoSwipeColors.NavyOutline
    ) {
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
private fun InfoPairRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = AutoSwipeColors.TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusActionRow(
    title: String,
    statusLabel: String,
    statusColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        StatusBadge(label = statusLabel, color = statusColor)
    }
}

@Composable
private fun QuickLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    NavyCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AutoSwipeColors.ElectricCyan
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AutoSwipeColors.TextSecondary
            )
        }
    }
}
