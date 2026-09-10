package com.example.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * High-speed, non-blocking local HTTP Trade Webhook Client for Quant Vision AI.
 *
 * Sends POST requests to local Python / Flask / FastAPI server (e.g. http://192.168.0.102:5000/trade or ngrok URL).
 * JSON Payload: {"signal": "UP"} or {"signal": "DOWN"}
 * Operates in non-blocking IO coroutines with 1-second timeout.
 * Zero UI impact, completely silent on connection drops or offline server.
 */
object HttpTradeRelay {

    private const val TAG = "HttpTradeRelay"
    const val DEFAULT_HTTP_URL = "http://192.168.0.102:5000/trade"

    @Volatile
    var webhookUrl: String = DEFAULT_HTTP_URL
        set(value) {
            val sanitized = NetworkUrlSanitizer.sanitizeHttpWebhookUrl(value)
            field = if (sanitized.isNotBlank()) sanitized else DEFAULT_HTTP_URL
        }

    private val _relayStatus = MutableStateFlow(RelayStatus())
    val relayStatus: StateFlow<RelayStatus> = _relayStatus.asStateFlow()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val moshi: com.squareup.moshi.Moshi by lazy {
        com.squareup.moshi.Moshi.Builder().build()
    }
    private val envelopeAdapter by lazy {
        moshi.adapter(TradeSignalEnvelope::class.java)
    }
    private val ackAdapter by lazy {
        moshi.adapter(TradeSignalAckResponse::class.java)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Sends the trade command ("CLICK_BUY" / "CLICK_SELL" or "UP" / "DOWN")
     * instantly and asynchronously to the HTTP webhook endpoint using OkHttp.
     * Guaranteed non-blocking, completely decoupled from WebSocket state.
     */
    fun sendTradeSignal(signal: String, amount: Double = 100.0) {
        val normalized = when (signal.trim().uppercase(java.util.Locale.US)) {
            "UP", "BUY", "CLICK_BUY" -> "CLICK_BUY"
            "DOWN", "SELL", "CLICK_SELL" -> "CLICK_SELL"
            else -> return
        }

        val targetUrl = NetworkUrlSanitizer.sanitizeHttpWebhookUrl(webhookUrl)
        if (targetUrl.isBlank()) {
            WebSocketTradeRelay.addLog("[Error] HTTP Webhook Endpoint is blank. Configure URL in Auto Trade tab.")
            return
        }

        val validAmount = if (amount.isFinite() && amount > 0.0) amount else 100.0
        val now = System.currentTimeMillis()
        val action = if (normalized == "CLICK_BUY") "BUY" else "SELL"
        val direction = if (normalized == "CLICK_BUY") "UP" else "DOWN"

        _relayStatus.value = RelayStatus(
            command = normalized,
            status = RelayDeliveryStatus.SENDING,
            timestamp = now,
            message = "Posting to $targetUrl..."
        )
        WebSocketTradeRelay.addLog("[HTTP Sending] POST $targetUrl (signal=$direction, command=$normalized, amount=$validAmount)")

        scope.launch {
            val callStart = System.currentTimeMillis()
            try {
                val jsonPayload = """{"command":"$normalized","signal":"$direction","action":"$action","direction":"$direction","amount":$validAmount,"timestamp":$now}"""
                val body = jsonPayload.toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(targetUrl)
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - callStart
                    if (response.isSuccessful) {
                        _relayStatus.value = RelayStatus(
                            command = normalized,
                            status = RelayDeliveryStatus.DELIVERED,
                            latencyMs = latency,
                            message = "HTTP ${response.code}: Delivered",
                            timestamp = System.currentTimeMillis()
                        )
                        WebSocketTradeRelay.addLog("[Success] HTTP ${response.code} - Webhook Delivered ($normalized) [${latency}ms]")
                        Log.d(TAG, "HTTP Trade Webhook delivered: $normalized in ${latency}ms")
                    } else {
                        _relayStatus.value = RelayStatus(
                            command = normalized,
                            status = RelayDeliveryStatus.FAILED,
                            latencyMs = latency,
                            message = "HTTP ${response.code}",
                            timestamp = System.currentTimeMillis()
                        )
                        WebSocketTradeRelay.addLog("[Error] HTTP ${response.code} - Webhook Rejected ($normalized)")
                        Log.w(TAG, "HTTP Trade Webhook rejected with code: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                val errMsg = when (e) {
                    is java.net.ConnectException -> "Connection Refused - Check IP/Port/Firewall"
                    is java.net.SocketTimeoutException -> "Connection Timeout - Server not responding"
                    is java.net.UnknownHostException -> "Unknown Host - Invalid IP"
                    else -> e.message ?: "Webhook unreachable"
                }
                _relayStatus.value = RelayStatus(
                    command = normalized,
                    status = RelayDeliveryStatus.FAILED,
                    message = errMsg,
                    timestamp = System.currentTimeMillis()
                )
                WebSocketTradeRelay.addLog("[Error] HTTP Webhook Failed: $errMsg ($targetUrl)")
                Log.d(TAG, "HTTP Trade Webhook fail: ${e.message}")
            }
        }
    }

    /**
     * Sends structured TradeSignalEvent with telemetry, response code tracking, and acknowledgement.
     */
    fun sendTradeSignalEvent(event: TradeSignalEvent, amount: Double = 100.0) {
        val targetUrl = NetworkUrlSanitizer.sanitizeHttpWebhookUrl(webhookUrl)
        if (targetUrl.isBlank()) {
            WebSocketTradeRelay.addLog("[Error] HTTP Webhook Endpoint is blank. Configure URL in Auto Trade tab.")
            return
        }

        val now = System.currentTimeMillis()
        _relayStatus.value = RelayStatus(
            signalId = event.signalId,
            command = event.command,
            status = RelayDeliveryStatus.SENDING,
            timestamp = now,
            message = "Posting HTTP webhook..."
        )
        WebSocketTradeRelay.addLog("[HTTP Sending] Webhook POST to $targetUrl (${event.command})")

        scope.launch {
            val callStart = System.currentTimeMillis()
            try {
                val validAmount = if (amount.isFinite() && amount > 0.0) amount else 100.0
                val up = if (event.upPercentage.isFinite() && event.upPercentage in 0.0..100.0) event.upPercentage else 50.0
                val down = if (event.downPercentage.isFinite() && event.downPercentage in 0.0..100.0) event.downPercentage else 50.0
                val st = if (event.strength.isFinite() && event.strength in 0.0..100.0) event.strength else 50.0
                val envelope = TradeSignalEnvelope(
                    signalId = event.signalId,
                    idempotencyKey = event.idempotencyKey,
                    command = event.command,
                    direction = event.direction.name,
                    fingerprint = event.fingerprint,
                    upPercentage = up,
                    downPercentage = down,
                    strength = st,
                    availableTimeframes = event.availableTimeframes.toList(),
                    timestamp = now,
                    matrixId = event.matrixId,
                    amount = validAmount,
                    signal = event.direction.name
                )
                val jsonPayload = envelopeAdapter.toJson(envelope)
                val body = jsonPayload.toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(targetUrl)
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - callStart
                    if (response.isSuccessful) {
                        val rawBody = response.body?.string() ?: ""
                        var parsedAck: TradeSignalAckResponse? = null
                        try {
                            parsedAck = ackAdapter.fromJson(rawBody)
                        } catch (_: Exception) {}

                        val deliveryStatus = when (parsedAck?.status?.uppercase(java.util.Locale.US)) {
                            "ACCEPTED" -> RelayDeliveryStatus.ACCEPTED
                            "FILLED", "EXECUTED" -> RelayDeliveryStatus.FILLED
                            "REJECTED" -> RelayDeliveryStatus.REJECTED
                            "EXPIRED" -> RelayDeliveryStatus.EXPIRED
                            else -> RelayDeliveryStatus.ACKNOWLEDGED
                        }
                        _relayStatus.value = RelayStatus(
                            signalId = event.signalId,
                            idempotencyKey = event.idempotencyKey,
                            command = event.command,
                            status = deliveryStatus,
                            latencyMs = latency,
                            message = "HTTP ${response.code}: ${parsedAck?.status ?: rawBody.take(60)}",
                            timestamp = System.currentTimeMillis()
                        )
                        WebSocketTradeRelay.addLog("[Success] HTTP ${response.code} - Webhook Delivered (${event.command}) [${latency}ms]")
                        Log.d(TAG, "HTTP Trade Webhook acknowledged (${response.code}) in ${latency}ms")
                    } else {
                        _relayStatus.value = RelayStatus(
                            signalId = event.signalId,
                            idempotencyKey = event.idempotencyKey,
                            command = event.command,
                            status = RelayDeliveryStatus.FAILED,
                            latencyMs = latency,
                            message = "HTTP ${response.code}",
                            timestamp = System.currentTimeMillis()
                        )
                        WebSocketTradeRelay.addLog("[Error] HTTP ${response.code} - Webhook Rejected (${event.command})")
                        Log.w(TAG, "HTTP Trade Webhook returned code: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                val errMsg = when (e) {
                    is java.net.ConnectException -> "Connection Refused - Check IP/Port/Firewall"
                    is java.net.SocketTimeoutException -> "Connection Timeout - Server not responding"
                    is java.net.UnknownHostException -> "Unknown Host - Invalid IP"
                    else -> e.message ?: "Webhook unreachable"
                }
                _relayStatus.value = RelayStatus(
                    signalId = event.signalId,
                    idempotencyKey = event.idempotencyKey,
                    command = event.command,
                    status = RelayDeliveryStatus.FAILED,
                    message = errMsg,
                    timestamp = System.currentTimeMillis()
                )
                WebSocketTradeRelay.addLog("[Error] HTTP Webhook Failed: $errMsg ($targetUrl)")
                Log.d(TAG, "HTTP Trade Webhook fail: ${e.message}")
            }
        }
    }
}
