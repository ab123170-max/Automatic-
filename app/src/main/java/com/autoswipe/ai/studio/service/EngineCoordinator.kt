package com.autoswipe.ai.studio.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.autoswipe.ai.studio.data.SettingsRepository
import com.autoswipe.ai.studio.data.StatsRepository
import com.autoswipe.ai.studio.data.UsageStatsSummary
import com.autoswipe.ai.studio.model.EnginePhase
import com.autoswipe.ai.studio.model.EngineRuntimeState
import com.autoswipe.ai.studio.model.PermissionStatus
import com.autoswipe.ai.studio.model.SupportedApp
import com.autoswipe.ai.studio.model.SwipeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EngineCoordinator(
    private val context: Context,
    val settingsRepository: SettingsRepository,
    val statsRepository: StatsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settingsState: StateFlow<SwipeSettings> = settingsRepository.settingsFlow
        .stateIn(scope, SharingStarted.Eagerly, SwipeSettings())

    val statsState: StateFlow<UsageStatsSummary> = statsRepository.summaryFlow
        .stateIn(scope, SharingStarted.Eagerly, UsageStatsSummary())

    private val _permissions = MutableStateFlow(PermissionHelper.evaluatePermissions(context))
    val permissionsState: StateFlow<PermissionStatus> = _permissions.asStateFlow()

    private val _runtimeState = MutableStateFlow(EngineRuntimeState())
    val runtimeState: StateFlow<EngineRuntimeState> = _runtimeState.asStateFlow()

    private var floatingControlManager: FloatingControlManager? = null

    init {
        scope.launch {
            statsState.collectLatest { summary ->
                _runtimeState.update { it.copy(totalSwipesAllTime = summary.totalSwipes) }
            }
        }

        scope.launch {
            settingsState.collectLatest { settings ->
                syncOverlayState(settings)
                if (!settings.autoScrollEnabled && _runtimeState.value.isForegroundServiceRunning) {
                    stopForegroundService()
                }
            }
        }
    }

    fun attachFloatingManager(manager: FloatingControlManager) {
        floatingControlManager = manager
        syncOverlayState(settingsState.value)
    }

    fun refreshPermissions(): PermissionStatus {
        val status = PermissionHelper.evaluatePermissions(context)
        _permissions.value = status
        val settings = settingsState.value

        if (!status.isOverlayGranted && _runtimeState.value.isFloatingOverlayVisible) {
            floatingControlManager?.hide()
            _runtimeState.update {
                it.copy(
                    isFloatingOverlayVisible = false,
                    lastError = if (settings.floatingPanelEnabled) {
                        "Overlay permission was revoked. Re-enable Display over other apps to show floating controls."
                    } else null
                )
            }
        }

        if (!status.isAccessibilityEnabled && settings.autoScrollEnabled) {
            scope.launch {
                settingsRepository.setAutoScrollEnabled(false)
            }
            _runtimeState.update {
                it.copy(
                    phase = EnginePhase.WAITING_PERMISSION,
                    isServiceConnected = false,
                    lastError = "Accessibility Service is turned off. Enable AutoSwipe AI in Accessibility settings."
                )
            }
            stopForegroundService()
        } else {
            syncOverlayState(settings)
        }
        return status
    }

    fun setAutoScrollEnabled(enabled: Boolean) {
        val perms = refreshPermissions()
        if (enabled && !perms.isAccessibilityEnabled) {
            _runtimeState.update {
                it.copy(
                    phase = EnginePhase.WAITING_PERMISSION,
                    lastError = "Enable AutoSwipe AI Accessibility Service before starting."
                )
            }
            return
        }

        scope.launch {
            settingsRepository.setAutoScrollEnabled(enabled)
            if (enabled) {
                statsRepository.recordSessionStarted()
                _runtimeState.update {
                    it.copy(
                        isPaused = false,
                        sessionSwipes = 0,
                        sessionActiveMs = 0L,
                        lastError = null,
                        phase = if (it.isServiceConnected) {
                            EnginePhase.WAITING_SUPPORTED_APP
                        } else {
                            EnginePhase.WAITING_PERMISSION
                        },
                        swipeStatusText = "Ready",
                        detectionDetail = "Waiting for supported short-video app"
                    )
                }
                startForegroundService()
            } else {
                _runtimeState.update {
                    it.copy(
                        isPaused = false,
                        phase = EnginePhase.OFF,
                        detectionConfidence = 0f,
                        detectionDetail = "Stopped by user",
                        swipeStatusText = "Stopped",
                        lastError = null
                    )
                }
                AutoSwipeAccessibilityService.getInstance()?.onEngineStoppedByUser()
                stopForegroundService()
            }
        }
    }

    fun pauseEngine(reason: String = "Paused by user") {
        if (!settingsState.value.autoScrollEnabled) return
        _runtimeState.update {
            it.copy(
                isPaused = true,
                phase = EnginePhase.PAUSED,
                detectionDetail = reason,
                swipeStatusText = "Paused"
            )
        }
        AutoSwipeAccessibilityService.getInstance()?.cancelPendingSwipe()
    }

    fun resumeEngine() {
        val perms = refreshPermissions()
        if (!perms.isAccessibilityEnabled) {
            _runtimeState.update {
                it.copy(
                    phase = EnginePhase.WAITING_PERMISSION,
                    lastError = "Accessibility Service disconnected. Please re-enable it in settings."
                )
            }
            return
        }
        if (!settingsState.value.autoScrollEnabled) {
            setAutoScrollEnabled(true)
            return
        }
        _runtimeState.update {
            it.copy(
                isPaused = false,
                phase = EnginePhase.WAITING_SUPPORTED_APP,
                detectionDetail = "Resumed monitoring",
                swipeStatusText = "Ready",
                lastError = null
            )
        }
        AutoSwipeAccessibilityService.getInstance()?.triggerReEvaluation()
    }

    fun emergencyStop() {
        AutoSwipeAccessibilityService.getInstance()?.onEngineStoppedByUser()
        scope.launch {
            settingsRepository.setAutoScrollEnabled(false)
        }
        _runtimeState.update {
            it.copy(
                isPaused = false,
                phase = EnginePhase.OFF,
                detectionConfidence = 0f,
                detectionDetail = "Emergency Stop activated",
                swipeStatusText = "Stopped",
                lastError = null
            )
        }
        stopForegroundService()
    }

    fun setFloatingPanelHidden(hidden: Boolean) {
        scope.launch {
            settingsRepository.setFloatingPanelHidden(hidden)
        }
    }

    fun showFloatingPanelFromUserAction() {
        val perms = refreshPermissions()
        if (!perms.isOverlayGranted) {
            _runtimeState.update {
                it.copy(lastError = "Grant 'Display over other apps' permission to show the floating panel.")
            }
            return
        }
        scope.launch {
            settingsRepository.setFloatingPanelEnabled(true)
            settingsRepository.setFloatingPanelHidden(false)
        }
    }

    fun onAccessibilityConnected() {
        _permissions.value = PermissionHelper.evaluatePermissions(context)
        val settings = settingsState.value
        _runtimeState.update {
            it.copy(
                isServiceConnected = true,
                lastError = null,
                phase = when {
                    !settings.autoScrollEnabled -> EnginePhase.OFF
                    it.isPaused -> EnginePhase.PAUSED
                    else -> EnginePhase.WAITING_SUPPORTED_APP
                }
            )
        }
        if (settings.autoScrollEnabled && !_runtimeState.value.isForegroundServiceRunning) {
            startForegroundService()
        }
        syncOverlayState(settings)
    }

    fun onAccessibilityDisconnected() {
        _permissions.value = PermissionHelper.evaluatePermissions(context)
        val wasRunning = settingsState.value.autoScrollEnabled
        _runtimeState.update {
            it.copy(
                isServiceConnected = false,
                phase = if (wasRunning) EnginePhase.ERROR else EnginePhase.OFF,
                lastError = if (wasRunning) {
                    "Accessibility service disconnected by Android or OEM battery manager. Re-enable AutoSwipe AI in Accessibility settings."
                } else null
            )
        }
        if (wasRunning) {
            scope.launch {
                settingsRepository.setAutoScrollEnabled(false)
            }
            stopForegroundService()
        }
    }

    fun onForegroundServiceStateChanged(running: Boolean) {
        _runtimeState.update { it.copy(isForegroundServiceRunning = running) }
    }

    fun updateActiveSessionMs(sessionMs: Long) {
        _runtimeState.update { it.copy(sessionActiveMs = sessionMs) }
    }

    fun updateRuntimeSnapshot(
        phase: EnginePhase? = null,
        isScreenLocked: Boolean? = null,
        activePackageName: String? = null,
        activeApp: SupportedApp? = null,
        isActiveAppEnabled: Boolean? = null,
        currentVideoSignature: String? = null,
        detectionConfidence: Float? = null,
        detectionDetail: String? = null,
        swipeStatusText: String? = null,
        incrementSessionSwipes: Boolean = false,
        lastError: String? = _runtimeState.value.lastError
    ) {
        _runtimeState.update { current ->
            current.copy(
                phase = phase ?: current.phase,
                isScreenLocked = isScreenLocked ?: current.isScreenLocked,
                activePackageName = activePackageName ?: current.activePackageName,
                activeApp = activeApp,
                isActiveAppEnabled = isActiveAppEnabled ?: current.isActiveAppEnabled,
                currentVideoSignature = currentVideoSignature ?: current.currentVideoSignature,
                detectionConfidence = detectionConfidence ?: current.detectionConfidence,
                detectionDetail = detectionDetail ?: current.detectionDetail,
                swipeStatusText = swipeStatusText ?: current.swipeStatusText,
                sessionSwipes = if (incrementSessionSwipes) current.sessionSwipes + 1 else current.sessionSwipes,
                lastError = lastError,
                lastUpdatedEpochMs = System.currentTimeMillis()
            )
        }
    }

    fun clearError() {
        _runtimeState.update { it.copy(lastError = null) }
    }

    private fun syncOverlayState(settings: SwipeSettings) {
        val manager = floatingControlManager ?: return
        val canShow = settings.floatingPanelEnabled &&
            !settings.floatingPanelHidden &&
            PermissionHelper.evaluatePermissions(context).isOverlayGranted &&
            (settings.autoScrollEnabled || _runtimeState.value.isServiceConnected)

        if (canShow) {
            manager.show(settings.floatingPanelAlpha, settings.floatingPanelExpandedByDefault)
            _runtimeState.update { it.copy(isFloatingOverlayVisible = true) }
        } else {
            manager.hide()
            _runtimeState.update { it.copy(isFloatingOverlayVisible = false) }
        }
    }

    private fun startForegroundService() {
        val intent = Intent(context, AutoSwipeForegroundService::class.java).apply {
            action = AutoSwipeForegroundService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            _runtimeState.update {
                it.copy(
                    phase = EnginePhase.ERROR,
                    lastError = "Unable to start foreground service: ${e.message ?: "Restricted by OS"}"
                )
            }
        }
    }

    private fun stopForegroundService() {
        val intent = Intent(context, AutoSwipeForegroundService::class.java).apply {
            action = AutoSwipeForegroundService.ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {
            // Service may not be running
        }
    }
}
