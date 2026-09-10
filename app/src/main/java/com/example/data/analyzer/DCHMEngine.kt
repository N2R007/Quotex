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
 * Delta-Confluence Hybrid Matrix Engine (DCHM-Engine / DCHM Alpha-1)
 *
 * Real-time signal processing engine for on-device OCR price changes.
 *
 * 4-Layer Processing Architecture:
 * - Filter 1: Velocity & Acceleration Vector (dΔ/dt):
 *     Calculates speed and acceleration between tick updates. Rapid momentum loss in 5m Delta
 *     invalidates bullish signals even if 60m Delta is high.
 * - Filter 2: Order Book Absorption & Exhaustion Index:
 *     Detects limit order absorption and liquidity sweeps. High Delta with stalled price action
 *     triggers risk warnings (Spike & Drop or Dip & Rebound), enforcing capital safety locks.
 * - Filter 3: MTF Weighted Divergence Matrix (65/35 Rule):
 *     Weighted Score = (0.65 * Delta_5m) + (0.35 * Delta_60m).
 *     If 5m and 60m Deltas contradict, enforces NO ENTRY (isConfirmed = false).
 * - Filter 4: Dynamic Noise Gate (|0.02%| Dead-Zone):
 *     Any Delta variation within -0.02% to +0.02% is treated as absolute 0.00% (Flat/Noise).
 *     No speculative entries when 5m Delta is 0.00% or in the dead-zone.
 */
enum class CandleBehavior(
    val icon: String,
    val arrow: String,
    val description: String
) {
    STRONG_UP("🟢", "⬆️", "Strong Up"),
    STRONG_DOWN("🔴", "⬇️", "Strong Down"),
    SPIKE_AND_DROP("🟡", "↗️", "Spike & Drop"),
    DIP_AND_REBOUND("🟡", "↘️", "Dip & Rebound"),
    SIDEWAYS_NEUTRAL("⚪", "➡️", "Sideways / Neutral");

    val displayText: String
        get() = "$icon $arrow $description"
}

data class DchmEvaluationResult(
    val prediction: TradeDirection,
    val delta5m: Double,
    val delta60m: Double,
    val weightedScore: Double,
    val candleBehavior: CandleBehavior,
    val isConfirmed: Boolean,
    val matchedRuleId: String? = null
)

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
    val effectiveDelta5m: Double = 0.0,
    val effectiveDelta60m: Double = 0.0,
    val current5m: Double = 0.0,
    val current60m: Double = 0.0,
    val velocity5m: Double = 0.0,
    val acceleration5m: Double = 0.0,
    val weightedScore: Double = 0.0,
    val candleBehavior: CandleBehavior = CandleBehavior.SIDEWAYS_NEUTRAL,
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
        const val DELTA_UPDATE_INTERVAL_MS = 0L // Non-blocking cadence for 10ms-100ms OCR streams
        const val MIN_DELTA_MAGNITUDE = 0.005 // 0.005% minimum significant delta

        @Volatile
        var instance: DCHMEngine? = null

        /**
         * Pure snapshot evaluator implementing the 4-layer engine logic
         */
        fun evaluateSnapshot(
            current5m: Double,
            current60m: Double,
            previous5m: Double?,
            previous60m: Double?,
            isRuleVerified: Boolean = false
        ): DchmEvaluationResult {
            if (previous5m == null || previous60m == null) {
                return DchmEvaluationResult(
                    prediction = TradeDirection.NEUTRAL,
                    delta5m = 0.0,
                    delta60m = 0.0,
                    weightedScore = 0.0,
                    candleBehavior = CandleBehavior.SIDEWAYS_NEUTRAL,
                    isConfirmed = false
                )
            }

            val rawDelta5m = current5m - previous5m
            val rawDelta60m = current60m - previous60m

            // Filter 4: Dynamic Noise Gate (|0.02%| Dead-Zone)
            val effDelta5m = if (abs(rawDelta5m) < DEAD_ZONE_THRESHOLD) 0.0 else rawDelta5m
            val effDelta60m = if (abs(rawDelta60m) < DEAD_ZONE_THRESHOLD) 0.0 else rawDelta60m

            if (effDelta5m == 0.0 && effDelta60m == 0.0) {
                return DchmEvaluationResult(
                    prediction = TradeDirection.NEUTRAL,
                    delta5m = rawDelta5m,
                    delta60m = rawDelta60m,
                    weightedScore = 0.0,
                    candleBehavior = CandleBehavior.SIDEWAYS_NEUTRAL,
                    isConfirmed = false
                )
            }

            // Filter 3: MTF Weighted Divergence Matrix (65/35 Rule)
            val weightedScore = (0.65 * effDelta5m) + (0.35 * effDelta60m)
            val isContradictory = (effDelta5m > 0.0 && effDelta60m < 0.0) ||
                    (effDelta5m < 0.0 && effDelta60m > 0.0)

            // Filter 2: Order Book Absorption & Exhaustion Index
            val candleBehavior = when {
                effDelta5m == 0.0 && effDelta60m == 0.0 -> CandleBehavior.SIDEWAYS_NEUTRAL
                (effDelta5m > 0.10 && effDelta60m < 0.0) || (effDelta60m > 0.15 && effDelta5m <= 0.0) -> CandleBehavior.SPIKE_AND_DROP
                (effDelta5m < -0.10 && effDelta60m > 0.0) || (effDelta60m < -0.15 && effDelta5m >= 0.0) -> CandleBehavior.DIP_AND_REBOUND
                effDelta5m > 0.0 && effDelta60m >= 0.0 -> CandleBehavior.STRONG_UP
                effDelta5m < 0.0 && effDelta60m <= 0.0 -> CandleBehavior.STRONG_DOWN
                weightedScore > 0.01 -> CandleBehavior.STRONG_UP
                weightedScore < -0.01 -> CandleBehavior.STRONG_DOWN
                else -> CandleBehavior.SIDEWAYS_NEUTRAL
            }

            val prediction = when {
                effDelta5m == 0.0 -> TradeDirection.NEUTRAL
                isContradictory -> TradeDirection.NEUTRAL
                candleBehavior == CandleBehavior.SPIKE_AND_DROP || candleBehavior == CandleBehavior.DIP_AND_REBOUND -> TradeDirection.NEUTRAL
                weightedScore > 0.0 -> TradeDirection.UP
                weightedScore < 0.0 -> TradeDirection.DOWN
                else -> TradeDirection.NEUTRAL
            }

            val match = if (prediction != TradeDirection.NEUTRAL) {
                Directional206MatrixEngine.evaluate(current5m, current60m)
            } else null

            val isConfirmed = !isContradictory &&
                    effDelta5m != 0.0 &&
                    (candleBehavior == CandleBehavior.STRONG_UP || candleBehavior == CandleBehavior.STRONG_DOWN) &&
                    match != null && match.direction == prediction

            return DchmEvaluationResult(
                prediction = prediction,
                delta5m = rawDelta5m,
                delta60m = rawDelta60m,
                weightedScore = weightedScore,
                candleBehavior = candleBehavior,
                isConfirmed = isConfirmed || (isRuleVerified && match != null),
                matchedRuleId = match?.id
            )
        }
    }

    init {
        instance = this
    }

    // Thread-safety lock
    private val stateMutex = Mutex()

    // State tracking
    private var lastSeen5m: Double? = null
    private var lastSeen60m: Double? = null
    private var lastUpdateTimestamp = 0L
    private var lastVelocity5m = 0.0
    private var lastVelocity60m = 0.0
    private var lastAcceleration5m = 0.0

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

        val (delta5m, delta60m, effDelta5m, effDelta60m, v5, a5, weightedScore, candleBehavior, isRapidLoss, shouldSkip) = stateMutex.withLock {
            // Rate limiting: enforce minimum cadence when configured > 0
            if (DELTA_UPDATE_INTERVAL_MS > 0L && currentTime - lastUpdateTimestamp < DELTA_UPDATE_INTERVAL_MS && lastUpdateTimestamp != 0L) {
                return@withLock ProcessingTuple(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, CandleBehavior.SIDEWAYS_NEUTRAL, false, true)
            }

            val prev5 = previous5mPercent ?: lastSeen5m
            val prev60 = previous60mPercent ?: lastSeen60m

            // Baseline initialization on first frame or after connection dropout (> 2.0s gap)
            val isDropoutGap = lastUpdateTimestamp > 0L && (currentTime - lastUpdateTimestamp) > 2000L
            if (prev5 == null || prev60 == null || isDropoutGap) {
                lastSeen5m = current5mPercent
                lastSeen60m = current60mPercent
                lastUpdateTimestamp = currentTime
                lastVelocity5m = 0.0
                lastVelocity60m = 0.0
                lastAcceleration5m = 0.0
                if (isDropoutGap) {
                    Log.d(TAG, "OCR stream gap detected (${currentTime - lastUpdateTimestamp}ms) -> Kinematics auto-reset to neutral")
                }
                return@withLock ProcessingTuple(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, CandleBehavior.SIDEWAYS_NEUTRAL, false, true)
            }

            // Calculate Raw Deltas: (New - Old)
            val d5 = current5mPercent - prev5
            val d60 = current60mPercent - prev60

            // Filter 4: Dynamic Noise Gate (|0.02%| Dead-Zone)
            // Any Delta variation within -0.02% to +0.02% must be treated as absolute 0.00% (Flat/Noise).
            val eff5 = if (abs(d5) < DEAD_ZONE_THRESHOLD) 0.0 else d5
            val eff60 = if (abs(d60) < DEAD_ZONE_THRESHOLD) 0.0 else d60

            // Dead-zone filter: If both changes are within dead-zone
            if (abs(d5) < DEAD_ZONE_THRESHOLD && abs(d60) < DEAD_ZONE_THRESHOLD) {
                Log.d(TAG, "Suppressed by Dead-Zone: Δ5m=${String.format("%.4f", d5)}%, Δ60m=${String.format("%.4f", d60)}%")
                return@withLock ProcessingTuple(d5, d60, 0.0, 0.0, 0.0, 0.0, 0.0, CandleBehavior.SIDEWAYS_NEUTRAL, false, true)
            }

            // Delta time in seconds (minimum 10ms = 0.010s)
            val dtSeconds = if (lastUpdateTimestamp > 0L) {
                ((currentTime - lastUpdateTimestamp).coerceAtLeast(10L)) / 1000.0
            } else 0.050

            // Filter 1: Velocity & Acceleration Vector (dΔ/dt)
            val vel5 = eff5 / dtSeconds
            val vel60 = eff60 / dtSeconds
            val acc5 = (vel5 - lastVelocity5m) / dtSeconds

            // Rapid momentum loss in 5m Delta invalidates bullish signals even if 60m Delta is high
            val isRapidMomentumLoss = (lastVelocity5m > 0.05 && vel5 < 0.3 * lastVelocity5m) ||
                    (eff60 > 0.05 && eff5 <= 0.0) ||
                    (vel5 > 0.0 && acc5 < -0.10)

            // Filter 3: MTF Weighted Divergence Matrix (65/35 Rule)
            val score = (0.65 * eff5) + (0.35 * eff60)

            // Filter 2: Order Book Absorption & Exhaustion Index
            // Detect limit order absorption and liquidity sweeps. High Delta with stalled price action triggers risk warnings (Spike & Drop or Dip & Rebound).
            val behavior = when {
                eff5 == 0.0 && eff60 == 0.0 -> CandleBehavior.SIDEWAYS_NEUTRAL
                // Spike & Drop: high delta or upward impulse with stalled/reversing momentum
                (eff5 > 0.08 && acc5 < -0.05) || (eff60 > 0.15 && eff5 <= 0.0) || (eff5 > 0.0 && vel5 <= 0.0) -> CandleBehavior.SPIKE_AND_DROP
                // Dip & Rebound: negative delta or downward plunge with stalled/rebounding momentum
                (eff5 < -0.08 && acc5 > 0.05) || (eff60 < -0.15 && eff5 >= 0.0) || (eff5 < 0.0 && vel5 >= 0.0) -> CandleBehavior.DIP_AND_REBOUND
                eff5 > 0.0 && eff60 >= 0.0 -> CandleBehavior.STRONG_UP
                eff5 < 0.0 && eff60 <= 0.0 -> CandleBehavior.STRONG_DOWN
                score > 0.01 -> CandleBehavior.STRONG_UP
                score < -0.01 -> CandleBehavior.STRONG_DOWN
                else -> CandleBehavior.SIDEWAYS_NEUTRAL
            }

            // Update tracked baseline & kinematics
            lastSeen5m = current5mPercent
            lastSeen60m = current60mPercent
            lastUpdateTimestamp = currentTime
            lastVelocity5m = vel5
            lastVelocity60m = vel60
            lastAcceleration5m = acc5

            ProcessingTuple(d5, d60, eff5, eff60, vel5, acc5, score, behavior, isRapidMomentumLoss, false)
        }

        if (shouldSkip) {
            return null
        }

        // Filter 3 Contradiction Check: If 5m and 60m Deltas contradict, enforce NO ENTRY
        val isContradictory = (effDelta5m > 0.0 && effDelta60m < 0.0) || (effDelta5m < 0.0 && effDelta60m > 0.0)

        // Layer 1 Instant Prediction Logic (0ms latency, no matrix delay)
        val prediction = when {
            effDelta5m == 0.0 -> TradeDirection.NEUTRAL // DO NOT generate speculative entries when 5m Delta is 0.00% or within the dead-zone
            isContradictory -> TradeDirection.NEUTRAL // Contradiction: enforce NO ENTRY
            candleBehavior == CandleBehavior.SPIKE_AND_DROP || candleBehavior == CandleBehavior.DIP_AND_REBOUND -> TradeDirection.NEUTRAL // Exhaustion: lock signals
            effDelta5m > 0.0 && effDelta60m >= 0.0 -> TradeDirection.UP
            effDelta5m < 0.0 && effDelta60m <= 0.0 -> TradeDirection.DOWN
            weightedScore > 0.01 -> TradeDirection.UP
            weightedScore < -0.01 -> TradeDirection.DOWN
            else -> TradeDirection.NEUTRAL
        }

        val instantSignal = TradeSignal(
            timestamp = currentTime,
            prediction = prediction,
            delta5m = delta5m,
            delta60m = delta60m,
            effectiveDelta5m = effDelta5m,
            effectiveDelta60m = effDelta60m,
            current5m = current5mPercent,
            current60m = current60mPercent,
            velocity5m = v5,
            acceleration5m = a5,
            weightedScore = weightedScore,
            candleBehavior = candleBehavior,
            isConfirmed = false, // Unconfirmed until Layer 2 validation completes
            reason = "Layer 1: DCHM Alpha-1 (Δ5m=${String.format("%.3f", delta5m)}%, Δ60m=${String.format("%.3f", delta60m)}%, Behavior=${candleBehavior.displayText})"
        )

        Log.d(TAG, "Layer 1 Instant Signal Emitted: ${instantSignal.prediction} | Δ5m=$delta5m%, Δ60m=$delta60m% | ${candleBehavior.displayText}")

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
        validateSignalAgainstMatrix(
            signal = instantSignal,
            current5m = current5mPercent,
            current60m = current60mPercent,
            delta5m = effDelta5m,
            delta60m = effDelta60m,
            isContradictory = isContradictory,
            isRapidMomentumLoss = isRapidLoss
        )

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
        delta60m: Double,
        isContradictory: Boolean = false,
        isRapidMomentumLoss: Boolean = false
    ): Job = scope.launch {
        val validationStartTime = System.currentTimeMillis()

        try {
            // Capital Safety Locks:
            // 1. If 5m and 60m contradict, enforce NO ENTRY (isConfirmed = false)
            // 2. If rapid momentum loss in 5m Delta invalidates bullish signal -> isConfirmed = false
            // 3. If exhaustion detected (Spike & Drop / Dip & Rebound) -> isConfirmed = false
            // 4. If 5m Delta is 0.00% or within dead-zone -> isConfirmed = false
            val isSafetyLocked = isContradictory ||
                    (signal.prediction == TradeDirection.UP && isRapidMomentumLoss) ||
                    signal.candleBehavior == CandleBehavior.SPIKE_AND_DROP ||
                    signal.candleBehavior == CandleBehavior.DIP_AND_REBOUND ||
                    signal.effectiveDelta5m == 0.0 ||
                    signal.prediction == TradeDirection.NEUTRAL

            val (isMatched, matchedRules) = if (!isSafetyLocked) {
                matrixValidator.validateAgainstRules(
                    direction = signal.prediction,
                    current5m = current5m,
                    current60m = current60m,
                    delta5m = delta5m,
                    delta60m = delta60m
                )
            } else {
                false to emptyList()
            }

            val validationTimeMs = (System.currentTimeMillis() - validationStartTime).coerceAtLeast(0L)
            val primaryRule = matchedRules.firstOrNull()

            val confirmedSignal = signal.copy(
                isConfirmed = isMatched && !isSafetyLocked,
                matchedRules = matchedRules,
                primaryRuleId = primaryRule,
                confidence = if (isMatched && !isSafetyLocked) calculateConfidence(true, matchedRules.size) else 0.0,
                reason = when {
                    isMatched && !isSafetyLocked -> "Layer 2: Validated against ${matchedRules.size} Directional Rule(s) [${matchedRules.joinToString()}]"
                    isSafetyLocked -> "Layer 2: Capital safety lock enforced (Contradiction/Exhaustion/Momentum Loss) - NO ENTRY"
                    else -> "Layer 2: No rule match - Display UI direction only, no trade execution"
                }
            )

            Log.d(
                TAG,
                "Layer 2 Validation Complete [${validationTimeMs}ms] | " +
                    "Direction=${signal.prediction} | IsConfirmed=${confirmedSignal.isConfirmed} | " +
                    "MatchedRules=$matchedRules | Confidence=${confirmedSignal.confidence}%"
            )

            // Emit validated signal
            _confirmedSignals.emit(confirmedSignal)
            _latestConfirmedSignal.value = confirmedSignal

            // Update metrics
            val currentMetrics = _engineMetrics.value
            _engineMetrics.value = currentMetrics.copy(
                confirmedSignals = if (confirmedSignal.isConfirmed) currentMetrics.confirmedSignals + 1 else currentMetrics.confirmedSignals,
                avgValidationTimeMs = if (currentMetrics.avgValidationTimeMs == 0.0) validationTimeMs.toDouble() else (currentMetrics.avgValidationTimeMs + validationTimeMs) / 2.0,
                lastValidationStatus = if (confirmedSignal.isConfirmed) "CONFIRMED (${primaryRule ?: "MATCH"})" else "REJECTED"
            )

            // High-confidence execution (Audio + WebSocket Relay)
            if (confirmedSignal.isConfirmed) {
                onHighConfidenceSignal(confirmedSignal)
            } else {
                Log.d(TAG, "Signal [${signal.prediction}] locked/not confirmed by 206 Rules. Displaying UI only.")
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
                lastVelocity5m = 0.0
                lastVelocity60m = 0.0
                lastAcceleration5m = 0.0
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

    private data class ProcessingTuple(
        val delta5m: Double,
        val delta60m: Double,
        val effDelta5m: Double,
        val effDelta60m: Double,
        val velocity5m: Double,
        val acceleration5m: Double,
        val weightedScore: Double,
        val candleBehavior: CandleBehavior,
        val isRapidMomentumLoss: Boolean,
        val shouldSkip: Boolean
    )
}
