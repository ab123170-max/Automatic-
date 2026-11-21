package com.autoswipe.ai.studio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AutoSwipeColors {
    val DeepNavy = Color(0xFF070D19)
    val NavySurface = Color(0xFF0E182C)
    val NavyElevated = Color(0xFF15223C)
    val NavyOutline = Color(0xFF243554)
    val ElectricBlue = Color(0xFF2979FF)
    val ElectricCyan = Color(0xFF38BDF8)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val Emerald = Color(0xFF10B981)
    val Amber = Color(0xFFF59E0B)
    val CoralRed = Color(0xFFEF4444)
    val RedContainer = Color(0xFF3B151A)
}

private val DarkColorScheme = darkColorScheme(
    primary = AutoSwipeColors.ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF10347A),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = AutoSwipeColors.ElectricCyan,
    onSecondary = AutoSwipeColors.DeepNavy,
    secondaryContainer = AutoSwipeColors.NavyElevated,
    onSecondaryContainer = AutoSwipeColors.TextPrimary,
    background = AutoSwipeColors.DeepNavy,
    onBackground = AutoSwipeColors.TextPrimary,
    surface = AutoSwipeColors.NavySurface,
    onSurface = AutoSwipeColors.TextPrimary,
    surfaceVariant = AutoSwipeColors.NavyElevated,
    onSurfaceVariant = AutoSwipeColors.TextSecondary,
    outline = AutoSwipeColors.NavyOutline,
    error = AutoSwipeColors.CoralRed,
    onError = Color.White,
    errorContainer = AutoSwipeColors.RedContainer,
    onErrorContainer = Color(0xFFFECACA)
)

private val AutoSwipeTypography = Typography(
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
        color = AutoSwipeColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold,
        color = AutoSwipeColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = AutoSwipeColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = AutoSwipeColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
        color = AutoSwipeColors.TextSecondary
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    ),
    labelMedium = TextStyle(
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Medium,
        color = AutoSwipeColors.TextSecondary
    )
)

private val AutoSwipeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun AutoSwipeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AutoSwipeTypography,
        shapes = AutoSwipeShapes,
        content = content
    )
}
