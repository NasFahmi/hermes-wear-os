package com.hermes.wearos.services.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hermes.wearos.core.auth.AuthManager
import com.hermes.wearos.data.repository.ChatRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var wearNotificationManager: WearNotificationManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "HermesFCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")

        serviceScope.launch {
            authManager.saveFcmToken(token)
            chatRepository.registerDevice(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        // 1. Extract title and body from notification payload or data payload
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Hermes"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["response"]
            ?: "Pesan baru dari Hermes"

        // 2. Trigger native Wear OS high-priority vibration notification
        wearNotificationManager.showFcmNotification(
            title = title,
            body = body,
            messageId = remoteMessage.messageId ?: System.currentTimeMillis().toString(),
            data = remoteMessage.data
        )
    }
}
