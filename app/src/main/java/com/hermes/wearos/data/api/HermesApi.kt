package com.hermes.wearos.data.api

import com.hermes.wearos.core.auth.AuthManager
import com.hermes.wearos.core.network.ApiConfig
import com.hermes.wearos.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    suspend fun registerDevice(deviceId: String, deviceModel: String): Boolean = withContext(Dispatchers.IO) {
        val payload = DeviceRegistration(
            deviceId = deviceId,
            deviceModel = deviceModel
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
