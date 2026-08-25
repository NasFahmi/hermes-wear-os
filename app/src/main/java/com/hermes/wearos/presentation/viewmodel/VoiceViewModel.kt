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

    val speechState: StateFlow<SpeechRecognizerManager.SpeechState> =
        speechRecognizerManager.state

    val voiceLanguage: StateFlow<String> = authManager.voiceLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "id-ID")

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
