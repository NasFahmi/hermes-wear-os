package com.hermes.wearos.domain.entities

data class ChatMessage(
    val id: Long = 0,
    val message: String,
    val response: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSending: Boolean = false,
    val isError: Boolean = false
)

data class WearNotification(
    val id: String,
    val type: NotificationLevel,
    val title: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val actionUrl: String? = null
)

enum class NotificationLevel {
    CRITICAL,
    IMPORTANT,
    INFORMATIONAL
}

data class CronItem(
    val id: String,
    val name: String,
    val message: String,
    val timestamp: Long,
    val target: String = "wearos"
)
