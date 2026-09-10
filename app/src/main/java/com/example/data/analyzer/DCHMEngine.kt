package com.example.data.analyzer

import android.util.Log
import com.example.audio.AudioSignalEngine
import com.example.data.matrix.Directional206MatrixEngine
import com.example.data.models.TradeDirection
import com.example.network.WebSocketTradeRelay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/**
 * Delta-Confluence Hybrid Matrix Engine (DCHM-Engine)
 *
 * Real-time signal processing engine for on-device OCR price changes.
 *
 * Two-Tier Architecture:
 * - Layer 1 (Instant Delta Layer):
 *     Compares current 5m and 60m percentages with previous values (10ms-100ms cadence).
 *     Calculates Delta 5m = (New5m - Old5m) and Delta 60m = (New60m - Old60m).
 *     Applies Dead-Zone filter (|Change| < 0.02%) to eliminate sideways noise.
 *     Instantly emits UI predictions (TradeDirection.UP / TradeDirection.DOWN) without delay.
 *
 * - Layer 2 (Matrix Validation Layer):
 *     Validates the Layer 1 instant prediction against the 206 Directional Rules
 *     (U001-U103 for UP, D001-D103 for DOWN) via Directional206MatrixEngine.
 *     If Prediction matches Rule -> isConfirmed = true (High-Confidence: triggers Audio & WebSocket Relay).
 *     If Prediction does NOT match -> isConfirmed = false (Display UI direction only, no execution).
 */
data class DeltaSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val delta5m: Double = 0.0,
    val delta60m: Double = 0.0,
    val previous5m: Double = 0.0,
    val previous60m: Double = 0.0,
    val current5m: Double = 0.0,
    val current60m: Double = 0.0
)

data class TradeSignal(
    val id: String = System.nanoTime().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val prediction: TradeDirection = TradeDirection.NEUTRAL,
    val delta5m: Double = 0.0,
    val delta60m: Double = 0.0,
    val current5m: Double = 0.0,
    val current60m: Double = 0.0,
    val isConfirmed: Boolean = false,
    val matchedRules: List<String> = emptyList(),
    val primaryRuleId: String? = null,
    val confidence: Double = 0.0,
    val reason: String = ""
)

interface MatrixValidator {
    /**
     * Validates direction and metrics against directional matrix rules.
     * Returns Pair(isConfirmed, matchedRuleIds).
     */
    suspend fun validateAgainstRules(
        direction: TradeDirection,
        current5m: Double,
        current60m: Double,
        delta5m: Double,
        delta60m: Double
    ): Pair<Boolean, List<String>>

    /**
     * Overload for delta-only validation.
     */
    suspend fun validateAgainstRules(
        direction: TradeDirection,
        delta5m: Double,
        delta60m: Double
    ): Pair<Boolean, List<String>> = validateAgainstRules(
        direction = direction,
        current5m = 0.0,
        current60m = 0.0,
        delta5m = delta5m,
        delta60m = delta60m
    )
}

/**
 * Default Validator integrating with the 206 Directional Rules
 * (U001-U103 for UP, D001-D103 for DOWN) in Directional206MatrixEngine.
 */
class Directional206MatrixValidator : MatrixValidator {
    override suspend fun validateAgainstRules(
        direction: TradeDirection,
        current5m: Double,
        current60m: Double,
        delta5m: Double,
        delta60m: Double
    ): Pair<Boolean, List<String>> {
        if (direction == TradeDirection.NEUTRAL) {
            return false to emptyList()
        }

        val match = Directional206MatrixEngine.evaluate(
            val5m = current5m,
            val60m = current60m,
            val1d = null,
            history = emptyList()
        )

        return if (match != null && match.direction == direction) {
            true to listOf(match.id)
        } else {
            false to emptyList()
        }
    }
}

class DCHMEngine(
    private val matrixValidator: MatrixValidator = Directional206MatrixValidator(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val enableAutomaticAudioAlerts: Boolean = true,
    private val enableAutomaticWebSocketRelay: Boolean = true
) {

    companion object {
        private const val TAG = "DCHMEngine"
        const val DEAD_ZONE_THRESHOLD = 0.02 // 0.02% dead-zone to filter noise
        const val DELTA_UPDATE_INTERVAL_MS = 10L // Minimum interval between evaluations (10ms-100ms)
        const val MIN_DELTA_MAGNITUDE = 0.005 // 0.005% minimum significant delta
    }

    // Thread-safety lock
    private val stateMutex = Mutex()

    // State tracking
    private var lastSeen5m: Double? = null
    private var lastSeen60m: Double? = null
    private var lastUpdateTimestamp = 0L

    // Layer 1: Instant Predictions (Hot stream & latest state)
    private val _instantPredictions = MutableSharedFlow<TradeSignal>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val instantPredictions: SharedFlow<TradeSignal> = _instantPredictions.asSharedFlow()

    private val _latestInstantSignal = MutableStateFlow<TradeSignal?>(null)
    val latestInstantSignal: StateFlow<TradeSignal?> = _latestInstantSignal.asStateFlow()

    // Layer 2: Confirmed Signals (Hot stream & latest state)
    private val _confirmedSignals = MutableSharedFlow<TradeSignal>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val confirmedSignals: SharedFlow<TradeSignal> = _confirmedSignals.asSharedFlow()

    private val _latestConfirmedSignal = MutableStateFlow<TradeSignal?>(null)
    val latestConfirmedSignal: StateFlow<TradeSignal?> = _latestConfirmedSignal.asStateFlow()

    // Engine Diagnostics & Metrics
    private val _engineMetrics = MutableStateFlow(EngineMetrics())
    val engineMetrics: StateFlow<EngineMetrics> = _engineMetrics.asStateFlow()

    data class EngineMetrics(
        val totalSignalsGenerated: Long = 0L,
        val confirmedSignals: Long = 0L,
        val avgValidationTimeMs: Double = 0.0,
        val lastSignalTimestamp: Long = 0L,
        val lastValidationStatus: String = "IDLE",
        val lastDelta5m: Double = 0.0,
        val lastDelta60m: Double = 0.0
    )

    // Optional listener hook for external listeners
    var onHighConfidenceSignalListener: (suspend (TradeSignal) -> Unit)? = null
    var onSignalGeneratedListener: (suspend (TradeSignal) -> Unit)? = null

    /**
     * Layer 1: Process real-time OCR percentage changes (10ms-100ms cadence)
     *
     * @param current5mPercent Current 5m timeframe percentage from ML Kit OCR
     * @param current60mPercent Current 60m timeframe percentage from ML Kit OCR
     * @param previous5mPercent Optional previous 5m percentage (uses internal state if omitted)
     * @param previous60mPercent Optional previous 60m percentage (uses internal state if omitted)
     * @return Instant TradeSignal or null if throttled / in dead-zone
     */
    suspend fun processInstantDelta(
        current5mPercent: Double,
        current60mPercent: Double,
        previous5mPercent: Double? = null,
        previous60mPercent: Double? = null
    ): TradeSignal? {
        val currentTime = System.currentTimeMillis()

        val (delta5m, delta60m, prev5, prev60, shouldSkip) = stateMutex.withLock {
            // Rate limiting: enforce minimum 10ms cadence
            if (currentTime - lastUpdateTimestamp < DELTA_UPDATE_INTERVAL_MS && lastUpdateTimestamp != 0L) {
                return@withLock Tuple5(0.0, 0.0, 0.0, 0.0, true)
            }

            val prev5 = previous5mPercent ?: lastSeen5m
            val prev60 = previous60mPercent ?: lastSeen60m

            // Baseline initialization on first frame
            if (prev5 == null || prev60 == null) {
                lastSeen5m = current5mPercent
                lastSeen60m = current60mPercent
                lastUpdateTimestamp = currentTime
                return@withLock Tuple5(0.0, 0.0, current5mPercent, current60mPercent, true)
            }

            // Calculate Deltas: (New - Old)
            val d5 = current5mPercent - prev5
            val d60 = current60mPercent - prev60

            // Dead-zone filter: |Change| < 0.02%
            if (abs(d5) < DEAD_ZONE_THRESHOLD && abs(d60) < DEAD_ZONE_THRESHOLD) {
                Log.d(TAG, "Suppressed by Dead-Zone: Δ5m=${String.format("%.4f", d5)}%, Δ60m=${String.format("%.4f", d60)}%")
                return@withLock Tuple5(d5, d60, prev5, prev60, true)
            }

            // Update tracked baseline
            lastSeen5m = current5mPercent
            lastSeen60m = current60mPercent
            lastUpdateTimestamp = currentTime

            Tuple5(d5, d60, prev5, prev60, false)
        }

        if (shouldSkip) {
            return null
        }

        // Layer 1 Instant Prediction Logic (0ms latency, no matrix delay)
        val prediction = determineInstantDirection(delta5m, delta60m)

        val instantSignal = TradeSignal(
            timestamp = currentTime,
            prediction = prediction,
            delta5m = delta5m,
            delta60m = delta60m,
            current5m = current5mPercent,
            current60m = current60mPercent,
            isConfirmed = false, // Unconfirmed until Layer 2 validation completes
            reason = "Layer 1: Instant Delta Analysis (Δ5m=${String.format("%.3f", delta5m)}%, Δ60m=${String.format("%.3f", delta60m)}%)"
        )

        Log.d(TAG, "Layer 1 Instant Signal Emitted: ${instantSignal.prediction} | Δ5m=$delta5m%, Δ60m=$delta60m%")

        // Emit instant signal for UI rendering immediately
        _instantPredictions.emit(instantSignal)
        _latestInstantSignal.value = instantSignal
        onSignalGeneratedListener?.invoke(instantSignal)

        // Update metrics
        _engineMetrics.value = _engineMetrics.value.copy(
            totalSignalsGenerated = _engineMetrics.value.totalSignalsGenerated + 1,
            lastSignalTimestamp = currentTime,
            lastValidationStatus = "LAYER_1_PENDING",
            lastDelta5m = delta5m,
            lastDelta60m = delta60m
        )

        // Asynchronously or concurrently validate against Layer 2
        validateSignalAgainstMatrix(instantSignal, current5mPercent, current60mPercent, delta5m, delta60m)

        return instantSignal
    }

    /**
     * Layer 2: Matrix Validation Layer
     * Validates instant prediction against 206 Directional Rules (U001-U103 for UP, D001-D103 for DOWN).
     */
    private fun validateSignalAgainstMatrix(
        signal: TradeSignal,
        current5m: Double,
        current60m: Double,
        delta5m: Double,
        delta60m: Double
    ): Job = scope.launch {
        val validationStartTime = System.currentTimeMillis()

        try {
            val (isMatched, matchedRules) = matrixValidator.validateAgainstRules(
                direction = signal.prediction,
                current5m = current5m,
                current60m = current60m,
                delta5m = delta5m,
                delta60m = delta60m
            )

            val validationTimeMs = (System.currentTimeMillis() - validationStartTime).coerceAtLeast(0L)
            val primaryRule = matchedRules.firstOrNull()

            val confirmedSignal = signal.copy(
                isConfirmed = isMatched,
                matchedRules = matchedRules,
                primaryRuleId = primaryRule,
                confidence = calculateConfidence(isMatched, matchedRules.size),
                reason = if (isMatched) {
                    "Layer 2: Validated against ${matchedRules.size} Directional Rule(s) [${matchedRules.joinToString()}]"
                } else {
                    "Layer 2: No rule match - Display UI direction only, no trade execution"
                }
            )

            Log.d(
                TAG,
                "Layer 2 Validation Complete [${validationTimeMs}ms] | " +
                    "Direction=${signal.prediction} | IsConfirmed=$isMatched | " +
                    "MatchedRules=$matchedRules | Confidence=${confirmedSignal.confidence}%"
            )

            // Emit validated signal
            _confirmedSignals.emit(confirmedSignal)
            _latestConfirmedSignal.value = confirmedSignal

            // Update metrics
            val currentMetrics = _engineMetrics.value
            _engineMetrics.value = currentMetrics.copy(
                confirmedSignals = if (isMatched) currentMetrics.confirmedSignals + 1 else currentMetrics.confirmedSignals,
                avgValidationTimeMs = if (currentMetrics.avgValidationTimeMs == 0.0) validationTimeMs.toDouble() else (currentMetrics.avgValidationTimeMs + validationTimeMs) / 2.0,
                lastValidationStatus = if (isMatched) "CONFIRMED (${primaryRule ?: "MATCH"})" else "REJECTED"
            )

            // High-confidence execution (Audio + WebSocket Relay)
            if (isMatched) {
                onHighConfidenceSignal(confirmedSignal)
            } else {
                Log.d(TAG, "Signal [${signal.prediction}] not confirmed by 206 Rules. Displaying UI only.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Layer 2 Validation Exception: ${e.message}", e)
            _engineMetrics.value = _engineMetrics.value.copy(lastValidationStatus = "ERROR: ${e.message}")
        }
    }

    /**
     * Layer 1 Instant Direction Logic
     * Evaluates micro-momentum vectors of 5m and 60m deltas.
     */
    private fun determineInstantDirection(delta5m: Double, delta60m: Double): TradeDirection {
        return when {
            // Strong UP: both deltas positive and above minimum magnitude
            delta5m > MIN_DELTA_MAGNITUDE && delta60m > MIN_DELTA_MAGNITUDE -> TradeDirection.UP

            // Strong DOWN: both deltas negative and below negative minimum magnitude
            delta5m < -MIN_DELTA_MAGNITUDE && delta60m < -MIN_DELTA_MAGNITUDE -> TradeDirection.DOWN

            // 5m timeframe divergence (5m leads momentum)
            delta5m > MIN_DELTA_MAGNITUDE && delta60m < -MIN_DELTA_MAGNITUDE -> TradeDirection.UP
            delta5m < -MIN_DELTA_MAGNITUDE && delta60m > MIN_DELTA_MAGNITUDE -> TradeDirection.DOWN

            // Net directional bias when one timeframe is neutral/minor
            (delta5m + delta60m) > MIN_DELTA_MAGNITUDE -> TradeDirection.UP
            (delta5m + delta60m) < -MIN_DELTA_MAGNITUDE -> TradeDirection.DOWN

            else -> TradeDirection.NEUTRAL
        }
    }

    /**
     * Calculate confidence score (0-100%)
     */
    private fun calculateConfidence(isMatched: Boolean, ruleCount: Int): Double {
        return when {
            !isMatched -> 0.0
            ruleCount >= 5 -> 98.0
            ruleCount >= 3 -> 90.0
            ruleCount >= 1 -> 80.0
            else -> 0.0
        }
    }

    /**
     * Dispatches high-confidence confirmed signal to Audio Alert and WebSocket Relay.
     */
    private suspend fun onHighConfidenceSignal(signal: TradeSignal) {
        Log.i(TAG, "🎯 HIGH-CONFIDENCE SIGNAL TRIGGERED: ${signal.prediction} [${signal.primaryRuleId}]")

        // External callback
        onHighConfidenceSignalListener?.invoke(signal)

        // 1. Audio Alert Trigger
        if (enableAutomaticAudioAlerts) {
            try {
                val callout = if (signal.prediction == TradeDirection.UP) {
                    "UP ${signal.primaryRuleId ?: ""}".trim()
                } else {
                    "DOWN ${signal.primaryRuleId ?: ""}".trim()
                }
                val event = if (signal.prediction == TradeDirection.UP) {
                    AudioSignalEngine.SOUND_UP_ALERT
                } else {
                    AudioSignalEngine.SOUND_DOWN_ALERT
                }
                AudioSignalEngine.playSoundEvent(event, callout)
            } catch (e: Exception) {
                Log.w(TAG, "Audio signal trigger notice: ${e.message}")
            }
        }

        // 2. WebSocket Relay Trigger
        if (enableAutomaticWebSocketRelay) {
            try {
                val command = if (signal.prediction == TradeDirection.UP) "CLICK_BUY" else "CLICK_SELL"
                WebSocketTradeRelay.sendCommand(command, signal.primaryRuleId)
                WebSocketTradeRelay.sendTradingSignal(signal.prediction.name)
            } catch (e: Exception) {
                Log.w(TAG, "WebSocket relay trigger notice: ${e.message}")
            }
        }
    }

    /**
     * Resets tracking state (e.g. when camera or test scenario changes).
     */
    fun reset() {
        scope.launch {
            stateMutex.withLock {
                lastSeen5m = null
                lastSeen60m = null
                lastUpdateTimestamp = 0L
            }
            _latestInstantSignal.value = null
            _latestConfirmedSignal.value = null
            _engineMetrics.value = EngineMetrics()
            Log.d(TAG, "DCHMEngine reset complete")
        }
    }

    /**
     * Shutdown coroutine scope and cleanup resources.
     */
    fun shutdown() {
        scope.cancel()
        Log.d(TAG, "DCHMEngine shutdown completed")
    }

    private data class Tuple5<A, B, C, D, E>(
        val a: A,
        val b: B,
        val c: C,
        val d: D,
        val e: E
    )
}
