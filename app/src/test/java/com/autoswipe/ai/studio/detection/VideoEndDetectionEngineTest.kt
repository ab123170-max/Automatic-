package com.autoswipe.ai.studio.detection

import com.autoswipe.ai.studio.model.SupportedAppRegistry
import com.autoswipe.ai.studio.model.SwipeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoEndDetectionEngineTest {

    private val settings = SwipeSettings(
        autoScrollEnabled = true,
        confidenceThreshold = 0.75f,
        detectionTimeoutMs = 30_000L
    )

    private val tiktokApp = SupportedAppRegistry.builtInApps.first { it.id == "tiktok" }
    private val youtubeApp = SupportedAppRegistry.builtInApps.first { it.id == "youtube_shorts" }
    private val instagramApp = SupportedAppRegistry.builtInApps.first { it.id == "instagram_reels" }

    private fun tiktokFeedNode(
        author: String = "@creator_one",
        replayText: String? = null,
        commentSheetOpen: Boolean = false
    ): AccessibilityNodeSnapshot {
        val children = mutableListOf(
            AccessibilityNodeSnapshot(
                className = "android.widget.TextView",
                viewIdResourceName = "com.zhiliaoapp.musically:id/title",
                text = author
            ),
            AccessibilityNodeSnapshot(
                className = "android.widget.ImageView",
                contentDescription = "Like video"
            )
        )
        if (replayText != null) {
            children += AccessibilityNodeSnapshot(
                className = "android.widget.Button",
                text = replayText,
                isClickable = true
            )
        }
        if (commentSheetOpen) {
            children += AccessibilityNodeSnapshot(
                className = "androidx.recyclerview.widget.RecyclerView",
                viewIdResourceName = "com.zhiliaoapp.musically:id/comment_list"
            )
        }
        return AccessibilityNodeSnapshot(
            className = "androidx.viewpager.widget.ViewPager",
            viewIdResourceName = "com.zhiliaoapp.musically:id/feed_viewpager",
            isScrollable = true,
            children = children
        )
    }

    @Test
    fun `monitors normally before minimum observation window and triggers on replay button`() {
        val engine = VideoEndDetectionEngine()
        val initialTree = tiktokFeedNode(author = "@alice")

        val d1 = engine.evaluate(initialTree, tiktokApp, settings, nowMs = 1_000L)
        assertTrue(d1 is EngineDecision.Monitoring)

        // Even with Replay appearing too early (< 1200ms), guard prevents premature swipe
        val earlyReplay = tiktokFeedNode(author = "@alice", replayText = "Watch again")
        val dEarly = engine.evaluate(earlyReplay, tiktokApp, settings, nowMs = 1_500L)
        assertTrue(dEarly is EngineDecision.Monitoring)

        // After observation window (>= 1200ms), clickable Replay triggers swipe
        val dTrigger = engine.evaluate(earlyReplay, tiktokApp, settings, nowMs = 3_200L)
        assertTrue(dTrigger is EngineDecision.TriggerSwipe)
        val trigger = dTrigger as EngineDecision.TriggerSwipe
        assertTrue(trigger.confidence >= settings.confidenceThreshold)
    }

    @Test
    fun `resets observation state when a new video signature appears`() {
        val engine = VideoEndDetectionEngine()
        engine.evaluate(tiktokFeedNode(author = "@first_video"), tiktokApp, settings, nowMs = 1_000L)

        // New video arrives at t = 4_000ms
        val secondDecision = engine.evaluate(
            tiktokFeedNode(author = "@second_video"),
            tiktokApp,
            settings,
            nowMs = 4_000L
        )
        assertTrue(secondDecision is EngineDecision.Monitoring)
        assertEquals(0L, (secondDecision as EngineDecision.Monitoring).elapsedMs)
    }

    @Test
    fun `suppresses swipe when comments overlay sheet is open`() {
        val engine = VideoEndDetectionEngine()
        engine.evaluate(tiktokFeedNode(author = "@alice"), tiktokApp, settings, nowMs = 1_000L)

        val withComments = tiktokFeedNode(
            author = "@alice",
            replayText = "Watch again",
            commentSheetOpen = true
        )
        val decision = engine.evaluate(withComments, tiktokApp, settings, nowMs = 5_000L)
        assertTrue(decision is EngineDecision.NotInVideoContext)
    }

    @Test
    fun `detects YouTube Shorts progress completion and loop wrap`() {
        val engine = VideoEndDetectionEngine()

        fun shortsNode(progress: Float): AccessibilityNodeSnapshot {
            return AccessibilityNodeSnapshot(
                className = "android.widget.FrameLayout",
                viewIdResourceName = "com.google.android.youtube:id/reel_watch_fragment_root",
                children = listOf(
                    AccessibilityNodeSnapshot(
                        className = "android.widget.TextView",
                        viewIdResourceName = "com.google.android.youtube:id/reel_main_title",
                        text = "How rockets land"
                    ),
                    AccessibilityNodeSnapshot(
                        className = "android.widget.SeekBar",
                        viewIdResourceName = "com.google.android.youtube:id/reel_progress_bar",
                        progressRange = ProgressRange(current = progress, min = 0f, max = 100f)
                    )
                )
            )
        }

        val dStart = engine.evaluate(shortsNode(15f), youtubeApp, settings, nowMs = 1_000L)
        assertTrue(dStart is EngineDecision.Monitoring)

        val dMid = engine.evaluate(shortsNode(92f), youtubeApp, settings, nowMs = 5_000L)
        assertTrue(dMid is EngineDecision.Monitoring)

        // Loop wraps back to 2% after having reached 92%
        val dWrapped = engine.evaluate(shortsNode(2f), youtubeApp, settings, nowMs = 6_200L)
        assertTrue(dWrapped is EngineDecision.TriggerSwipe)
    }

    @Test
    fun `reports detection timeout instead of guessing when looping silently`() {
        val engine = VideoEndDetectionEngine()
        val igReelsNoSignals = AccessibilityNodeSnapshot(
            className = "android.widget.FrameLayout",
            viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
            children = listOf(
                AccessibilityNodeSnapshot(
                    className = "android.widget.TextView",
                    viewIdResourceName = "com.instagram.android:id/clips_author_username",
                    text = "@nature_clips"
                )
            )
        )

        val d1 = engine.evaluate(igReelsNoSignals, instagramApp, settings, nowMs = 1_000L)
        assertTrue(d1 is EngineDecision.Monitoring)

        val dTimeout = engine.evaluate(igReelsNoSignals, instagramApp, settings, nowMs = 32_000L)
        assertTrue(dTimeout is EngineDecision.DetectionTimeout)
    }
}
