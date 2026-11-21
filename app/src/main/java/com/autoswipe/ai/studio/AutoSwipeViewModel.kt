package com.autoswipe.ai.studio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoswipe.ai.studio.data.UsageStatsSummary
import com.autoswipe.ai.studio.model.EngineRuntimeState
import com.autoswipe.ai.studio.model.PermissionStatus
import com.autoswipe.ai.studio.model.SwipeSensitivity
import com.autoswipe.ai.studio.model.SwipeSettings
import com.autoswipe.ai.studio.service.EngineCoordinator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AutoSwipeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AutoSwipeApplication
    private val coordinator: EngineCoordinator = app.coordinator

    val settings: StateFlow<SwipeSettings> = coordinator.settingsState
    val runtime: StateFlow<EngineRuntimeState> = coordinator.runtimeState
    val permissions: StateFlow<PermissionStatus> = coordinator.permissionsState
    val stats: StateFlow<UsageStatsSummary> = coordinator.statsState

    fun refreshPermissions() {
        coordinator.refreshPermissions()
    }

    fun setAutoScrollEnabled(enabled: Boolean) {
        coordinator.setAutoScrollEnabled(enabled)
    }

    fun pauseOrResume() {
        if (runtime.value.isPaused) {
            coordinator.resumeEngine()
        } else {
            coordinator.pauseEngine()
        }
    }

    fun emergencyStop() {
        coordinator.emergencyStop()
    }

    fun showFloatingPanel() {
        coordinator.showFloatingPanelFromUserAction()
    }

    fun setFloatingPanelHidden(hidden: Boolean) {
        coordinator.setFloatingPanelHidden(hidden)
    }

    fun clearError() {
        coordinator.clearError()
    }

    fun updateSwipeDelay(delayMs: Long) {
        viewModelScope.launch { coordinator.settingsRepository.setSwipeDelayMs(delayMs) }
    }

    fun updateSwipeDuration(durationMs: Long) {
        viewModelScope.launch { coordinator.settingsRepository.setSwipeDurationMs(durationMs) }
    }

    fun updateSwipeSensitivity(sensitivity: SwipeSensitivity) {
        viewModelScope.launch { coordinator.settingsRepository.setSwipeSensitivity(sensitivity) }
    }

    fun updateCooldown(cooldownMs: Long) {
        viewModelScope.launch { coordinator.settingsRepository.setCooldownMs(cooldownMs) }
    }

    fun updateConfidenceThreshold(threshold: Float) {
        viewModelScope.launch { coordinator.settingsRepository.setConfidenceThreshold(threshold) }
    }

    fun updateDetectionTimeout(timeoutMs: Long) {
        viewModelScope.launch { coordinator.settingsRepository.setDetectionTimeoutMs(timeoutMs) }
    }

    fun updateSignalToggles(
        replayText: Boolean? = null,
        overlayControls: Boolean? = null,
        progressCompletion: Boolean? = null
    ) {
        viewModelScope.launch {
            coordinator.settingsRepository.setSignalToggles(
                replayText = replayText,
                overlayControls = overlayControls,
                progressCompletion = progressCompletion
            )
        }
    }

    fun updateFloatingEnabled(enabled: Boolean) {
        viewModelScope.launch { coordinator.settingsRepository.setFloatingPanelEnabled(enabled) }
    }

    fun updateFloatingAlpha(alpha: Float) {
        viewModelScope.launch { coordinator.settingsRepository.setFloatingPanelAlpha(alpha) }
    }

    fun updateFloatingExpandedByDefault(expanded: Boolean) {
        viewModelScope.launch {
            coordinator.settingsRepository.setFloatingPanelExpandedByDefault(expanded)
        }
    }

    fun updatePauseOnScreenOff(pause: Boolean) {
        viewModelScope.launch { coordinator.settingsRepository.setPauseOnScreenOff(pause) }
    }

    fun setAppEnabled(appId: String, enabled: Boolean) {
        viewModelScope.launch { coordinator.settingsRepository.setAppEnabled(appId, enabled) }
    }

    fun addCustomPackage(packageName: String) {
        viewModelScope.launch { coordinator.settingsRepository.addCustomPackage(packageName) }
    }

    fun removeCustomPackage(packageName: String) {
        viewModelScope.launch { coordinator.settingsRepository.removeCustomPackage(packageName) }
    }

    fun completeOnboarding(startEngineImmediately: Boolean) {
        viewModelScope.launch {
            coordinator.settingsRepository.setOnboardingCompleted(true)
            if (startEngineImmediately) {
                coordinator.setAutoScrollEnabled(true)
            }
        }
    }

    fun resetSettingsToDefault() {
        viewModelScope.launch {
            coordinator.settingsRepository.resetDefaultsPreservingOnboarding()
        }
    }

    fun resetUsageStats() {
        viewModelScope.launch {
            coordinator.statsRepository.clearAll()
        }
    }
}
