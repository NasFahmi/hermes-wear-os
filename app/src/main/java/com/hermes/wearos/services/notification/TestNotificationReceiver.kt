package com.hermes.wearos.services.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TestNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        Log.d("TestNotification", "Broadcast received! Triggering notification...")
        val title = intent?.getStringExtra("title") ?: "Hermes Updates"
        val body = intent?.getStringExtra("body") ?: "Tes notifikasi getar berhasil terkirim ke smartwatch Anda!"
        
        try {
            val notificationManager = WearNotificationManager(context.applicationContext)
            notificationManager.showFcmNotification(title = title, body = body)
            Log.d("TestNotification", "Notification dispatched successfully: $title - $body")
        } catch (e: Exception) {
            Log.e("TestNotification", "Failed to dispatch notification", e)
        }
    }
}
