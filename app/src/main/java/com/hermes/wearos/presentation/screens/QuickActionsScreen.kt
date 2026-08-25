package com.hermes.wearos.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.hermes.wearos.R
import com.hermes.wearos.presentation.components.IconoirIcon
import com.hermes.wearos.presentation.theme.HermesColors
import com.hermes.wearos.presentation.theme.HermesTypography
import com.hermes.wearos.presentation.viewmodel.MainViewModel

@Composable
fun QuickActionsScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    onSendQuickAction: (String) -> Unit
) {
    val listState = rememberScalingLazyListState()
    val quickActions = mainViewModel.getQuickActions()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Aksi Cepat",
                    style = HermesTypography.title,
                    color = HermesColors.Primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            items(quickActions) { action ->
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSendQuickAction(action.command) },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant),
                    label = {
                        Text(
                            text = action.label,
                            style = HermesTypography.body
                        )
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                CompactChip(
                    onClick = onBack,
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_arrow_left,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text("Kembali") },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
        }
    }
}
