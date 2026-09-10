package com.example.network

import com.example.data.models.TradeDirection
import com.squareup.moshi.JsonClass

data class TradeSignalEvent(
    val signalId: String,
    val idempotencyKey: String = "",
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

@JsonClass(generateAdapter = true)
data class TradeSignalEnvelope(
    val type: String,
    val payload: TradeSignalEvent,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class TradeSignalAckResponse(
    val status: String,
    val message: String? = null,
    val signalId: String? = null
)
