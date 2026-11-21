package com.autoswipe.ai.studio.service

import com.autoswipe.ai.studio.detection.SwipeGestureCalculator
import com.autoswipe.ai.studio.model.EnginePhase
import com.autoswipe.ai.studio.model.EngineRuntimeState
import com.autoswipe.ai.studio.model.PermissionStatus
import com.autoswipe.ai.studio.model.SwipeSensitivity
import com.autoswipe.ai.studio.model.SwipeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionAndGestureTest {

    @Test
    fun `parses accessibility enabled services setting accurately`() {
        val flattened = "com.autoswipe.ai.studio/com.autoswipe.ai.studio.service.AutoSwipeAccessibilityService"
        val shortName = "com.autoswipe.ai.studio/.service.AutoSwipeAccessibilityService"

        assertTrue(
            PermissionHelper.isComponentListedInAccessibilitySetting(
                enabledServicesValue = "com.other/.Service:$flattened",
                expectedFlattened = flattened,
                expectedShort = shortName
            )
        )
        assertTrue(
            PermissionHelper.isComponentListedInAccessibilitySetting(
                enabledServicesValue = shortName,
                expectedFlattened = flattened,
                expectedShort = shortName
            )
        )
        assertFalse(
            PermissionHelper.isComponentListedInAccessibilitySetting(
                enabledServicesValue = "com.other/.Service",
                expectedFlattened = flattened,
                expectedShort = shortName
            )
        )
    }

    @Test
    fun `default swipe settings match required specification`() {
        val defaults = SwipeSettings()
        assertFalse(defaults.autoScrollEnabled)
        assertEquals(800L, defaults.swipeDelayMs)
        assertEquals(350L, defaults.swipeDurationMs)
        assertEquals(EnginePhase.OFF, EngineRuntimeState().phase)
    }

    @Test
    fun `permission status requires both accessibility and overlay for full readiness`() {
        assertFalse(PermissionStatus(isAccessibilityEnabled = true, isOverlayGranted = false).hasCorePermissions)
        assertFalse(PermissionStatus(isAccessibilityEnabled = false, isOverlayGranted = true).hasCorePermissions)
        assertTrue(PermissionStatus(isAccessibilityEnabled = true, isOverlayGranted = true).hasCorePermissions)
    }

    @Test
    fun `gesture calculator builds safe upward trajectory avoiding gesture nav bar`() {
        assertNull(SwipeGestureCalculator.buildUpwardSwipe(0, 1920, SwipeSensitivity.BALANCED, 350L))

        val spec = SwipeGestureCalculator.buildUpwardSwipe(
            screenWidthPx = 1080,
            screenHeightPx = 2400,
            sensitivity = SwipeSensitivity.BALANCED,
            durationMs = 350L
        )
        assertNotNull(spec)
        assertTrue(spec!!.startY > spec.endY)
        assertTrue(spec.startY <= 2400f * 0.86f)
        assertEquals(350L, spec.durationMs)
    }
}
