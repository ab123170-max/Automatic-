package com.autoswipe.ai.studio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.autoswipe.ai.studio.AutoSwipeApplication
import com.autoswipe.ai.studio.MainActivity
import com.autoswipe.ai.studio.R
import com.autoswipe.ai.studio.model.EnginePhase
import com.autoswipe.ai.studio.model.EngineRuntimeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AutoSwipeForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timerJob: Job? = null
    private var stateJob: Job? = null
    private var sessionStartMs: Long = 0L
    private var lastPersistedTimeMs: Long = 0L

    private val coordinator: EngineCoordinator
        get() = (application as AutoSwipeApplication).coordinator

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP, ACTION_EMERGENCY_STOP -> {
                if (intent.action == ACTION_EMERGENCY_STOP) {
                    coordinator.emergencyStop()
                }
                stopSelfCleanly()
                return START_NOT_STICKY
            }
            ACTION_PAUSE_RESUME -> {
                if (coordinator.runtimeState.value.isPaused) {
                    coordinator.resumeEngine()
                } else {
                    coordinator.pauseEngine("Paused from notification")
                }
            }
            ACTION_TOGGLE_PANEL -> {
                val currentlyHidden = coordinator.settingsState.value.floatingPanelHidden
                coordinator.setFloatingPanelHidden(!currentlyHidden)
            }
            else -> {
                if (sessionStartMs == 0L) {
                    sessionStartMs = System.currentTimeMillis()
                    lastPersistedTimeMs = sessionStartMs
                }
                promoteToForeground(coordinator.runtimeState.value)
                coordinator.onForegroundServiceStateChanged(true)
                startObservers()
            }
        }
        return START_STICKY
    }

    private fun promoteToForeground(state: EngineRuntimeState) {
        val notification = buildNotification(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startObservers() {
        if (stateJob == null) {
            stateJob = scope.launch {
                coordinator.runtimeState.collectLatest { state ->
                    if (!coordinator.settingsState.value.autoScrollEnabled) {
                        stopSelfCleanly()
                    } else {
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIFICATION_ID, buildNotification(state))
                    }
                }
            }
        }

        if (timerJob == null) {
            timerJob = scope.launch {
                while (isActive) {
                    delay(1000L)
                    val now = System.currentTimeMillis()
                    val state = coordinator.runtimeState.value
                    if (!state.isPaused && state.phase != EnginePhase.OFF) {
                        coordinator.updateActiveSessionMs(now - sessionStartMs)
                        if (now - lastPersistedTimeMs >= 10_000L) {
                            val delta = now - lastPersistedTimeMs
                            lastPersistedTimeMs = now
                            coordinator.statsRepository.recordActiveTimeDelta(delta)
                        }
                    } else {
                        lastPersistedTimeMs = now
                    }
                }
            }
        }
    }

    private fun buildNotification(state: EngineRuntimeState): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseLabel = if (state.isPaused) "Resume" else "Pause"
        val pauseIntent = PendingIntent.getService(
            this,
            101,
            Intent(this, AutoSwipeForegroundService::class.java).apply {
                action = ACTION_PAUSE_RESUME
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val panelLabel = if (coordinator.settingsState.value.floatingPanelHidden) "Show Panel" else "Hide Panel"
        val panelIntent = PendingIntent.getService(
            this,
            102,
            Intent(this, AutoSwipeForegroundService::class.java).apply {
                action = ACTION_TOGGLE_PANEL
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            103,
            Intent(this, AutoSwipeForegroundService::class.java).apply {
                action = ACTION_EMERGENCY_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appLabel = state.activeApp?.displayName ?: "Waiting for video app"
        val contentText = "${state.phase.label} • $appLabel • ${state.sessionSwipes} swipes"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_autoswipe)
            .setContentTitle("AutoSwipe AI Active")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\n${state.detectionDetail}"))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .addAction(0, pauseLabel, pauseIntent)
            .addAction(0, panelLabel, panelIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notification_channel_desc)
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun stopSelfCleanly() {
        val now = System.currentTimeMillis()
        val remainingDelta = (now - lastPersistedTimeMs).coerceAtLeast(0L)
        if (remainingDelta > 0L) {
            scope.launch {
                coordinator.statsRepository.recordActiveTimeDelta(remainingDelta)
            }
        }
        coordinator.onForegroundServiceStateChanged(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        coordinator.onForegroundServiceStateChanged(false)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "autoswipe_engine_channel"
        const val NOTIFICATION_ID = 4001

        const val ACTION_START = "com.autoswipe.ai.studio.ACTION_START"
        const val ACTION_STOP = "com.autoswipe.ai.studio.ACTION_STOP"
        const val ACTION_EMERGENCY_STOP = "com.autoswipe.ai.studio.ACTION_EMERGENCY_STOP"
        const val ACTION_PAUSE_RESUME = "com.autoswipe.ai.studio.ACTION_PAUSE_RESUME"
        const val ACTION_TOGGLE_PANEL = "com.autoswipe.ai.studio.ACTION_TOGGLE_PANEL"
    }
}
