package com.autoswipe.ai.studio.service

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.autoswipe.ai.studio.model.PermissionStatus

object PermissionHelper {

    fun evaluatePermissions(context: Context): PermissionStatus {
        return PermissionStatus(
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context),
            isOverlayGranted = Settings.canDrawOverlays(context),
            isNotificationGranted = isNotificationPermissionGranted(context),
            isBatteryOptimizationIgnored = isIgnoringBatteryOptimizations(context)
        )
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = ComponentName(context, AutoSwipeAccessibilityService::class.java)
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledList = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if (enabledList != null) {
            val foundInManager = enabledList.any { info ->
                val svcInfo = info.resolveInfo?.serviceInfo ?: return@any false
                svcInfo.packageName == expectedComponent.packageName &&
                    svcInfo.name == expectedComponent.className
            }
            if (foundInManager) return true
        }

        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            ) == 1
        } catch (_: Settings.SettingNotFoundException) {
            false
        }
        if (!accessibilityEnabled) return false

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return isComponentListedInAccessibilitySetting(
            enabledServicesValue = enabledServices,
            expectedFlattened = expectedComponent.flattenToString(),
            expectedShort = expectedComponent.flattenToShortString()
        )
    }

    fun isComponentListedInAccessibilitySetting(
        enabledServicesValue: String?,
        expectedFlattened: String,
        expectedShort: String
    ): Boolean {
        if (enabledServicesValue.isNullOrBlank()) return false
        return enabledServicesValue
            .split(':')
            .any { entry ->
                val trimmed = entry.trim()
                trimmed.equals(expectedFlattened, ignoreCase = true) ||
                    trimmed.equals(expectedShort, ignoreCase = true)
            }
    }

    fun isNotificationPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isAnyPackageInstalled(context: Context, packageNames: List<String>): Boolean {
        val pm = context.packageManager
        return packageNames.any { pkg ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun openAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openOverlaySettingsIntent(packageName: String): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openBatteryOptimizationSettingsIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openAppDetailsSettingsIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun launchTargetAppIntent(context: Context, packageNames: List<String>): Intent? {
        val pm = context.packageManager
        for (pkg in packageNames) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return intent
            }
        }
        return null
    }
}
