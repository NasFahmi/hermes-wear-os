package com.hermes.wearos.data.models

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModelsSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun testChatRequestSerialization() {
        val request = ChatRequest(message = "Status server?", channel = "wearos")
        val jsonStr = json.encodeToString(ChatRequest.serializer(), request)
        val decoded = json.decodeFromString<ChatRequest>(jsonStr)

        assertEquals("Status server?", decoded.message)
        assertEquals("wearos", decoded.channel)
    }

    @Test
    fun testChatResponseDeserialization() {
        val jsonStr = """{"response": "Server normal. CPU 21%. RAM 58%."}"""
        val decoded = json.decodeFromString<ChatResponse>(jsonStr)

        assertEquals("Server normal. CPU 21%. RAM 58%.", decoded.response)
    }

    @Test
    fun testNotificationPayloadSerialization() {
        val notif = NotificationPayload(
            id = "notif-1",
            type = NotificationType.CRITICAL,
            title = "VPS Down",
            body = "Server VPS utama tidak merespon",
            timestamp = 1720000000000L
        )
        val jsonStr = json.encodeToString(NotificationPayload.serializer(), notif)
        val decoded = json.decodeFromString<NotificationPayload>(jsonStr)

        assertEquals("notif-1", decoded.id)
        assertEquals(NotificationType.CRITICAL, decoded.type)
        assertEquals("VPS Down", decoded.title)
    }

    @Test
    fun testCronFeedSerialization() {
        val cron = CronFeed(
            id = "cron-1",
            name = "Daily Briefing",
            message = "Market BTC naik 4.5%. Agenda meeting pukul 14:00.",
            timestamp = 1720000000000L,
            target = "wearos"
        )
        val jsonStr = json.encodeToString(CronFeed.serializer(), cron)
        val decoded = json.decodeFromString<CronFeed>(jsonStr)

        assertEquals("Daily Briefing", decoded.name)
        assertEquals("wearos", decoded.target)
    }

    @Test
    fun testServerStatusSerialization() {
        val status = ServerStatus(
            cpu = 21.0f,
            ram = 58.0f,
            disk = 42.0f,
            uptime = 360000L,
            status = ServerHealth.NORMAL
        )
        val jsonStr = json.encodeToString(ServerStatus.serializer(), status)
        val decoded = json.decodeFromString<ServerStatus>(jsonStr)

        assertEquals(21.0f, decoded.cpu, 0.01f)
        assertEquals(ServerHealth.NORMAL, decoded.status)
    }
}
