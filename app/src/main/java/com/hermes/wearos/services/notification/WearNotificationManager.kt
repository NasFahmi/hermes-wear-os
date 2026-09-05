package com.hermes.wearos.services.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hermes.wearos.R
import com.hermes.wearos.domain.entities.NotificationLevel
import com.hermes.wearos.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_CRITICAL = "hermes_critical"
        const val CHANNEL_IMPORTANT = "hermes_important"
        const val CHANNEL_INFO = "hermes_info"
        const val CHANNEL_HERMES_UPDATES = "hermes_updates"

        const val GROUP_MESSAGES = "hermes_messages"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_HERMES_UPDATES,
                "Hermes Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pembaruan chat dan laporan Hermes"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            },
            NotificationChannel(
                CHANNEL_CRITICAL,
                "Hermes - Critical",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts from Hermes"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_IMPORTANT,
                "Hermes - Important",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Important notifications from Hermes"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_INFO,
                "Hermes - Info",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Informational notifications from Hermes"
                enableVibration(false)
            }
        )

        notificationManager.createNotificationChannels(channels)
    }

    fun showFcmNotification(
        title: String,
        body: String,
        messageId: String = System.currentTimeMillis().toString(),
        data: Map<String, String> = emptyMap()
    ) {
        val notificationId = (messageId + System.currentTimeMillis()).hashCode()
        android.util.Log.d("WearNotification", "showFcmNotification called: id=$notificationId, title=$title, body=$body")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("fcm_title", title)
            putExtra("fcm_body", body)
            data.forEach { (k, v) -> putExtra(k, v) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_HERMES_UPDATES)
            .setSmallIcon(R.drawable.ic_hermes_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
        android.util.Log.d("WearNotification", "notificationManager.notify executed successfully for id=$notificationId")
    }

    fun showNotification(
        id: String,
        title: String,
        body: String,
        level: NotificationLevel,
        actionUrl: String? = null
    ) {
        val notificationId = id.hashCode()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            actionUrl?.let { putExtra("action_url", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when (level) {
            NotificationLevel.CRITICAL -> CHANNEL_CRITICAL
            NotificationLevel.IMPORTANT -> CHANNEL_IMPORTANT
            NotificationLevel.INFORMATIONAL -> CHANNEL_INFO
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_hermes_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                when (level) {
                    NotificationLevel.CRITICAL -> NotificationCompat.PRIORITY_MAX
                    NotificationLevel.IMPORTANT -> NotificationCompat.PRIORITY_DEFAULT
                    NotificationLevel.INFORMATIONAL -> NotificationCompat.PRIORITY_LOW
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_MESSAGES)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun dismissNotification(id: String) {
        notificationManager.cancel(id.hashCode())
    }
}
