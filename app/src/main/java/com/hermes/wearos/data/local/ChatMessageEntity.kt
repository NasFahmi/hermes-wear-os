package com.hermes.wearos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val message: String,
    val response: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSending: Boolean = false,
    val isError: Boolean = false
)
