package com.autoswipe.ai.studio.detection

import com.autoswipe.ai.studio.model.SupportedApp
import com.autoswipe.ai.studio.model.SwipeSettings

interface PlatformDetectionStrategy {
    fun evaluate(
        root: AccessibilityNodeSnapshot,
        nodes: List<AccessibilityNodeSnapshot>,
        settings: SwipeSettings
    ): PlatformEvaluation
}

object DetectionStrategyFactory {
    fun forApp(app: SupportedApp): PlatformDetectionStrategy {
        return when (app.id) {
            "tiktok" -> TikTokDetectionStrategy(isLite = false)
            "tiktok_lite" -> TikTokDetectionStrategy(isLite = true)
            "instagram_reels" -> InstagramReelsDetectionStrategy()
            "youtube_shorts" -> YouTubeShortsDetectionStrategy()
            "facebook_reels" -> FacebookReelsDetectionStrategy()
            else -> GenericCompatibleStrategy()
        }
    }
}

internal object CommonSignalMatcher {

    val replayTextPhrases = listOf(
        "watch again",
        "replay video",
        "play again",
        "end of video",
        "video ended",
        "rewatch",
        "replay"
    )

    val blockingSheetKeywords = listOf(
        "add a comment",
        "comments",
        "share to",
        "send to",
        "save video",
        "report",
        "search"
    )

    fun extractAllText(node: AccessibilityNodeSnapshot): String {
        val t = node.text?.trim().orEmpty()
        val d = node.contentDescription?.trim().orEmpty()
        return when {
            t.isNotEmpty() && d.isNotEmpty() && !t.equals(d, ignoreCase = true) -> "$t | $d"
            t.isNotEmpty() -> t
            else -> d
        }
    }

    fun findMatchedReplayPhrase(text: String): String? {
        if (text.isBlank()) return null
        val lower = text.lowercase()
        for (phrase in replayTextPhrases) {
            if (lower == phrase || lower.contains(phrase)) {
                // Avoid matching unrelated sentences like "Watch again tomorrow" when very long
                if (lower.length <= 42) return phrase
            }
        }
        return null
    }

    fun hasReplayResourceId(viewId: String?): Boolean {
        if (viewId.isNullOrBlank()) return false
        val lower = viewId.lowercase()
        return lower.contains("replay") ||
            lower.contains("watch_again") ||
            lower.contains("rewatch") ||
            lower.contains("end_screen") ||
            lower.contains("video_end")
    }

    fun extractBestProgressRatio(nodes: List<AccessibilityNodeSnapshot>): Float? {
        var best: Float? = null
        for (node in nodes) {
            if (!node.isVisibleToUser) continue
            val ratio = node.progressRange?.ratio ?: continue
            val cls = node.className.lowercase()
            val id = node.viewIdResourceName?.lowercase().orEmpty()
            val isPlaybackBar = cls.contains("seekbar") ||
                cls.contains("progressbar") ||
                id.contains("progress") ||
                id.contains("seek") ||
                id.contains("scrubber") ||
                id.contains("timeline")
            if (isPlaybackBar) {
                best = if (best == null) ratio else maxOf(best, ratio)
            }
        }
        return best
    }

    fun collectCommonSignals(
        nodes: List<AccessibilityNodeSnapshot>,
        settings: SwipeSettings
    ): MutableList<VideoEndSignal> {
        val signals = mutableListOf<VideoEndSignal>()
        var foundReplayText = false
        var foundReplayControl = false
        var foundEndOverlay = false

        for (node in nodes) {
            if (!node.isVisibleToUser) continue
            val combined = extractAllText(node)
            val viewId = node.viewIdResourceName

            if (settings.detectReplayTextSignals && !foundReplayText) {
                val phrase = findMatchedReplayPhrase(combined)
                if (phrase != null) {
                    val weight = if (node.isClickable) 0.85f else 0.75f
                    signals += VideoEndSignal(
                        type = SignalType.REPLAY_TEXT,
                        weight = weight,
                        evidence = "Matched \"$phrase\""
                    )
                    foundReplayText = true
                }
            }

            if (settings.detectOverlayControls && !foundReplayControl && hasReplayResourceId(viewId)) {
                signals += VideoEndSignal(
                    type = SignalType.REPLAY_CONTROL,
                    weight = 0.85f,
                    evidence = "Replay control id: ${viewId?.substringAfterLast('/')}"
                )
                foundReplayControl = true
            }

            if (settings.detectOverlayControls && !foundEndOverlay) {
                val lower = combined.lowercase()
                if (lower.contains("video ended") || lower.contains("end of video") || lower.contains("up next")) {
                    signals += VideoEndSignal(
                        type = SignalType.END_OVERLAY,
                        weight = 0.80f,
                        evidence = "End overlay: $combined"
                    )
                    foundEndOverlay = true
                }
            }
        }

        if (settings.detectProgressCompletion) {
            val ratio = extractBestProgressRatio(nodes)
            if (ratio != null && ratio >= 0.97f) {
                signals += VideoEndSignal(
                    type = SignalType.PROGRESS_COMPLETED,
                    weight = 0.80f,
                    evidence = "Progress at ${(ratio * 100).toInt()}%"
                )
            }
        }

        return signals
    }

    fun detectBlockingSheet(nodes: List<AccessibilityNodeSnapshot>): Boolean {
        for (node in nodes) {
            if (!node.isVisibleToUser) continue
            val id = node.viewIdResourceName?.lowercase().orEmpty()
            if (id.contains("comment_list") || id.contains("comments_sheet") || id.contains("share_panel")) {
                return true
            }
            val text = extractAllText(node).lowercase()
            if (text.startsWith("comments (") || text == "add comment..." || text == "add a comment...") {
                return true
            }
        }
        return false
    }

    fun buildStableSignature(
        prefix: String,
        candidates: List<String>
    ): String? {
        val cleaned = candidates
            .map { it.trim() }
            .filter { it.length >= 2 }
            .filterNot { isVolatileCounterOrTime(it) }
            .distinct()
            .take(3)
        if (cleaned.isEmpty()) return null
        return "$prefix:${cleaned.joinToString("|").hashCode()}"
    }

    private fun isVolatileCounterOrTime(value: String): Boolean {
        val lower = value.lowercase()
        if (lower.matches(Regex("^\\d+[:/]\\d+.*"))) return true
        if (lower.matches(Regex("^[\\d,.]+[km]?$"))) return true
        if (replayTextPhrases.any { lower == it }) return true
        return false
    }
}

class TikTokDetectionStrategy(private val isLite: Boolean) : PlatformDetectionStrategy {
    override fun evaluate(
        root: AccessibilityNodeSnapshot,
        nodes: List<AccessibilityNodeSnapshot>,
        settings: SwipeSettings
    ): PlatformEvaluation {
        val blocking = CommonSignalMatcher.detectBlockingSheet(nodes)
        val hasFeedIndicators = nodes.any { node ->
            val id = node.viewIdResourceName?.lowercase().orEmpty()
            val desc = node.contentDescription?.lowercase().orEmpty()
            id.contains("viewpager") ||
                id.contains("feed") ||
                id.contains("video") ||
                desc.contains("like video") ||
                desc.contains("comment") ||
                desc.contains("share")
        }

        if (!hasFeedIndicators) {
            return PlatformEvaluation(
                isShortVideoPlayerVisible = false,
                contextReason = if (isLite) "Not in TikTok Lite video feed" else "Not in TikTok video feed",
                videoSignature = null,
                progressRatio = null,
                signals = emptyList(),
                isBlockingOverlayOpen = blocking
            )
        }

        val signatureCandidates = nodes.mapNotNull { node ->
            val id = node.viewIdResourceName?.lowercase().orEmpty()
            val text = node.text?.trim().orEmpty()
            val desc = node.contentDescription?.trim().orEmpty()
            when {
                id.contains("title") || id.contains("author") || id.contains("desc") || id.contains("caption") ->
                    text.ifEmpty { desc }
                desc.startsWith("@") || text.startsWith("@") -> text.ifEmpty { desc }
                else -> null
            }
        }

        val signature = CommonSignalMatcher.buildStableSignature(
            if (isLite) "tiktok_lite" else "tiktok",
            signatureCandidates
        )

        val signals = CommonSignalMatcher.collectCommonSignals(nodes, settings)
        val progress = CommonSignalMatcher.extractBestProgressRatio(nodes)

        return PlatformEvaluation(
            isShortVideoPlayerVisible = true,
            contextReason = if (isLite) "Monitoring TikTok Lite feed" else "Monitoring TikTok feed",
            videoSignature = signature,
            progressRatio = progress,
            signals = signals,
            isBlockingOverlayOpen = blocking
        )
    }
}

class InstagramReelsDetectionStrategy : PlatformDetectionStrategy {
    override fun evaluate(
        root: AccessibilityNodeSnapshot,
        nodes: List<AccessibilityNodeSnapshot>,
        settings: SwipeSettings
    ): PlatformEvaluation {
        val blocking = CommonSignalMatcher.detectBlockingSheet(nodes)
        val inReels = nodes.any { node ->
            val id = node.viewIdResourceName?.lowercase().orEmpty()
            val desc = node.contentDescription?.lowercase().orEmpty()
            val text = node.text?.lowercase().orEmpty()
            id.contains("clips") ||
                id.contains("reel") ||
                desc.contains("reel") ||
                text == "reels"
        }

        if (!inReels) {
            return PlatformEvaluation(
                isShortVideoPlayerVisible = false,
                contextReason = "Instagram open, but not inside Reels viewer",
                videoSignature = null,
                progressRatio = null,
                signals = emptyList(),
                isBlockingOverlayOpen = blocking
            )
        }

        val candidates = nodes.mapNotNull { node ->
            val id = node.viewIdResourceName?.lowercase().orEmpty()
            val text = CommonSignalMatcher.extractAllText(node)
            when {
                id.contains("clips_author") || id.contains("caption") || id.contains("username") -> text
                text.startsWith("Reel by ", ignoreCase = true) -> text
                else -> null
            }
        }

        val signature = CommonSignalMatcher.buildStableSignature("ig_reels", candidates)
        val signals = CommonSignalMatcher.collectCommonSignals(nodes, settings)
        val progress = CommonSignalMatcher.extractBestProgressRatio(nodes)

        return PlatformEvaluation(
            isShortVideoPlayerVisible = true,
            contextReason = "Monitoring Instagram Reels",
            videoSignature = signature,
            progressRatio = progress,
            signals = signals,
            isBlockingOverlayOpen = blocking
        )
    }
}

class YouTubeShortsDetectionStrategy : PlatformDetectionStrategy {
    override fun evaluate(
        root: AccessibilityNodeSnapshot,
        nodes: List<AccessibilityNodeSnapshot>,
        settings: SwipeSettings
    ): PlatformEvaluation {
        val blocking = CommonSignalMatcher.detectBlockingSheet(nodes)
        val inShorts = nodes.any { node ->
            val id = node.viewIdResourceName?.lowercase().orEmpty()
            val desc = node.contentDescription?.lowercase().orEmpty()
            id.contains("reel_watch") ||
                id.contains("shorts") ||
                id.contains("reel_player") ||
                desc.contains("dislike this video") ||
                desc.contains("remix this short")
        }

        if (!inShorts) {
            return PlatformEvaluation(
                isShortVideoPlayerVisible = false,
                contextReason = "YouTube open, waiting for Shorts player",
                videoSignature = null,
                progressRatio = null,
                signals = emptyList(),
                isBlockingOverlayOpen = blocking
            )
        }

        val candidates = nodes.mapNotNull { node ->
            val id = node.viewIdResourceName?.lowercase().orEmpty()
            val text = CommonSignalMatcher.extractAllText(node)
            when {
                id.contains("reel_main_title") ||
                    id.contains("channel_name") ||
                    id.contains("reel_channel") ||
                    id.contains("title") -> text
                text.startsWith("@") -> text
                else -> null
            }
        }

        val signature = CommonSignalMatcher.buildStableSignature("yt_shorts", candidates)
        val signals = CommonSignalMatcher.collectCommonSignals(nodes, settings)

        if (settings.detectOverlayControls) {
            val replayButtonNode = nodes.firstOrNull { node ->
                val id = node.viewIdResourceName?.lowercase().orEmpty()
                val desc = node.contentDescription?.lowercase().orEmpty()
                node.isVisibleToUser && (
                    id.contains("player_control_play_pause_replay_button") &&
                        desc.contains("replay")
                    )
            }
            if (replayButtonNode != null && signals.none { it.type == SignalType.REPLAY_CONTROL }) {
                signals += VideoEndSignal(
                    type = SignalType.REPLAY_CONTROL,
                    weight = 0.90f,
                    evidence = "YouTube Shorts Replay button visible"
                )
            }
        }

        val progress = CommonSignalMatcher.extractBestProgressRatio(nodes)

        return PlatformEvaluation(
            isShortVideoPlayerVisible = true,
            contextReason = "Monitoring YouTube Shorts",
            videoSignature = signature,
            progressRatio = progress,
            signals = signals,
            isBlockingOverlayOpen = blocking
        )
    }
}

class FacebookReelsDetectionStrategy : PlatformDetectionStrategy {
    override fun evaluate(
        root: AccessibilityNodeSnapshot,
        nodes: List<AccessibilityNodeSnapshot>,
        settings: SwipeSettings
    ): PlatformEvaluation {
        val blocking = CommonSignalMatcher.detectBlockingSheet(nodes)
        val inReels = nodes.any { node ->
            val id = node.viewIdResourceName?.lowercase().orEmpty()
            val text = CommonSignalMatcher.extractAllText(node).lowercase()
            id.contains("reel") ||
                text.contains("reels") ||
                text.contains("watch again") ||
                text.contains("like") && nodes.any { it.isScrollable }
        }

        if (!inReels) {
            return PlatformEvaluation(
                isShortVideoPlayerVisible = false,
                contextReason = "Facebook open, waiting for Reels player",
                videoSignature = null,
                progressRatio = null,
                signals = emptyList(),
                isBlockingOverlayOpen = blocking
            )
        }

        val candidates = nodes.mapNotNull { node ->
            val text = CommonSignalMatcher.extractAllText(node)
            val id = node.viewIdResourceName?.lowercase().orEmpty()
            if (id.contains("actor") || id.contains("title") || id.contains("caption")) text else null
        }

        val signature = CommonSignalMatcher.buildStableSignature("fb_reels", candidates)
        val signals = CommonSignalMatcher.collectCommonSignals(nodes, settings)
        val progress = CommonSignalMatcher.extractBestProgressRatio(nodes)

        return PlatformEvaluation(
            isShortVideoPlayerVisible = true,
            contextReason = "Monitoring Facebook Reels",
            videoSignature = signature,
            progressRatio = progress,
            signals = signals,
            isBlockingOverlayOpen = blocking
        )
    }
}

class GenericCompatibleStrategy : PlatformDetectionStrategy {
    override fun evaluate(
        root: AccessibilityNodeSnapshot,
        nodes: List<AccessibilityNodeSnapshot>,
        settings: SwipeSettings
    ): PlatformEvaluation {
        val blocking = CommonSignalMatcher.detectBlockingSheet(nodes)
        val hasScrollableFeed = nodes.any { it.isScrollable && it.isVisibleToUser }
        if (!hasScrollableFeed) {
            return PlatformEvaluation(
                isShortVideoPlayerVisible = false,
                contextReason = "No active scrollable feed detected",
                videoSignature = null,
                progressRatio = null,
                signals = emptyList(),
                isBlockingOverlayOpen = blocking
            )
        }

        val candidates = nodes.mapNotNull { node ->
            val t = node.text?.trim().orEmpty()
            if (t.length in 3..80) t else null
        }

        val signature = CommonSignalMatcher.buildStableSignature("generic", candidates)
        val signals = CommonSignalMatcher.collectCommonSignals(nodes, settings)
        val progress = CommonSignalMatcher.extractBestProgressRatio(nodes)

        return PlatformEvaluation(
            isShortVideoPlayerVisible = true,
            contextReason = "Monitoring compatible app (generic)",
            videoSignature = signature,
            progressRatio = progress,
            signals = signals,
            isBlockingOverlayOpen = blocking
        )
    }
}
