package com.hermes.wearos.services.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TtsManager"
        private const val GOOGLE_TTS_ENGINE = "com.google.android.tts"
    }

    private var tts: TextToSpeech? = null
    private var pendingSpeak: Pair<String, String>? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        try {
            // Explicitly use Google Speech Services (com.google.android.tts)
            Log.i(TAG, "Initializing TextToSpeech with engine: $GOOGLE_TTS_ENGINE")
            tts = TextToSpeech(context.applicationContext, this, GOOGLE_TTS_ENGINE)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Google TextToSpeech, falling back to default", e)
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e2: Exception) {
                Log.e(TAG, "Error initializing fallback TextToSpeech", e2)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                try {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    engine.setAudioAttributes(audioAttributes)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set audio attributes", e)
                }

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        Log.e(TAG, "TTS Utterance error: $errorCode")
                    }
                })
            }
            _isReady.value = true
            Log.i(TAG, "TTS Engine successfully initialized")

            pendingSpeak?.let { (text, lang) ->
                pendingSpeak = null
                speak(text, lang)
            }
        } else {
            _isReady.value = false
            Log.e(TAG, "TTS onInit failed: $status")
        }
    }

    fun speak(text: String, languageTag: String = "id-ID") {
        if (text.isBlank()) return

        if (!_isReady.value || tts == null) {
            Log.d(TAG, "TTS not ready yet, queuing speech")
            pendingSpeak = Pair(text, languageTag)
            if (tts == null) initTts()
            return
        }

        val engine = tts ?: return

        val targetLocale = when (languageTag.lowercase()) {
            "en-us", "en" -> Locale.US
            else -> Locale("id", "ID")
        }

        try {
            var res = engine.setLanguage(targetLocale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Target locale $targetLocale not supported, checking default")
                res = engine.setLanguage(Locale.getDefault())
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Default locale not supported, falling back to US English")
                    engine.setLanguage(Locale.US)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception setting TTS language", e)
            try { engine.setLanguage(Locale.US) } catch (_: Exception) {}
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "hermes_response")
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "hermes_response")
        Log.i(TAG, "TTS speak result: $result for: $text")
    }

    fun stop() {
        pendingSpeak = null
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        _isReady.value = false
        _isSpeaking.value = false
    }
}
