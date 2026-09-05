package com.hermes.wearos.services.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        data class Error(val message: String, val errorCode: Int = 0) : SpeechState()
    }

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private fun getOrCreateRecognizer(): SpeechRecognizer {
        speechRecognizer?.let { return it }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    android.util.Log.d("SpeechRecognizer", "onReadyForSpeech")
                    _state.value = SpeechState.Listening()
                }

                override fun onBeginningOfSpeech() {
                    android.util.Log.d("SpeechRecognizer", "onBeginningOfSpeech")
                    _state.value = SpeechState.Listening()
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    android.util.Log.d("SpeechRecognizer", "onEndOfSpeech")
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Suara tidak terdeteksi. Coba lagi."
                        SpeechRecognizer.ERROR_NETWORK -> "Koneksi internet bermasalah."
                        SpeechRecognizer.ERROR_AUDIO -> "Error audio/mikrofon."
                        SpeechRecognizer.ERROR_SERVER -> "Error server speech."
                        SpeechRecognizer.ERROR_CLIENT -> "Koneksi speech terputus."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu bicara habis."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Mikrofon sibuk, coba lagi."
                        10 -> "Terlalu banyak permintaan."
                        11 -> "Koneksi server terputus. Silakan coba lagi."
                        12 -> "Bahasa tidak didukung."
                        13 -> "Bahasa belum tersedia."
                        else -> "Error speech ($error)"
                    }
                    android.util.Log.e("SpeechRecognizer", "onError: code=$error, msg=$errorMessage")
                    
                    // Always clean up disconnected/corrupted recognizer so next listen binds fresh
                    if (error == SpeechRecognizer.ERROR_CLIENT || 
                        error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || 
                        error == 11 /* ERROR_SERVER_DISCONNECTED */ ||
                        error == SpeechRecognizer.ERROR_SERVER ||
                        error == SpeechRecognizer.ERROR_NETWORK) {
                        destroyRecognizerInternal()
                    }
                    
                    _state.value = SpeechState.Error(errorMessage, errorCode = error)
                }

                // Final complete speech results when user finishes speaking
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val resultText = matches?.getOrNull(0)
                    android.util.Log.d("SpeechRecognizer", "onResults: matches=$resultText")
                    if (!resultText.isNullOrBlank()) {
                        _state.value = SpeechState.Result(resultText)
                    } else {
                        _state.value = SpeechState.Error("Tidak ada suara terdeteksi.")
                    }
                }

                // Real-time interim preview
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.getOrNull(0) ?: ""
                    android.util.Log.d("SpeechRecognizer", "onPartialResults: $text")
                    if (text.isNotBlank()) {
                        _state.value = SpeechState.Listening(partialText = text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        speechRecognizer = recognizer
        return recognizer
    }

    private fun destroyRecognizerInternal() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun startListening(language: String = "id-ID") {
        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    android.util.Log.e("SpeechRecognizer", "Speech recognition not available")
                    _state.value = SpeechState.Error("Fitur speech tidak tersedia di jam ini.", errorCode = -1)
                    return@post
                }

                // Cancel previous session before starting a new one
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}

                _state.value = SpeechState.Idle
                android.util.Log.d("SpeechRecognizer", "startListening: lang=$language")

                val recognizer = getOrCreateRecognizer()

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
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
                        3500L
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                        2500L
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

                recognizer.startListening(intent)
            } catch (e: Exception) {
                android.util.Log.e("SpeechRecognizer", "Failed to start listening", e)
                destroyRecognizerInternal()
                _state.value = SpeechState.Error(e.message ?: "Gagal memulai mikrofon", errorCode = -1)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Exception) {
                // Ignore cancel errors
            }
            _state.value = SpeechState.Idle
        }
    }

    fun resetState() {
        _state.value = SpeechState.Idle
    }

    fun destroy() {
        mainHandler.post {
            destroyRecognizerInternal()
            _state.value = SpeechState.Idle
        }
    }
}
