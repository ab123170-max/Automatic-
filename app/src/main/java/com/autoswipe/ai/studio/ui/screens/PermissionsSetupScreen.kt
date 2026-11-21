package com.autoswipe.ai.studio.ui.screens

import android.os.Build
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.autoswipe.ai.studio.model.PermissionStatus
import com.autoswipe.ai.studio.service.PermissionHelper
import com.autoswipe.ai.studio.ui.components.NavyCard
import com.autoswipe.ai.studio.ui.components.StatusBadge
import com.autoswipe.ai.studio.ui.theme.AutoSwipeColors

@Composable
fun PermissionsSetupScreen(
    permissions: PermissionStatus,
    onRefreshPermissions: () -> Unit,
    onRequestNotificationPermission: () -> Unit
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
                Text("1. Accessibility Service", style = MaterialTheme.typography.titleMedium)
                StatusBadge(
                    label = if (permissions.isAccessibilityEnabled) "Enabled" else "Required",
                    color = if (permissions.isAccessibilityEnabled) AutoSwipeColors.Emerald else AutoSwipeColors.CoralRed
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Used exclusively to read video-end UI cues inside supported short-video apps and perform upward swipe gestures when you enable AutoSwipe AI. Android blocks silent activation for your security.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { context.startActivity(PermissionHelper.openAccessibilitySettingsIntent()) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Accessibility Settings")
            }
        }

        NavyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("2. Display Over Other Apps", style = MaterialTheme.typography.titleMedium)
                StatusBadge(
                    label = if (permissions.isOverlayGranted) "Granted" else "Recommended",
                    color = if (permissions.isOverlayGranted) AutoSwipeColors.Emerald else AutoSwipeColors.Amber
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Required to show the floating Start / Pause / Stop control panel above TikTok, Reels, or YouTube Shorts.",
                style = MaterialTheme.typography.bodyMedium
            )
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
                Text("Open Overlay Permission")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NavyCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("3. Persistent Notification", style = MaterialTheme.typography.titleMedium)
                    StatusBadge(
                        label = if (permissions.isNotificationGranted) "Granted" else "Recommended",
                        color = if (permissions.isNotificationGranted) AutoSwipeColors.Emerald else AutoSwipeColors.Amber
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Android 13–16 requires notification permission to display the persistent Pause / Stop controls in your shade while auto-scrolling is active.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!permissions.isNotificationGranted) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRequestNotificationPermission,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow Notifications")
                    }
                }
            }
        }

        NavyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Battery Optimization", style = MaterialTheme.typography.titleMedium)
                StatusBadge(
                    label = if (permissions.isBatteryOptimizationIgnored) "Unrestricted" else "Standard",
                    color = if (permissions.isBatteryOptimizationIgnored) AutoSwipeColors.Emerald else AutoSwipeColors.Amber
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Some manufacturers (Samsung, Xiaomi, Oppo, Vivo, Realme) aggressively stop background Accessibility services when battery optimization is enabled. Setting AutoSwipe AI to 'Unrestricted' improves stability, though OS memory limits can still stop background processes.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    try {
                        context.startActivity(
                            PermissionHelper.openBatteryOptimizationSettingsIntent(context.packageName)
                        )
                    } catch (_: Exception) {
                        context.startActivity(
                            PermissionHelper.openAppDetailsSettingsIntent(context.packageName)
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Battery Optimization Settings")
            }
        }

        OutlinedButton(
            onClick = onRefreshPermissions,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Permission Status")
        }
    }
}
