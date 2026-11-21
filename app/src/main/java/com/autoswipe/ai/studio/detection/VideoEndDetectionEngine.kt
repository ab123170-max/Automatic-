package com.autoswipe.ai.studio.detection

import com.autoswipe.ai.studio.model.SupportedApp
import com.autoswipe.ai.studio.model.SwipeSettings

sealed interface EngineDecision {
    data class NotInVideoContext(val reason: String) : EngineDecision
    data class InCooldown(val remainingMs: Long) : EngineDecision
    data class AlreadySwiped(val signature: String) : EngineDecision
    data class Monitoring(
        val signature: String?,
        val confidence: Float,
        val detail: String,
        val elapsedMs: Long
    ) : EngineDecision
    data class TriggerSwipe(
        val signature: String,
        val confidence: Float,
        val reason: String,
        val observedVideoDurationMs: Long
    ) : EngineDecision
    data class DetectionTimeout(
        val signature: String?,
        val elapsedMs: Long
    ) : EngineDecision
}

class VideoEndDetectionEngine {

    private var currentAppId: String? = null
    private var currentVideoSignature: String? = null
    private var videoFirstSeenMs: Long = 0L
    private var maxObservedProgress: Float = 0f
    private var lastObservedProgress: Float? = null
    private var hasWrappedLoopSignal: Boolean = false
    private var timeoutReportedForCurrentVideo: Boolean = false

    private var lastSwipedSignature: String? = null
    private var cooldownUntilMs: Long = 0L
    private var endSignalStreakCount: Int = 0

    fun resetAll() {
        currentAppId = null
        currentVideoSignature = null
        videoFirstSeenMs = 0L
        maxObservedProgress = 0f
        lastObservedProgress = null
        hasWrappedLoopSignal = false
        timeoutReportedForCurrentVideo = false
        lastSwipedSignature = null
        cooldownUntilMs = 0L
        endSignalStreakCount = 0
    }

    fun onUserManualScroll(nowMs: Long, cooldownMs: Long = 1500L) {
        currentVideoSignature = null
        videoFirstSeenMs = nowMs
        maxObservedProgress = 0f
        lastObservedProgress = null
        hasWrappedLoopSignal = false
        timeoutReportedForCurrentVideo = false
        endSignalStreakCount = 0
        cooldownUntilMs = maxOf(cooldownUntilMs, nowMs + cooldownMs)
    }

    fun markSwipeDispatched(signature: String, nowMs: Long, cooldownMs: Long) {
        lastSwipedSignature = signature
        cooldownUntilMs = nowMs + cooldownMs
        endSignalStreakCount = 0
        maxObservedProgress = 0f
        lastObservedProgress = null
        hasWrappedLoopSignal = false
    }

    fun evaluate(
        root: AccessibilityNodeSnapshot,
        app: SupportedApp,
        settings: SwipeSettings,
        nowMs: Long
    ): EngineDecision {
        if (currentAppId != app.id) {
            currentAppId = app.id
            currentVideoSignature = null
            videoFirstSeenMs = nowMs
            maxObservedProgress = 0f
            lastObservedProgress = null
            hasWrappedLoopSignal = false
            timeoutReportedForCurrentVideo = false
            endSignalStreakCount = 0
        }

        if (nowMs < cooldownUntilMs) {
            return EngineDecision.InCooldown(cooldownUntilMs - nowMs)
        }

        val strategy = DetectionStrategyFactory.forApp(app)
        val nodes = root.flatten()
        val evaluation = strategy.evaluate(root, nodes, settings)

        if (!evaluation.isShortVideoPlayerVisible) {
            return EngineDecision.NotInVideoContext(evaluation.contextReason)
        }

        if (evaluation.isBlockingOverlayOpen) {
            return EngineDecision.NotInVideoContext("Comments or share sheet open")
        }

        val resolvedSignature = evaluation.videoSignature
            ?: currentVideoSignature
            ?: "${app.id}_anon_${nowMs / 60_000L}"

        if (resolvedSignature != currentVideoSignature) {
            // New video detected -> reset tracking state cleanly
            currentVideoSignature = resolvedSignature
            videoFirstSeenMs = nowMs
            maxObservedProgress = 0f
            lastObservedProgress = null
            hasWrappedLoopSignal = false
            timeoutReportedForCurrentVideo = false
            endSignalStreakCount = 0
        }

        if (lastSwipedSignature != null && lastSwipedSignature == resolvedSignature) {
            return EngineDecision.AlreadySwiped(resolvedSignature)
        }

        val signals = evaluation.signals.toMutableList()
        val progress = evaluation.progressRatio

        if (settings.detectProgressCompletion && progress != null) {
            // Detect natural loop wrap: previously reached >= 88%, now dropped back below 15% on the same video
            val prev = lastObservedProgress
            if (maxObservedProgress >= 0.88f && prev != null && progress <= 0.15f && (prev - progress) >= 0.50f) {
                hasWrappedLoopSignal = true
            }
            if (hasWrappedLoopSignal) {
                signals += VideoEndSignal(
                    type = SignalType.PROGRESS_WRAPPED_LOOP,
                    weight = 0.88f,
                    evidence = "Progress looped after reaching ${(maxObservedProgress * 100).toInt()}%"
                )
            }
            maxObservedProgress = maxOf(maxObservedProgress, progress)
            lastObservedProgress = progress
        }

        val elapsedMs = (nowMs - videoFirstSeenMs).coerceAtLeast(0L)

        // Compute bounded multi-signal confidence (0f..1f)
        val rawScore = signals.sumOf { it.weight.toDouble() }.toFloat()
        val hasDirectEndSignal = signals.isNotEmpty()
        val confidence = if (hasDirectEndSignal) {
            rawScore.coerceIn(0f, 1f)
        } else {
            0f
        }

        // Minimum observation guard (avoid swiping within first 1200ms of seeing a video unless strong replay control exists)
        val minObserveMs = 1200L
        if (confidence >= settings.confidenceThreshold && elapsedMs >= minObserveMs) {
            endSignalStreakCount += 1
            // Require either very high confidence (>= 0.85) or 2 consecutive observations to avoid transient flicker
            if (confidence >= 0.85f || endSignalStreakCount >= 2) {
                val primaryReason = signals.joinToString("; ") { it.evidence }
                return EngineDecision.TriggerSwipe(
                    signature = resolvedSignature,
                    confidence = confidence,
                    reason = primaryReason,
                    observedVideoDurationMs = elapsedMs
                )
            }
        } else {
            endSignalStreakCount = 0
        }

        if (!timeoutReportedForCurrentVideo && elapsedMs >= settings.detectionTimeoutMs) {
            timeoutReportedForCurrentVideo = true
            return EngineDecision.DetectionTimeout(
                signature = resolvedSignature,
                elapsedMs = elapsedMs
            )
        }

        val detail = if (signals.isNotEmpty()) {
            signals.joinToString(" • ") { it.evidence }
        } else if (progress != null) {
            "${evaluation.contextReason} (${(progress * 100).toInt()}%)"
        } else {
            evaluation.contextReason
        }

        return EngineDecision.Monitoring(
            signature = resolvedSignature,
            confidence = confidence,
            detail = detail,
            elapsedMs = elapsedMs
        )
    }
}
