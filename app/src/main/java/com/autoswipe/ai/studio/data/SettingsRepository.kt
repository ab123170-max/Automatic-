package com.autoswipe.ai.studio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autoswipe.ai.studio.model.SwipeSensitivity
import com.autoswipe.ai.studio.model.SwipeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.autoSwipeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "autoswipe_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val AUTO_SCROLL_ENABLED = booleanPreferencesKey("auto_scroll_enabled")
        val SWIPE_DELAY_MS = longPreferencesKey("swipe_delay_ms")
        val SWIPE_DURATION_MS = longPreferencesKey("swipe_duration_ms")
        val SWIPE_SENSITIVITY = stringPreferencesKey("swipe_sensitivity")
        val COOLDOWN_MS = longPreferencesKey("cooldown_ms")
        val DETECTION_TIMEOUT_MS = longPreferencesKey("detection_timeout_ms")
        val CONFIDENCE_THRESHOLD = floatPreferencesKey("confidence_threshold")
        val DETECT_REPLAY_TEXT = booleanPreferencesKey("detect_replay_text")
        val DETECT_OVERLAY_CONTROLS = booleanPreferencesKey("detect_overlay_controls")
        val DETECT_PROGRESS = booleanPreferencesKey("detect_progress")
        val FLOATING_PANEL_ENABLED = booleanPreferencesKey("floating_panel_enabled")
        val FLOATING_PANEL_ALPHA = floatPreferencesKey("floating_panel_alpha")
        val FLOATING_PANEL_EXPANDED = booleanPreferencesKey("floating_panel_expanded")
        val FLOATING_PANEL_HIDDEN = booleanPreferencesKey("floating_panel_hidden")
        val PAUSE_ON_SCREEN_OFF = booleanPreferencesKey("pause_on_screen_off")
        val DISABLED_APP_IDS = stringSetPreferencesKey("disabled_app_ids")
        val CUSTOM_PACKAGES = stringSetPreferencesKey("custom_packages")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val settingsFlow: Flow<SwipeSettings> = context.autoSwipeDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            SwipeSettings(
                autoScrollEnabled = prefs[Keys.AUTO_SCROLL_ENABLED] ?: false,
                swipeDelayMs = (prefs[Keys.SWIPE_DELAY_MS] ?: SwipeSettings.DEFAULT_SWIPE_DELAY_MS)
                    .coerceIn(SwipeSettings.MIN_SWIPE_DELAY_MS, SwipeSettings.MAX_SWIPE_DELAY_MS),
                swipeDurationMs = (prefs[Keys.SWIPE_DURATION_MS] ?: SwipeSettings.DEFAULT_SWIPE_DURATION_MS)
                    .coerceIn(SwipeSettings.MIN_SWIPE_DURATION_MS, SwipeSettings.MAX_SWIPE_DURATION_MS),
                swipeSensitivity = SwipeSensitivity.fromName(prefs[Keys.SWIPE_SENSITIVITY]),
                cooldownMs = (prefs[Keys.COOLDOWN_MS] ?: SwipeSettings.DEFAULT_COOLDOWN_MS)
                    .coerceIn(SwipeSettings.MIN_COOLDOWN_MS, SwipeSettings.MAX_COOLDOWN_MS),
                detectionTimeoutMs = (prefs[Keys.DETECTION_TIMEOUT_MS] ?: SwipeSettings.DEFAULT_DETECTION_TIMEOUT_MS)
                    .coerceIn(15_000L, 300_000L),
                confidenceThreshold = (prefs[Keys.CONFIDENCE_THRESHOLD] ?: SwipeSettings.DEFAULT_CONFIDENCE_THRESHOLD)
                    .coerceIn(0.50f, 0.95f),
                detectReplayTextSignals = prefs[Keys.DETECT_REPLAY_TEXT] ?: true,
                detectOverlayControls = prefs[Keys.DETECT_OVERLAY_CONTROLS] ?: true,
                detectProgressCompletion = prefs[Keys.DETECT_PROGRESS] ?: true,
                floatingPanelEnabled = prefs[Keys.FLOATING_PANEL_ENABLED] ?: true,
                floatingPanelAlpha = (prefs[Keys.FLOATING_PANEL_ALPHA] ?: 0.90f).coerceIn(0.45f, 1.0f),
                floatingPanelExpandedByDefault = prefs[Keys.FLOATING_PANEL_EXPANDED] ?: true,
                floatingPanelHidden = prefs[Keys.FLOATING_PANEL_HIDDEN] ?: false,
                pauseOnScreenOff = prefs[Keys.PAUSE_ON_SCREEN_OFF] ?: true,
                disabledAppIds = prefs[Keys.DISABLED_APP_IDS] ?: emptySet(),
                customCompatiblePackages = prefs[Keys.CUSTOM_PACKAGES] ?: emptySet(),
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
            )
        }

    suspend fun setAutoScrollEnabled(enabled: Boolean) {
        context.autoSwipeDataStore.edit { it[Keys.AUTO_SCROLL_ENABLED] = enabled }
    }

    suspend fun setSwipeDelayMs(delayMs: Long) {
        context.autoSwipeDataStore.edit {
            it[Keys.SWIPE_DELAY_MS] = delayMs.coerceIn(
                SwipeSettings.MIN_SWIPE_DELAY_MS,
                SwipeSettings.MAX_SWIPE_DELAY_MS
            )
        }
    }

    suspend fun setSwipeDurationMs(durationMs: Long) {
        context.autoSwipeDataStore.edit {
            it[Keys.SWIPE_DURATION_MS] = durationMs.coerceIn(
                SwipeSettings.MIN_SWIPE_DURATION_MS,
                SwipeSettings.MAX_SWIPE_DURATION_MS
            )
        }
    }

    suspend fun setSwipeSensitivity(sensitivity: SwipeSensitivity) {
        context.autoSwipeDataStore.edit { it[Keys.SWIPE_SENSITIVITY] = sensitivity.name }
    }

    suspend fun setCooldownMs(cooldownMs: Long) {
        context.autoSwipeDataStore.edit {
            it[Keys.COOLDOWN_MS] = cooldownMs.coerceIn(
                SwipeSettings.MIN_COOLDOWN_MS,
                SwipeSettings.MAX_COOLDOWN_MS
            )
        }
    }

    suspend fun setDetectionTimeoutMs(timeoutMs: Long) {
        context.autoSwipeDataStore.edit {
            it[Keys.DETECTION_TIMEOUT_MS] = timeoutMs.coerceIn(15_000L, 300_000L)
        }
    }

    suspend fun setConfidenceThreshold(threshold: Float) {
        context.autoSwipeDataStore.edit {
            it[Keys.CONFIDENCE_THRESHOLD] = threshold.coerceIn(0.50f, 0.95f)
        }
    }

    suspend fun setSignalToggles(
        replayText: Boolean? = null,
        overlayControls: Boolean? = null,
        progressCompletion: Boolean? = null
    ) {
        context.autoSwipeDataStore.edit { prefs ->
            replayText?.let { prefs[Keys.DETECT_REPLAY_TEXT] = it }
            overlayControls?.let { prefs[Keys.DETECT_OVERLAY_CONTROLS] = it }
            progressCompletion?.let { prefs[Keys.DETECT_PROGRESS] = it }
        }
    }

    suspend fun setFloatingPanelEnabled(enabled: Boolean) {
        context.autoSwipeDataStore.edit {
            it[Keys.FLOATING_PANEL_ENABLED] = enabled
            if (enabled) {
                it[Keys.FLOATING_PANEL_HIDDEN] = false
            }
        }
    }

    suspend fun setFloatingPanelAlpha(alpha: Float) {
        context.autoSwipeDataStore.edit {
            it[Keys.FLOATING_PANEL_ALPHA] = alpha.coerceIn(0.45f, 1.0f)
        }
    }

    suspend fun setFloatingPanelExpandedByDefault(expanded: Boolean) {
        context.autoSwipeDataStore.edit { it[Keys.FLOATING_PANEL_EXPANDED] = expanded }
    }

    suspend fun setFloatingPanelHidden(hidden: Boolean) {
        context.autoSwipeDataStore.edit { it[Keys.FLOATING_PANEL_HIDDEN] = hidden }
    }

    suspend fun setPauseOnScreenOff(pause: Boolean) {
        context.autoSwipeDataStore.edit { it[Keys.PAUSE_ON_SCREEN_OFF] = pause }
    }

    suspend fun setAppEnabled(appId: String, enabled: Boolean) {
        context.autoSwipeDataStore.edit { prefs ->
            val current = prefs[Keys.DISABLED_APP_IDS]?.toMutableSet() ?: mutableSetOf()
            if (enabled) current.remove(appId) else current.add(appId)
            prefs[Keys.DISABLED_APP_IDS] = current
        }
    }

    suspend fun addCustomPackage(packageName: String) {
        val cleaned = packageName.trim()
        if (cleaned.isEmpty() || !cleaned.contains('.')) return
        context.autoSwipeDataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            current.add(cleaned)
            prefs[Keys.CUSTOM_PACKAGES] = current
        }
    }

    suspend fun removeCustomPackage(packageName: String) {
        context.autoSwipeDataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            current.remove(packageName)
            prefs[Keys.CUSTOM_PACKAGES] = current
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.autoSwipeDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun resetDefaultsPreservingOnboarding() {
        context.autoSwipeDataStore.edit { prefs ->
            val onboarded = prefs[Keys.ONBOARDING_COMPLETED] ?: false
            prefs.clear()
            prefs[Keys.ONBOARDING_COMPLETED] = onboarded
        }
    }
}
