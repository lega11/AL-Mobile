package com.example.deedtrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF14336E)
val NavyLight = Color(0xFFE6ECF7)
val PageBackground = Color(0xFFF6F7F9)
val CardBorder = Color(0xFFE2E5EA)
val MutedText = Color(0xFF6B7280)
val SoftFill = Color(0xFFF1F3F6)
val SuccessGreen = Color(0xFF2E7D32)
val SuccessBg = Color(0xFFE8F5E9)
val ErrorRed = Color(0xFFC62828)

private val AgenticLenderColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = NavyLight,
    onPrimaryContainer = Navy,
    secondary = Navy,
    background = PageBackground,
    surface = Color.White,
    onSurface = Color(0xFF111827),
    onSurfaceVariant = MutedText,
    surfaceVariant = SoftFill,
    outline = CardBorder,
    outlineVariant = CardBorder,
    error = ErrorRed
)

/** Agentic Lender look from the Figma wireframes (light, navy accent). */
@Composable
fun DeedTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AgenticLenderColors, content = content)
}
