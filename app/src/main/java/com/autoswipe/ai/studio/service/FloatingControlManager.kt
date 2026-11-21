package com.autoswipe.ai.studio.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.autoswipe.ai.studio.model.EnginePhase
import com.autoswipe.ai.studio.model.EngineRuntimeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingControlManager(
    private val context: Context,
    private val coordinator: EngineCoordinator
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var rootView: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var observeJob: Job? = null
    private var isExpanded: Boolean = true
    private var currentAlpha: Float = 0.90f

    private var statusDotView: View? = null
    private var headerTitleView: TextView? = null
    private var statusTextView: TextView? = null
    private var appTextView: TextView? = null
    private var detectionTextView: TextView? = null
    private var swipeTextView: TextView? = null
    private var expandedContentLayout: LinearLayout? = null
    private var startButton: TextView? = null
    private var pauseButton: TextView? = null
    private var stopButton: TextView? = null
    private var toggleExpandButton: TextView? = null
    private var hideButton: TextView? = null

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    @Synchronized
    fun show(alpha: Float, expandedByDefault: Boolean) {
        if (!Settings.canDrawOverlays(context)) {
            hide()
            return
        }
        currentAlpha = alpha.coerceIn(0.45f, 1.0f)

        if (rootView != null) {
            rootView?.alpha = currentAlpha
            return
        }

        isExpanded = expandedByDefault
        val view = buildPanelView()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(140)
        }

        try {
            windowManager.addView(view, params)
            rootView = view
            layoutParams = params
            startObservingState()
        } catch (_: Exception) {
            rootView = null
            layoutParams = null
        }
    }

    @Synchronized
    fun hide() {
        observeJob?.cancel()
        observeJob = null
        val view = rootView ?: return
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {
        }
        rootView = null
        layoutParams = null
    }

    @Synchronized
    fun onScreenConfigurationChanged() {
        val view = rootView ?: return
        val params = layoutParams ?: return
        val metrics = context.resources.displayMetrics
        params.x = params.x.coerceIn(0, (metrics.widthPixels - dp(120)).coerceAtLeast(0))
        params.y = params.y.coerceIn(dp(32), (metrics.heightPixels - dp(120)).coerceAtLeast(dp(32)))
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
        }
    }

    private fun startObservingState() {
        observeJob?.cancel()
        observeJob = scope.launch {
            coordinator.runtimeState.collectLatest { state ->
                val settings = coordinator.settingsState.value
                renderState(state, settings.autoScrollEnabled)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildPanelView(): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            alpha = currentAlpha
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E80B1426"))
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.parseColor("#662979FF"))
            }
            elevation = dp(8).toFloat()
        }

        // Drag handle / Header row
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val dot = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(9), dp(9)).apply {
                marginEnd = dp(6)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#64748B"))
            }
        }
        statusDotView = dot

        val title = TextView(context).apply {
            text = "AutoSwipe"
            setTextColor(Color.parseColor("#F1F5F9"))
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        headerTitleView = title

        val expandBtn = createMiniChipButton(if (isExpanded) "−" else "+", "#1E293B", "#93C5FD") {
            isExpanded = !isExpanded
            expandedContentLayout?.visibility = if (isExpanded) View.VISIBLE else View.GONE
            toggleExpandButton?.text = if (isExpanded) "−" else "+"
        }.apply {
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(24)).apply {
                marginStart = dp(6)
            }
        }
        toggleExpandButton = expandBtn

        val hideBtn = createMiniChipButton("✕", "#1E293B", "#CBD5E1") {
            coordinator.setFloatingPanelHidden(true)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(24)).apply {
                marginStart = dp(4)
            }
        }
        hideButton = hideBtn

        headerRow.addView(dot)
        headerRow.addView(title)
        headerRow.addView(expandBtn)
        headerRow.addView(hideBtn)

        // Expanded content
        val expanded = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (isExpanded) View.VISIBLE else View.GONE
            setPadding(0, dp(6), 0, 0)
        }

        statusTextView = createInfoRow(expanded, "Status: Idle")
        appTextView = createInfoRow(expanded, "App: None")
        detectionTextView = createInfoRow(expanded, "Detect: Standby")
        swipeTextView = createInfoRow(expanded, "Swipe: Off")

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }

        val startBtn = createActionButton("Start", "#1565C0", "#FFFFFF") {
            val enabled = coordinator.settingsState.value.autoScrollEnabled
            if (!enabled) {
                coordinator.setAutoScrollEnabled(true)
            } else if (coordinator.runtimeState.value.isPaused) {
                coordinator.resumeEngine()
            }
        }
        startButton = startBtn

        val pauseBtn = createActionButton("Pause", "#1E293B", "#38BDF8") {
            val state = coordinator.runtimeState.value
            if (state.isPaused) {
                coordinator.resumeEngine()
            } else {
                coordinator.pauseEngine()
            }
        }.apply {
            (layoutParams as? LinearLayout.LayoutParams)?.marginStart = dp(4)
        }
        pauseButton = pauseBtn

        val stopBtn = createActionButton("Stop", "#7F1D1D", "#FCA5A5") {
            coordinator.emergencyStop()
        }.apply {
            (layoutParams as? LinearLayout.LayoutParams)?.marginStart = dp(4)
        }
        stopButton = stopBtn

        buttonRow.addView(startBtn)
        buttonRow.addView(pauseBtn)
        buttonRow.addView(stopBtn)
        expanded.addView(buttonRow)
        expandedContentLayout = expanded

        container.addView(headerRow)
        container.addView(expanded)

        // Attach drag listener on container/header so user can reposition anywhere smoothly
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var dragging = false

        headerRow.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 6 || abs(dy) > 6) {
                        dragging = true
                    }
                    if (dragging) {
                        val metrics = context.resources.displayMetrics
                        params.x = (initialX + dx).coerceIn(0, (metrics.widthPixels - dp(80)).coerceAtLeast(0))
                        params.y = (initialY + dy).coerceIn(dp(24), (metrics.heightPixels - dp(80)).coerceAtLeast(dp(24)))
                        try {
                            windowManager.updateViewLayout(container, params)
                        } catch (_: Exception) {
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        // Tap on header toggles expand/collapse for effortless one-hand use
                        isExpanded = !isExpanded
                        expandedContentLayout?.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        toggleExpandButton?.text = if (isExpanded) "−" else "+"
                    }
                    true
                }
                else -> false
            }
        }

        return container
    }

    private fun createInfoRow(parent: LinearLayout, initialText: String): TextView {
        return TextView(context).apply {
            text = initialText
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 11f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(dp(196), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(2)
            }
        }.also { parent.addView(it) }
    }

    private fun createMiniChipButton(
        label: String,
        bgHex: String,
        fgHex: String,
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(fgHex))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgHex))
                cornerRadius = dp(8).toFloat()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createActionButton(
        label: String,
        bgHex: String,
        fgHex: String,
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(fgHex))
            setPadding(dp(8), dp(6), dp(8), dp(6))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(3)
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgHex))
                cornerRadius = dp(10).toFloat()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun renderState(state: EngineRuntimeState, autoScrollEnabled: Boolean) {
        val dotColor = when (state.phase) {
            EnginePhase.MONITORING, EnginePhase.VIDEO_END_DETECTED, EnginePhase.SWIPING -> "#10B981"
            EnginePhase.WAITING_DELAY, EnginePhase.COOLDOWN, EnginePhase.WAITING_SUPPORTED_APP -> "#38BDF8"
            EnginePhase.PAUSED, EnginePhase.UNSUPPORTED_APP -> "#F59E0B"
            EnginePhase.ERROR, EnginePhase.WAITING_PERMISSION -> "#EF4444"
            EnginePhase.OFF -> "#64748B"
        }

        (statusDotView?.background as? GradientDrawable)?.setColor(Color.parseColor(dotColor))
        headerTitleView?.text = if (isExpanded) {
            "AutoSwipe AI"
        } else {
            when {
                !autoScrollEnabled -> "Off"
                state.isPaused -> "Paused"
                else -> "${state.sessionSwipes} • ${state.activeApp?.displayName ?: "Ready"}"
            }
        }

        statusTextView?.text = "Status: ${state.phase.label}"
        appTextView?.text = "App: ${state.activeApp?.displayName ?: state.activePackageName?.substringAfterLast('.') ?: "None"}"
        detectionTextView?.text = "Detect: ${state.detectionDetail}"
        swipeTextView?.text = "Swipe: ${state.swipeStatusText} (${state.sessionSwipes})"

        val isRunning = autoScrollEnabled && !state.isPaused
        startButton?.alpha = if (isRunning) 0.45f else 1.0f
        pauseButton?.text = if (state.isPaused) "Resume" else "Pause"
        pauseButton?.alpha = if (autoScrollEnabled) 1.0f else 0.45f
        stopButton?.alpha = if (autoScrollEnabled) 1.0f else 0.55f
    }
}
