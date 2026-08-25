package com.hermes.wearos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.wearos.core.utils.Result
import com.hermes.wearos.data.local.ChatMessageEntity
import com.hermes.wearos.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val messages: StateFlow<List<ChatMessageEntity>> = chatRepository.recentMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.getRecentMessagesSync(10).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }

            when (val result = chatRepository.sendMessage(message)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            error = null,
                            currentResponse = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            error = result.exception.message ?: "Gagal mengirim"
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearResponse() {
        _uiState.update { it.copy(currentResponse = null) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
        }
    }
}

data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val isSending: Boolean = false,
    val error: String? = null,
    val currentResponse: String? = null
)
