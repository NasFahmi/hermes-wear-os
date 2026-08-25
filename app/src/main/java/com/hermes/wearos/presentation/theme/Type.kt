package com.hermes.wearos.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object HermesTypography {

    // Title — 14sp (from PRD)
    val title: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = HermesColors.OnBackground
        )

    // Body — 12sp (from PRD)
    val body: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = HermesColors.OnSurface
        )

    // Caption — 10sp (from PRD)
    val caption: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = HermesColors.OnSurfaceVariant
        )

    // Button
    val button: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = HermesColors.OnPrimary
        )

    // Status value (e.g., CPU 21%)
    val statusValue: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = HermesColors.OnSurface
        )

    // Status label (e.g., "CPU", "RAM")
    val statusLabel: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = HermesColors.OnSurfaceVariant
        )

    // Notification title
    val notificationTitle: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = HermesColors.OnBackground
        )

    // Notification body
    val notificationBody: TextStyle
        @Composable
        get() = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = HermesColors.OnSurfaceVariant
        )
}
