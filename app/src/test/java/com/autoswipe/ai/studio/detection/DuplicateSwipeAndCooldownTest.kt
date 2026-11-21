package com.autoswipe.ai.studio.detection

import com.autoswipe.ai.studio.model.SupportedAppRegistry
import com.autoswipe.ai.studio.model.SwipeSettings
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateSwipeAndCooldownTest {

    private val settings = SwipeSettings(
        autoScrollEnabled = true,
        cooldownMs = 2200L,
        confidenceThreshold = 0.75f
    )

    private val tiktokApp = SupportedAppRegistry.builtInApps.first { it.id == "tiktok" }

    private fun replayNode(author: String) = AccessibilityNodeSnapshot(
        className = "androidx.viewpager.widget.ViewPager",
        viewIdResourceName = "com.zhiliaoapp.musically:id/feed_viewpager",
        children = listOf(
            AccessibilityNodeSnapshot(
                viewIdResourceName = "com.zhiliaoapp.musically:id/author",
                text = author
            ),
            AccessibilityNodeSnapshot(
                text = "Replay video",
                isClickable = true
            )
        )
    )

    @Test
    fun `prevents duplicate swipe on same video and enforces cooldown`() {
        val engine = VideoEndDetectionEngine()
        val node = replayNode("@creator_a")

        engine.evaluate(node, tiktokApp, settings, nowMs = 1_000L)
        val firstTrigger = engine.evaluate(node, tiktokApp, settings, nowMs = 3_000L)
        assertTrue(firstTrigger is EngineDecision.TriggerSwipe)

        val signature = (firstTrigger as EngineDecision.TriggerSwipe).signature
        engine.markSwipeDispatched(signature, nowMs = 3_800L, cooldownMs = settings.cooldownMs)

        // During cooldown window
        val inCooldown = engine.evaluate(node, tiktokApp, settings, nowMs = 4_500L)
        assertTrue(inCooldown is EngineDecision.InCooldown)

        // After cooldown, same video still cannot trigger a second swipe
        val duplicateBlocked = engine.evaluate(node, tiktokApp, settings, nowMs = 7_000L)
        assertTrue(duplicateBlocked is EngineDecision.AlreadySwiped)

        // When next video loads, engine resumes normally
        val nextVideo = replayNode("@creator_b")
        val nextMonitor = engine.evaluate(nextVideo, tiktokApp, settings, nowMs = 7_500L)
        assertTrue(nextMonitor is EngineDecision.Monitoring)
    }

    @Test
    fun `manual user scroll resets engine and applies override cooldown`() {
        val engine = VideoEndDetectionEngine()
        val node = replayNode("@creator_a")

        engine.evaluate(node, tiktokApp, settings, nowMs = 1_000L)
        engine.onUserManualScroll(nowMs = 2_000L, cooldownMs = 1500L)

        val duringManualCooldown = engine.evaluate(node, tiktokApp, settings, nowMs = 2_800L)
        assertTrue(duringManualCooldown is EngineDecision.InCooldown)

        // After cooldown, observation clock was reset by manual scroll so it monitors fresh
        val afterManualCooldown = engine.evaluate(node, tiktokApp, settings, nowMs = 3_600L)
        assertTrue(afterManualCooldown is EngineDecision.Monitoring)
    }
}
