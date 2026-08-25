package com.hermes.wearos.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerStatus(
    val cpu: Float,
    val ram: Float,
    val disk: Float,
    val uptime: Long,
    val status: ServerHealth = ServerHealth.NORMAL
)

enum class ServerHealth {
    NORMAL,
    WARNING,
    CRITICAL,
    OFFLINE
}

@Serializable
data class MarketSummary(
    val symbol: String,
    val price: Float,
    val change24h: Float,
    val changePercent: Float,
    val trend: MarketTrend = MarketTrend.NEUTRAL
)

enum class MarketTrend {
    BULLISH,
    BEARISH,
    NEUTRAL
}

@Serializable
data class CronStatus(
    val name: String,
    val status: CronRunStatus,
    val lastRun: Long? = null,
    val nextRun: Long? = null
)

enum class CronRunStatus {
    SUCCESS,
    FAILED,
    RUNNING,
    SCHEDULED
}
