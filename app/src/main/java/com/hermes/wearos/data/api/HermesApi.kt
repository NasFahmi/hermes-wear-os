package com.hermes.wearos.data.api

import com.hermes.wearos.core.auth.AuthManager
import com.hermes.wearos.core.network.ApiConfig
import com.hermes.wearos.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HermesApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val authManager: AuthManager
) {
    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private suspend fun buildUrl(endpoint: String): String {
        val baseUrl = authManager.getServerUrl()
        val formattedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return "$formattedBase$endpoint"
    }

    suspend fun sendMessage(message: String): ChatResponse = withContext(Dispatchers.IO) {
        val requestPayload = ChatRequest(message = message, channel = ApiConfig.WEAR_CHANNEL)
        val body = json.encodeToString(ChatRequest.serializer(), requestPayload)
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(buildUrl(ApiConfig.ENDPOINT_CHAT))
            .post(body)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                throw ApiException(response.code, errorBody)
            }
            val responseBody = response.body?.string() ?: throw ApiException(0, "Empty response")
            json.decodeFromString<ChatResponse>(responseBody)
        }
    }

    fun streamChat(message: String, deviceId: String): Flow<StreamChatEvent> = callbackFlow {
        val requestPayload = ChatRequest(message = message, channel = ApiConfig.WEAR_CHANNEL)
        val body = json.encodeToString(ChatRequest.serializer(), requestPayload)
            .toRequestBody(JSON_MEDIA_TYPE)

        val url = buildUrl(ApiConfig.ENDPOINT_CHAT_STREAM)
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Device-Id", deviceId)
            .post(body)
            .build()

        val sseClient = okHttpClient.newBuilder()
            .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        val factory = EventSources.createFactory(sseClient)
        val eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                android.util.Log.d("HermesStream", "onOpen: HTTP ${response.code}")
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                android.util.Log.d("HermesStream", "onEvent: type=$type, data=$data")
                val event = parseStreamEvent(type, data)
                if (event != null) {
                    when (event) {
                        is StreamChatEvent.Done -> {
                            trySend(event)
                            close()
                        }
                        is StreamChatEvent.Error -> {
                            trySend(event)
                            close()
                        }
                        else -> {
                            trySend(event)
                        }
                    }
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val errMsg = t?.message ?: (response?.let { "HTTP ${it.code}: ${it.message}" } ?: "Koneksi stream terputus")
                android.util.Log.e("HermesStream", "onFailure: $errMsg", t)
                trySend(StreamChatEvent.Error(errMsg))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                android.util.Log.d("HermesStream", "onClosed")
                close()
            }
        })

        awaitClose {
            eventSource.cancel()
        }
    }

    private fun parseStreamEvent(eventType: String?, data: String): StreamChatEvent? {
        val trimmed = data.trim()
        if (trimmed.isEmpty()) return null

        if (trimmed == "[DONE]" || eventType == "done") {
            try {
                val element = json.parseToJsonElement(trimmed)
                if (element is JsonObject) {
                    val full = element["fullResponse"]?.jsonPrimitive?.contentOrNull
                        ?: element["response"]?.jsonPrimitive?.contentOrNull
                    return StreamChatEvent.Done(full ?: "")
                }
            } catch (_: Exception) {}
            return StreamChatEvent.Done("")
        }

        try {
            val element = json.parseToJsonElement(trimmed)
            if (element is JsonObject) {
                val error = element["error"]?.jsonPrimitive?.contentOrNull
                if (error != null) {
                    return StreamChatEvent.Error(error)
                }

                val fullResponse = element["fullResponse"]?.jsonPrimitive?.contentOrNull
                    ?: element["response"]?.jsonPrimitive?.contentOrNull
                val typeVal = element["type"]?.jsonPrimitive?.contentOrNull ?: eventType
                val status = element["status"]?.jsonPrimitive?.contentOrNull

                // Check for completion first
                if (typeVal == "done" || status == "completed" || status == "done" || (fullResponse != null && element["token"] == null)) {
                    return StreamChatEvent.Done(fullResponse ?: "")
                }

                // Check for actual word token
                val token = element["token"]?.jsonPrimitive?.contentOrNull
                    ?: element["content"]?.jsonPrimitive?.contentOrNull
                    ?: element["delta"]?.jsonPrimitive?.contentOrNull

                if (token != null) {
                    return StreamChatEvent.Token(token)
                }

                // Check for task start metadata - DO NOT emit as text!
                val taskId = element["taskId"]?.jsonPrimitive?.contentOrNull
                if (typeVal == "start" || status == "processing" || taskId != null) {
                    return StreamChatEvent.Start(taskId ?: "")
                }
            }
        } catch (_: Exception) {
            // Raw text token (not JSON)
            if (!trimmed.startsWith("{") && !trimmed.endsWith("}")) {
                return StreamChatEvent.Token(data)
            }
        }

        return null
    }

    suspend fun registerDevice(
        deviceId: String,
        deviceModel: String,
        fcmToken: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val payload = DeviceRegistration(
            deviceId = deviceId,
            deviceModel = deviceModel,
            fcmToken = fcmToken
        )
        val body = json.encodeToString(DeviceRegistration.serializer(), payload)
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(buildUrl(ApiConfig.ENDPOINT_REGISTER))
            .post(body)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchNotifications(): List<NotificationPayload> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(buildUrl(ApiConfig.ENDPOINT_NOTIFICATIONS))
            .get()
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string() ?: return@use emptyList()
                json.decodeFromString<List<NotificationPayload>>(body)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchCronFeed(): List<CronFeed> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(buildUrl(ApiConfig.ENDPOINT_CRON))
            .get()
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string() ?: return@use emptyList()
                json.decodeFromString<List<CronFeed>>(body)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchServerStatus(): ServerStatus = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(buildUrl(ApiConfig.ENDPOINT_SERVER))
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ApiException(response.code, "Gagal mengambil status server")
            }
            val body = response.body?.string() ?: throw ApiException(0, "Empty response")
            json.decodeFromString<ServerStatus>(body)
        }
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(buildUrl(ApiConfig.ENDPOINT_NOTIFICATIONS))
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful || response.code == 401
            }
        } catch (_: Exception) {
            false
        }
    }
}

class ApiException(val code: Int, override val message: String) : Exception(message)
