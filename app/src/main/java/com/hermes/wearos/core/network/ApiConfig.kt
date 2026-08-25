package com.hermes.wearos.core.network

object ApiConfig {
    const val DEFAULT_BASE_URL = "http://43.134.102.35:8000/"
    const val DEFAULT_WS_URL = "ws://43.134.102.35:8000/ws"
    const val DEFAULT_API_TOKEN = "d0422c491b333cdb7287318bdd7bc6ab"
    const val CONNECT_TIMEOUT = 15_000L
    const val READ_TIMEOUT = 30_000L
    const val WEAR_CHANNEL = "wearos"
    const val MAX_RESPONSE_WORDS = 50

    const val ENDPOINT_CHAT = "api/wear/chat"
    const val ENDPOINT_REGISTER = "api/wear/register"
    const val ENDPOINT_NOTIFICATIONS = "api/wear/notifications"
    const val ENDPOINT_CRON = "api/wear/cron"
    const val ENDPOINT_SERVER = "api/wear/server"
    const val ENDPOINT_MARKET = "api/wear/market"
}
