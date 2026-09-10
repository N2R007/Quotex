package com.example.network

import com.example.data.models.TradeDirection
import com.squareup.moshi.JsonClass

data class TradeSignalEvent(
    val signalId: String,
    val idempotencyKey: String = java.util.UUID.randomUUID().toString(),
    val fingerprint: String,
    val direction: TradeDirection,
    val command: String,
    val upPercentage: Double,
    val downPercentage: Double,
    val strength: Double,
    val availableTimeframes: Set<String>,
    val timestamp: Long = System.currentTimeMillis(),
    val matrixId: String? = null
)

enum class RelayDeliveryStatus {
    IDLE,
    QUEUED,
    SENDING,
    DELIVERED,
    ACKNOWLEDGED,
    ACCEPTED,
    FILLED,
    REJECTED,
    EXPIRED,
    FAILED
}

data class RelayStatus(
    val signalId: String? = null,
    val idempotencyKey: String? = null,
    val command: String? = null,
    val status: RelayDeliveryStatus = RelayDeliveryStatus.IDLE,
    val latencyMs: Long = 0L,
    val message: String = "",
    val timestamp: Long = 0L
)

@JsonClass(generateAdapter = true)
data class TradeSignalEnvelope(
    val signalId: String,
    val idempotencyKey: String,
    val command: String,
    val direction: String,
    val fingerprint: String,
    val upPercentage: Double,
    val downPercentage: Double,
    val strength: Double,
    val availableTimeframes: List<String>,
    val timestamp: Long,
    val matrixId: String? = null,
    val amount: Double? = null,
    val signal: String? = null
)

@JsonClass(generateAdapter = true)
data class TradeSignalAckResponse(
    val signalId: String? = null,
    val idempotencyKey: String? = null,
    val status: String? = null,
    val executionPrice: Double? = null,
    val orderId: String? = null,
    val message: String? = null
)
