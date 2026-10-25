package com.example.deedtrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Agentic Lender brand navy (same as AL-Web). Used for fills with white text in both themes. */
val Navy = Color(0xFF14336E)

private val NavyLightDay = Color(0xFFE6ECF7)
private val NavyLightNight = Color(0xFF233A66)
private val SurfaceDay = Color.White
private val SurfaceNight = Color(0xFF1A2130)
private val BackgroundDay = Color(0xFFF6F7F9)
private val BackgroundNight = Color(0xFF10151F)

private val LocalDarkTheme = staticCompositionLocalOf { false }

/** True when the dark theme is showing. */
val isDarkTheme: Boolean
    @Composable @ReadOnlyComposable get() = LocalDarkTheme.current

/** Card / bar / screen surface (white in light mode). */
val SurfaceColor: Color
    @Composable @ReadOnlyComposable get() = if (LocalDarkTheme.current) SurfaceNight else SurfaceDay

val PageBackground: Color
    @Composable @ReadOnlyComposable get() = if (LocalDarkTheme.current) BackgroundNight else BackgroundDay

val NavyLight: Color
    @Composable @ReadOnlyComposable get() = if (LocalDarkTheme.current) NavyLightNight else NavyLightDay

val CardBorder: Color
    @Composable @ReadOnlyComposable get() = if (LocalDarkTheme.current) Color(0xFF2E3748) else Color(0xFFE2E5EA)

val MutedText: Color
    @Composable @ReadOnlyComposable get() = if (LocalDarkTheme.current) Color(0xFFA3ABB9) else Color(0xFF6B7280)

val SoftFill: Color
    @Composable @ReadOnlyComposable get() = if (LocalDarkTheme.current) Color(0xFF252D3D) else Color(0xFFF1F3F6)

val SuccessGreen: Color
    @Composable @ReadOnlyComposable get() = if (LocalDarkTheme.current) Color(0xFF81C784) else Color(0xFF2E7D32)

val SuccessBg: Color
    @Composable @ReadOnlyComposable get() = if (LocalDarkTheme.current) Color(0xFF1C3321) else Color(0xFFE8F5E9)

val ErrorRed: Color
    @Composable @ReadOnlyComposable get() = if (LocalDarkTheme.current) Color(0xFFEF9A9A) else Color(0xFFC62828)

val ErrorBg: Color
    @Composable @ReadOnlyComposable get() = if (LocalDarkTheme.current) Color(0xFF3B1F22) else Color(0xFFFFEBEE)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = NavyLightDay,
    onPrimaryContainer = Navy,
    secondary = Navy,
    background = BackgroundDay,
    onBackground = Color(0xFF111827),
    surface = SurfaceDay,
    onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceVariant = Color(0xFFF1F3F6),
    outline = Color(0xFFE2E5EA),
    outlineVariant = Color(0xFFE2E5EA),
    error = Color(0xFFC62828)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAFC6FF),
    onPrimary = Color(0xFF0B2350),
    primaryContainer = NavyLightNight,
    onPrimaryContainer = Color(0xFFDCE4F7),
    secondary = Color(0xFFAFC6FF),
    background = BackgroundNight,
    onBackground = Color(0xFFE5E7EB),
    surface = SurfaceNight,
    onSurface = Color(0xFFE5E7EB),
    onSurfaceVariant = Color(0xFFA3ABB9),
    surfaceVariant = Color(0xFF252D3D),
    outline = Color(0xFF2E3748),
    outlineVariant = Color(0xFF2E3748),
    error = Color(0xFFEF9A9A)
)

/** Agentic Lender look from the Figma wireframes. Follows the phone's light / dark setting. */
@Composable
fun DeedTrackTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
    }
}
