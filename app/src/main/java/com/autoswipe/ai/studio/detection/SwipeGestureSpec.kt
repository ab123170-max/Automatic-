package com.autoswipe.ai.studio.detection

import com.autoswipe.ai.studio.model.SwipeSensitivity
import com.autoswipe.ai.studio.model.SwipeSettings

data class SwipeGestureSpec(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val durationMs: Long
)

object SwipeGestureCalculator {
    fun buildUpwardSwipe(
        screenWidthPx: Int,
        screenHeightPx: Int,
        sensitivity: SwipeSensitivity,
        durationMs: Long
    ): SwipeGestureSpec? {
        if (screenWidthPx <= 0 || screenHeightPx <= 0) return null

        val safeDuration = durationMs.coerceIn(
            SwipeSettings.MIN_SWIPE_DURATION_MS,
            SwipeSettings.MAX_SWIPE_DURATION_MS
        )

        // Slightly offset from exact center to avoid center play/pause tap-zones if gesture cancels early
        val centerX = screenWidthPx * 0.52f

        // Keep startY well above Android 10–16 bottom gesture navigation bar (margin >= 12% of screen height)
        val rawStartY = screenHeightPx * sensitivity.startYRatio
        val rawEndY = screenHeightPx * sensitivity.endYRatio
        val maxBottomY = screenHeightPx * 0.86f
        val minTopY = screenHeightPx * 0.14f

        val startY = rawStartY.coerceIn(minTopY + 120f, maxBottomY)
        val endY = rawEndY.coerceIn(minTopY, startY - 120f)

        return SwipeGestureSpec(
            startX = centerX,
            startY = startY,
            endX = centerX,
            endY = endY,
            durationMs = safeDuration
        )
    }
}
