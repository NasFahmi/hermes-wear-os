package com.hermes.wearos.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.hermes.wearos.R
import com.hermes.wearos.presentation.theme.HermesColors
import com.hermes.wearos.presentation.theme.HermesTypography

// ── Iconoir Vector Icon Composable ─────────────────────────────────────────
@Composable
fun IconoirIcon(
    @DrawableRes id: Int,
    contentDescription: String? = null,
    modifier: Modifier = Modifier.size(18.dp),
    tint: Color = LocalContentColor.current
) {
    Icon(
        painter = painterResource(id = id),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

// ── Ask Hermes Button (FR-001 main CTA with Iconoir Mic) ───────────────────
@Composable
fun AskHermesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = HermesColors.Primary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconoirIcon(
                id = R.drawable.ic_iconoir_mic,
                tint = HermesColors.OnPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Ask Hermes",
                style = HermesTypography.button,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Send Button with Iconoir Send ──────────────────────────────────────────
@Composable
fun SendButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        shape = RoundedCornerShape(20.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = HermesColors.Primary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconoirIcon(
                id = R.drawable.ic_iconoir_send,
                tint = HermesColors.OnPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Kirim", style = HermesTypography.button)
        }
    }
}

// ── Edit Button with Iconoir Edit Pencil ──────────────────────────────────
@Composable
fun EditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompactChip(
        onClick = onClick,
        modifier = modifier,
        icon = {
            IconoirIcon(
                id = R.drawable.ic_iconoir_edit,
                modifier = Modifier.size(14.dp)
            )
        },
        label = { Text("Edit", style = HermesTypography.button) },
        colors = ChipDefaults.chipColors(
            backgroundColor = HermesColors.SurfaceVariant
        )
    )
}

// ── Retry Button with Iconoir Refresh ─────────────────────────────────────
@Composable
fun RetryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompactChip(
        onClick = onClick,
        modifier = modifier,
        icon = {
            IconoirIcon(
                id = R.drawable.ic_iconoir_refresh,
                modifier = Modifier.size(14.dp)
            )
        },
        label = { Text("Coba Lagi", style = HermesTypography.button) },
        colors = ChipDefaults.chipColors(
            backgroundColor = HermesColors.Warning
        )
    )
}

// ── Status Card (Server / CPU / RAM display) ────────────────────────────
@Composable
fun StatusCard(
    title: String,
    value: String,
    subtitle: String = "",
    statusColor: Color = HermesColors.StatusOnline,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = {},
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = HermesColors.Surface,
            endBackgroundColor = HermesColors.Surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconoirIcon(
                    id = R.drawable.ic_iconoir_server,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = HermesTypography.statusLabel,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = HermesTypography.statusValue.copy(color = statusColor),
                textAlign = TextAlign.Center
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = HermesTypography.caption,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Notification Card with Iconoir Bell / Warning ────────────────────────
@Composable
fun NotificationCard(
    title: String,
    body: String,
    isCritical: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = if (isCritical) HermesColors.Error.copy(alpha = 0.15f) else HermesColors.Surface,
            endBackgroundColor = HermesColors.Surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconoirIcon(
                        id = if (isCritical) R.drawable.ic_iconoir_warning else R.drawable.ic_iconoir_bell,
                        tint = if (isCritical) HermesColors.Error else HermesColors.Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = title,
                        style = HermesTypography.notificationTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = body,
                style = HermesTypography.notificationBody,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ketuk untuk melihat detail",
                style = HermesTypography.caption,
                color = HermesColors.Primary
            )
        }
    }
}

// ── Connection Status Indicator ──────────────────────────────────────────
@Composable
fun ConnectionIndicator(isConnected: Boolean) {
    val color = if (isConnected) HermesColors.StatusOnline else HermesColors.StatusOffline
    val text = if (isConnected) "Online" else "Offline"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = HermesTypography.caption,
            color = color
        )
    }
}

// ── Loading Indicator ─────────────────────────────────────────────────────
@Composable
fun HermesLoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            indicatorColor = HermesColors.Primary,
            trackColor = HermesColors.SurfaceVariant,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp)
        )
    }
}
