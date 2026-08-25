package com.hermes.wearos.services.speech

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VoiceInputActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TEXT = "voice_text"
        const val EXTRA_LANGUAGE = "voice_language"

        fun newIntent(context: Context, language: String = "id-ID"): Intent {
            return Intent(context, VoiceInputActivity::class.java).apply {
                putExtra(EXTRA_LANGUAGE, language)
            }
        }
    }

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull() ?: ""

            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(EXTRA_TEXT, text)
            })
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val language = intent.getStringExtra(EXTRA_LANGUAGE) ?: "id-ID"

        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechLauncher.launch(speechIntent)
    }
}
