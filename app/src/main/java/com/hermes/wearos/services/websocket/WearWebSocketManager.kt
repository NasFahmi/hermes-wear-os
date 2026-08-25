package com.hermes.wearos.services.websocket

import com.hermes.wearos.core.auth.AuthManager
import com.hermes.wearos.core.network.ApiConfig
import com.hermes.wearos.data.models.NotificationPayload
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearWebSocketManager @Inject constructor(
    private val authManager: AuthManager,
    private val json: Json
) {
    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationPayload>>(emptyList())
    val notifications: StateFlow<List<NotificationPayload>> = _notifications.asStateFlow()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var retryCount = 0

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE })
        .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun connect(token: String) {
        disconnect()
        _connectionState.value = ConnectionState.Connecting

        scope.launch {
            val baseUrl = authManager.getServerUrl()
            val wsUrl = if (baseUrl.startsWith("https://")) {
                baseUrl.replaceFirst("https://", "wss://") + "ws"
            } else if (baseUrl.startsWith("http://")) {
                baseUrl.replaceFirst("http://", "ws://") + "ws"
            } else {
                ApiConfig.DEFAULT_WS_URL
            }

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("X-Channel", ApiConfig.WEAR_CHANNEL)
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    retryCount = 0
                    _connectionState.value = ConnectionState.Connected
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleIncomingMessage(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {}

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                    _connectionState.value = ConnectionState.Disconnected
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _connectionState.value = ConnectionState.Disconnected
                    scheduleReconnect(token)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _connectionState.value = ConnectionState.Error(t.message ?: "Koneksi WebSocket gagal")
                    scheduleReconnect(token)
                }
            })
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val payload = json.decodeFromString<NotificationPayload>(text)
            val current = _notifications.value.toMutableList()
            current.add(0, payload)
            _notifications.value = current.take(20)
        } catch (e: Exception) {
            // Ignore malformed messages
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        try {
            webSocket?.close(1000, "App disconnect")
        } catch (e: Exception) {
            // Ignore close errors
        }
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun scheduleReconnect(token: String) {
        reconnectJob?.cancel()

        if (retryCount >= 5) {
            _connectionState.value = ConnectionState.Error("Batas coba ulang koneksi tercapai")
            return
        }

        val delayMillis = minOf((retryCount + 1) * 5000L, 30000L)
        retryCount++

        reconnectJob = scope.launch {
            delay(delayMillis)
            connect(token)
        }
    }
}
