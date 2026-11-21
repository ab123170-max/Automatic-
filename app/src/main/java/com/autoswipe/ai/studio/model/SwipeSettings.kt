package com.autoswipe.ai.studio.model

enum class SwipeSensitivity(
    val label: String,
    val startYRatio: Float,
    val endYRatio: Float,
    val description: String
) {
    GENTLE(
        label = "Gentle",
        startYRatio = 0.72f,
        endYRatio = 0.32f,
        description = "Shorter vertical travel (40% of screen height) for sensitive feeds."
    ),
    BALANCED(
        label = "Balanced",
        startYRatio = 0.78f,
        endYRatio = 0.22f,
        description = "Standard upward fling (56% of screen height) reliable across Android 10–16."
    ),
    STRONG(
        label = "Strong",
        startYRatio = 0.84f,
        endYRatio = 0.16f,
        description = "Long upward sweep (68% of screen height) for sticky video containers."
    );

    companion object {
        fun fromName(name: String?): SwipeSensitivity =
            entries.firstOrNull { it.name == name } ?: BALANCED
    }
}

data class SwipeSettings(
    val autoScrollEnabled: Boolean = false,
    val swipeDelayMs: Long = DEFAULT_SWIPE_DELAY_MS,
    val swipeDurationMs: Long = DEFAULT_SWIPE_DURATION_MS,
    val swipeSensitivity: SwipeSensitivity = SwipeSensitivity.BALANCED,
    val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    val detectionTimeoutMs: Long = DEFAULT_DETECTION_TIMEOUT_MS,
    val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,
    val detectReplayTextSignals: Boolean = true,
    val detectOverlayControls: Boolean = true,
    val detectProgressCompletion: Boolean = true,
    val floatingPanelEnabled: Boolean = true,
    val floatingPanelAlpha: Float = 0.90f,
    val floatingPanelExpandedByDefault: Boolean = true,
    val floatingPanelHidden: Boolean = false,
    val pauseOnScreenOff: Boolean = true,
    val disabledAppIds: Set<String> = emptySet(),
    val customCompatiblePackages: Set<String> = emptySet(),
    val onboardingCompleted: Boolean = false
) {
    fun isAppEnabled(app: SupportedApp): Boolean = !disabledAppIds.contains(app.id)

    companion object {
        const val DEFAULT_SWIPE_DELAY_MS = 800L
        const val DEFAULT_SWIPE_DURATION_MS = 350L
        const val DEFAULT_COOLDOWN_MS = 2200L
        const val DEFAULT_DETECTION_TIMEOUT_MS = 90_000L
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.75f

        const val MIN_SWIPE_DELAY_MS = 200L
        const val MAX_SWIPE_DELAY_MS = 5000L
        const val MIN_SWIPE_DURATION_MS = 180L
        const val MAX_SWIPE_DURATION_MS = 900L
        const val MIN_COOLDOWN_MS = 1000L
        const val MAX_COOLDOWN_MS = 10_000L
    }
}
