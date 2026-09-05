package com.hermes.wearos.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessaging
import com.hermes.wearos.presentation.navigation.WearNavHost
import com.hermes.wearos.presentation.theme.HermesWearTheme
import com.hermes.wearos.presentation.viewmodel.ChatViewModel
import com.hermes.wearos.presentation.viewmodel.MainViewModel
import com.hermes.wearos.presentation.viewmodel.VoiceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private val voiceViewModel: VoiceViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()
        initFcm()
        handleFcmNotificationIntent(intent)

        setContent {
            HermesWearTheme {
                WearNavHost(
                    mainViewModel = mainViewModel,
                    chatViewModel = chatViewModel,
                    voiceViewModel = voiceViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleFcmNotificationIntent(intent)
    }

    private fun initFcm() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrBlank()) {
                        mainViewModel.registerFcmToken(token)
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback if Google Play Services / Firebase is not initialized
        }
    }

    private fun handleFcmNotificationIntent(intent: Intent?) {
        val fcmTitle = intent?.getStringExtra("fcm_title")
        val fcmBody = intent?.getStringExtra("fcm_body")
        if (!fcmBody.isNullOrBlank()) {
            chatViewModel.showNotificationAnswer(
                title = fcmTitle ?: "Hermes Notification",
                body = fcmBody
            )
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
