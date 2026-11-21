package com.autoswipe.ai.studio.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Path
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.autoswipe.ai.studio.AutoSwipeApplication
import com.autoswipe.ai.studio.detection.EngineDecision
import com.autoswipe.ai.studio.detection.SwipeGestureCalculator
import com.autoswipe.ai.studio.detection.VideoEndDetectionEngine
import com.autoswipe.ai.studio.model.EnginePhase
import com.autoswipe.ai.studio.model.SupportedApp
import com.autoswipe.ai.studio.model.SupportedAppRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class AutoSwipeAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val detectionEngine = VideoEndDetectionEngine()
    private val isDispatchingGesture = AtomicBoolean(false)

    private var pendingSwipeJob: Job? = null
    private var lastProcessedEventMs: Long = 0L
    private var currentForegroundPackage: String? = null

    private val coordinator: EngineCoordinator
        get() = (application as AutoSwipeApplication).coordinator

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    cancelPendingSwipe()
                    detectionEngine.resetAll()
                    val settings = coordinator.settingsState.value
                    if (settings.autoScrollEnabled && settings.pauseOnScreenOff) {
                        coordinator.updateRuntimeSnapshot(
                            isScreenLocked = true,
                            phase = EnginePhase.PAUSED,
                            detectionDetail = "Screen off — paused safely",
                            swipeStatusText = "Screen Off"
                        )
                    } else {
                        coordinator.updateRuntimeSnapshot(isScreenLocked = true)
                    }
                }
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> {
                    val locked = isDeviceLocked()
                    coordinator.updateRuntimeSnapshot(isScreenLocked = locked)
                    if (!locked) {
                        triggerReEvaluation()
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val existing = activeInstance.getAndSet(this)
        if (existing != null && existing !== this) {
            // Prevent duplicate concurrent service state
            existing.cancelPendingSwipe()
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }

        coordinator.onAccessibilityConnected()
        triggerReEvaluation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (application as? AutoSwipeApplication)?.floatingControlManager?.onScreenConfigurationChanged()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString()
        if (!pkg.isNullOrBlank() && pkg != "com.android.systemui") {
            currentForegroundPackage = pkg
        }

        val settings = coordinator.settingsState.value
        val activePkg = currentForegroundPackage

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            !isDispatchingGesture.get() &&
            activePkg != packageName
        ) {
            // User scrolled manually -> reset observation & honor manual override cooldown
            cancelPendingSwipe()
            detectionEngine.onUserManualScroll(System.currentTimeMillis())
        }

        if (!settings.autoScrollEnabled) {
            // Update detected foreground app quietly for dashboard awareness without running engine
            val matchedApp = SupportedAppRegistry.findByPackage(activePkg, settings.customCompatiblePackages)
            coordinator.updateRuntimeSnapshot(
                activePackageName = activePkg,
                activeApp = matchedApp,
                isActiveAppEnabled = matchedApp?.let { settings.isAppEnabled(it) } ?: false,
                phase = EnginePhase.OFF
            )
            return
        }

        val now = System.currentTimeMillis()
        // Throttle high-frequency content change events to keep CPU/battery usage low
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            now - lastProcessedEventMs < 240L
        ) {
            return
        }
        lastProcessedEventMs = now

        evaluateCurrentWindow(now)
    }

    fun triggerReEvaluation() {
        serviceScope.launch {
            evaluateCurrentWindow(System.currentTimeMillis())
        }
    }

    fun cancelPendingSwipe() {
        pendingSwipeJob?.cancel()
        pendingSwipeJob = null
    }

    fun onEngineStoppedByUser() {
        cancelPendingSwipe()
        detectionEngine.resetAll()
    }

    private fun evaluateCurrentWindow(nowMs: Long) {
        val settings = coordinator.settingsState.value
        if (!settings.autoScrollEnabled) return

        val runtime = coordinator.runtimeState.value
        if (runtime.isPaused) return

        if (isDeviceLocked()) {
            cancelPendingSwipe()
            coordinator.updateRuntimeSnapshot(
                isScreenLocked = true,
                phase = EnginePhase.PAUSED,
                detectionDetail = "Device locked — waiting for unlock",
                swipeStatusText = "Locked"
            )
            return
        }

        val root = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        }
        val activePkg = root?.packageName?.toString() ?: currentForegroundPackage
        if (activePkg.isNullOrBlank() || activePkg == packageName) {
            cancelPendingSwipe()
            coordinator.updateRuntimeSnapshot(
                activePackageName = activePkg,
                activeApp = null,
                isActiveAppEnabled = false,
                phase = EnginePhase.WAITING_SUPPORTED_APP,
                detectionConfidence = 0f,
                detectionDetail = "In AutoSwipe AI — switch to a video app",
                swipeStatusText = "Standby"
            )
            return
        }

        val supportedApp = SupportedAppRegistry.findByPackage(
            activePkg,
            settings.customCompatiblePackages
        )

        if (supportedApp == null || !settings.isAppEnabled(supportedApp)) {
            cancelPendingSwipe()
            detectionEngine.resetAll()
            coordinator.updateRuntimeSnapshot(
                activePackageName = activePkg,
                activeApp = supportedApp,
                isActiveAppEnabled = false,
                phase = EnginePhase.UNSUPPORTED_APP,
                detectionConfidence = 0f,
                detectionDetail = if (supportedApp == null) {
                    "Unrelated app (${activePkg.substringAfterLast('.')}) — no gestures allowed"
                } else {
                    "${supportedApp.displayName} is disabled in Supported Apps"
                },
                swipeStatusText = "Blocked (Safety)"
            )
            return
        }

        val snapshot = AccessibilityTreeExtractor.extract(root)
        if (snapshot == null) {
            coordinator.updateRuntimeSnapshot(
                activePackageName = activePkg,
                activeApp = supportedApp,
                isActiveAppEnabled = true,
                phase = EnginePhase.WAITING_SUPPORTED_APP,
                detectionDetail = "Window content unavailable for ${supportedApp.displayName}",
                swipeStatusText = "Waiting"
            )
            return
        }

        if (pendingSwipeJob?.isActive == true || isDispatchingGesture.get()) {
            return
        }

        when (val decision = detectionEngine.evaluate(snapshot, supportedApp, settings, nowMs)) {
            is EngineDecision.NotInVideoContext -> {
                coordinator.updateRuntimeSnapshot(
                    activePackageName = activePkg,
                    activeApp = supportedApp,
                    isActiveAppEnabled = true,
                    phase = EnginePhase.WAITING_SUPPORTED_APP,
                    detectionConfidence = 0f,
                    detectionDetail = decision.reason,
                    swipeStatusText = "Standby"
                )
            }
            is EngineDecision.InCooldown -> {
                coordinator.updateRuntimeSnapshot(
                    activePackageName = activePkg,
                    activeApp = supportedApp,
                    isActiveAppEnabled = true,
                    phase = EnginePhase.COOLDOWN,
                    detectionDetail = "Cooldown (${decision.remainingMs}ms)",
                    swipeStatusText = "Cooldown"
                )
            }
            is EngineDecision.AlreadySwiped -> {
                coordinator.updateRuntimeSnapshot(
                    activePackageName = activePkg,
                    activeApp = supportedApp,
                    isActiveAppEnabled = true,
                    currentVideoSignature = decision.signature,
                    phase = EnginePhase.COOLDOWN,
                    detectionDetail = "Waiting for next video to load",
                    swipeStatusText = "Swiped"
                )
            }
            is EngineDecision.Monitoring -> {
                coordinator.updateRuntimeSnapshot(
                    activePackageName = activePkg,
                    activeApp = supportedApp,
                    isActiveAppEnabled = true,
                    currentVideoSignature = decision.signature,
                    phase = EnginePhase.MONITORING,
                    detectionConfidence = decision.confidence,
                    detectionDetail = decision.detail,
                    swipeStatusText = "Watching"
                )
            }
            is EngineDecision.DetectionTimeout -> {
                coordinator.updateRuntimeSnapshot(
                    activePackageName = activePkg,
                    activeApp = supportedApp,
                    isActiveAppEnabled = true,
                    currentVideoSignature = decision.signature,
                    phase = EnginePhase.MONITORING,
                    detectionConfidence = 0f,
                    detectionDetail = "No end signal after ${decision.elapsedMs / 1000}s (clip may loop without accessibility cues)",
                    swipeStatusText = "Waiting"
                )
                serviceScope.launch {
                    coordinator.statsRepository.recordDetectionTimeout(
                        appId = supportedApp.id,
                        displayName = supportedApp.displayName,
                        nowMs = System.currentTimeMillis()
                    )
                }
            }
            is EngineDecision.TriggerSwipe -> {
                scheduleSwipe(supportedApp, decision)
            }
        }
    }

    private fun scheduleSwipe(
        app: SupportedApp,
        decision: EngineDecision.TriggerSwipe
    ) {
        val settings = coordinator.settingsState.value
        coordinator.updateRuntimeSnapshot(
            activePackageName = currentForegroundPackage,
            activeApp = app,
            isActiveAppEnabled = true,
            currentVideoSignature = decision.signature,
            phase = EnginePhase.WAITING_DELAY,
            detectionConfidence = decision.confidence,
            detectionDetail = decision.reason,
            swipeStatusText = "Waiting ${settings.swipeDelayMs}ms"
        )

        pendingSwipeJob?.cancel()
        pendingSwipeJob = serviceScope.launch {
            delay(settings.swipeDelayMs)

            // Re-verify all safety invariants before dispatching gesture
            val latestSettings = coordinator.settingsState.value
            val latestRuntime = coordinator.runtimeState.value
            if (!latestSettings.autoScrollEnabled || latestRuntime.isPaused || isDeviceLocked()) {
                return@launch
            }

            val livePackage = try {
                rootInActiveWindow?.packageName?.toString()
            } catch (_: Exception) {
                null
            } ?: currentForegroundPackage

            val liveApp = SupportedAppRegistry.findByPackage(
                livePackage,
                latestSettings.customCompatiblePackages
            )
            if (liveApp?.id != app.id || !latestSettings.isAppEnabled(app)) {
                coordinator.updateRuntimeSnapshot(
                    phase = EnginePhase.UNSUPPORTED_APP,
                    detectionDetail = "App changed before swipe — gesture cancelled",
                    swipeStatusText = "Cancelled"
                )
                return@launch
            }

            dispatchUpwardSwipe(app, decision, latestSettings.swipeDurationMs, latestSettings.cooldownMs)
        }
    }

    private fun dispatchUpwardSwipe(
        app: SupportedApp,
        decision: EngineDecision.TriggerSwipe,
        durationMs: Long,
        cooldownMs: Long
    ) {
        val (widthPx, heightPx) = getScreenDimensionsPx()
        val spec = SwipeGestureCalculator.buildUpwardSwipe(
            screenWidthPx = widthPx,
            screenHeightPx = heightPx,
            sensitivity = coordinator.settingsState.value.swipeSensitivity,
            durationMs = durationMs
        )

        if (spec == null) {
            coordinator.updateRuntimeSnapshot(
                phase = EnginePhase.ERROR,
                lastError = "Invalid screen metrics for swipe gesture",
                swipeStatusText = "Failed"
            )
            return
        }

        val path = Path().apply {
            moveTo(spec.startX, spec.startY)
            lineTo(spec.endX, spec.endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, spec.durationMs))
            .build()

        isDispatchingGesture.set(true)
        coordinator.updateRuntimeSnapshot(
            phase = EnginePhase.SWIPING,
            swipeStatusText = "Swiping..."
        )

        val accepted = try {
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        isDispatchingGesture.set(false)
                        val now = System.currentTimeMillis()
                        detectionEngine.markSwipeDispatched(decision.signature, now, cooldownMs)
                        coordinator.updateRuntimeSnapshot(
                            phase = EnginePhase.COOLDOWN,
                            detectionConfidence = 0f,
                            detectionDetail = "Swiped on ${app.displayName}",
                            swipeStatusText = "Completed",
                            incrementSessionSwipes = true,
                            lastError = null
                        )
                        serviceScope.launch {
                            coordinator.statsRepository.recordSuccessfulSwipe(
                                appId = app.id,
                                displayName = app.displayName,
                                videoIntervalMs = decision.observedVideoDurationMs,
                                nowMs = now
                            )
                        }
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        isDispatchingGesture.set(false)
                        val now = System.currentTimeMillis()
                        coordinator.updateRuntimeSnapshot(
                            phase = EnginePhase.MONITORING,
                            swipeStatusText = "Interrupted",
                            lastError = "Swipe gesture was cancelled by screen touch or system overlay."
                        )
                        serviceScope.launch {
                            coordinator.statsRepository.recordGestureFailure(
                                appId = app.id,
                                displayName = app.displayName,
                                nowMs = now
                            )
                        }
                    }
                },
                null
            )
        } catch (e: Exception) {
            isDispatchingGesture.set(false)
            false
        }

        if (!accepted) {
            isDispatchingGesture.set(false)
            coordinator.updateRuntimeSnapshot(
                phase = EnginePhase.ERROR,
                swipeStatusText = "Rejected",
                lastError = "System rejected gesture dispatch. Ensure Accessibility service has gesture permission."
            )
            serviceScope.launch {
                coordinator.statsRepository.recordGestureFailure(
                    appId = app.id,
                    displayName = app.displayName,
                    nowMs = System.currentTimeMillis()
                )
            }
        }
    }

    private fun getScreenDimensionsPx(): Pair<Int, Int> {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }

    private fun isDeviceLocked(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return false
        return km.isKeyguardLocked
    }

    override fun onInterrupt() {
        cancelPendingSwipe()
    }

    override fun onDestroy() {
        cancelPendingSwipe()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        activeInstance.compareAndSet(this, null)
        coordinator.onAccessibilityDisconnected()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private val activeInstance = AtomicReference<AutoSwipeAccessibilityService?>(null)

        fun getInstance(): AutoSwipeAccessibilityService? = activeInstance.get()
    }
}
