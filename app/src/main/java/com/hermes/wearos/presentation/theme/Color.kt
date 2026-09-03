package com.hermes.wearos.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Taste-Skill & UI/UX PRO MAX Design System Tokens for Wear OS AMOLED
object HermesColors {
    // Primary - Hermes Solar Amber & Gemini Warmth (Anti-Slop, High Polish)
    val Primary = Color(0xFFF59E0B)
    val PrimaryVariant = Color(0xFFD97706)
    val PrimaryLight = Color(0xFFFCD34D)
    val PrimaryDark = Color(0xFFB45309)

    // Vibrant Gradients for Wear OS Buttons & Orbs
    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
    )
    val PrimaryGlow = Color(0x33F59E0B)
    val ListeningGlow = Color(0x4DF59E0B)

    // Semantic Accents
    val Success = Color(0xFF10B981) // Emerald 500
    val Warning = Color(0xFFFBBF24) // Amber 400
    val Error = Color(0xFFEF4444)   // Red 500
    val ErrorSurface = Color(0x26EF4444)

    // Background & Surfaces (True OLED Black for Zero Battery Drain & Borderless Bezel)
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF111215)
    val SurfaceBorder = Color(0xFF22242B)
    val SurfaceVariant = Color(0xFF1B1C22)
    val SurfaceElevated = Color(0xFF262832)

    // Typography & Contrast (WCAG AAA / AA Compliant for Round Wear Displays)
    val OnPrimary = Color(0xFF000000)        // Maximum contrast on gold
    val OnBackground = Color(0xFFFFFFFF)     // 100% Crisp White
    val OnSurface = Color(0xFFF1F5F9)        // Slate 100
    val OnSurfaceVariant = Color(0xFFA1A1AA) // Zinc 400 (Contrast > 5.5:1)
    val OnSurfaceMuted = Color(0xFF71717A)   // Zinc 500

    // Status Dots
    val StatusOnline = Color(0xFF10B981)
    val StatusOffline = Color(0xFFEF4444)
    val StatusWarning = Color(0xFFFBBF24)
}
