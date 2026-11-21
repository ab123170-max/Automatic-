package com.autoswipe.ai.studio

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoswipe.ai.studio.model.ScreenRoute
import com.autoswipe.ai.studio.ui.components.AutoSwipeLogo
import com.autoswipe.ai.studio.ui.components.PersistentEmergencyBar
import com.autoswipe.ai.studio.ui.screens.AboutScreen
import com.autoswipe.ai.studio.ui.screens.DetectionSettingsScreen
import com.autoswipe.ai.studio.ui.screens.FloatingControlsSettingsScreen
import com.autoswipe.ai.studio.ui.screens.HelpTroubleshootingScreen
import com.autoswipe.ai.studio.ui.screens.HomeDashboardScreen
import com.autoswipe.ai.studio.ui.screens.OnboardingScreen
import com.autoswipe.ai.studio.ui.screens.PermissionsSetupScreen
import com.autoswipe.ai.studio.ui.screens.SupportedAppsScreen
import com.autoswipe.ai.studio.ui.screens.SwipeSettingsScreen
import com.autoswipe.ai.studio.ui.screens.UsageStatisticsScreen
import com.autoswipe.ai.studio.ui.theme.AutoSwipeColors
import com.autoswipe.ai.studio.ui.theme.AutoSwipeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AutoSwipeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoSwipeTheme {
                AutoSwipeAppShell(viewModel = viewModel)
            }
        }
    }
}

private data class DrawerNavItem(
    val route: ScreenRoute,
    val icon: ImageVector
)

private val drawerNavItems = listOf(
    DrawerNavItem(ScreenRoute.HOME_DASHBOARD, Icons.Rounded.Home),
    DrawerNavItem(ScreenRoute.SUPPORTED_APPS, Icons.Rounded.Apps),
    DrawerNavItem(ScreenRoute.DETECTION_SETTINGS, Icons.Rounded.Visibility),
    DrawerNavItem(ScreenRoute.SWIPE_SETTINGS, Icons.Rounded.Speed),
    DrawerNavItem(ScreenRoute.FLOATING_CONTROLS, Icons.Rounded.PictureInPictureAlt),
    DrawerNavItem(ScreenRoute.USAGE_STATISTICS, Icons.Rounded.BarChart),
    DrawerNavItem(ScreenRoute.PERMISSIONS_SETUP, Icons.Rounded.Security),
    DrawerNavItem(ScreenRoute.HELP_TROUBLESHOOTING, Icons.AutoMirrored.Rounded.HelpOutline),
    DrawerNavItem(ScreenRoute.ABOUT, Icons.Rounded.Info)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoSwipeAppShell(viewModel: AutoSwipeViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val runtime by viewModel.runtime.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    if (!settings.onboardingCompleted) {
        Scaffold(containerColor = AutoSwipeColors.DeepNavy) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                OnboardingScreen(
                    permissions = permissions,
                    onRefreshPermissions = { viewModel.refreshPermissions() },
                    onFinish = { startImmediately ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !permissions.isNotificationGranted
                        ) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.completeOnboarding(startImmediately)
                    }
                )
            }
        }
        return
    }

    var currentRoute by rememberSaveable { mutableStateOf(ScreenRoute.HOME_DASHBOARD) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AutoSwipeColors.NavySurface,
                drawerContentColor = AutoSwipeColors.TextPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AutoSwipeLogo(size = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("AutoSwipe AI", style = MaterialTheme.typography.titleLarge)
                        Text("Short-Video Engine", style = MaterialTheme.typography.labelMedium)
                    }
                }
                HorizontalDivider(color = AutoSwipeColors.NavyOutline)
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    drawerNavItems.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.route.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                currentRoute = item.route
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = AutoSwipeColors.ElectricBlue.copy(alpha = 0.20f),
                                selectedIconColor = AutoSwipeColors.ElectricCyan,
                                selectedTextColor = Color.White,
                                unselectedIconColor = AutoSwipeColors.TextSecondary,
                                unselectedTextColor = AutoSwipeColors.TextPrimary
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            containerColor = AutoSwipeColors.DeepNavy,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentRoute.title,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = currentRoute.subtitle,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    },
                    navigationIcon = {
                        if (currentRoute == ScreenRoute.HOME_DASHBOARD) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Rounded.Menu, contentDescription = "Open navigation menu")
                            }
                        } else {
                            IconButton(onClick = { currentRoute = ScreenRoute.HOME_DASHBOARD }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to dashboard")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AutoSwipeColors.DeepNavy,
                        titleContentColor = AutoSwipeColors.TextPrimary,
                        navigationIconContentColor = AutoSwipeColors.ElectricCyan
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                PersistentEmergencyBar(
                    runtime = runtime,
                    autoScrollEnabled = settings.autoScrollEnabled,
                    onPauseResume = { viewModel.pauseOrResume() },
                    onEmergencyStop = { viewModel.emergencyStop() },
                    onDismissError = { viewModel.clearError() }
                )

                when (currentRoute) {
                    ScreenRoute.HOME_DASHBOARD, ScreenRoute.ONBOARDING -> {
                        HomeDashboardScreen(
                            settings = settings,
                            runtime = runtime,
                            permissions = permissions,
                            stats = stats,
                            onToggleAutoScroll = { enable ->
                                if (enable &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !permissions.isNotificationGranted
                                ) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                viewModel.setAutoScrollEnabled(enable)
                            },
                            onShowFloatingPanel = { viewModel.showFloatingPanel() },
                            onHideFloatingPanel = { viewModel.setFloatingPanelHidden(true) },
                            onNavigate = { currentRoute = it }
                        )
                    }
                    ScreenRoute.SUPPORTED_APPS -> {
                        SupportedAppsScreen(
                            settings = settings,
                            runtime = runtime,
                            onSetAppEnabled = { id, enabled -> viewModel.setAppEnabled(id, enabled) },
                            onAddCustomPackage = { viewModel.addCustomPackage(it) },
                            onRemoveCustomPackage = { viewModel.removeCustomPackage(it) }
                        )
                    }
                    ScreenRoute.DETECTION_SETTINGS -> {
                        DetectionSettingsScreen(
                            settings = settings,
                            onConfidenceChange = { viewModel.updateConfidenceThreshold(it) },
                            onTimeoutChange = { viewModel.updateDetectionTimeout(it) },
                            onToggleSignals = { replay, overlay, progress ->
                                viewModel.updateSignalToggles(replay, overlay, progress)
                            }
                        )
                    }
                    ScreenRoute.SWIPE_SETTINGS -> {
                        SwipeSettingsScreen(
                            settings = settings,
                            onSwipeDelayChange = { viewModel.updateSwipeDelay(it) },
                            onSwipeDurationChange = { viewModel.updateSwipeDuration(it) },
                            onSensitivityChange = { viewModel.updateSwipeSensitivity(it) },
                            onCooldownChange = { viewModel.updateCooldown(it) },
                            onPauseOnScreenOffChange = { viewModel.updatePauseOnScreenOff(it) },
                            onResetDefaults = { viewModel.resetSettingsToDefault() }
                        )
                    }
                    ScreenRoute.FLOATING_CONTROLS -> {
                        FloatingControlsSettingsScreen(
                            settings = settings,
                            runtime = runtime,
                            permissions = permissions,
                            onToggleFloatingEnabled = { viewModel.updateFloatingEnabled(it) },
                            onAlphaChange = { viewModel.updateFloatingAlpha(it) },
                            onExpandedByDefaultChange = { viewModel.updateFloatingExpandedByDefault(it) },
                            onShowFloatingPanel = { viewModel.showFloatingPanel() },
                            onHideFloatingPanel = { viewModel.setFloatingPanelHidden(true) }
                        )
                    }
                    ScreenRoute.USAGE_STATISTICS -> {
                        UsageStatisticsScreen(
                            stats = stats,
                            onResetStats = { viewModel.resetUsageStats() }
                        )
                    }
                    ScreenRoute.PERMISSIONS_SETUP -> {
                        PermissionsSetupScreen(
                            permissions = permissions,
                            onRefreshPermissions = { viewModel.refreshPermissions() },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }
                    ScreenRoute.HELP_TROUBLESHOOTING -> HelpTroubleshootingScreen()
                    ScreenRoute.ABOUT -> AboutScreen()
                }
            }
        }
    }
}
