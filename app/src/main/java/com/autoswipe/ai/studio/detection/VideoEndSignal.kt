package com.autoswipe.ai.studio.detection

enum class SignalType(val label: String) {
    REPLAY_TEXT("Replay / Watch Again text"),
    REPLAY_CONTROL("Replay UI control"),
    END_OVERLAY("End-of-video overlay"),
    PROGRESS_COMPLETED("Playback progress completed"),
    PROGRESS_WRAPPED_LOOP("Playback looped after reaching end"),
    PLAYBACK_STATE_COMPLETED("Player transitioned to ended state")
}

data class VideoEndSignal(
    val type: SignalType,
    val weight: Float,
    val evidence: String
)

data class PlatformEvaluation(
    val isShortVideoPlayerVisible: Boolean,
    val contextReason: String,
    val videoSignature: String?,
    val progressRatio: Float?,
    val signals: List<VideoEndSignal>,
    val isBlockingOverlayOpen: Boolean = false
)
