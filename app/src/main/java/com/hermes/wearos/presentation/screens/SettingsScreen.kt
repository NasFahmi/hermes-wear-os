package com.hermes.wearos.presentation.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.hermes.wearos.R
import com.hermes.wearos.presentation.components.IconoirIcon
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
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val serverUrl by mainViewModel.serverUrl.collectAsState()
    val voiceLang by voiceViewModel.voiceLanguage.collectAsState()
    val isTtsMuted by chatViewModel.isTtsMuted.collectAsState()
    val uiState by mainViewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pengaturan",
                        style = HermesTypography.title.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Section 1: Default Audio / TTS Mute Toggle ─────────────
            item {
                Text(
                    text = "Suara Jawaban (TTS)",
                    style = HermesTypography.caption,
                    color = HermesColors.OnSurfaceVariant
                )
            }

            item {
                Chip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        chatViewModel.toggleMute()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .border(
                            1.dp,
                            if (!isTtsMuted) HermesColors.Primary.copy(alpha = 0.5f) else HermesColors.SurfaceBorder,
                            RoundedCornerShape(21.dp)
                        ),
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (isTtsMuted) HermesColors.SurfaceVariant else HermesColors.SurfaceElevated
                    ),
                    icon = {
                        IconoirIcon(
                            id = if (isTtsMuted) R.drawable.ic_iconoir_sound_off else R.drawable.ic_iconoir_sound_high,
                            tint = if (isTtsMuted) HermesColors.OnSurfaceVariant else HermesColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (isTtsMuted) "Senyap (Default)" else "Suara Aktif",
                            style = HermesTypography.body.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                )
            }

            // ── Section 2: Voice Language (STT) ───────────────────────
            item {
                Text(
                    text = "Bahasa Suara (STT)",
                    style = HermesTypography.caption,
                    color = HermesColors.OnSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(0.72f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            voiceViewModel.setVoiceLanguage("id-ID")
                        },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = "ID",
                                style = HermesTypography.button.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (voiceLang == "id-ID") HermesColors.Primary else HermesColors.SurfaceVariant
                        )
                    )
                    CompactChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            voiceViewModel.setVoiceLanguage("en-US")
                        },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = "EN",
                                style = HermesTypography.button.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (voiceLang == "en-US") HermesColors.Primary else HermesColors.SurfaceVariant
                        )
                    )
                }
            }

            // ── Section 3: Server URL & Test Connection ───────────────
            item {
                Text(
                    text = "Server Hermes",
                    style = HermesTypography.caption,
                    color = HermesColors.OnSurfaceVariant
                )
            }

            item {
                Card(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, HermesColors.SurfaceBorder, RoundedCornerShape(12.dp)),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = HermesColors.Surface,
                        endBackgroundColor = HermesColors.Surface
                    )
                ) {
                    Text(
                        text = serverUrl,
                        style = HermesTypography.caption,
                        color = HermesColors.OnSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            // Test Connection Button
            item {
                Chip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isTesting = true
                        testStatus = null
                        mainViewModel.refreshAll()
                        testStatus = if (uiState.isConnected) "Terhubung" else "Gagal terhubung"
                        isTesting = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .border(1.dp, HermesColors.SurfaceBorder, RoundedCornerShape(20.dp)),
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant),
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_search,
                            tint = HermesColors.PrimaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (isTesting) "Menguji..." else "Tes Koneksi Server",
                            style = HermesTypography.caption.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                )
            }

            if (testStatus != null) {
                item {
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
                            style = HermesTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isOk) HermesColors.StatusOnline else HermesColors.Error
                        )
                    }
                }
            }

            // Back Button
            item {
                CompactChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBack()
                    },
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
