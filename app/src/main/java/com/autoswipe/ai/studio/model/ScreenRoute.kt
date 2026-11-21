package com.autoswipe.ai.studio.model

enum class ScreenRoute(val title: String, val subtitle: String) {
    ONBOARDING("Welcome Setup", "Guided permission & safety setup"),
    HOME_DASHBOARD("AutoSwipe AI", "Live engine status & controls"),
    SUPPORTED_APPS("Supported Applications", "Short-video platforms & detection capability"),
    DETECTION_SETTINGS("Detection Settings", "Video-end signals & confidence threshold"),
    SWIPE_SETTINGS("Swipe Settings", "Delay, gesture speed, sensitivity & cooldown"),
    FLOATING_CONTROLS("Floating Controls", "Overlay panel appearance & quick hide"),
    USAGE_STATISTICS("Usage Statistics", "Local swipe counts, sessions & reliability"),
    PERMISSIONS_SETUP("Permissions & Setup", "Accessibility, overlay & battery reliability"),
    HELP_TROUBLESHOOTING("Help & Troubleshooting", "Platform limits & device diagnostics"),
    ABOUT("About AutoSwipe AI", "Architecture, privacy & build details")
}
