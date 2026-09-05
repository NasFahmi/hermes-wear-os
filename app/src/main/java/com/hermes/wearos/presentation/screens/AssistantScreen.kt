package com.hermes.wearos.presentation.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.hermes.wearos.R
import com.hermes.wearos.presentation.components.IconoirIcon
import com.hermes.wearos.presentation.theme.HermesColors
import com.hermes.wearos.presentation.theme.HermesTypography
import com.hermes.wearos.presentation.viewmodel.AssistantUiState
import com.hermes.wearos.presentation.viewmodel.ChatViewModel
import com.hermes.wearos.presentation.viewmodel.VoiceViewModel
import com.hermes.wearos.services.speech.SpeechRecognizerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AssistantInputMode {
    NONE,
    VOICE,
    KEYBOARD
}

@Composable
fun AssistantScreen(
    chatViewModel: ChatViewModel,
    voiceViewModel: VoiceViewModel,
    onNavigateToSettings: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val assistantState by chatViewModel.assistantState.collectAsState()
    val speechState by voiceViewModel.speechState.collectAsState()
    val haptic = LocalHapticFeedback.current

    var inputMode by remember { mutableStateOf(AssistantInputMode.NONE) }
    var currentText by remember { mutableStateOf("") }
    var directLiveText by remember { mutableStateOf("") }
    var directAutoRetryCount by remember { mutableIntStateOf(0) }

    // Auto-listen for wake phrases (Hello Hermes, etc.) when on Idle screen
    LaunchedEffect(assistantState, inputMode, speechState) {
        if (assistantState is AssistantUiState.Idle && inputMode == AssistantInputMode.NONE) {
            when (speechState) {
                is SpeechRecognizerManager.SpeechState.Idle -> {
                    delay(300)
                    voiceViewModel.startListening()
                }
                is SpeechRecognizerManager.SpeechState.Error -> {
                    // Back off briefly before restarting idle wake listener to avoid CPU/mic churning
                    delay(1500)
                    if (assistantState is AssistantUiState.Idle && inputMode == AssistantInputMode.NONE) {
                        voiceViewModel.startListening()
                    }
                }
                else -> {}
            }
        }
    }

    // Direct Mode lifecycle: cleanly start listening when entering DirectListening
    LaunchedEffect(assistantState) {
        if (assistantState is AssistantUiState.DirectListening) {
            directAutoRetryCount = 0
            directLiveText = ""
            voiceViewModel.stopListening()
            voiceViewModel.resetState()
            delay(150)
            voiceViewModel.startListening()
        }
    }

    // Direct Mode Debounce: Auto-send after 2.2 seconds of silence
    LaunchedEffect(directLiveText, assistantState) {
        if (assistantState is AssistantUiState.DirectListening && directLiveText.isNotBlank()) {
            delay(2200)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val queryToSend = directLiveText
            directLiveText = ""
            voiceViewModel.stopListening()
            voiceViewModel.resetState()
            chatViewModel.askDirectQuestion(queryToSend)
        }
    }

    // Sync speech recognized result & partial words in real-time
    LaunchedEffect(speechState, assistantState, inputMode) {
        val currentSpeech = speechState
        when {
            // Wake word detection on Idle
            assistantState is AssistantUiState.Idle && inputMode == AssistantInputMode.NONE -> {
                val recognized = when (currentSpeech) {
                    is SpeechRecognizerManager.SpeechState.Listening -> currentSpeech.partialText
                    is SpeechRecognizerManager.SpeechState.Result -> currentSpeech.text
                    else -> ""
                }
                if (recognized.isNotBlank()) {
                    val (isWake, remaining) = voiceViewModel.checkWakeWord(recognized)
                    if (isWake) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        voiceViewModel.stopListening()
                        voiceViewModel.resetState()
                        if (remaining.isNotBlank()) {
                            chatViewModel.askDirectQuestion(remaining)
                        } else {
                            directLiveText = ""
                            chatViewModel.startDirectListening()
                        }
                    }
                }
            }

            // Direct Mode STT Sync
            assistantState is AssistantUiState.DirectListening -> {
                when (currentSpeech) {
                    is SpeechRecognizerManager.SpeechState.Listening -> {
                        val partial = currentSpeech.partialText
                        if (partial.isNotBlank()) {
                            directLiveText = partial
                        }
                    }
                    is SpeechRecognizerManager.SpeechState.Result -> {
                        val text = currentSpeech.text
                        if (text.isNotBlank()) {
                            directLiveText = text
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            voiceViewModel.stopListening()
                            voiceViewModel.resetState()
                            chatViewModel.askDirectQuestion(text)
                            directLiveText = ""
                        }
                    }
                    is SpeechRecognizerManager.SpeechState.Error -> {
                        // If error occurred (silence timeout) but text was already partially captured, send it!
                        if (directLiveText.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val queryToSend = directLiveText
                            directLiveText = ""
                            voiceViewModel.stopListening()
                            voiceViewModel.resetState()
                            chatViewModel.askDirectQuestion(queryToSend)
                        } else if (currentSpeech.errorCode == 11 && directAutoRetryCount < 1) {
                            // Automatically rebind if system speech service was disconnected
                            directAutoRetryCount++
                            delay(300)
                            voiceViewModel.startListening()
                        }
                    }
                    else -> {}
                }
            }

            // Standard Voice Input Mode
            inputMode == AssistantInputMode.VOICE -> {
                when (currentSpeech) {
                    is SpeechRecognizerManager.SpeechState.Listening -> {
                        val partial = currentSpeech.partialText
                        if (partial.isNotBlank()) {
                            currentText = partial
                        }
                    }
                    is SpeechRecognizerManager.SpeechState.Result -> {
                        currentText = currentSpeech.text
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    else -> {}
                }
            }
        }
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HermesColors.Background)
        ) {
            Crossfade(
                targetState = Pair(inputMode, assistantState),
                animationSpec = tween(200),
                label = "screen_crossfade"
            ) { (mode, state) ->
                when {
                    // ── 1. Voice Input & Real-time STT Preview ─────────────────
                    mode == AssistantInputMode.VOICE -> {
                        VoiceInputSection(
                            speechState = speechState,
                            currentText = currentText,
                            onSend = { textToSend ->
                                if (textToSend.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    inputMode = AssistantInputMode.NONE
                                    voiceViewModel.resetState()
                                    chatViewModel.askQuestion(textToSend)
                                    currentText = ""
                                }
                            },
                            onEdit = { textToEdit ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentText = textToEdit
                                voiceViewModel.stopListening()
                                voiceViewModel.resetState()
                                inputMode = AssistantInputMode.KEYBOARD
                            },
                            onRetry = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentText = ""
                                voiceViewModel.startListening()
                            },
                            onCancel = {
                                voiceViewModel.stopListening()
                                voiceViewModel.resetState()
                                currentText = ""
                                inputMode = AssistantInputMode.NONE
                            }
                        )
                    }

                    // ── 2. Keyboard Input Mode ──────────────────────────────────
                    mode == AssistantInputMode.KEYBOARD -> {
                        key(currentText) {
                            KeyboardInputSection(
                                initialText = currentText,
                                onSend = { textToSend ->
                                    if (textToSend.isNotBlank()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        inputMode = AssistantInputMode.NONE
                                        currentText = ""
                                        chatViewModel.askQuestion(textToSend)
                                    }
                                },
                                onSwitchToVoice = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentText = ""
                                    inputMode = AssistantInputMode.VOICE
                                    voiceViewModel.startListening()
                                },
                                onCancel = {
                                    currentText = ""
                                    inputMode = AssistantInputMode.NONE
                                }
                            )
                        }
                    }

                    // ── 3. Assistant States: Loading, Answer, Error, Idle ────────
                    else -> {
                        when (state) {
                            is AssistantUiState.Loading -> {
                                AssistantLoadingSection(query = state.query)
                            }

                            is AssistantUiState.Streaming -> {
                                AssistantStreamingSection(
                                    query = state.query,
                                    currentText = state.currentText,
                                    onCancel = { chatViewModel.resetToIdle() }
                                )
                            }

                            is AssistantUiState.Answer -> {
                                AssistantAnswerSection(
                                    answerState = state,
                                    onToggleMute = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        chatViewModel.toggleMute()
                                    },
                                    onAskAgainVoice = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        chatViewModel.resetToIdle()
                                        currentText = ""
                                        inputMode = AssistantInputMode.VOICE
                                        voiceViewModel.startListening()
                                    },
                                    onAskAgainKeyboard = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        chatViewModel.resetToIdle()
                                        currentText = ""
                                        inputMode = AssistantInputMode.KEYBOARD
                                    },
                                    onClose = { chatViewModel.resetToIdle() }
                                )
                            }

                            is AssistantUiState.Error -> {
                                AssistantErrorSection(
                                    query = state.query,
                                    errorMessage = state.message,
                                    onRetry = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        chatViewModel.askQuestion(state.query)
                                    },
                                    onDismiss = { chatViewModel.resetToIdle() }
                                )
                            }

                            // ── Direct Mode States (Minimalist, Ephemeral, Large Plain Text) ──
                            is AssistantUiState.DirectListening -> {
                                DirectListeningSection(
                                    liveText = directLiveText,
                                    speechState = speechState,
                                    onRetry = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        directLiveText = ""
                                        voiceViewModel.stopListening()
                                        voiceViewModel.resetState()
                                        voiceViewModel.startListening()
                                    },
                                    onCancel = {
                                        voiceViewModel.stopListening()
                                        voiceViewModel.resetState()
                                        directLiveText = ""
                                        chatViewModel.resetToIdle()
                                    }
                                )
                            }

                            is AssistantUiState.DirectStreaming -> {
                                DirectStreamingSection(
                                    query = state.query,
                                    currentText = state.currentText,
                                    onCancel = { chatViewModel.resetToIdle() }
                                )
                            }

                            is AssistantUiState.DirectAnswer -> {
                                DirectAnswerSection(
                                    query = state.query,
                                    response = state.response,
                                    isMuted = state.isMuted,
                                    onToggleMute = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        chatViewModel.toggleMute()
                                    },
                                    onAskAgain = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        voiceViewModel.stopListening()
                                        voiceViewModel.resetState()
                                        directLiveText = ""
                                        chatViewModel.startDirectListening()
                                    },
                                    onClose = { chatViewModel.resetToIdle() }
                                )
                            }

                            is AssistantUiState.Idle -> {
                                AssistantIdleSection(
                                    onStartVoice = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentText = ""
                                        inputMode = AssistantInputMode.VOICE
                                        voiceViewModel.startListening()
                                    },
                                    onStartDirect = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        voiceViewModel.stopListening()
                                        voiceViewModel.resetState()
                                        directLiveText = ""
                                        chatViewModel.startDirectListening()
                                    },
                                    onStartKeyboard = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentText = ""
                                        inputMode = AssistantInputMode.KEYBOARD
                                    },
                                    onOpenSettings = onNavigateToSettings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 1. Idle Screen (Centered, Clean Vertical Ergonomics) ──────────────────
@Composable
private fun AssistantIdleSection(
    onStartVoice: () -> Unit,
    onStartDirect: () -> Unit,
    onStartKeyboard: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    val infiniteTransition = rememberInfiniteTransition(label = "idle_aura")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_scale"
    )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title
        item {
            Text(
                text = "Hermes",
                style = HermesTypography.title.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Central Mic Button with Breathing Aura
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(76.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(auraScale)
                        .clip(CircleShape)
                        .background(HermesColors.PrimaryGlow)
                )

                Button(
                    onClick = onStartVoice,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, HermesColors.PrimaryLight.copy(alpha = 0.6f), CircleShape),
                    colors = ButtonDefaults.buttonColors(backgroundColor = HermesColors.Primary)
                ) {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_mic,
                        tint = HermesColors.OnPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Action: Mode Direct (Hands-Free Speech AI)
        item {
            Chip(
                onClick = onStartDirect,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(40.dp)
                    .border(1.dp, HermesColors.Primary.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceElevated),
                icon = {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_sound_high,
                        tint = HermesColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = "Mode Direct",
                        style = HermesTypography.body.copy(fontWeight = FontWeight.SemiBold, color = HermesColors.PrimaryLight)
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Secondary Action: Keyboard Input Chip
        item {
            Chip(
                onClick = onStartKeyboard,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(40.dp)
                    .border(1.dp, HermesColors.SurfaceBorder, RoundedCornerShape(20.dp)),
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant),
                icon = {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_keyboard,
                        tint = HermesColors.PrimaryLight,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = "Ketik Pertanyaan",
                        style = HermesTypography.body.copy(fontWeight = FontWeight.Medium)
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Settings Access
        item {
            CompactChip(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(0.65f),
                icon = {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_settings,
                        tint = HermesColors.OnSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                },
                label = {
                    Text(
                        text = "Pengaturan",
                        style = HermesTypography.caption,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Surface)
            )
        }
    }
}

// ── 2. Voice Input Section (Pure Vertical, Zero Horizontal Flex) ─────────
@Composable
private fun VoiceInputSection(
    speechState: SpeechRecognizerManager.SpeechState,
    currentText: String,
    onSend: (String) -> Unit,
    onEdit: (String) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    val liveText = when (speechState) {
        is SpeechRecognizerManager.SpeechState.Listening -> {
            speechState.partialText.ifBlank { currentText }
        }
        is SpeechRecognizerManager.SpeechState.Result -> {
            speechState.text.ifBlank { currentText }
        }
        else -> currentText
    }

    val isListening = speechState is SpeechRecognizerManager.SpeechState.Listening
    val isError = speechState is SpeechRecognizerManager.SpeechState.Error

    val infiniteTransition = rememberInfiniteTransition(label = "listening_dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Kata kecil "Mendengarkan..." di atas
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(HermesColors.Primary.copy(alpha = dotAlpha))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                    text = if (isError) "Gagal mendeteksi" else "Mendengarkan...",
                    style = HermesTypography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = if (isError) HermesColors.Error else HermesColors.PrimaryLight,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Hasil Realtime dari STT (Plain text tanpa kotak box)
        item {
            Text(
                text = if (liveText.isNotBlank()) liveText else "Bicara sekarang...",
                style = HermesTypography.body.copy(
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = if (liveText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (liveText.isNotBlank()) HermesColors.OnBackground else HermesColors.OnSurfaceMuted,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        // Action Buttons (Pure Vertical Stack - No horizontal flex)
        if (liveText.isNotBlank()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Tombol Kirim Utama
            item {
                Button(
                    onClick = { onSend(liveText) },
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = HermesColors.Primary)
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

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Edit via Keyboard
            item {
                CompactChip(
                    onClick = { onEdit(liveText) },
                    modifier = Modifier.fillMaxWidth(0.88f),
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_edit,
                            tint = HermesColors.PrimaryLight,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Edit via Keyboard",
                            style = HermesTypography.caption.copy(fontWeight = FontWeight.Medium),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Ulangi Suara
            item {
                CompactChip(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(0.88f),
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_refresh,
                            tint = HermesColors.Warning,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Ulangi Suara",
                            style = HermesTypography.caption.copy(fontWeight = FontWeight.Medium),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Batal
            item {
                CompactChip(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(0.65f),
                    label = {
                        Text(
                            text = "Batal",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
        } else if (isError) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
            item {
                Text(
                    text = (speechState as SpeechRecognizerManager.SpeechState.Error).message,
                    style = HermesTypography.caption,
                    color = HermesColors.Error,
                    textAlign = TextAlign.Center
                )
            }
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
            item {
                CompactChip(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(0.85f),
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_refresh,
                            tint = HermesColors.Warning,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Coba Lagi",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
            item {
                CompactChip(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(0.65f),
                    label = {
                        Text(
                            text = "Kembali",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
        } else {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                CompactChip(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(0.65f),
                    label = {
                        Text(
                            text = "Batal",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
        }
    }
}

// ── 3. Keyboard Input Section (Pure Vertical Stack) ───────────────────────
@Composable
private fun KeyboardInputSection(
    initialText: String,
    onSend: (String) -> Unit,
    onSwitchToVoice: () -> Unit,
    onCancel: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var textFieldValue by remember(initialText) {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(60)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
    }

    val scope = rememberCoroutineScope()
    var awaitingImeCommit by remember { mutableStateOf(false) }

    val handleSend: (String) -> Unit = { text ->
        if (!awaitingImeCommit) {
            awaitingImeCommit = true
            val fallback = text.trim()
            keyboardController?.hide()
            focusManager.clearFocus()
            scope.launch {
                delay(180)
                val committed = textFieldValue.text.trim()
                val finalText = if (committed.length >= fallback.length) committed else fallback
                awaitingImeCommit = false
                if (finalText.isNotBlank()) {
                    onSend(finalText)
                }
            }
        }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconoirIcon(
                    id = R.drawable.ic_iconoir_keyboard,
                    tint = HermesColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ketik Pertanyaan",
                    style = HermesTypography.title.copy(fontSize = 14.sp),
                    color = HermesColors.Primary
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Text input field box
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(min = 50.dp, max = 80.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HermesColors.Surface)
                    .border(1.dp, HermesColors.Primary, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = "Tulis pertanyaan...",
                        style = HermesTypography.body,
                        color = HermesColors.OnSurfaceMuted
                    )
                }
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = false,
                    maxLines = 3,
                    textStyle = HermesTypography.body.copy(color = HermesColors.OnBackground),
                    cursorBrush = SolidColor(HermesColors.PrimaryLight),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { handleSend(textFieldValue.text) },
                        onDone = { handleSend(textFieldValue.text) }
                    )
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Send Button
        item {
            Button(
                onClick = { handleSend(textFieldValue.text) },
                enabled = textFieldValue.text.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(40.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = HermesColors.Primary)
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

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Switch to Voice
        item {
            CompactChip(
                onClick = onSwitchToVoice,
                modifier = Modifier.fillMaxWidth(0.88f),
                icon = {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_mic,
                        tint = HermesColors.PrimaryLight,
                        modifier = Modifier.size(14.dp)
                    )
                },
                label = {
                    Text(
                        text = "Ganti ke Suara",
                        style = HermesTypography.caption.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Cancel
        item {
            CompactChip(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(0.65f),
                label = {
                    Text(
                        text = "Batal",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
        }
    }
}

// ── 4. Assistant Loading Section ──────────────────────────────────────────
@Composable
private fun AssistantLoadingSection(query: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_rotation")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                indicatorColor = HermesColors.Primary,
                trackColor = HermesColors.SurfaceVariant,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Hermes berpikir...",
                style = HermesTypography.caption.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = HermesColors.PrimaryLight.copy(alpha = glowAlpha)
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"$query\"",
                style = HermesTypography.caption,
                color = HermesColors.OnSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── 4b. Assistant Streaming Section (Real-time Typewriter SSE) ────────────
@Composable
private fun AssistantStreamingSection(
    query: String,
    currentText: String,
    onCancel: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    LaunchedEffect(currentText.length) {
        if (currentText.isNotBlank()) {
            listState.animateScrollToItem(index = 2)
        }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(1.dp, HermesColors.Primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                backgroundPainter = CardDefaults.cardBackgroundPainter(
                    startBackgroundColor = HermesColors.Primary.copy(alpha = 0.12f),
                    endBackgroundColor = HermesColors.Primary.copy(alpha = 0.12f)
                )
            ) {
                Text(
                    text = query,
                    style = HermesTypography.caption.copy(fontWeight = FontWeight.Medium),
                    color = HermesColors.PrimaryLight,
                    modifier = Modifier.padding(6.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            Card(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(1.dp, HermesColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                backgroundPainter = CardDefaults.cardBackgroundPainter(
                    startBackgroundColor = HermesColors.Surface,
                    endBackgroundColor = HermesColors.Surface
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(HermesColors.Primary.copy(alpha = cursorAlpha))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Mengetik...",
                            style = HermesTypography.caption.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HermesColors.PrimaryLight
                            )
                        )
                    }

                    Text(
                        text = if (currentText.isNotBlank()) currentText else "...",
                        style = HermesTypography.body.copy(fontSize = 13.sp, lineHeight = 18.sp),
                        color = HermesColors.OnSurface
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            CompactChip(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(0.65f),
                label = {
                    Text(
                        text = "Batal",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
        }
    }
}

// ── 5. Direct Answer Section (Pure Vertical Stack) ───────────────────────
@Composable
private fun AssistantAnswerSection(
    answerState: AssistantUiState.Answer,
    onToggleMute: () -> Unit,
    onAskAgainVoice: () -> Unit,
    onAskAgainKeyboard: () -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    val infiniteTransition = rememberInfiniteTransition(label = "speaking_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Query reminder card
        item {
            Card(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(1.dp, HermesColors.Primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                backgroundPainter = CardDefaults.cardBackgroundPainter(
                    startBackgroundColor = HermesColors.Primary.copy(alpha = 0.15f),
                    endBackgroundColor = HermesColors.Primary.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    text = answerState.query,
                    style = HermesTypography.caption.copy(fontWeight = FontWeight.Medium),
                    color = HermesColors.PrimaryLight,
                    modifier = Modifier.padding(6.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Full AI Answer Text Card
        item {
            Card(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(1.dp, HermesColors.SurfaceBorder, RoundedCornerShape(14.dp)),
                backgroundPainter = CardDefaults.cardBackgroundPainter(
                    startBackgroundColor = HermesColors.Surface,
                    endBackgroundColor = HermesColors.Surface
                )
            ) {
                Text(
                    text = answerState.response,
                    style = HermesTypography.body,
                    color = HermesColors.OnSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // TTS Mute / Unmute Control
        item {
            val isMuted = answerState.isMuted
            val isSpeaking = answerState.isSpeaking

            Chip(
                onClick = onToggleMute,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(42.dp)
                    .border(
                        1.dp,
                        if (!isMuted) HermesColors.Primary.copy(alpha = 0.6f) else HermesColors.SurfaceBorder,
                        RoundedCornerShape(21.dp)
                    ),
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (!isMuted) HermesColors.SurfaceElevated else HermesColors.SurfaceVariant
                ),
                icon = {
                    IconoirIcon(
                        id = if (isMuted) R.drawable.ic_iconoir_sound_off else R.drawable.ic_iconoir_sound_high,
                        tint = if (!isMuted) HermesColors.Primary else HermesColors.OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = {
                    Text(
                        text = when {
                            isMuted -> "Suara: Senyap"
                            isSpeaking -> "Sedang Berbicara..."
                            else -> "Suara: Aktif"
                        },
                        style = HermesTypography.caption.copy(
                            fontWeight = if (isSpeaking) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSpeaking) HermesColors.Primary.copy(alpha = pulseAlpha) else HermesColors.OnSurface
                        )
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Tanya Lagi (Voice)
        item {
            Chip(
                onClick = onAskAgainVoice,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(40.dp),
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Primary),
                icon = {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_mic,
                        tint = HermesColors.OnPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = "Tanya Lagi",
                        style = HermesTypography.button
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Tanya via Keyboard
        item {
            CompactChip(
                onClick = onAskAgainKeyboard,
                modifier = Modifier.fillMaxWidth(0.88f),
                icon = {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_keyboard,
                        tint = HermesColors.PrimaryLight,
                        modifier = Modifier.size(14.dp)
                    )
                },
                label = {
                    Text(
                        text = "Ketik Pertanyaan",
                        style = HermesTypography.caption.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Close / Home button
        item {
            CompactChip(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(0.65f),
                label = {
                    Text(
                        text = "Selesai",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
        }
    }
}

// ── 6. Assistant Error Section (Pure Vertical Stack) ──────────────────────
@Composable
private fun AssistantErrorSection(
    query: String,
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconoirIcon(
                id = R.drawable.ic_iconoir_warning,
                tint = HermesColors.Error,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Gagal Menjawab",
                style = HermesTypography.title.copy(fontSize = 14.sp),
                color = HermesColors.Error
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = HermesTypography.caption,
                color = HermesColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            CompactChip(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.85f),
                icon = {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_refresh,
                        tint = HermesColors.OnPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                },
                label = {
                    Text(
                        text = "Coba Lagi",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Primary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            CompactChip(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(0.65f),
                label = {
                    Text(
                        text = "Batal",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
        }
    }
}

// ── 5. Direct Mode Sections (Minimalist STT, Plain Large Text, Zero History) ─

@Composable
private fun DirectListeningSection(
    liveText: String,
    speechState: SpeechRecognizerManager.SpeechState,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val isError = speechState is SpeechRecognizerManager.SpeechState.Error && liveText.isBlank()
    val infiniteTransition = rememberInfiniteTransition(label = "direct_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isError) {
            val errorMsg = (speechState as SpeechRecognizerManager.SpeechState.Error).message
            item {
                IconoirIcon(
                    id = R.drawable.ic_iconoir_sound_off,
                    tint = HermesColors.Warning,
                    modifier = Modifier.size(24.dp)
                )
            }
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
            item {
                Text(
                    text = errorMsg,
                    style = HermesTypography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = HermesColors.Error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
            }
            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
            item {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, HermesColors.PrimaryLight, CircleShape),
                    colors = ButtonDefaults.buttonColors(backgroundColor = HermesColors.Primary)
                ) {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_mic,
                        tint = HermesColors.OnPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
            item {
                Text(
                    text = "Coba Lagi",
                    style = HermesTypography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = HermesColors.PrimaryLight
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                CompactChip(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(0.55f),
                    label = {
                        Text(
                            text = "Batal",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
        } else {
            // Minimalist Status Tag
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(HermesColors.Primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (liveText.isNotBlank()) "Mendengar..." else "Mendengarkan...",
                        style = HermesTypography.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HermesColors.PrimaryLight
                        )
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Live recognized speech in clean typography (no container card)
            item {
                Text(
                    text = if (liveText.isNotBlank()) liveText else "Bicara sekarang...",
                    style = HermesTypography.title.copy(
                        fontSize = if (liveText.isNotBlank()) 17.sp else 14.sp,
                        lineHeight = 23.sp,
                        fontWeight = if (liveText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (liveText.isNotBlank()) HermesColors.OnBackground else HermesColors.OnSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .padding(horizontal = 4.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                CompactChip(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(0.55f),
                    label = {
                        Text(
                            text = "Batal",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun DirectStreamingSection(
    query: String,
    currentText: String,
    onCancel: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    LaunchedEffect(currentText.length) {
        if (currentText.isNotBlank()) {
            listState.animateScrollToItem(index = 2)
        }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Query text preview
        item {
            Text(
                text = "\"$query\"",
                style = HermesTypography.caption.copy(
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                color = HermesColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.92f)
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Plain large text streaming directly without container cards
        item {
            Text(
                text = if (currentText.isNotBlank()) currentText else "Mengetik...",
                style = HermesTypography.body.copy(
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = HermesColors.OnBackground,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(0.94f)
            )
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
        }

        item {
            CompactChip(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(0.55f),
                label = {
                    Text(
                        text = "Batal",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
        }
    }
}

@Composable
private fun DirectAnswerSection(
    query: String,
    response: String,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onAskAgain: () -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with query and sound toggle
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(0.94f)
            ) {
                Text(
                    text = query,
                    style = HermesTypography.caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HermesColors.PrimaryLight
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                CompactChip(
                    onClick = onToggleMute,
                    icon = {
                        IconoirIcon(
                            id = if (isMuted) R.drawable.ic_iconoir_sound_off else R.drawable.ic_iconoir_sound_high,
                            tint = if (isMuted) HermesColors.OnSurfaceVariant else HermesColors.Primary,
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Clean plain large text (No container/card, easily scrollable)
        item {
            Text(
                text = response,
                style = HermesTypography.body.copy(
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = HermesColors.OnBackground,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(0.94f)
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Direct Mic Button below response: Clears previous answer immediately, no history saved
        item {
            Button(
                onClick = onAskAgain,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, HermesColors.PrimaryLight, CircleShape),
                colors = ButtonDefaults.buttonColors(backgroundColor = HermesColors.Primary)
            ) {
                IconoirIcon(
                    id = R.drawable.ic_iconoir_mic,
                    tint = HermesColors.OnPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            Text(
                text = "Tanya Lagi",
                style = HermesTypography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = HermesColors.PrimaryLight
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Close / Return to Home
        item {
            CompactChip(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(0.55f),
                label = {
                    Text(
                        text = "Selesai",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
        }
    }
}
