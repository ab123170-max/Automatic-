package com.autoswipe.ai.studio.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoswipe.ai.studio.model.DetectionSupportLevel
import com.autoswipe.ai.studio.model.EngineRuntimeState
import com.autoswipe.ai.studio.model.SupportedAppRegistry
import com.autoswipe.ai.studio.model.SwipeSettings
import com.autoswipe.ai.studio.service.PermissionHelper
import com.autoswipe.ai.studio.ui.components.NavyCard
import com.autoswipe.ai.studio.ui.components.StatusBadge
import com.autoswipe.ai.studio.ui.theme.AutoSwipeColors

@Composable
fun SupportedAppsScreen(
    settings: SwipeSettings,
    runtime: EngineRuntimeState,
    onSetAppEnabled: (appId: String, enabled: Boolean) -> Unit,
    onAddCustomPackage: (String) -> Unit,
    onRemoveCustomPackage: (String) -> Unit
) {
    val context = LocalContext.current
    val apps = remember(settings.customCompatiblePackages) {
        SupportedAppRegistry.getAllApps(settings.customCompatiblePackages)
    }
    var customPkgInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NavyCard {
            Text("Platform Safety Guarantee", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "AutoSwipe AI inspects accessibility nodes and performs gestures ONLY inside enabled applications below. Unrelated apps are automatically ignored.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        apps.forEach { app ->
            val installed = remember(app.packageNames) {
                PermissionHelper.isAnyPackageInstalled(context, app.packageNames)
            }
            val enabled = settings.isAppEnabled(app)
            val isCurrentlyActive = runtime.activeApp?.id == app.id

            NavyCard(
                borderColor = if (isCurrentlyActive) AutoSwipeColors.ElectricBlue else AutoSwipeColors.NavyOutline
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = app.packageNames.joinToString(", "),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onSetAppEnabled(app.id, it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        label = if (installed) "Installed" else "Not Installed",
                        color = if (installed) AutoSwipeColors.Emerald else AutoSwipeColors.TextSecondary
                    )
                    StatusBadge(
                        label = if (app.isVideoEndDetectionSupported) app.supportLevel.label else "Unsupported",
                        color = when (app.supportLevel) {
                            DetectionSupportLevel.FULL_ACCESSIBILITY -> AutoSwipeColors.ElectricCyan
                            DetectionSupportLevel.PARTIAL_LOOP_LIMITED -> AutoSwipeColors.Amber
                            DetectionSupportLevel.EXPERIMENTAL_GENERIC -> AutoSwipeColors.TextSecondary
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = app.limitationsNote,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-scroll: ${if (enabled && settings.autoScrollEnabled) "Active" else if (enabled) "Ready (Engine Off)" else "Disabled"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (enabled) AutoSwipeColors.ElectricCyan else AutoSwipeColors.TextSecondary
                    )
                    Row {
                        if (app.isCustom) {
                            TextButton(onClick = {
                                app.packageNames.firstOrNull()?.let(onRemoveCustomPackage)
                            }) {
                                Text("Remove", color = AutoSwipeColors.CoralRed)
                            }
                        }
                        if (installed) {
                            OutlinedButton(
                                onClick = {
                                    PermissionHelper.launchTargetAppIntent(context, app.packageNames)
                                        ?.let { context.startActivity(it) }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Open")
                            }
                        }
                    }
                }
            }
        }

        // Custom compatible app addition
        NavyCard {
            Text("Add Other Compatible App", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Enter an Android package name (for example com.example.shortvideo) to apply generic accessibility end-of-video detection.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customPkgInput,
                    onValueChange = { customPkgInput = it },
                    placeholder = { Text("com.package.name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (customPkgInput.contains('.')) {
                            onAddCustomPackage(customPkgInput)
                            customPkgInput = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add")
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
