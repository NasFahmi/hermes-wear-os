package com.hermes.wearos.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.hermes.wearos.R
import com.hermes.wearos.core.utils.WearTextUtils
import com.hermes.wearos.presentation.components.IconoirIcon
import com.hermes.wearos.presentation.components.StatusCard
import com.hermes.wearos.presentation.theme.HermesColors
import com.hermes.wearos.presentation.theme.HermesTypography
import com.hermes.wearos.presentation.viewmodel.ChatViewModel
import com.hermes.wearos.presentation.viewmodel.MainViewModel
import com.hermes.wearos.presentation.viewmodel.VoiceViewModel

@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    chatViewModel: ChatViewModel,
    voiceViewModel: VoiceViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCron: () -> Unit,
    onNavigateToRecent: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val serverUrl by mainViewModel.serverUrl.collectAsState()
    val serverStatus by mainViewModel.serverStatus.collectAsState()
    val notifications by mainViewModel.notifications.collectAsState()
    val cronFeed by mainViewModel.cronFeed.collectAsState()
    val voiceLang by voiceViewModel.voiceLanguage.collectAsState()
    val uiState by mainViewModel.uiState.collectAsState()

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

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
            // Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_settings,
                        tint = HermesColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pengaturan",
                        style = HermesTypography.title,
                        color = HermesColors.Primary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }

            // ── Section 1: Server Status ──────────────────────────────
            item {
                Text(
                    text = "Status Server",
                    style = HermesTypography.caption,
                    color = HermesColors.OnSurfaceVariant
                )
            }

            item {
                Spacer(modifier = Modifier.height(2.dp))
                if (serverStatus != null) {
                    val status = serverStatus!!
                    StatusCard(
                        title = if (uiState.isConnected) "Online" else "Offline",
                        value = WearTextUtils.formatServerStatus(status.cpu, status.ram),
                        subtitle = if (status.disk > 0) "Disk ${status.disk.toInt()}%" else "Normal",
                        statusColor = if (uiState.isConnected) HermesColors.StatusOnline else HermesColors.StatusOffline
                    )
                } else {
                    Card(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = HermesColors.Surface,
                            endBackgroundColor = HermesColors.Surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconoirIcon(
                                id = R.drawable.ic_iconoir_server,
                                tint = if (uiState.isConnected) HermesColors.StatusOnline else HermesColors.StatusOffline,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isConnected) "Server Terhubung" else "Server Terputus",
                                style = HermesTypography.caption,
                                color = if (uiState.isConnected) HermesColors.StatusOnline else HermesColors.StatusOffline
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Section 2: Menu Pantauan & Riwayat ────────────────────
            item {
                Text(
                    text = "Pantauan & Riwayat",
                    style = HermesTypography.caption,
                    color = HermesColors.OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Notifications Submenu
            item {
                val notifCount = notifications.size
                val notifLabel = if (notifCount > 0) "Notifikasi ($notifCount)" else "Notifikasi"
                Chip(
                    onClick = onNavigateToNotifications,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Surface),
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_bell,
                            tint = HermesColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(notifLabel, style = HermesTypography.body) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Cron Feed Submenu
            item {
                val cronCount = cronFeed.size
                val cronLabel = if (cronCount > 0) "Cron Feed ($cronCount)" else "Cron Feed"
                Chip(
                    onClick = onNavigateToCron,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Surface),
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_clock,
                            tint = HermesColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(cronLabel, style = HermesTypography.body) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Recent Chats Submenu
            item {
                Chip(
                    onClick = onNavigateToRecent,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Surface),
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_notes,
                            tint = HermesColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Riwayat Chat", style = HermesTypography.body) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Section 3: Voice Language (STT) ───────────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_mic,
                        tint = HermesColors.OnSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Bahasa Suara (STT)",
                        style = HermesTypography.caption,
                        color = HermesColors.OnSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompactChip(
                        onClick = { voiceViewModel.setVoiceLanguage("id-ID") },
                        label = { Text("ID (Indonesia)") },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (voiceLang == "id-ID") HermesColors.Primary else HermesColors.SurfaceVariant
                        )
                    )
                    CompactChip(
                        onClick = { voiceViewModel.setVoiceLanguage("en-US") },
                        label = { Text("EN (English)") },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (voiceLang == "en-US") HermesColors.Primary else HermesColors.SurfaceVariant
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Section 4: Server URL & Test Connection ───────────────
            item {
                Text(
                    text = "Server URL",
                    style = HermesTypography.caption,
                    color = HermesColors.OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            item {
                Card(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = HermesColors.Surface,
                        endBackgroundColor = HermesColors.Surface
                    )
                ) {
                    Text(
                        text = serverUrl,
                        style = HermesTypography.caption,
                        color = HermesColors.OnSurfaceVariant,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Test Connection Button
            item {
                Chip(
                    onClick = {
                        isTesting = true
                        testStatus = null
                        mainViewModel.refreshAll()
                        testStatus = if (uiState.isConnected) "Terhubung" else "Gagal terhubung"
                        isTesting = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.PrimaryVariant),
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_search,
                            tint = HermesColors.OnPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (isTesting) "Menguji..." else "Tes Koneksi",
                            style = HermesTypography.button
                        )
                    }
                )
            }

            if (testStatus != null) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val isOk = testStatus == "Terhubung"
                        IconoirIcon(
                            id = if (isOk) R.drawable.ic_iconoir_check else R.drawable.ic_iconoir_warning,
                            tint = if (isOk) HermesColors.StatusOnline else HermesColors.Error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = testStatus!!,
                            style = HermesTypography.caption,
                            color = if (isOk) HermesColors.StatusOnline else HermesColors.Error
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // ── Section 5: Clear Cache / Chat History ────────────────
            item {
                Chip(
                    onClick = {
                        chatViewModel.clearHistory()
                        testStatus = "Histori dibersihkan"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Error.copy(alpha = 0.3f)),
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_trash,
                            tint = HermesColors.Error,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Hapus Histori Chat", style = HermesTypography.body) }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
