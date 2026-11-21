package com.autoswipe.ai.studio.model

enum class EnginePhase(val label: String) {
    OFF("Stopped"),
    WAITING_PERMISSION("Permission Required"),
    WAITING_SUPPORTED_APP("Waiting for Video App"),
    UNSUPPORTED_APP("Unrelated App Paused"),
    MONITORING("Monitoring Playback"),
    VIDEO_END_DETECTED("Video End Detected"),
    WAITING_DELAY("Waiting Swipe Delay"),
    SWIPING("Swiping to Next"),
    COOLDOWN("Swipe Cooldown"),
    PAUSED("Paused"),
    ERROR("Attention Needed")
}

data class PermissionStatus(
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isNotificationGranted: Boolean = true,
    val isBatteryOptimizationIgnored: Boolean = false
) {
    val hasCorePermissions: Boolean
        get() = isAccessibilityEnabled && isOverlayGranted
}

data class EngineRuntimeState(
    val phase: EnginePhase = EnginePhase.OFF,
    val isServiceConnected: Boolean = false,
    val isForegroundServiceRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isScreenLocked: Boolean = false,
    val isFloatingOverlayVisible: Boolean = false,
    val activePackageName: String? = null,
    val activeApp: SupportedApp? = null,
    val isActiveAppEnabled: Boolean = false,
    val currentVideoSignature: String? = null,
    val detectionConfidence: Float = 0f,
    val detectionDetail: String = "Engine idle",
    val swipeStatusText: String = "Standby",
    val sessionActiveMs: Long = 0L,
    val sessionSwipes: Int = 0,
    val totalSwipesAllTime: Int = 0,
    val lastError: String? = null,
    val lastUpdatedEpochMs: Long = 0L
)
