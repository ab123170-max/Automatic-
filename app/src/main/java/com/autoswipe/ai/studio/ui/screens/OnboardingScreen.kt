package com.autoswipe.ai.studio.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoswipe.ai.studio.model.PermissionStatus
import com.autoswipe.ai.studio.service.PermissionHelper
import com.autoswipe.ai.studio.ui.components.AutoSwipeLogo
import com.autoswipe.ai.studio.ui.components.NavyCard
import com.autoswipe.ai.studio.ui.components.StatusBadge
import com.autoswipe.ai.studio.ui.theme.AutoSwipeColors

@Composable
fun OnboardingScreen(
    permissions: PermissionStatus,
    onRefreshPermissions: () -> Unit,
    onFinish: (startImmediately: Boolean) -> Unit
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 5

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AutoSwipeLogo(size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("AutoSwipe AI", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Step ${step + 1} of $totalSteps",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        LinearProgressIndicator(
            progress = { (step + 1) / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = AutoSwipeColors.ElectricBlue,
            trackColor = AutoSwipeColors.NavyElevated
        )

        AnimatedContent(targetState = step, label = "onboarding_step") { currentStep ->
            when (currentStep) {
                0 -> {
                    NavyCard {
                        Text(
                            "Hands-Free Short Video Scrolling",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "AutoSwipe AI detects when a short video finishes on supported apps (TikTok, TikTok Lite, Instagram Reels, YouTube Shorts, and Facebook Reels) and performs one smooth upward swipe.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "• 100% on-device Accessibility detection — no accounts, no servers, no screen recording.\n" +
                                "• Never swipes inside unrelated apps or while your screen is locked.\n" +
                                "• Includes a movable floating control panel with instant Pause and Emergency Stop.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                1 -> {
                    NavyCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1. Accessibility Service", style = MaterialTheme.typography.titleLarge)
                            StatusBadge(
                                label = if (permissions.isAccessibilityEnabled) "Enabled" else "Required",
                                color = if (permissions.isAccessibilityEnabled) AutoSwipeColors.Emerald else AutoSwipeColors.Amber
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Android requires your explicit permission for AutoSwipe AI to inspect Replay/end-of-video controls and dispatch an upward swipe gesture.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "How to enable:\n" +
                                "1. Tap Open Accessibility Settings below.\n" +
                                "2. Find 'AutoSwipe AI Engine' (under Downloaded Apps on some devices).\n" +
                                "3. Turn it ON and confirm the system dialog.\n" +
                                "4. Return here — status updates automatically.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                context.startActivity(PermissionHelper.openAccessibilitySettingsIntent())
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Accessibility Settings")
                        }
                    }
                }

                2 -> {
                    NavyCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("2. Floating Overlay", style = MaterialTheme.typography.titleLarge)
                            StatusBadge(
                                label = if (permissions.isOverlayGranted) "Granted" else "Recommended",
                                color = if (permissions.isOverlayGranted) AutoSwipeColors.Emerald else AutoSwipeColors.Amber
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "The 'Display over other apps' permission lets AutoSwipe AI place a small, semi-transparent Start / Pause / Stop control above your video app so you can stop scrolling with one tap.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                context.startActivity(
                                    PermissionHelper.openOverlaySettingsIntent(context.packageName)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Overlay Permission")
                        }
                    }
                }

                3 -> {
                    NavyCard {
                        Text("Verify Permissions", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(10.dp))
                        PermissionCheckRow(
                            title = "Accessibility Service",
                            granted = permissions.isAccessibilityEnabled,
                            requiredLabel = "Required for detection & swipe gestures"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PermissionCheckRow(
                            title = "Floating Overlay Permission",
                            granted = permissions.isOverlayGranted,
                            requiredLabel = "Required for floating Start/Pause/Stop controls"
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onRefreshPermissions,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Re-check Permissions")
                        }
                    }
                }

                else -> {
                    NavyCard(borderColor = AutoSwipeColors.Emerald.copy(alpha = 0.6f)) {
                        Text(
                            if (permissions.hasCorePermissions) "Setup Complete" else "Ready to Explore",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (permissions.hasCorePermissions) {
                                "All core permissions are verified. You can start automatic scrolling right away or customize swipe delay and supported apps first."
                            } else {
                                "You can enter the dashboard now and finish enabling any remaining permissions anytime under Permissions & Setup."
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (permissions.isAccessibilityEnabled) {
                            Button(
                                onClick = { onFinish(true) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AutoSwipeColors.ElectricBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Start AutoSwipe Engine & Open Dashboard")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedButton(
                            onClick = { onFinish(false) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Dashboard (Engine Off)")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step -= 1 },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
            }
            if (step < totalSteps - 1) {
                Button(
                    onClick = {
                        onRefreshPermissions()
                        step += 1
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AutoSwipeColors.ElectricBlue)
                ) {
                    Text(if (step == 0) "Get Started" else "Continue")
                }
            }
        }
    }
}

@Composable
private fun PermissionCheckRow(
    title: String,
    granted: Boolean,
    requiredLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(requiredLabel, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.width(8.dp))
        StatusBadge(
            label = if (granted) "Verified" else "Missing",
            color = if (granted) AutoSwipeColors.Emerald else AutoSwipeColors.CoralRed
        )
    }
}
