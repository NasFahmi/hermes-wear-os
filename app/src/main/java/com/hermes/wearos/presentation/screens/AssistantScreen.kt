package com.hermes.wearos.presentation.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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

    var directLiveText by remember { mutableStateOf("") }
    var hasAutoLaunchedVoice by rememberSaveable { mutableStateOf(false) }
    var hasTriggeredReadyHaptic by remember { mutableStateOf(false) }

    // Fallback: Native Wear OS System Speech Recognition Dialog Launcher
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()?.trim() ?: ""
            if (spokenText.isNotBlank()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                directLiveText = ""
                chatViewModel.askDirectQuestion(spokenText)
            } else {
                if (assistantState is AssistantUiState.DirectListening) {
                    chatViewModel.resetToIdle()
                }
            }
        } else {
            if (assistantState is AssistantUiState.DirectListening && directLiveText.isBlank()) {
                chatViewModel.resetToIdle()
            }
        }
    }

    val launchSystemSpeech: () -> Unit = {
        voiceViewModel.stopListening()
        voiceViewModel.resetState()
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, voiceViewModel.voiceLanguage.value)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara dengan Hermes...")
        }
        try {
            systemSpeechLauncher.launch(speechIntent)
        } catch (e: Exception) {
            android.util.Log.e("AssistantScreen", "Failed to launch speech dialog", e)
        }
    }

    // In-App Seamless Voice Recognition (Hands-Free Gemini Style)
    val startDirectVoice: () -> Unit = {
        hasTriggeredReadyHaptic = false
        directLiveText = ""
        chatViewModel.startDirectListening()
        voiceViewModel.startListening()
    }

    // Cleanup voice listener when screen unmounts
    DisposableEffect(Unit) {
        onDispose {
            voiceViewModel.stopListening()
        }
    }

    // ── 1. Auto-Launch Voice on App Launch (Gemini-Style: Buka langsung mendengarkan) ──
    LaunchedEffect(Unit) {
        if (!hasAutoLaunchedVoice) {
            hasAutoLaunchedVoice = true
            delay(350)
            startDirectVoice()
        }
    }

    // ── 2. Speech State Handling (Real-time live partial text & automatic result) ──
    LaunchedEffect(speechState) {
        when (val state = speechState) {
            is SpeechRecognizerManager.SpeechState.Initializing -> {
                hasTriggeredReadyHaptic = false
            }
            is SpeechRecognizerManager.SpeechState.Listening -> {
                if (!hasTriggeredReadyHaptic) {
                    hasTriggeredReadyHaptic = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                if (state.partialText.isNotBlank()) {
                    directLiveText = state.partialText
                }
            }
            is SpeechRecognizerManager.SpeechState.Result -> {
                val finalText = state.text.trim()
                if (finalText.isNotBlank()) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    voiceViewModel.stopListening()
                    directLiveText = ""
                    chatViewModel.askDirectQuestion(finalText)
                }
            }
            is SpeechRecognizerManager.SpeechState.Error -> {
                android.util.Log.w("AssistantScreen", "Speech error: ${state.message} (code=${state.errorCode})")
                // If text was recognized before error/timeout, send it immediately
                if (directLiveText.isNotBlank()) {
                    val fallbackQuery = directLiveText.trim()
                    voiceViewModel.stopListening()
                    directLiveText = ""
                    chatViewModel.askDirectQuestion(fallbackQuery)
                }
            }
            is SpeechRecognizerManager.SpeechState.Idle -> {
                hasTriggeredReadyHaptic = false
            }
        }
    }

    // ── 3. Direct Mode Debounce: Auto-send after 2.2s of silence ─────────────────
    LaunchedEffect(directLiveText, assistantState) {
        if (assistantState is AssistantUiState.DirectListening && directLiveText.isNotBlank()) {
            delay(2200)
            val queryToSend = directLiveText.trim()
            if (queryToSend.isNotBlank()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                voiceViewModel.stopListening()
                directLiveText = ""
                chatViewModel.askDirectQuestion(queryToSend)
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
                targetState = assistantState,
                animationSpec = tween(220),
                label = "direct_assistant_crossfade"
            ) { state ->
                when (state) {
                    is AssistantUiState.DirectStreaming, is AssistantUiState.Streaming -> {
                        val query = if (state is AssistantUiState.DirectStreaming) state.query else (state as AssistantUiState.Streaming).query
                        val currentText = if (state is AssistantUiState.DirectStreaming) state.currentText else (state as AssistantUiState.Streaming).currentText
                        DirectStreamingSection(
                            query = query,
                            currentText = currentText,
                            onCancel = { chatViewModel.resetToIdle() }
                        )
                    }

                    is AssistantUiState.Loading -> {
                        DirectLoadingSection(
                            query = state.query,
                            onCancel = { chatViewModel.resetToIdle() }
                        )
                    }

                    is AssistantUiState.DirectAnswer, is AssistantUiState.Answer -> {
                        val query = if (state is AssistantUiState.DirectAnswer) state.query else (state as AssistantUiState.Answer).query
                        val response = if (state is AssistantUiState.DirectAnswer) state.response else (state as AssistantUiState.Answer).response
                        val isMuted = if (state is AssistantUiState.DirectAnswer) state.isMuted else (state as AssistantUiState.Answer).isMuted
                        val isSpeaking = if (state is AssistantUiState.DirectAnswer) state.isSpeaking else (state as AssistantUiState.Answer).isSpeaking

                        DirectAnswerSection(
                            query = query,
                            response = response,
                            isMuted = isMuted,
                            isSpeaking = isSpeaking,
                            onToggleMute = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                chatViewModel.toggleMute()
                            },
                            onAskAgain = {
                                startDirectVoice()
                            },
                            onOpenSettings = onNavigateToSettings
                        )
                    }

                    is AssistantUiState.DirectListening -> {
                        DirectListeningSection(
                            liveText = directLiveText,
                            isInitializing = speechState is SpeechRecognizerManager.SpeechState.Initializing,
                            onRetry = {
                                startDirectVoice()
                            },
                            onCancel = {
                                voiceViewModel.stopListening()
                                directLiveText = ""
                                chatViewModel.resetToIdle()
                            },
                            onSendNow = {
                                if (directLiveText.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val queryToSend = directLiveText.trim()
                                    voiceViewModel.stopListening()
                                    directLiveText = ""
                                    chatViewModel.askDirectQuestion(queryToSend)
                                }
                            },
                            onLaunchSystemDialog = launchSystemSpeech
                        )
                    }

                    is AssistantUiState.Error -> {
                        DirectErrorSection(
                            query = state.query,
                            errorMessage = state.message,
                            onRetry = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                chatViewModel.askDirectQuestion(state.query)
                            },
                            onDismiss = { chatViewModel.resetToIdle() }
                        )
                    }

                    is AssistantUiState.Idle -> {
                        AssistantIdleSection(
                            onStartVoice = {
                                startDirectVoice()
                            },
                            onOpenSettings = onNavigateToSettings
                        )
                    }
                }
            }
        }
    }
}

// ── 1. Minimalist Idle Section (Standby Hub: Mic + Settings) ──────────────────
@Composable
private fun AssistantIdleSection(
    onStartVoice: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    val infiniteTransition = rememberInfiniteTransition(label = "idle_aura")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_scale"
    )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App / Agent Title
        item {
            Text(
                text = "Hermes AI",
                style = HermesTypography.caption.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = HermesColors.PrimaryLight
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Primary Voice Trigger Button
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(auraScale)
                        .clip(CircleShape)
                        .background(HermesColors.Primary.copy(alpha = 0.22f))
                )
                Button(
                    onClick = onStartVoice,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, HermesColors.PrimaryLight, CircleShape),
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
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            Text(
                text = "Sentuh untuk bertanya",
                style = HermesTypography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal),
                color = HermesColors.OnSurfaceMuted,
                textAlign = TextAlign.Center
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Settings Button
        item {
            CompactChip(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(0.72f),
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
                        style = HermesTypography.caption.copy(fontSize = 11.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
        }
    }
}

// ── 2. Direct Listening Section (Visual pulse when microphone dialog is ready) ─
@Composable
private fun DirectListeningSection(
    liveText: String,
    isInitializing: Boolean = false,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onSendNow: () -> Unit,
    onLaunchSystemDialog: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    val infiniteTransition = rememberInfiniteTransition(label = "direct_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isInitializing) 1.10f else 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isInitializing) 1000 else 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val dotColor = if (isInitializing) HermesColors.Warning else HermesColors.Primary
    val statusText = if (isInitializing) "Menyiapkan mikrofon..." else "Mendengarkan..."
    val statusColor = if (isInitializing) HermesColors.Warning else HermesColors.PrimaryLight
    val promptText = when {
        liveText.isNotBlank() -> liveText
        isInitializing -> "Tunggu sebentar..."
        else -> "Bicara sekarang..."
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    style = HermesTypography.caption.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            Text(
                text = promptText,
                style = HermesTypography.title.copy(
                    fontSize = if (liveText.isNotBlank()) 16.sp else 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = if (liveText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (liveText.isNotBlank()) HermesColors.OnBackground else HermesColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.92f)
            )
        }

        if (liveText.isNotBlank()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
            item {
                Text(
                    text = "Otomatis kirim saat hening (2 dtk)",
                    style = HermesTypography.caption.copy(fontSize = 10.sp),
                    color = HermesColors.PrimaryLight,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Button(
                onClick = {
                    if (liveText.isNotBlank()) {
                        onSendNow()
                    } else {
                        onRetry()
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, HermesColors.PrimaryLight, CircleShape),
                colors = ButtonDefaults.buttonColors(backgroundColor = HermesColors.Primary)
            ) {
                IconoirIcon(
                    id = if (liveText.isNotBlank()) R.drawable.ic_iconoir_send else R.drawable.ic_iconoir_mic,
                    tint = HermesColors.OnPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
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

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            CompactChip(
                onClick = onLaunchSystemDialog,
                modifier = Modifier.fillMaxWidth(0.70f),
                label = {
                    Text(
                        text = "Gunakan Dialog Google",
                        style = HermesTypography.caption.copy(fontSize = 10.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant.copy(alpha = 0.5f))
            )
        }
    }
}

// ── 3. Direct Streaming Section (Real-time token typing) ─────────────────────
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

        // Plain large text streaming directly
        item {
            Text(
                text = if (currentText.isNotBlank()) currentText else "Berpikir...",
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

// ── 4. Direct Loading Section ───────────────────────────────────────────────
@Composable
private fun DirectLoadingSection(
    query: String,
    onCancel: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            CircularProgressIndicator(
                modifier = Modifier.size(34.dp),
                indicatorColor = HermesColors.Primary,
                trackColor = HermesColors.SurfaceVariant,
                strokeWidth = 3.dp
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Text(
                text = "Menghubungi Hermes...",
                style = HermesTypography.caption.copy(fontSize = 11.sp),
                color = HermesColors.PrimaryLight
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

// ── 5. Direct Answer Section (Large Plain Text + Mic Tanya Lagi + Pengaturan) ──
@Composable
private fun DirectAnswerSection(
    query: String,
    response: String,
    isMuted: Boolean,
    isSpeaking: Boolean,
    onToggleMute: () -> Unit,
    onAskAgain: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Query header with mute toggle
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
                            tint = if (isMuted) HermesColors.OnSurfaceVariant else (if (isSpeaking) HermesColors.Success else HermesColors.Primary),
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

        // Plain large readable text without container borders
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
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Scroll Down Feature 1: Large Mic Button "Tanya Lagi" ──────────────
        item {
            Button(
                onClick = onAskAgain,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, HermesColors.PrimaryLight, CircleShape),
                colors = ButtonDefaults.buttonColors(backgroundColor = HermesColors.Primary)
            ) {
                IconoirIcon(
                    id = R.drawable.ic_iconoir_mic,
                    tint = HermesColors.OnPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            Text(
                text = "Tanya Lagi",
                style = HermesTypography.caption.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = HermesColors.PrimaryLight
            )
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ── Scroll Down Feature 2: Settings Button "Pengaturan" ───────────────
        item {
            CompactChip(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(0.72f),
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
                        style = HermesTypography.caption.copy(fontSize = 11.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── 6. Direct Error Section ──────────────────────────────────────────────────
@Composable
private fun DirectErrorSection(
    query: String,
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            IconoirIcon(
                id = R.drawable.ic_iconoir_warning,
                tint = HermesColors.Error,
                modifier = Modifier.size(26.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            Text(
                text = errorMessage.ifBlank { "Terjadi kesalahan koneksi" },
                style = HermesTypography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                color = HermesColors.Error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.92f)
            )
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
        }

        item {
            CompactChip(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.75f),
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
            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            CompactChip(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(0.55f),
                label = {
                    Text(
                        text = "Tutup",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
        }
    }
}
