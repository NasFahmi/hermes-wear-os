package com.hermes.wearos.services.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechRecognizerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    sealed class SpeechState {
        data object Idle : SpeechState()
        data class Listening(val partialText: String = "") : SpeechState()
        data class Result(val text: String) : SpeechState()
        data class Error(val message: String) : SpeechState()
    }

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state

    private var speechRecognizer: SpeechRecognizer? = null

    private fun createRecognizer(): SpeechRecognizer {
        return SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = SpeechState.Listening()
                }

                override fun onBeginningOfSpeech() {
                    _state.value = SpeechState.Listening()
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Suara tidak terdeteksi. Coba lagi."
                        SpeechRecognizer.ERROR_NETWORK -> "Error jaringan."
                        SpeechRecognizer.ERROR_AUDIO -> "Error audio/mikrofon."
                        SpeechRecognizer.ERROR_SERVER -> "Error server speech."
                        SpeechRecognizer.ERROR_CLIENT -> "Error klien speech."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu habis. Coba lagi."
                        else -> "Error speech ($error)"
                    }
                    _state.value = SpeechState.Error(errorMessage)
                }

                // Final complete speech results when user finishes speaking
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _state.value = SpeechState.Result(matches[0])
                    } else {
                        _state.value = SpeechState.Error("Tidak ada hasil.")
                    }
                }

                // Real-time interim preview - does NOT cut off speech listening
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _state.value = SpeechState.Listening(partialText = matches[0])
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening(language: String = "id-ID") {
        stopListening()

        try {
            speechRecognizer = createRecognizer()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    2500L
                )
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    2000L
                )
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                    1500L
                )
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Bicara untuk bertanya ke Hermes..."
                )
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _state.value = SpeechState.Error(e.message ?: "Gagal memulai mikrofon")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {
            // Ignore cancel errors
        }
        speechRecognizer?.destroy()
        speechRecognizer = null
        if (_state.value is SpeechState.Listening) {
            _state.value = SpeechState.Idle
        }
    }

    fun resetState() {
        _state.value = SpeechState.Idle
    }
}
