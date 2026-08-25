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
import com.hermes.wearos.data.models.NotificationPayload
import com.hermes.wearos.data.models.NotificationType
import com.hermes.wearos.presentation.components.IconoirIcon
import com.hermes.wearos.presentation.components.NotificationCard
import com.hermes.wearos.presentation.theme.HermesColors
import com.hermes.wearos.presentation.theme.HermesTypography

@Composable
fun NotificationsScreen(
    notifications: List<NotificationPayload>,
    onDismiss: () -> Unit,
    onNotificationTap: (NotificationPayload) -> Unit
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
                        id = R.drawable.ic_iconoir_bell,
                        tint = HermesColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Notifikasi",
                        style = HermesTypography.title,
                        color = HermesColors.Primary,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (notifications.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tidak ada notifikasi baru.",
                        style = HermesTypography.caption,
                        color = HermesColors.OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            items(notifications) { notification ->
                NotificationCard(
                    title = notification.title,
                    body = notification.body,
                    isCritical = notification.type == NotificationType.CRITICAL,
                    onClick = { onNotificationTap(notification) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                CompactChip(
                    onClick = onDismiss,
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
