package com.hermes.wearos.services.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionService : Service() {

    @Inject
    lateinit var notificationManager: WearNotificationManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val notificationId = it.getStringExtra(EXTRA_NOTIFICATION_ID) ?: return@let
            val action = it.getStringExtra(EXTRA_ACTION)

            when (action) {
                ACTION_DISMISS -> notificationManager.dismissNotification(notificationId)
                ACTION_TAP -> {
                    // Handle notification tap — could navigate to detail
                    notificationManager.dismissNotification(notificationId)
                }
            }
        }

        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_ACTION = "action"

        const val ACTION_DISMISS = "dismiss"
        const val ACTION_TAP = "tap"

        fun dismissIntent(context: Context, notificationId: String): Intent {
            return Intent(context, NotificationActionService::class.java).apply {
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_ACTION, ACTION_DISMISS)
            }
        }
    }
}
