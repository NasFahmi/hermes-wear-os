package com.hermes.wearos.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.hermes.wearos.R
import com.hermes.wearos.core.utils.DateUtils
import com.hermes.wearos.data.local.ChatMessageEntity
import com.hermes.wearos.presentation.components.IconoirIcon
import com.hermes.wearos.presentation.theme.HermesColors
import com.hermes.wearos.presentation.theme.HermesTypography

@Composable
fun RecentChatsScreen(
    messages: List<ChatMessageEntity>,
    onSelectMessage: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_notes,
                        tint = HermesColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Riwayat Chat",
                        style = HermesTypography.title,
                        color = HermesColors.Primary,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (messages.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum ada riwayat percakapan.",
                        style = HermesTypography.caption,
                        color = HermesColors.OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            items(messages) { message ->
                Card(
                    onClick = { onSelectMessage(message.message) },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = HermesColors.Surface,
                        endBackgroundColor = HermesColors.Surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = message.message,
                            style = HermesTypography.body,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!message.response.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = message.response,
                                style = HermesTypography.caption,
                                color = HermesColors.OnSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = DateUtils.formatRelativeTime(message.timestamp),
                            style = HermesTypography.caption,
                            color = HermesColors.PrimaryLight
                        )
                    }
                }
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
