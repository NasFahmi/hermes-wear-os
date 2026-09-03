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

sealed interface AssistantUiState {
    data object Idle : AssistantUiState
    data class Loading(val query: String) : AssistantUiState
    data class Answer(
        val query: String,
        val response: String,
        val isMuted: Boolean = true,
        val isSpeaking: Boolean = false
    ) : AssistantUiState
    data class Error(val query: String, val message: String) : AssistantUiState
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
        // Sync speaking state into Answer if current state is Answer
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { speaking ->
                val current = _assistantState.value
                if (current is AssistantUiState.Answer) {
                    _assistantState.value = current.copy(isSpeaking = speaking)
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
            when (val result = chatRepository.sendMessage(trimmed)) {
                is Result.Success -> {
                    val muted = authManager.getIsTtsMuted()
                    _assistantState.value = AssistantUiState.Answer(
                        query = trimmed,
                        response = result.data,
                        isMuted = muted,
                        isSpeaking = false
                    )

                    if (!muted) {
                        val lang = authManager.getVoiceLanguage()
                        ttsManager.speak(result.data, lang)
                    }
                }
                is Result.Error -> {
                    _assistantState.value = AssistantUiState.Error(
                        query = trimmed,
                        message = result.exception.message ?: "Gagal mendapatkan jawaban"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun toggleMute() {
        val current = _assistantState.value
        val newMutedState = !isTtsMuted.value

        viewModelScope.launch {
            authManager.saveTtsMuted(newMutedState)

            if (current is AssistantUiState.Answer) {
                _assistantState.value = current.copy(isMuted = newMutedState)

                if (newMutedState) {
                    ttsManager.stop()
                } else {
                    val lang = authManager.getVoiceLanguage()
                    ttsManager.speak(current.response, lang)
                }
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

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }
}
