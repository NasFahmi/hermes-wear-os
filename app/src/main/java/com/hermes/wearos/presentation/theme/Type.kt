package com.hermes.wearos.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Taste-Skill Typography Tokens (Glanceable, High-Rhythm Wear OS Scale)
object HermesTypography {

    // Main App & Section Titles
    val title: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            color = HermesColors.OnBackground
        )

    // Body text for AI Answer & Prompts (Optimized for readability on circular displays)
    val body: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 17.5.sp,
            letterSpacing = 0.15.sp,
            color = HermesColors.OnSurface
        )

    // Captions & Subtitles
    val caption: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
            color = HermesColors.OnSurfaceVariant
        )

    // Button Labels
    val button: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            color = HermesColors.OnPrimary
        )

    // Status Numbers & Metrics
    val statusValue: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = HermesColors.OnSurface
        )

    // Status Labels
    val statusLabel: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
            color = HermesColors.OnSurfaceVariant
        )

    // Notification / Card Title
    val notificationTitle: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = HermesColors.OnBackground
        )

    // Notification / Card Body
    val notificationBody: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = HermesColors.OnSurfaceVariant
        )
}
