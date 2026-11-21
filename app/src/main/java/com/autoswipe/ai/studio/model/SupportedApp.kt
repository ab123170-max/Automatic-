package com.autoswipe.ai.studio.model

enum class DetectionSupportLevel(val label: String) {
    FULL_ACCESSIBILITY("Accessibility Signals + Progress"),
    PARTIAL_LOOP_LIMITED("Replay / End-Screen Signals"),
    EXPERIMENTAL_GENERIC("Best-Effort Generic")
}

data class SupportedApp(
    val id: String,
    val displayName: String,
    val packageNames: List<String>,
    val supportLevel: DetectionSupportLevel,
    val isVideoEndDetectionSupported: Boolean,
    val limitationsNote: String,
    val isCustom: Boolean = false
)

object SupportedAppRegistry {

    val builtInApps: List<SupportedApp> = listOf(
        SupportedApp(
            id = "tiktok",
            displayName = "TikTok",
            packageNames = listOf(
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.trill"
            ),
            supportLevel = DetectionSupportLevel.PARTIAL_LOOP_LIMITED,
            isVideoEndDetectionSupported = true,
            limitationsNote = "TikTok loops most feed videos silently without exposing a time progress bar to Android Accessibility. Detection relies on end-of-video overlays, Replay/Watch again prompts, and player control state changes. Continuously looping clips with no UI change will time out rather than guessing."
        ),
        SupportedApp(
            id = "tiktok_lite",
            displayName = "TikTok Lite",
            packageNames = listOf(
                "com.zhiliaoapp.musically.go",
                "com.ss.android.ugc.tiktok.lite"
            ),
            supportLevel = DetectionSupportLevel.PARTIAL_LOOP_LIMITED,
            isVideoEndDetectionSupported = true,
            limitationsNote = "TikTok Lite exposes a simplified view tree without playback timestamps. Detection triggers on Replay/Watch again overlays and end-card transitions when surfaced by the player."
        ),
        SupportedApp(
            id = "instagram_reels",
            displayName = "Instagram Reels",
            packageNames = listOf("com.instagram.android"),
            supportLevel = DetectionSupportLevel.PARTIAL_LOOP_LIMITED,
            isVideoEndDetectionSupported = true,
            limitationsNote = "Instagram Reels loops clips by default and rarely exposes scrubber progress in Accessibility. AutoSwipe AI activates only when inside Reels views and triggers on Replay/Watch again controls or end-of-reel overlays."
        ),
        SupportedApp(
            id = "youtube_shorts",
            displayName = "YouTube Shorts",
            packageNames = listOf("com.google.android.youtube"),
            supportLevel = DetectionSupportLevel.FULL_ACCESSIBILITY,
            isVideoEndDetectionSupported = true,
            limitationsNote = "YouTube Shorts often exposes SeekBar/ProgressBar range values and explicit Replay controls when playback completes. If a Short loops without resetting progress or showing a Replay button, AutoSwipe AI waits rather than swiping mid-video."
        ),
        SupportedApp(
            id = "facebook_reels",
            displayName = "Facebook Reels",
            packageNames = listOf(
                "com.facebook.katana",
                "com.facebook.lite"
            ),
            supportLevel = DetectionSupportLevel.PARTIAL_LOOP_LIMITED,
            isVideoEndDetectionSupported = true,
            limitationsNote = "Facebook Reels exposes Replay/Watch again buttons on completed reels in standard builds. Facebook Lite uses compressed canvas views that may hide playback controls, limiting detection."
        )
    )

    val allBuiltInPackageNames: Set<String> =
        builtInApps.flatMap { it.packageNames }.toSet()

    fun findByPackage(
        packageName: String?,
        customPackages: Set<String> = emptySet()
    ): SupportedApp? {
        if (packageName.isNullOrBlank()) return null
        val normalized = packageName.trim()
        builtInApps.firstOrNull { app ->
            app.packageNames.any { it.equals(normalized, ignoreCase = true) }
        }?.let { return it }

        if (customPackages.any { it.equals(normalized, ignoreCase = true) }) {
            return SupportedApp(
                id = "custom_$normalized",
                displayName = normalized.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                packageNames = listOf(normalized),
                supportLevel = DetectionSupportLevel.EXPERIMENTAL_GENERIC,
                isVideoEndDetectionSupported = true,
                limitationsNote = "Custom compatible package using generic accessibility Replay/End-of-video text and progress-bar heuristics. Reliability depends on what UI nodes the app exposes.",
                isCustom = true
            )
        }
        return null
    }

    fun getAllApps(customPackages: Set<String>): List<SupportedApp> {
        val customApps = customPackages
            .filter { pkg -> !allBuiltInPackageNames.contains(pkg) }
            .sorted()
            .mapNotNull { findByPackage(it, customPackages) }
        return builtInApps + customApps
    }
}
