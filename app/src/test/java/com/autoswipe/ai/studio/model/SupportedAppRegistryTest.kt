package com.autoswipe.ai.studio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedAppRegistryTest {

    @Test
    fun `identifies all official supported short-video packages`() {
        assertEquals("tiktok", SupportedAppRegistry.findByPackage("com.zhiliaoapp.musically")?.id)
        assertEquals("tiktok", SupportedAppRegistry.findByPackage("com.ss.android.ugc.trill")?.id)
        assertEquals("tiktok_lite", SupportedAppRegistry.findByPackage("com.zhiliaoapp.musically.go")?.id)
        assertEquals("tiktok_lite", SupportedAppRegistry.findByPackage("com.ss.android.ugc.tiktok.lite")?.id)
        assertEquals("instagram_reels", SupportedAppRegistry.findByPackage("com.instagram.android")?.id)
        assertEquals("youtube_shorts", SupportedAppRegistry.findByPackage("com.google.android.youtube")?.id)
        assertEquals("facebook_reels", SupportedAppRegistry.findByPackage("com.facebook.katana")?.id)
        assertEquals("facebook_reels", SupportedAppRegistry.findByPackage("com.facebook.lite")?.id)
    }

    @Test
    fun `rejects unrelated applications and self package`() {
        assertNull(SupportedAppRegistry.findByPackage("com.android.chrome"))
        assertNull(SupportedAppRegistry.findByPackage("com.android.settings"))
        assertNull(SupportedAppRegistry.findByPackage("com.autoswipe.ai.studio"))
        assertNull(SupportedAppRegistry.findByPackage(null))
        assertNull(SupportedAppRegistry.findByPackage("   "))
    }

    @Test
    fun `supports custom compatible packages when added by user`() {
        val custom = setOf("com.example.shortreels")
        val resolved = SupportedAppRegistry.findByPackage("com.example.shortreels", custom)
        assertNotNull(resolved)
        assertTrue(resolved!!.isCustom)
        assertEquals(DetectionSupportLevel.EXPERIMENTAL_GENERIC, resolved.supportLevel)
    }
}
