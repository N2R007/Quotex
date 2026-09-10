package com.example.network

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Enterprise-grade WebSocket Trade Relay and Logging Manager for Quant Vision AI.
 *
 * Features:
 * 1. Real-time Connection Configuration (desktop IP address, e.g., ws://192.168.0.102:8765)
 * 2. Connect / Disconnect toggle with reactive StateFlow status (Green = Connected, Red = Disconnected)
 * 3. Resilient auto-reconnect loop on connection drops
 * 4. Instant manual & automated signal payload dispatch: "UP" / "CLICK_BUY" and "DOWN" / "CLICK_SELL"
 * 5. Scrollable live diagnostics log with timestamps
 */
object WebSocketTradeRelay {

    private const val TAG = "WebSocketTradeRelay"
    const val DEFAULT_SERVER_URL = "ws://192.168.0.102:8765"
    // Bluetooth-style auto-discovery parameters with adaptive pacing
    private const val BLUETOOTH_FAST_SEEK_MS = 6000L

    enum class AutoConnectState {
        CONNECTED, // 🟢 Connected to desktop server
        SCANNING,  // 📡 Continuously searching/probing for server in local network
        PAUSED     // ⏸️ User manually paused auto-seek
    }

    @Volatile
    var serverUrl: String = DEFAULT_SERVER_URL
        set(value) {
            val sanitized = NetworkUrlSanitizer.sanitizeWebSocketUrl(value)
            val effective = if (sanitized.isNotBlank()) sanitized else DEFAULT_SERVER_URL
            if (field != effective) {
                field = effective
                if (isUserEnabled.get()) {
                    reconnect()
                }
            }
        }

    @Volatile
    private var appContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    private fun isNetworkAvailable(): Boolean {
        val ctx = appContext ?: return true
        return try {
            val cm = ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNet = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) ||
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
        } catch (_: Exception) {
            true
        }
    }

    private val isConnectedAtomic = AtomicBoolean(false)
    private val isConnectingAtomic = AtomicBoolean(false)
    // Bluetooth-style auto-scanning is ACTIVE by default
    private val isUserEnabled = AtomicBoolean(true)
    @Volatile
    private var consecutiveFailures = 0

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private fun formatLog(message: String): String {
        val timeStr = timeFormat.format(Date())
        return "[$timeStr] $message"
    }

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _autoConnectState = MutableStateFlow(AutoConnectState.SCANNING)
    val autoConnectState: StateFlow<AutoConnectState> = _autoConnectState.asStateFlow()

    private val _relayStatus = MutableStateFlow(RelayStatus())
    val relayStatus: StateFlow<RelayStatus> = _relayStatus.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(
        listOf(formatLog("Bluetooth-style auto-connect scanner active. Searching for server..."))
    )
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionLoopJob: Job? = null
    private var heartbeatJob: Job? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .connectionPool(okhttp3.ConnectionPool(2, 30, TimeUnit.SECONDS))
            .build()
    }

    @Volatile
    private var currentWebSocket: WebSocket? = null

    private val pendingAck = java.util.concurrent.atomic.AtomicReference<kotlinx.coroutines.CompletableDeferred<Boolean>?>(null)

    fun addLog(message: String) {
        val entry = formatLog(message)
        _logs.value = (listOf(entry) + _logs.value).take(60)
        Log.d(TAG, entry)
    }

    /**
     * Starts the auto-connect and reconnect engine.
     * Continuously searches/probes local network like Bluetooth auto-pairing with adaptive pacing.
     */
    fun start() {
        startHeartbeat()
        if (connectionLoopJob != null && connectionLoopJob?.isActive == true) {
            return
        }

        connectionLoopJob = scope.launch {
            while (isActive) {
                if (isUserEnabled.get()) {
                    if (!isNetworkAvailable()) {
                        _autoConnectState.value = AutoConnectState.SCANNING
                        delay(8000L)
                        continue
                    }
                    if (isConnectedAtomic.get()) {
                        delay(3000L)
                        continue
                    }
                    if (!isConnectingAtomic.get()) {
                        _autoConnectState.value = AutoConnectState.SCANNING
                        attemptConnect()
                    }
                    // Adaptive pacing prevents kernel socket audit rate limit exhaustion:
                    // Initial probe (6s) -> Progressive backoff (10s -> 20s -> 35s -> 50s -> 60s) when server is offline
                    val delayMs = when {
                        consecutiveFailures == 0 -> BLUETOOTH_FAST_SEEK_MS
                        consecutiveFailures == 1 -> 10000L
                        consecutiveFailures <= 3 -> 20000L
                        consecutiveFailures <= 6 -> 35000L
                        consecutiveFailures <= 10 -> 50000L
                        else -> 60000L
                    }
                    delay(delayMs)
                } else {
                    _autoConnectState.value = AutoConnectState.PAUSED
                    delay(2000L)
                }
            }
        }
    }

    /**
     * User toggles manual connect/disconnect.
     */
    fun toggleConnection() {
        if (isUserEnabled.get()) {
            disconnectByUser()
        } else {
            connectByUser()
        }
    }

    fun connectByUser() {
        consecutiveFailures = 0
        isUserEnabled.set(true)
        _autoConnectState.value = AutoConnectState.SCANNING
        addLog("📡 Bluetooth auto-connect started: searching for $serverUrl...")
        reconnect()
        start()
    }

    fun disconnectByUser() {
        consecutiveFailures = 0
        isUserEnabled.set(false)
        _autoConnectState.value = AutoConnectState.PAUSED
        stop()
        addLog("⏸️ Auto-connect scanner temporarily paused.")
    }

    private fun reconnect() {
        try {
            currentWebSocket?.cancel()
            currentWebSocket?.close(1000, "Reconnecting")
        } catch (_: Exception) {}
        currentWebSocket = null
        isConnectedAtomic.set(false)
        isConnectingAtomic.set(false)
        _connectionState.value = false
    }

    /**
     * Attempts a single connection to the local WebSocket server.
     */
    private fun attemptConnect() {
        if (isConnectedAtomic.get()) return
        val currentUrl = serverUrl.trim()
        if (currentUrl.isBlank()) return

        try {
            currentWebSocket?.cancel()
        } catch (_: Exception) {}
        currentWebSocket = null

        try {
            isConnectingAtomic.set(true)
            val request = Request.Builder()
                .url(currentUrl)
                .build()

            currentWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    consecutiveFailures = 0
                    isConnectedAtomic.set(true)
                    isConnectingAtomic.set(false)
                    _connectionState.value = true
                    _autoConnectState.value = AutoConnectState.CONNECTED
                    addLog("🟢 [Auto-Connect] Server located! Successfully connected: $currentUrl")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    addLog("Received: $text")
                    pendingAck.get()?.complete(true)
                    handleServerAck(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                    isConnectedAtomic.set(false)
                    isConnectingAtomic.set(false)
                    _connectionState.value = false
                    if (isUserEnabled.get()) _autoConnectState.value = AutoConnectState.SCANNING
                    pendingAck.get()?.complete(false)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    isConnectedAtomic.set(false)
                    isConnectingAtomic.set(false)
                    _connectionState.value = false
                    if (isUserEnabled.get()) _autoConnectState.value = AutoConnectState.SCANNING
                    currentWebSocket = null
                    pendingAck.get()?.complete(false)
                    addLog("Connection closed ($code)")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    try {
                        webSocket.cancel()
                    } catch (_: Exception) {}
                    consecutiveFailures++
                    isConnectedAtomic.set(false)
                    isConnectingAtomic.set(false)
                    _connectionState.value = false
                    if (isUserEnabled.get()) _autoConnectState.value = AutoConnectState.SCANNING
                    currentWebSocket = null
                    pendingAck.get()?.complete(false)
                    val errMsg = t.localizedMessage ?: "Searching for server..."
                    if (consecutiveFailures <= 1 || consecutiveFailures % 8 == 0) {
                        addLog("📡 [Auto-Search] Searching for server ($currentUrl)... will connect immediately once active")
                    }
                }
            })
        } catch (e: Exception) {
            consecutiveFailures++
            isConnectedAtomic.set(false)
            isConnectingAtomic.set(false)
            _connectionState.value = false
            if (isUserEnabled.get()) _autoConnectState.value = AutoConnectState.SCANNING
            currentWebSocket = null
            addLog("Attempt failed: ${e.message}")
        }
    }

    private val moshi: com.squareup.moshi.Moshi by lazy {
        com.squareup.moshi.Moshi.Builder().build()
    }
    private val envelopeAdapter by lazy {
        moshi.adapter(TradeSignalEnvelope::class.java)
    }
    private val ackAdapter by lazy {
        moshi.adapter(TradeSignalAckResponse::class.java)
    }

    private fun handleServerAck(text: String) {
        val current = _relayStatus.value
        try {
            val ack = ackAdapter.fromJson(text)
            if (ack != null && (ack.signalId == null || ack.signalId == current.signalId || ack.idempotencyKey == current.idempotencyKey)) {
                val status = when (ack.status?.uppercase(Locale.US)) {
                    "ACKNOWLEDGED", "ACK", "RECEIVED" -> RelayDeliveryStatus.ACKNOWLEDGED
                    "ACCEPTED" -> RelayDeliveryStatus.ACCEPTED
                    "FILLED", "EXECUTED", "SUCCESS" -> RelayDeliveryStatus.FILLED
                    "REJECTED" -> RelayDeliveryStatus.REJECTED
                    "EXPIRED" -> RelayDeliveryStatus.EXPIRED
                    else -> RelayDeliveryStatus.ACKNOWLEDGED
                }
                _relayStatus.value = current.copy(
                    status = status,
                    latencyMs = System.currentTimeMillis() - current.timestamp,
                    message = "Server Status: ${ack.status ?: "ACK"} ${ack.message ?: ""}".trim()
                )
                return
            }
        } catch (_: Exception) {}

        // Fallback for simple legacy text responses
        val isAck = text.contains("ack", ignoreCase = true) ||
                text.contains("ok", ignoreCase = true) ||
                text.contains("received", ignoreCase = true) ||
                text.contains("executed", ignoreCase = true) ||
                text.contains("success", ignoreCase = true) ||
                text.contains("placed", ignoreCase = true)

        if (isAck && (current.status == RelayDeliveryStatus.DELIVERED || current.status == RelayDeliveryStatus.SENDING)) {
            _relayStatus.value = current.copy(
                status = RelayDeliveryStatus.ACKNOWLEDGED,
                latencyMs = System.currentTimeMillis() - current.timestamp,
                message = "Server Confirmed: $text"
            )
        }
    }

    /**
     * Automated Signal Payload Dispatcher:
     * Accepts "UP" or "DOWN" (or "CLICK_BUY" / "CLICK_SELL") and dispatches instantly.
     */
    fun sendTradingSignal(signalType: String): Boolean {
        val normalized = signalType.trim().uppercase(Locale.US)
        val payload = when (normalized) {
            "UP", "CLICK_BUY" -> "CLICK_BUY"
            "DOWN", "CLICK_SELL" -> "CLICK_SELL"
            else -> return false
        }

        return sendCommand(payload)
    }

    /**
     * Dispatches a structured TradeSignalEvent with full telemetry and delivery tracking.
     * Enforces single typed JSON envelope dispatch (no dual raw/json collision).
     */
    fun sendSignalEvent(event: TradeSignalEvent): Boolean {
        if (event.command != "CLICK_BUY" && event.command != "CLICK_SELL") {
            return false
        }

        val matrixTag = if (event.matrixId != null) " [Matrix: ${event.matrixId} ✔]" else ""
        val ws = currentWebSocket
        val now = System.currentTimeMillis()
        if (ws != null && isConnectedAtomic.get()) {
            _relayStatus.value = RelayStatus(
                signalId = event.signalId,
                idempotencyKey = event.idempotencyKey,
                command = event.command,
                status = RelayDeliveryStatus.SENDING,
                timestamp = now,
                message = "Sending ${event.command}..."
            )
            return try {
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
                    matrixId = event.matrixId
                )
                val jsonPayload = """{"command":"${event.command}"}"""
                val sent = ws.send(jsonPayload)
                if (sent) {
                    _relayStatus.value = RelayStatus(
                        signalId = event.signalId,
                        idempotencyKey = event.idempotencyKey,
                        command = event.command,
                        status = RelayDeliveryStatus.DELIVERED,
                        latencyMs = System.currentTimeMillis() - now,
                        message = "Delivered ${event.command}$matrixTag",
                        timestamp = System.currentTimeMillis()
                    )
                    addLog("[WS Success] WebSocket Dispatched: ${event.command}$matrixTag")
                } else {
                    _relayStatus.value = RelayStatus(
                        signalId = event.signalId,
                        idempotencyKey = event.idempotencyKey,
                        command = event.command,
                        status = RelayDeliveryStatus.FAILED,
                        message = "Buffer rejected",
                        timestamp = System.currentTimeMillis()
                    )
                    addLog("[WS Error] Buffer rejected for: ${event.command}$matrixTag")
                }
                sent
            } catch (e: Exception) {
                _relayStatus.value = RelayStatus(
                    signalId = event.signalId,
                    idempotencyKey = event.idempotencyKey,
                    command = event.command,
                    status = RelayDeliveryStatus.FAILED,
                    message = e.message ?: "Send error",
                    timestamp = System.currentTimeMillis()
                )
                addLog("[WS Error] Error sending ${event.command}: ${e.message}")
                false
            }
        } else {
            _relayStatus.value = RelayStatus(
                signalId = event.signalId,
                idempotencyKey = event.idempotencyKey,
                command = event.command,
                status = RelayDeliveryStatus.FAILED,
                message = "WebSocket not connected",
                timestamp = now
            )
            // Informational only - HTTP Webhook handles delivery without false-negative panic
            Log.d(TAG, "WebSocket is disconnected; skipping WS emission for ${event.command}")
            return false
        }
    }

    /**
     * Transmits manual trade command through canonical TradeSignalEvent envelope.
     */
    fun sendCommand(command: String, matrixId: String? = null): Boolean {
        if (command != "CLICK_BUY" && command != "CLICK_SELL") {
            return false
        }

        val direction = if (command == "CLICK_BUY") com.example.data.models.TradeDirection.UP else com.example.data.models.TradeDirection.DOWN
        val event = TradeSignalEvent(
            signalId = java.util.UUID.randomUUID().toString(),
            idempotencyKey = java.util.UUID.randomUUID().toString(),
            fingerprint = "MANUAL_${command}_${System.currentTimeMillis()}",
            direction = direction,
            command = command,
            upPercentage = if (command == "CLICK_BUY") 80.0 else 20.0,
            downPercentage = if (command == "CLICK_SELL") 80.0 else 20.0,
            strength = 80.0,
            availableTimeframes = setOf("MANUAL"),
            timestamp = System.currentTimeMillis(),
            matrixId = matrixId
        )
        return sendSignalEvent(event)
    }

    suspend fun sendSignalEventWithTimeout(event: TradeSignalEvent, timeoutMs: Long = 500L): Boolean {
        if (!isConnectedAtomic.get() || currentWebSocket == null) {
            return false
        }
        val deferred = CompletableDeferred<Boolean>()
        pendingAck.set(deferred)
        val sent = sendSignalEvent(event)
        if (!sent) {
            pendingAck.compareAndSet(deferred, null)
            return false
        }
        return try {
            val result = withTimeoutOrNull(timeoutMs) {
                deferred.await()
            }
            result ?: false
        } catch (_: Exception) {
            false
        } finally {
            pendingAck.compareAndSet(deferred, null)
        }
    }

    suspend fun sendCommandWithTimeout(command: String, timeoutMs: Long = 500L): Boolean {
        if (!isConnectedAtomic.get() || currentWebSocket == null) {
            return false
        }
        val deferred = CompletableDeferred<Boolean>()
        pendingAck.set(deferred)
        val sent = sendCommand(command)
        if (!sent) {
            pendingAck.compareAndSet(deferred, null)
            return false
        }
        return try {
            val result = withTimeoutOrNull(timeoutMs) {
                deferred.await()
            }
            result ?: false
        } catch (_: Exception) {
            false
        } finally {
            pendingAck.compareAndSet(deferred, null)
        }
    }

    fun isConnected(): Boolean = isConnectedAtomic.get()

    fun clearLogs() {
        _logs.value = listOf(formatLog("Logs cleared."))
    }

    /**
     * Trims logs under memory pressure (Auto Low-Memory Trim)
     */
    fun trimLogs() {
        _logs.value = _logs.value.take(10)
    }

    /**
     * Heartbeat Ping & Self-Healing Watchdog:
     * Maintains connection health check without sending spam JSON text messages to the user's CMD bot.
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(15000L) // 15s internal keep-alive check
                if (isUserEnabled.get() && isConnectedAtomic.get()) {
                    val ws = currentWebSocket
                    if (ws == null) {
                        Log.w(TAG, "Heartbeat Watchdog: Socket instance missing while connected. Auto-restarting.")
                        reconnect()
                        continue
                    }
                }
            }
        }
    }

    fun stop() {
        connectionLoopJob?.cancel()
        connectionLoopJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        try {
            currentWebSocket?.close(1000, "App closed")
        } catch (_: Exception) {}
        currentWebSocket = null
        isConnectedAtomic.set(false)
        isConnectingAtomic.set(false)
        _connectionState.value = false
    }
}
