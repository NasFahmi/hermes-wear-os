package com.hermes.wearos.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
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

    // Sync speech recognized result to pending text
    LaunchedEffect(speechState) {
        if (speechState is SpeechRecognizerManager.SpeechState.Result) {
            currentText = (speechState as SpeechRecognizerManager.SpeechState.Result).text
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                animationSpec = tween(220),
                label = "screen_crossfade"
            ) { (mode, state) ->
                when {
                    // ── 1. Voice Input & Review Mode (STT) ─────────────────────
                    mode == AssistantInputMode.VOICE -> {
                        VoiceInputSection(
                            speechState = speechState,
                            recognizedText = currentText,
                            onSend = {
                                val textToSend = currentText.trim()
                                if (textToSend.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    inputMode = AssistantInputMode.NONE
                                    voiceViewModel.resetState()
                                    chatViewModel.askQuestion(textToSend)
                                    currentText = ""
                                }
                            },
                            onEdit = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

// ── Idle Screen (Gemini Aura & Tactile Ergonomics) ────────────────────────
@Composable
private fun AssistantIdleSection(
    onStartVoice: () -> Unit,
    onStartKeyboard: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    // Breathing pulse animation for mic aura
    val infiniteTransition = rememberInfiniteTransition(label = "idle_aura")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_scale"
    )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title & Tagline
        item {
            Text(
                text = "Hermes",
                style = HermesTypography.title.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tanya apa saja",
                style = HermesTypography.caption,
                color = HermesColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Central Mic Button with Gemini Glowing Aura
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                // Soft glow background
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(auraScale)
                        .clip(CircleShape)
                        .background(HermesColors.PrimaryGlow)
                )

                // Main Touch Target (68dp >= 48dp Android requirement)
                Button(
                    onClick = onStartVoice,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, HermesColors.PrimaryLight.copy(alpha = 0.5f), CircleShape),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = HermesColors.Primary
                    )
                ) {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_mic,
                        tint = HermesColors.OnPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ketuk untuk Bicara",
                style = HermesTypography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = HermesColors.PrimaryLight
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Secondary Action: Keyboard Input
        item {
            Chip(
                onClick = onStartKeyboard,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(44.dp)
                    .border(1.dp, HermesColors.SurfaceBorder, RoundedCornerShape(22.dp)),
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
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Quick Settings Access
        item {
            CompactChip(
                onClick = onOpenSettings,
                icon = {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_settings,
                        tint = HermesColors.OnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
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

// ── Voice Input & STT Review Section ──────────────────────────────────────
@Composable
private fun VoiceInputSection(
    speechState: SpeechRecognizerManager.SpeechState,
    recognizedText: String,
    onSend: () -> Unit,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    // Dynamic wave pulse during speech listening
    val infiniteTransition = rememberInfiniteTransition(label = "listening_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (speechState) {
            is SpeechRecognizerManager.SpeechState.Listening -> {
                item {
                    Text(
                        text = "Mendengarkan...",
                        style = HermesTypography.title.copy(fontSize = 14.sp),
                        color = HermesColors.PrimaryLight
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Breathing Waveform Orb
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(HermesColors.Primary.copy(alpha = glowAlpha))
                        )
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(HermesColors.Primary)
                                .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconoirIcon(
                                id = R.drawable.ic_iconoir_mic,
                                tint = HermesColors.OnPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Real-time live transcript preview
                if (speechState.partialText.isNotBlank()) {
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
                                text = "\"${speechState.partialText}\"",
                                style = HermesTypography.body,
                                color = HermesColors.PrimaryLight,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    CompactChip(
                        onClick = onCancel,
                        label = { Text("Batal") },
                        colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                    )
                }
            }

            is SpeechRecognizerManager.SpeechState.Result -> {
                item {
                    Text(
                        text = "Tinjau Pertanyaan:",
                        style = HermesTypography.caption,
                        color = HermesColors.OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Recognized Text Card
                item {
                    Card(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, HermesColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = HermesColors.Surface,
                            endBackgroundColor = HermesColors.Surface
                        )
                    ) {
                        Text(
                            text = "\"$recognizedText\"",
                            style = HermesTypography.body.copy(fontWeight = FontWeight.Medium),
                            color = HermesColors.OnBackground,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Main Send Button (Gold Accent, Height 44dp)
                item {
                    Button(
                        onClick = onSend,
                        enabled = recognizedText.isNotBlank(),
                        modifier = Modifier
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
                                id = R.drawable.ic_iconoir_send,
                                tint = HermesColors.OnPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kirim",
                                style = HermesTypography.button
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Edit (✏️), Retry (🔄), Cancel
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CompactChip(
                            onClick = onEdit,
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
            }

            is SpeechRecognizerManager.SpeechState.Error -> {
                item {
                    IconoirIcon(
                        id = R.drawable.ic_iconoir_warning,
                        tint = HermesColors.Error,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = speechState.message,
                        style = HermesTypography.caption,
                        color = HermesColors.Error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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
            }

            is SpeechRecognizerManager.SpeechState.Idle -> {
                item {
                    Text(
                        text = "Siap mendengarkan.",
                        style = HermesTypography.caption,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CompactChip(
                        onClick = onRetry,
                        label = { Text("Mulai Bicara") },
                        colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Primary)
                    )
                }
            }
        }
    }
}

// ── Keyboard Input Section ────────────────────────────────────────────────
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
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Text input field box
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp, max = 88.dp)
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Send Button
        item {
            Button(
                onClick = { handleSend(textFieldValue.text) },
                enabled = textFieldValue.text.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
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

// ── Assistant Loading Section ─────────────────────────────────────────────
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
                modifier = Modifier
                    .size(42.dp)
                    .scale(1f)
            )
            Spacer(modifier = Modifier.height(12.dp))
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

// ── Direct Answer Section (Q&A Result + TTS Mute Toggle) ─────────────────
@Composable
private fun AssistantAnswerSection(
    answerState: AssistantUiState.Answer,
    onToggleMute: () -> Unit,
    onAskAgainVoice: () -> Unit,
    onAskAgainKeyboard: () -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    // Speaking pulse animation for audio indicator
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
            Spacer(modifier = Modifier.height(10.dp))
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
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Close / Home button
        item {
            CompactChip(
                onClick = onClose,
                label = { Text("Selesai") },
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── Assistant Error Section ───────────────────────────────────────────────
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
