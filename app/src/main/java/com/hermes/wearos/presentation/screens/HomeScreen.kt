package com.hermes.wearos.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.hermes.wearos.R
import com.hermes.wearos.presentation.components.AskHermesButton
import com.hermes.wearos.presentation.components.IconoirIcon
import com.hermes.wearos.presentation.theme.HermesColors
import com.hermes.wearos.presentation.theme.HermesTypography
import com.hermes.wearos.presentation.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    onNavigateToChat: (inputMode: ChatInputMode) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(HermesColors.Background),
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Header (Pure White Text)
            item {
                Text(
                    text = "Hermes",
                    style = HermesTypography.title,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Main CTA 1 — Ask Hermes (Voice / STT)
            item {
                AskHermesButton(
                    onClick = { onNavigateToChat(ChatInputMode.VOICE) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Main CTA 2 — Ask with Keyboard
            item {
                Chip(
                    onClick = { onNavigateToChat(ChatInputMode.KEYBOARD) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant),
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_keyboard,
                            tint = HermesColors.PrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Ketik Pertanyaan",
                            style = HermesTypography.body
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Settings
            item {
                Chip(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Surface),
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_settings,
                            tint = HermesColors.OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Pengaturan",
                            style = HermesTypography.body
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
