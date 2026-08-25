package com.hermes.wearos.services.websocket

import android.content.Intent
import com.google.android.gms.wearable.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WearableListenerServiceImpl : WearableListenerService() {

    @Inject
    lateinit var webSocketManager: WearWebSocketManager

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                val path = dataItem.uri.path ?: continue

                when (path) {
                    "/wear/token" -> {
                        val token = DataMapItem.fromDataItem(dataItem).dataMap.getString("token")
                        if (!token.isNullOrBlank()) {
                            webSocketManager.connect(token)
                        }
                    }
                    "/wear/notification" -> {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        // Forward to notification handler
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/wear/command" -> {
                val message = String(messageEvent.data)
                // Handle commands from phone
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        // Handle capability changes
    }
}
