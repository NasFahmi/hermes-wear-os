package com.hermes.wearos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.wearos.core.auth.AuthManager
import com.hermes.wearos.core.utils.onSuccess
import com.hermes.wearos.data.models.CronFeed
import com.hermes.wearos.data.models.NotificationPayload
import com.hermes.wearos.data.models.QuickAction
import com.hermes.wearos.data.models.ServerStatus
import com.hermes.wearos.data.repository.ChatRepository
import com.hermes.wearos.services.notification.WearNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authManager: AuthManager,
    private val notificationManager: WearNotificationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationPayload>>(emptyList())
    val notifications: StateFlow<List<NotificationPayload>> = _notifications.asStateFlow()

    private val _cronFeed = MutableStateFlow<List<CronFeed>>(emptyList())
    val cronFeed: StateFlow<List<CronFeed>> = _cronFeed.asStateFlow()

    private val _serverStatus = MutableStateFlow<ServerStatus?>(null)
    val serverStatus: StateFlow<ServerStatus?> = _serverStatus.asStateFlow()

    val serverUrl: StateFlow<String> = authManager.serverUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://hermes-api.example.com/")

    val apiToken: StateFlow<String?> = authManager.token
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Quick Actions defined in PRD FR-008
    private val quickActions = listOf(
        QuickAction(id = "market", label = "Market", icon = "📈", command = "Market summary"),
        QuickAction(id = "server", label = "Server", icon = "🖥", command = "Status server"),
        QuickAction(id = "cron", label = "Cron", icon = "⏰", command = "Status cron"),
        QuickAction(id = "agenda", label = "Agenda", icon = "📋", command = "Agenda hari ini")
    )

    // Network requests are only called on explicit user action (e.g. Test Connection)
    // to keep Wear OS startup instantaneous and responsive.

    fun getQuickActions(): List<QuickAction> = quickActions

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            // Check connection
            val connected = chatRepository.testConnection()
            _uiState.update { it.copy(isConnected = connected) }

            // Fetch server status
            chatRepository.fetchServerStatus().onSuccess { status ->
                _serverStatus.value = status
            }

            // Fetch notifications
            chatRepository.fetchNotifications().onSuccess { notifs ->
                _notifications.value = notifs
            }

            // Fetch cron feed
            chatRepository.fetchCronFeed().onSuccess { crons ->
                _cronFeed.value = crons
            }

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun saveServerUrl(url: String) {
        viewModelScope.launch {
            authManager.saveServerUrl(url)
            refreshAll()
        }
    }

    fun saveApiToken(token: String) {
        viewModelScope.launch {
            authManager.saveToken(token)
            refreshAll()
        }
    }

    fun addNotification(notification: NotificationPayload) {
        val current = _notifications.value.toMutableList()
        current.add(0, notification)
        _notifications.value = current.take(20)

        // Show local push notification
        val level = when (notification.type) {
            com.hermes.wearos.data.models.NotificationType.CRITICAL ->
                com.hermes.wearos.domain.entities.NotificationLevel.CRITICAL
            com.hermes.wearos.data.models.NotificationType.IMPORTANT ->
                com.hermes.wearos.domain.entities.NotificationLevel.IMPORTANT
            com.hermes.wearos.data.models.NotificationType.INFORMATIONAL ->
                com.hermes.wearos.domain.entities.NotificationLevel.INFORMATIONAL
        }
        notificationManager.showNotification(
            id = notification.id,
            title = notification.title,
            body = notification.body,
            level = level,
            actionUrl = notification.actionUrl
        )
    }

    fun dismissNotification(id: String) {
        _notifications.update { list -> list.filterNot { it.id == id } }
        notificationManager.dismissNotification(id)
    }
}

data class MainUiState(
    val currentRoute: String = "home",
    val isConnected: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)
