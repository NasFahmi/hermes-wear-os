package com.hermes.wearos.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun HermesWearTheme(
    content: @Composable () -> Unit
) {
    // Wear OS is always dark-themed for battery efficiency
    MaterialTheme(
        colors = hermesWearColorPalette(),
        content = content
    )
}

@Composable
fun hermesWearColorPalette() = androidx.wear.compose.material.Colors(
    primary = HermesColors.Primary,
    primaryVariant = HermesColors.PrimaryVariant,
    secondary = HermesColors.PrimaryLight,
    secondaryVariant = HermesColors.PrimaryLight,
    error = HermesColors.Error,
    onPrimary = HermesColors.OnPrimary,
    onSecondary = HermesColors.OnBackground,
    onError = HermesColors.OnPrimary,
    background = HermesColors.Background,
    onBackground = HermesColors.OnBackground,
    surface = HermesColors.Surface,
    onSurface = HermesColors.OnSurface,
)
