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

    // Sync speech recognized result & partial words to currentText in real-time
    LaunchedEffect(speechState) {
        when (speechState) {
            is SpeechRecognizerManager.SpeechState.Listening -> {
                val partial = (speechState as SpeechRecognizerManager.SpeechState.Listening).partialText
                if (partial.isNotBlank()) {
                    currentText = partial
                }
            }
            is SpeechRecognizerManager.SpeechState.Result -> {
                currentText = (speechState as SpeechRecognizerManager.SpeechState.Result).text
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            else -> {}
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

                            is AssistantUiState.Idle -> {
                                AssistantIdleSection(
                                    onStartVoice = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentText = ""
                                        inputMode = AssistantInputMode.VOICE
                                        voiceViewModel.startListening()
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

// ── 1. Idle Screen (Clean Gemini Style) ──────────────────────────────────
@Composable
private fun AssistantIdleSection(
    onStartVoice: () -> Unit,
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
                // Soft glowing background pulse
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(auraScale)
                        .clip(CircleShape)
                        .background(HermesColors.PrimaryGlow)
                )

                // Main Touch Target (62dp)
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

        // Secondary Action: Keyboard Input Chip
        item {
            Chip(
                onClick = onStartKeyboard,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
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
                        style = HermesTypography.caption
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Surface)
            )
        }
    }
}

// ── 2. Voice Input Section (Real-time Live STT Preview) ───────────────────
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

    // Determine live text: partial words while speaking or final speech result
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

    // Subtle pulsing amber indicator dot while listening
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
            Spacer(modifier = Modifier.height(6.dp))
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
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        // Action Buttons: Tampil jika sudah ada kata terdeteksi
        if (liveText.isNotBlank()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Tombol Kirim Utama
            item {
                Button(
                    onClick = { onSend(liveText) },
                    modifier = Modifier
                        .fillMaxWidth()
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
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Row Opsi: Edit via Keyboard (✏️), Ulang (🔄), Batal
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompactChip(
                        onClick = { onEdit(liveText) },
                        icon = {
                            IconoirIcon(
                                id = R.drawable.ic_iconoir_edit,
                                tint = HermesColors.PrimaryLight,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = { Text("Edit") },
                        colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                    )
                    CompactChip(
                        onClick = onRetry,
                        icon = {
                            IconoirIcon(
                                id = R.drawable.ic_iconoir_refresh,
                                tint = HermesColors.Warning,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = { Text("Ulang") },
                        colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                    )
                    CompactChip(
                        onClick = onCancel,
                        label = { Text("Batal") },
                        colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                    )
                }
            }
        } else if (isError) {
            // Tampilan jika terjadi error audio/timeout
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
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompactChip(
                        onClick = onRetry,
                        icon = {
                            IconoirIcon(
                                id = R.drawable.ic_iconoir_refresh,
                                tint = HermesColors.Warning,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = { Text("Coba Lagi") },
                        colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                    )
                    CompactChip(
                        onClick = onCancel,
                        label = { Text("Kembali") },
                        colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                    )
                }
            }
        } else {
            // Jika belum ada kata terdeteksi: tampilkan tombol Batal
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                CompactChip(
                    onClick = onCancel,
                    label = { Text("Batal") },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
        }
    }
}

// ── 3. Keyboard Input Section ─────────────────────────────────────────────
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
                    .fillMaxWidth()
                    .heightIn(min = 50.dp, max = 84.dp)
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
                    .fillMaxWidth()
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
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Switch to Voice & Cancel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactChip(
                    onClick = onSwitchToVoice,
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_mic,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text("Suara") },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
                CompactChip(
                    onClick = onCancel,
                    label = { Text("Batal") },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
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

// ── 5. Direct Answer Section (Q&A Result + TTS Mute Toggle) ──────────────
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
                    .fillMaxWidth()
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

        // Full AI Answer Text Card with Obsidian Surface
        item {
            Card(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
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

        // TTS Mute / Unmute Control with Speaking Pulse
        item {
            val isMuted = answerState.isMuted
            val isSpeaking = answerState.isSpeaking

            Chip(
                onClick = onToggleMute,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Ask Again Actions: Mic and Keyboard
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactChip(
                    onClick = onAskAgainVoice,
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_mic,
                            tint = HermesColors.OnPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text("Tanya Lagi") },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Primary)
                )
                CompactChip(
                    onClick = onAskAgainKeyboard,
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_keyboard,
                            tint = HermesColors.PrimaryLight,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text("Ketik") },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Close / Home button
        item {
            CompactChip(
                onClick = onClose,
                label = { Text("Selesai") },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
        }
    }
}

// ── 6. Assistant Error Section ────────────────────────────────────────────
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactChip(
                    onClick = onRetry,
                    icon = {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_refresh,
                            tint = HermesColors.OnPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text("Coba Lagi") },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Primary)
                )
                CompactChip(
                    onClick = onDismiss,
                    label = { Text("Batal") },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
        }
    }
}
