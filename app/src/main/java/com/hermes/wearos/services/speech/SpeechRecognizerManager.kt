package com.hermes.wearos.services.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Build
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
        data object Initializing : SpeechState()
        data class Listening(val partialText: String = "") : SpeechState()
        data class Result(val text: String) : SpeechState()
        data class Error(val message: String, val errorCode: Int = 0) : SpeechState()
    }

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    val isOnDeviceSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun warmUp() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    getOrCreateRecognizer()
                }
            } catch (e: Exception) {
                android.util.Log.w("SpeechRecognizer", "warmUp failed: ${e.message}")
            }
        }
    }

    private fun getOrCreateRecognizer(): SpeechRecognizer {
        speechRecognizer?.let { return it }

        val recognizer = try {
            if (isOnDeviceSupported) {
                android.util.Log.i("SpeechRecognizer", "Using On-Device Hardware Speech Recognition")
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                android.util.Log.i("SpeechRecognizer", "Using Standard System Speech Recognition")
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        } catch (e: Exception) {
            android.util.Log.w("SpeechRecognizer", "Fallback to default recognizer: ${e.message}")
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                android.util.Log.d("SpeechRecognizer", "onReadyForSpeech - Mic is officially open")
                _state.value = SpeechState.Listening()
            }

            override fun onBeginningOfSpeech() {
                android.util.Log.d("SpeechRecognizer", "onBeginningOfSpeech - User speech detected")
                if (_state.value !is SpeechState.Listening) {
                    _state.value = SpeechState.Listening()
                }
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

                destroyRecognizerInternal()
                _state.value = SpeechState.Error(errorMessage, errorCode = error)
            }

            // Final complete speech results when user finishes speaking
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val resultText = matches?.getOrNull(0)
                android.util.Log.d("SpeechRecognizer", "onResults: matches=$resultText")
                destroyRecognizerInternal()
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

                // Safely cancel any active audio session
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}

                _state.value = SpeechState.Initializing
                android.util.Log.d("SpeechRecognizer", "startListening: initializing lang=$language (onDevice=$isOnDeviceSupported)")

                val recognizer = getOrCreateRecognizer()

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(language, "id-ID", "id", "in-ID", "in"))
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
                        500L
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
            destroyRecognizerInternal()
            _state.value = SpeechState.Idle
        }
    }

    fun resetState() {
        mainHandler.post {
            destroyRecognizerInternal()
            _state.value = SpeechState.Idle
        }
    }

    fun destroy() {
        mainHandler.post {
            destroyRecognizerInternal()
            _state.value = SpeechState.Idle
        }
    }
}
