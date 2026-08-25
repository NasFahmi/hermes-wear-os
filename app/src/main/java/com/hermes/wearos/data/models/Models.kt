package com.hermes.wearos.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val message: String,
    val channel: String = "wearos"
)

@Serializable
data class ChatResponse(
    val response: String,
    val timestamp: Long? = null
)

@Serializable
data class DeviceRegistration(
    val deviceId: String,
    val deviceModel: String,
    val platform: String = "wearos"
)

@Serializable
data class NotificationPayload(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val timestamp: Long,
    val actionUrl: String? = null
)

enum class NotificationType {
    CRITICAL,
    IMPORTANT,
    INFORMATIONAL
}

@Serializable
data class CronFeed(
    val id: String,
    val name: String,
    val message: String,
    val timestamp: Long,
    val target: String = "wearos"
)

@Serializable
data class QuickAction(
    val id: String,
    val label: String,
    val icon: String,
    val command: String
)
