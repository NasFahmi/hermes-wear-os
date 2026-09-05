package com.hermes.wearos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.wearos.core.auth.AuthManager
import com.hermes.wearos.services.speech.SpeechRecognizerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val speechRecognizerManager: SpeechRecognizerManager,
    private val authManager: AuthManager
) : ViewModel() {

    companion object {
        private val WAKE_WORD_REGEX = Regex(
            """^(?:(?:halo|hello|hallo|hei|hey|yo|oy|oi)\s+)?hermes\b[\s,.:;!?-]*(.*)$""",
            RegexOption.IGNORE_CASE
        )
    }

    val speechState: StateFlow<SpeechRecognizerManager.SpeechState> =
        speechRecognizerManager.state

    val voiceLanguage: StateFlow<String> = authManager.voiceLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "id-ID")

    fun checkWakeWord(input: String): Pair<Boolean, String> {
        val trimmed = input.trim()
        val match = WAKE_WORD_REGEX.find(trimmed) ?: return Pair(false, "")
        val remainingQuery = match.groupValues.getOrNull(1)?.trim() ?: ""
        return Pair(true, remainingQuery)
    }

    fun startListening() {
        val currentLang = voiceLanguage.value
        speechRecognizerManager.startListening(currentLang)
    }

    fun startListeningWithLang(language: String) {
        speechRecognizerManager.startListening(language)
    }

    fun stopListening() {
        speechRecognizerManager.stopListening()
    }

    fun resetState() {
        speechRecognizerManager.resetState()
    }

    fun setVoiceLanguage(language: String) {
        viewModelScope.launch {
            authManager.saveVoiceLanguage(language)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
