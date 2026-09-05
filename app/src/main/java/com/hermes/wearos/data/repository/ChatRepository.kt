package com.hermes.wearos.data.repository

import com.hermes.wearos.core.auth.AuthManager
import com.hermes.wearos.core.utils.Result
import com.hermes.wearos.data.api.HermesApi
import com.hermes.wearos.data.local.ChatDao
import com.hermes.wearos.data.local.ChatMessageEntity
import com.hermes.wearos.data.models.CronFeed
import com.hermes.wearos.data.models.NotificationPayload
import com.hermes.wearos.data.models.ServerStatus
import com.hermes.wearos.data.models.StreamChatEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val api: HermesApi,
    private val chatDao: ChatDao,
    private val authManager: AuthManager
) {
    val recentMessages: Flow<List<ChatMessageEntity>> =
        chatDao.getRecentMessages(10)

    fun getRecentMessagesSync(limit: Int = 10): Flow<List<ChatMessageEntity>> =
        chatDao.getRecentMessages(limit)

    suspend fun sendMessage(message: String): Result<String> = withContext(Dispatchers.IO) {
        val entityId = chatDao.insertMessage(
            ChatMessageEntity(
                message = message,
                response = null,
                isSending = true,
                isError = false
            )
        )

        try {
            val response = api.sendMessage(message)
            chatDao.updateResponse(entityId, response.response)
            Result.Success(response.response)
        } catch (e: Exception) {
            chatDao.markAsError(entityId)
            Result.Error(e)
        }
    }

    fun streamMessage(message: String, saveHistory: Boolean = true): Flow<StreamChatEvent> = flow {
        val entityId = if (saveHistory) {
            withContext(Dispatchers.IO) {
                chatDao.insertMessage(
                    ChatMessageEntity(
                        message = message,
                        response = null,
                        isSending = true,
                        isError = false
                    )
                )
            }
        } else {
            0L
        }

        val deviceId = authManager.getDeviceId()
        val accumulatedResponse = StringBuilder()

        try {
            api.streamChat(message, deviceId).collect { event ->
                when (event) {
                    is StreamChatEvent.Token -> {
                        accumulatedResponse.append(event.token)
                    }
                    is StreamChatEvent.Done -> {
                        val finalResp = if (event.fullResponse.isNotBlank()) event.fullResponse else accumulatedResponse.toString()
                        if (saveHistory && entityId != 0L) {
                            withContext(Dispatchers.IO) {
                                chatDao.updateResponse(entityId, finalResp)
                            }
                        }
                    }
                    is StreamChatEvent.Error -> {
                        if (saveHistory && entityId != 0L) {
                            withContext(Dispatchers.IO) {
                                chatDao.markAsError(entityId)
                            }
                        }
                    }
                    else -> {}
                }
                emit(event)
            }
        } catch (e: Exception) {
            if (saveHistory && entityId != 0L) {
                withContext(Dispatchers.IO) {
                    chatDao.markAsError(entityId)
                }
            }
            emit(StreamChatEvent.Error(e.message ?: "Koneksi stream terputus"))
        }
    }

    suspend fun registerDevice(fcmToken: String? = null): Boolean = withContext(Dispatchers.IO) {
        val deviceId = authManager.getDeviceId()
        val deviceModel = authManager.getDeviceModel()
        val token = fcmToken ?: authManager.getFcmToken()
        api.registerDevice(deviceId = deviceId, deviceModel = deviceModel, fcmToken = token)
    }

    suspend fun fetchNotifications(): Result<List<NotificationPayload>> = withContext(Dispatchers.IO) {
        try {
            val list = api.fetchNotifications()
            Result.Success(list)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun fetchCronFeed(): Result<List<CronFeed>> = withContext(Dispatchers.IO) {
        try {
            val list = api.fetchCronFeed()
            Result.Success(list)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun fetchServerStatus(): Result<ServerStatus> = withContext(Dispatchers.IO) {
        try {
            val status = api.fetchServerStatus()
            Result.Success(status)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        api.testConnection()
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        chatDao.clearAll()
    }

    suspend fun getMessageCount(): Int = withContext(Dispatchers.IO) {
        chatDao.getMessageCount()
    }
}
