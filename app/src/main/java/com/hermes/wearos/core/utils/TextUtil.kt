package com.hermes.wearos.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private const val PATTERN_TIME = "HH:mm"
    private const val PATTERN_DATE = "dd MMM"

    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat(PATTERN_TIME, Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat(PATTERN_DATE, Locale.getDefault()).format(Date(timestamp))
    }

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Baru saja"
            diff < 3_600_000 -> "${diff / 60_000}m lalu"
            diff < 86_400_000 -> "${diff / 3_600_000}j lalu"
            else -> formatDate(timestamp)
        }
    }
}

object WearTextUtils {
    const val MAX_RESPONSE_WORDS = 50

    fun truncateToWearLimit(text: String, maxWords: Int = MAX_RESPONSE_WORDS): String {
        val words = text.split(" ")
        return if (words.size <= maxWords) {
            text
        } else {
            words.take(maxWords).joinToString(" ") + "..."
        }
    }

    fun formatServerStatus(cpu: Float, ram: Float): String {
        return "CPU ${"%.0f".format(cpu)}% • RAM ${"%.0f".format(ram)}%"
    }
}
