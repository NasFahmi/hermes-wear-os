package com.hermes.wearos.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.hermes.wearos.R
import com.hermes.wearos.core.utils.DateUtils
import com.hermes.wearos.core.utils.WearTextUtils
import com.hermes.wearos.data.local.ChatMessageEntity
import com.hermes.wearos.presentation.components.*
import com.hermes.wearos.presentation.theme.HermesColors
import com.hermes.wearos.presentation.theme.HermesTypography
import com.hermes.wearos.presentation.viewmodel.ChatViewModel
import com.hermes.wearos.presentation.viewmodel.VoiceViewModel
import com.hermes.wearos.services.speech.SpeechRecognizerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ChatInputMode {
    NONE,
    VOICE,
    KEYBOARD
}

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    voiceViewModel: VoiceViewModel,
    initialMode: ChatInputMode = ChatInputMode.NONE,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val uiState by chatViewModel.uiState.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val speechState by voiceViewModel.speechState.collectAsState()

    var currentMode by remember { mutableStateOf(initialMode) }
    var pendingText by remember { mutableStateOf("") }

    // Synchronize mode whenever initialMode changes
    LaunchedEffect(initialMode) {
        currentMode = initialMode
        if (initialMode == ChatInputMode.VOICE) {
            pendingText = ""
            voiceViewModel.startListening()
        }
    }

    LaunchedEffect(speechState) {
        if (speechState is SpeechRecognizerManager.SpeechState.Result) {
            pendingText = (speechState as SpeechRecognizerManager.SpeechState.Result).text
        }
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentMode) {
                // ── Voice Input & Review (STT) ───────────────────────────
                ChatInputMode.VOICE -> {
                    VoiceReviewContent(
                        speechState = speechState,
                        pendingText = pendingText,
                        onSend = {
                            val textToSend = pendingText.trim()
                            if (textToSend.isNotBlank()) {
                                chatViewModel.sendMessage(textToSend)
                                pendingText = ""
                                voiceViewModel.resetState()
                                currentMode = ChatInputMode.NONE
                            }
                        },
                        onEdit = {
                            // Switch to Keyboard mode with recognized text populated
                            voiceViewModel.stopListening()
                            voiceViewModel.resetState()
                            currentMode = ChatInputMode.KEYBOARD
                        },
                        onRetry = {
                            pendingText = ""
                            voiceViewModel.startListening()
                        },
                        onCancel = {
                            voiceViewModel.stopListening()
                            voiceViewModel.resetState()
                            pendingText = ""
                            if (initialMode == ChatInputMode.VOICE && messages.isEmpty()) {
                                onBack()
                            } else {
                                currentMode = ChatInputMode.NONE
                            }
                        }
                    )
                }

                // ── Keyboard Text Input ───────────────────────────────────
                ChatInputMode.KEYBOARD -> {
                    key(pendingText, currentMode) {
                        KeyboardInputContent(
                            initialText = pendingText,
                            onSend = { textToSend ->
                                if (textToSend.isNotBlank()) {
                                    chatViewModel.sendMessage(textToSend)
                                    pendingText = ""
                                    currentMode = ChatInputMode.NONE
                                }
                            },
                            onSwitchToVoice = {
                                pendingText = ""
                                currentMode = ChatInputMode.VOICE
                                voiceViewModel.startListening()
                            },
                            onCancel = {
                                pendingText = ""
                                if (initialMode == ChatInputMode.KEYBOARD && messages.isEmpty()) {
                                    onBack()
                                } else {
                                    currentMode = ChatInputMode.NONE
                                }
                            }
                        )
                    }
                }

                // ── Main Chat Conversation ────────────────────────────────
                ChatInputMode.NONE -> {
                    ScalingLazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        item {
                            Text(
                                text = "Hermes Chat",
                                style = HermesTypography.title,
                                color = HermesColors.Primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Primary Action 1: Voice Input CTA
                        item {
                            AskHermesButton(
                                onClick = {
                                    pendingText = ""
                                    currentMode = ChatInputMode.VOICE
                                    voiceViewModel.startListening()
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Primary Action 2: Keyboard Input CTA
                        item {
                            Chip(
                                onClick = {
                                    pendingText = ""
                                    currentMode = ChatInputMode.KEYBOARD
                                },
                                modifier = Modifier.fillMaxWidth(),
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
                                        style = HermesTypography.body
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Error Banner (if any)
                        if (uiState.error != null) {
                            item {
                                Card(
                                    onClick = { chatViewModel.clearError() },
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                                        startBackgroundColor = HermesColors.Error.copy(alpha = 0.2f),
                                        endBackgroundColor = HermesColors.Error.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconoirIcon(
                                            id = R.drawable.ic_iconoir_warning,
                                            tint = HermesColors.Error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "${uiState.error}",
                                                style = HermesTypography.caption,
                                                color = HermesColors.Error
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Ketuk untuk menutup",
                                                style = HermesTypography.caption,
                                                color = HermesColors.OnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        // Empty History State
                        if (messages.isEmpty() && !uiState.isSending) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Belum ada pesan.\nGunakan Suara atau Keyboard untuk bertanya.",
                                    style = HermesTypography.caption,
                                    color = HermesColors.OnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Chat Messages List
                        items(messages) { msg ->
                            ChatMessageBubble(
                                message = msg,
                                onRetry = { chatViewModel.sendMessage(msg.message) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Back to Home Button
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            CompactChip(
                                onClick = onBack,
                                icon = {
                                    IconoirIcon(
                                        id = R.drawable.ic_iconoir_arrow_left,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                label = { Text("Menu Utama") },
                                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    // Sending Loading Overlay
                    if (uiState.isSending) {
                        HermesLoadingIndicator()
                    }
                }
            }
        }
    }
}

// ── Keyboard Input Composable ─────────────────────────────────────────────
@Composable
private fun KeyboardInputContent(
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
                delay(200)
                val committed = textFieldValue.text.trim()
                val finalText = if (committed.length >= fallback.length) committed else fallback
                awaitingImeCommit = false
                if (finalText.isNotBlank()) {
                    onSend(finalText)
                }
            }
        }
    }

    val handleCancel: () -> Unit = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onCancel()
    }

    val handleSwitchToVoice: () -> Unit = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onSwitchToVoice()
    }

    val quickSuggestions = listOf(
        "Status server hari ini",
        "Market summary BTC",
        "Agenda hari ini",
        "Berapa memori RAM tersisa?",
        "Apakah ada alert terbaru?"
    )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 16.dp),
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
                    style = HermesTypography.title,
                    color = HermesColors.Primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Text Input Field Box
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HermesColors.Surface)
                    .border(1.dp, HermesColors.Primary, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = "Tulis pertanyaan...",
                        style = HermesTypography.body,
                        color = HermesColors.OnSurfaceVariant
                    )
                }
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    maxLines = 1,
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
                        onDone = { handleSend(textFieldValue.text) },
                        onGo = { handleSend(textFieldValue.text) }
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Send Button
        item {
            SendButton(
                onClick = { handleSend(textFieldValue.text) },
                enabled = textFieldValue.text.isNotBlank()
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Switch to Voice & Cancel Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactChip(
                    onClick = handleSwitchToVoice,
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
                    onClick = handleCancel,
                    label = { Text("Batal") },
                    colors = ChipDefaults.chipColors(backgroundColor = HermesColors.SurfaceVariant)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Quick Suggestion Chips
        item {
            Text(
                text = "Pilih pertanyaan cepat:",
                style = HermesTypography.caption,
                color = HermesColors.OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        items(quickSuggestions) { suggestion ->
            Chip(
                onClick = { handleSend(suggestion) },
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.chipColors(backgroundColor = HermesColors.Surface),
                label = {
                    Text(
                        text = suggestion,
                        style = HermesTypography.body,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

// ── Review Before Send Composable (STT) ───────────────────────────────────
@Composable
private fun VoiceReviewContent(
    speechState: SpeechRecognizerManager.SpeechState,
    pendingText: String,
    onSend: () -> Unit,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val reviewListState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = reviewListState,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (speechState) {
            is SpeechRecognizerManager.SpeechState.Listening -> {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_mic,
                            tint = HermesColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mendengarkan...",
                            style = HermesTypography.title,
                            color = HermesColors.Primary
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(
                        indicatorColor = HermesColors.Primary,
                        trackColor = HermesColors.SurfaceVariant,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Live Interim Speech Preview
                if (speechState.partialText.isNotBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            backgroundPainter = CardDefaults.cardBackgroundPainter(
                                startBackgroundColor = HermesColors.Surface,
                                endBackgroundColor = HermesColors.Surface
                            )
                        ) {
                            Text(
                                text = "\"${speechState.partialText}\"",
                                style = HermesTypography.body,
                                color = HermesColors.PrimaryLight,
                                modifier = Modifier.padding(6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    CompactChip(
                        onClick = onCancel,
                        label = { Text("Batal", style = HermesTypography.button) },
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
                            text = "\"$pendingText\"",
                            style = HermesTypography.body,
                            color = HermesColors.OnBackground,
                            modifier = Modifier.padding(6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Send Button (Main Action)
                item {
                    SendButton(onClick = onSend)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Edit with Keyboard, Retry, Cancel
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EditButton(onClick = onEdit)
                        RetryButton(onClick = onRetry)
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
                        modifier = Modifier.size(24.dp)
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
                        RetryButton(onClick = onRetry)
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
                        text = "Siap mendengarkan suara.",
                        style = HermesTypography.caption,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AskHermesButton(onClick = onRetry)
                }
            }
        }
    }
}

// ── Chat Message Bubble ──────────────────────────────────────────────────
@Composable
private fun ChatMessageBubble(
    message: ChatMessageEntity,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // User query bubble
        Card(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            backgroundPainter = CardDefaults.cardBackgroundPainter(
                startBackgroundColor = HermesColors.Primary.copy(alpha = 0.25f),
                endBackgroundColor = HermesColors.Primary.copy(alpha = 0.25f)
            )
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Text(
                    text = "Anda",
                    style = HermesTypography.caption,
                    color = HermesColors.PrimaryLight
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message.message,
                    style = HermesTypography.body,
                    color = HermesColors.OnBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Hermes response bubble
        if (message.response != null) {
            val formattedResponse = WearTextUtils.truncateToWearLimit(message.response)
            Card(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                backgroundPainter = CardDefaults.cardBackgroundPainter(
                    startBackgroundColor = HermesColors.Surface,
                    endBackgroundColor = HermesColors.Surface
                )
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hermes",
                            style = HermesTypography.caption,
                            color = HermesColors.Success
                        )
                        Text(
                            text = DateUtils.formatRelativeTime(message.timestamp),
                            style = HermesTypography.caption,
                            color = HermesColors.OnSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = formattedResponse,
                        style = HermesTypography.body,
                        color = HermesColors.OnSurface
                    )
                }
            }
        } else if (message.isSending) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    indicatorColor = HermesColors.Primary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Hermes sedang menjawab...",
                    style = HermesTypography.caption,
                    color = HermesColors.OnSurfaceVariant
                )
            }
        } else if (message.isError) {
            Card(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                backgroundPainter = CardDefaults.cardBackgroundPainter(
                    startBackgroundColor = HermesColors.Error.copy(alpha = 0.2f),
                    endBackgroundColor = HermesColors.Error.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconoirIcon(
                            id = R.drawable.ic_iconoir_warning,
                            tint = HermesColors.Error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gagal mengirim",
                            style = HermesTypography.caption,
                            color = HermesColors.Error
                        )
                    }
                    Text(
                        text = "Coba Lagi",
                        style = HermesTypography.caption,
                        color = HermesColors.Primary
                    )
                }
            }
        }
    }
}
