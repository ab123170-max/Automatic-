package com.autoswipe.ai.studio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoswipe.ai.studio.BuildConfig
import com.autoswipe.ai.studio.ui.components.AutoSwipeLogo
import com.autoswipe.ai.studio.ui.components.NavyCard
import com.autoswipe.ai.studio.ui.theme.AutoSwipeColors

@Composable
fun HelpTroubleshootingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HelpFaqCard(
            question = "Why didn't AutoSwipe AI swipe when a video looped?",
            answer = "Short-video apps like TikTok and Instagram Reels often loop clips silently without exposing a time scrubber or Replay button to Android Accessibility. Rather than guessing and cutting off videos mid-play, AutoSwipe AI waits for genuine end-of-video UI signals and logs a timeout if none appear."
        )
        HelpFaqCard(
            question = "Accessibility Service keeps turning off automatically",
            answer = "On Android 10–16 devices from Samsung, Xiaomi, OnePlus, Oppo, or Vivo, battery managers may terminate accessibility services in the background. Open Permissions & Setup -> Battery Optimization and set AutoSwipe AI to Unrestricted, and lock the app in Recent Apps if your OEM supports it."
        )
        HelpFaqCard(
            question = "Android 13–16 'Restricted Setting' blocked Accessibility?",
            answer = "If you installed the APK manually (sideloaded) on Android 13+, Android protects Accessibility settings by default. Open Settings -> Apps -> AutoSwipe AI -> tap the top-right 3-dot menu -> 'Allow restricted settings', then return to Accessibility Settings."
        )
        HelpFaqCard(
            question = "Swiping triggers Android's Home gesture or opens Comments",
            answer = "AutoSwipe AI starts gestures safely above the bottom navigation bar. If an app's layout responds weakly or too strongly, adjust Swipe Sensitivity (Gentle / Balanced / Strong) and Swipe Duration in Swipe Settings."
        )
        HelpFaqCard(
            question = "Floating panel disappeared while scrolling",
            answer = "If you tapped '✕' on the floating panel, only the overlay was hidden while auto-scrolling kept running. Pull down your notification shade and tap 'Show Panel', or open AutoSwipe AI Dashboard."
        )
    }
}

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NavyCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AutoSwipeLogo(size = 56.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        "AutoSwipe AI",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AutoSwipeColors.ElectricCyan
                    )
                    Text(
                        "Package: ${BuildConfig.APPLICATION_ID}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        NavyCard {
            Text("Privacy & On-Device Design", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "• Zero servers, zero accounts, zero analytics tracking.\n" +
                    "• No screen recording, screenshots, or MediaProjection.\n" +
                    "• Inspects Accessibility UI nodes only when you explicitly activate the engine inside supported short-video apps.\n" +
                    "• All settings (DataStore) and usage statistics (Room) stay locally on your device.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        NavyCard {
            Text("Supported Android Versions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Built natively in Kotlin with Jetpack Compose and Material 3 for Android 8.0 (API 26) through Android 16 (API 36), including full Android 10 compatibility.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HelpFaqCard(question: String, answer: String) {
    NavyCard {
        Text(question, style = MaterialTheme.typography.titleMedium, color = AutoSwipeColors.ElectricCyan)
        Spacer(modifier = Modifier.height(6.dp))
        Text(answer, style = MaterialTheme.typography.bodyMedium)
    }
}
