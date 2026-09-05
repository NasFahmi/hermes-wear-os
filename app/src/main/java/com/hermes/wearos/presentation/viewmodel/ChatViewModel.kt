package com.hermes.wearos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.wearos.core.auth.AuthManager
import com.hermes.wearos.core.utils.Result
import com.hermes.wearos.data.repository.ChatRepository
import com.hermes.wearos.services.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.hermes.wearos.data.models.StreamChatEvent

sealed interface AssistantUiState {
    data object Idle : AssistantUiState
    data class Loading(val query: String) : AssistantUiState
    data class Streaming(val query: String, val currentText: String) : AssistantUiState
    data class Answer(
        val query: String,
        val response: String,
        val isMuted: Boolean = true,
        val isSpeaking: Boolean = false
    ) : AssistantUiState
    data class Error(val query: String, val message: String) : AssistantUiState

    // ── Direct Mode States (Minimalist, Ephemeral, Large Plain Text) ──────
    data class DirectListening(val liveText: String = "") : AssistantUiState
    data class DirectStreaming(val query: String, val currentText: String) : AssistantUiState
    data class DirectAnswer(
        val query: String,
        val response: String,
        val isMuted: Boolean = true,
        val isSpeaking: Boolean = false
    ) : AssistantUiState
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val ttsManager: TtsManager,
    private val authManager: AuthManager
) : ViewModel() {

    private val _assistantState = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val assistantState: StateFlow<AssistantUiState> = _assistantState.asStateFlow()

    val isTtsMuted: StateFlow<Boolean> = authManager.isTtsMuted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    init {
        // Sync speaking state into Answer or DirectAnswer
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { speaking ->
                when (val current = _assistantState.value) {
                    is AssistantUiState.Answer -> _assistantState.value = current.copy(isSpeaking = speaking)
                    is AssistantUiState.DirectAnswer -> _assistantState.value = current.copy(isSpeaking = speaking)
                    else -> {}
                }
            }
        }
    }

    fun askQuestion(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        ttsManager.stop()
        _assistantState.value = AssistantUiState.Loading(trimmed)

        viewModelScope.launch {
            val responseBuffer = StringBuilder()

            chatRepository.streamMessage(trimmed).collect { event ->
                when (event) {
                    is StreamChatEvent.Start -> {
                        _assistantState.value = AssistantUiState.Streaming(
                            query = trimmed,
                            currentText = ""
                        )
                    }
                    is StreamChatEvent.Token -> {
                        responseBuffer.append(event.token)
                        _assistantState.value = AssistantUiState.Streaming(
                            query = trimmed,
                            currentText = responseBuffer.toString()
                        )
                    }
                    is StreamChatEvent.Done -> {
                        val finalResponse = if (event.fullResponse.isNotBlank()) {
                            event.fullResponse
                        } else {
                            responseBuffer.toString()
                        }

                        val muted = authManager.getIsTtsMuted()
                        _assistantState.value = AssistantUiState.Answer(
                            query = trimmed,
                            response = finalResponse,
                            isMuted = muted,
                            isSpeaking = false
                        )

                        if (!muted && finalResponse.isNotBlank()) {
                            val lang = authManager.getVoiceLanguage()
                            ttsManager.speak(finalResponse, lang)
                        }
                    }
                    is StreamChatEvent.Error -> {
                        android.util.Log.e("ChatViewModel", "Stream error received: ${event.message}")
                        if (responseBuffer.isNotEmpty()) {
                            val muted = authManager.getIsTtsMuted()
                            _assistantState.value = AssistantUiState.Answer(
                                query = trimmed,
                                response = responseBuffer.toString(),
                                isMuted = muted,
                                isSpeaking = false
                            )
                        } else {
                            // Show loading state while attempting fallback
                            _assistantState.value = AssistantUiState.Loading(trimmed)
                            when (val fallback = chatRepository.sendMessage(trimmed)) {
                                is Result.Success -> {
                                    val muted = authManager.getIsTtsMuted()
                                    _assistantState.value = AssistantUiState.Answer(
                                        query = trimmed,
                                        response = fallback.data,
                                        isMuted = muted,
                                        isSpeaking = false
                                    )
                                    if (!muted && fallback.data.isNotBlank()) {
                                        val lang = authManager.getVoiceLanguage()
                                        ttsManager.speak(fallback.data, lang)
                                    }
                                }
                                is Result.Error -> {
                                    _assistantState.value = AssistantUiState.Error(
                                        query = trimmed,
                                        message = fallback.exception.message ?: event.message
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    fun startDirectListening(initialText: String = "") {
        ttsManager.stop()
        _assistantState.value = AssistantUiState.DirectListening(liveText = initialText)
    }

    fun updateDirectListeningText(text: String) {
        val current = _assistantState.value
        if (current is AssistantUiState.DirectListening) {
            _assistantState.value = current.copy(liveText = text)
        }
    }

    fun askDirectQuestion(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        ttsManager.stop()
        _assistantState.value = AssistantUiState.DirectStreaming(query = trimmed, currentText = "")

        viewModelScope.launch {
            val responseBuffer = StringBuilder()

            chatRepository.streamMessage(trimmed, saveHistory = false).collect { event ->
                when (event) {
                    is StreamChatEvent.Start -> {
                        _assistantState.value = AssistantUiState.DirectStreaming(
                            query = trimmed,
                            currentText = ""
                        )
                    }
                    is StreamChatEvent.Token -> {
                        responseBuffer.append(event.token)
                        _assistantState.value = AssistantUiState.DirectStreaming(
                            query = trimmed,
                            currentText = responseBuffer.toString()
                        )
                    }
                    is StreamChatEvent.Done -> {
                        val finalResponse = if (event.fullResponse.isNotBlank()) {
                            event.fullResponse
                        } else {
                            responseBuffer.toString()
                        }

                        val muted = authManager.getIsTtsMuted()
                        _assistantState.value = AssistantUiState.DirectAnswer(
                            query = trimmed,
                            response = finalResponse,
                            isMuted = muted,
                            isSpeaking = false
                        )

                        if (!muted && finalResponse.isNotBlank()) {
                            val lang = authManager.getVoiceLanguage()
                            ttsManager.speak(finalResponse, lang)
                        }
                    }
                    is StreamChatEvent.Error -> {
                        android.util.Log.e("ChatViewModel", "Direct stream error: ${event.message}")
                        if (responseBuffer.isNotEmpty()) {
                            val muted = authManager.getIsTtsMuted()
                            _assistantState.value = AssistantUiState.DirectAnswer(
                                query = trimmed,
                                response = responseBuffer.toString(),
                                isMuted = muted,
                                isSpeaking = false
                            )
                        } else {
                            _assistantState.value = AssistantUiState.Error(
                                query = trimmed,
                                message = event.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun toggleMute() {
        val current = _assistantState.value
        val newMutedState = !isTtsMuted.value

        viewModelScope.launch {
            authManager.saveTtsMuted(newMutedState)

            when (current) {
                is AssistantUiState.Answer -> {
                    _assistantState.value = current.copy(isMuted = newMutedState)
                    if (newMutedState) {
                        ttsManager.stop()
                    } else {
                        val lang = authManager.getVoiceLanguage()
                        ttsManager.speak(current.response, lang)
                    }
                }
                is AssistantUiState.DirectAnswer -> {
                    _assistantState.value = current.copy(isMuted = newMutedState)
                    if (newMutedState) {
                        ttsManager.stop()
                    } else {
                        val lang = authManager.getVoiceLanguage()
                        ttsManager.speak(current.response, lang)
                    }
                }
                else -> {}
            }
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun resetToIdle() {
        ttsManager.stop()
        _assistantState.value = AssistantUiState.Idle
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            resetToIdle()
        }
    }

    fun showNotificationAnswer(title: String, body: String) {
        ttsManager.stop()
        viewModelScope.launch {
            val muted = authManager.getIsTtsMuted()
            _assistantState.value = AssistantUiState.Answer(
                query = title,
                response = body,
                isMuted = muted,
                isSpeaking = false
            )
            if (!muted && body.isNotBlank()) {
                val lang = authManager.getVoiceLanguage()
                ttsManager.speak(body, lang)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }
}
