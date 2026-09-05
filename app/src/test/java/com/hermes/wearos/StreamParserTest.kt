package com.hermes.wearos

import com.hermes.wearos.data.models.StreamChatEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamParserTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun parseEvent(eventType: String?, data: String): StreamChatEvent? {
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

                if (typeVal == "done" || status == "completed" || status == "done" || (fullResponse != null && element["token"] == null)) {
                    return StreamChatEvent.Done(fullResponse ?: "")
                }

                val token = element["token"]?.jsonPrimitive?.contentOrNull
                    ?: element["content"]?.jsonPrimitive?.contentOrNull
                    ?: element["delta"]?.jsonPrimitive?.contentOrNull

                if (token != null) {
                    return StreamChatEvent.Token(token)
                }

                val taskId = element["taskId"]?.jsonPrimitive?.contentOrNull
                if (typeVal == "start" || status == "processing" || taskId != null) {
                    return StreamChatEvent.Start(taskId ?: "")
                }
            }
        } catch (_: Exception) {
            if (!trimmed.startsWith("{") && !trimmed.endsWith("}")) {
                return StreamChatEvent.Token(data)
            }
        }

        return null
    }

    @Test
    fun testRealVpsEvents() {
        // Event 1: start
        val ev1 = parseEvent("start", """{"taskId": "task-3e9bdd69", "status": "processing"}""")
        assertTrue(ev1 is StreamChatEvent.Start)
        assertEquals("task-3e9bdd69", (ev1 as StreamChatEvent.Start).taskId)

        // Event 2: token
        val ev2 = parseEvent("token", """{"token": "Investasi Anda: Rp 1.500.000"}""")
        assertTrue(ev2 is StreamChatEvent.Token)
        assertEquals("Investasi Anda: Rp 1.500.000", (ev2 as StreamChatEvent.Token).token)

        // Event 3: done
        val ev3 = parseEvent("done", """{"taskId": "task-3e9bdd69", "fullResponse": "Investasi Anda: Rp 1.500.000"}""")
        assertTrue(ev3 is StreamChatEvent.Done)
        assertEquals("Investasi Anda: Rp 1.500.000", (ev3 as StreamChatEvent.Done).fullResponse)
    }
}
