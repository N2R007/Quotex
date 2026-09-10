package com.example.data.analyzer

import com.example.data.matrix.EvaluatedMatrixRecord
import com.example.data.matrix.MatrixEvaluationResult
import com.example.data.matrix.MatrixUserState
import com.example.data.matrix.MatrixUserStateRegistry
import com.example.data.models.CanonicalDecision
import com.example.data.models.MetricSnapshot
import com.example.data.models.StrengthLevel
import com.example.data.models.TradeDirection
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.tanh

/**
 * Advanced Evidence-Weighted Micro-Pressure Decision Engine.
 *
 * Implements rigorous mathematical evidence aggregation across:
 * 1. Active Checked Matrix Evidence
 * 2. Multi-timeframe Micro-Pressure Vector (Level, Velocity, Acceleration, Persistence, Candle)
 * 3. Robust Median & MAD Adaptive Normalization
 * 4. Micro-Energy Dominance (upEnergy, downEnergy, netEnergy)
 * 5. Deterministic Execution Eligibility
 */
object MicroPressureDecisionEngine {

    private const val MIN_DELTA_TIME_SEC = 0.05 // 50ms ultra stream minimal delta
    private const val EPSILON = 1e-6

    data class EngineInput(
        val val5m: Double?,
        val val60m: Double?,
        val val1d: Double?,
        val matrixResult: MatrixEvaluationResult,
        val history: List<MetricSnapshot> = emptyList(),
        val elapsedSeconds: Double = 1.0,
        val dataQuality: String = "VERIFIED",
        val isStale: Boolean = false,
        val isApproximate: Boolean = false,
        val lastDispatchedFingerprint: String? = null
    )

    data class MicroPressureVector(
        val levelComponent: Double,
        val velocityComponent: Double,
        val accelerationComponent: Double,
        val persistenceComponent: Double,
        val candleComponent: Double,
        val microPressure: Double,
        val historyInsufficient: Boolean,
        val isCandleDataUnavailable: Boolean,
        val candleConflictPenalty: Double
    )

    data class RobustStats(
        val median: Double,
        val mad: Double
    )

    /**
     * Computes rolling median and MAD (Median Absolute Deviation) for robust scaling.
     */
    fun calculateRobustStats(values: List<Double>): RobustStats {
        if (values.isEmpty()) return RobustStats(median = 0.0, mad = 1.0)
        val sorted = values.sorted()
        val n = sorted.size
        val median = if (n % 2 == 1) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }
        val absoluteDeviations = values.map { abs(it - median) }.sorted()
        val madMedian = if (n % 2 == 1) {
            absoluteDeviations[n / 2]
        } else {
            (absoluteDeviations[n / 2 - 1] + absoluteDeviations[n / 2]) / 2.0
        }
        // Consistent estimator for normal distribution: 1.4826 * MAD
        val madScale = max(1.4826 * madMedian, EPSILON)
        return RobustStats(median = median, mad = madScale)
    }

    /**
     * Evaluates micro-pressure components deterministically.
     */
    fun evaluateMicroPressure(
        val5m: Double?,
        val60m: Double?,
        val1d: Double?,
        history: List<MetricSnapshot>,
        elapsedSeconds: Double
    ): MicroPressureVector {
        val dt = max(elapsedSeconds, MIN_DELTA_TIME_SEC)

        // Ensure chronological ordering of valid snapshots (oldest to newest)
        val chronological = history.filter { it.isValid }.sortedBy { it.timestamp }

        // Disambiguate whether history already contains the current sample as its final element
        val (prev1, prev2) = if (chronological.isNotEmpty() &&
            val5m != null && chronological.last().val5m == val5m &&
            (val60m == null || chronological.last().val60m == val60m)
        ) {
            Pair(
                chronological.getOrNull(chronological.size - 2),
                chronological.getOrNull(chronological.size - 3)
            )
        } else {
            Pair(
                chronological.lastOrNull(),
                chronological.getOrNull(chronological.size - 2)
            )
        }

        // 1. Robust Level Component with adaptive MAD scaling
        val net5m = val5m ?: 0.0
        val net60m = val60m ?: 0.0
        val net1d = val1d ?: 0.0

        // Adaptive scales based on rolling MAD when sufficient samples are present (>= 4)
        val scale5m = if (chronological.size >= 4) {
            calculateRobustStats(chronological.map { it.val5m }).mad.coerceIn(0.20, 2.5)
        } else {
            1.0
        }
        val scale60m = if (chronological.size >= 4) {
            calculateRobustStats(chronological.map { it.val60m }).mad.coerceIn(0.30, 3.5)
        } else {
            1.5
        }
        val scale1d = if (chronological.count { it.val1d != null } >= 4) {
            calculateRobustStats(chronological.mapNotNull { it.val1d }).mad.coerceIn(0.50, 5.0)
        } else {
            3.0
        }

        val norm5m = tanh(net5m / scale5m)
        val norm60m = tanh(net60m / scale60m)
        val norm1d = tanh(net1d / scale1d)

        val levelWeight5m = if (val5m != null) 0.50 else 0.0
        val levelWeight60m = if (val60m != null) 0.35 else 0.0
        val sum5m60mWeight = levelWeight5m + levelWeight60m

        // 1d change is macro daily context and must NOT affect direction or up/down calculations when 5m/60m are available
        val levelComponent = if (sum5m60mWeight > 0.0) {
            ((norm5m * levelWeight5m) + (norm60m * levelWeight60m)) / sum5m60mWeight
        } else {
            norm1d
        }

        // 2. Velocity Component (Derived from previous snapshot)
        val v5m = if (prev1 != null && val5m != null) (val5m - prev1.val5m) / dt else 0.0
        val v60m = if (prev1 != null && val60m != null) (val60m - prev1.val60m) / dt else 0.0
        val rawVelocity = (0.70 * v5m + 0.30 * v60m)
        val velocityComponent = tanh(rawVelocity / 0.5).coerceIn(-1.0, 1.0)

        // 3. Acceleration Component (Derived from two previous snapshots)
        val historyInsufficient = (prev2 == null || prev1 == null)
        val accelerationComponent: Double
        if (!historyInsufficient) {
            val dtPrev = max((prev1!!.timestamp - prev2!!.timestamp) / 1000.0, MIN_DELTA_TIME_SEC)
            val prevV5m = (prev1.val5m - prev2.val5m) / dtPrev
            val prevV60m = (prev1.val60m - prev2.val60m) / dtPrev
            val prevRawVelocity = (0.70 * prevV5m + 0.30 * prevV60m)
            val rawAcc = (rawVelocity - prevRawVelocity) / dt
            accelerationComponent = tanh(rawAcc / 0.5).coerceIn(-1.0, 1.0)
        } else {
            accelerationComponent = 0.0
        }

        // 4. Persistence Component (Chronological historical streak consistency)
        val recentSnapshots = chronological.takeLast(10)
        val persistenceComponent = if (recentSnapshots.size >= 2) {
            // Because recentSnapshots is sorted chronologically, a is older and b is newer
            val upMoves = recentSnapshots.zipWithNext().count { (a, b) -> b.netSum > a.netSum }
            val downMoves = recentSnapshots.zipWithNext().count { (a, b) -> b.netSum < a.netSum }
            val totalMoves = upMoves + downMoves
            if (totalMoves > 0) {
                ((upMoves - downMoves).toDouble() / totalMoves.toDouble()).coerceIn(-1.0, 1.0)
            } else {
                0.0
            }
        } else {
            levelComponent * 0.5
        }

        // 5. Candle Component
        // Verify whether true OHLC exists in snapshots
        val latestSnapshot = chronological.lastOrNull()
        val hasRealOhlc = latestSnapshot?.open != null && latestSnapshot.close != null
        val candleComponent: Double
        val candleConflictPenalty: Double
        val isCandleDataUnavailable = !hasRealOhlc

        if (hasRealOhlc) {
            val o = latestSnapshot.open!!
            val c = latestSnapshot.close!!
            val h = latestSnapshot.high ?: max(o, c)
            val l = latestSnapshot.low ?: min(o, c)
            val body = c - o
            val range = max(h - l, EPSILON)
            val bodyRatio = abs(body) / range
            val cDir = sign(body)
            candleComponent = (cDir * bodyRatio).coerceIn(-1.0, 1.0)

            // False alignment check: e.g. level is strongly UP but latest candle is strongly DOWN
            val opposingMove = if (levelComponent > 0.3 && cDir < 0) {
                bodyRatio * abs(levelComponent)
            } else if (levelComponent < -0.3 && cDir > 0) {
                bodyRatio * abs(levelComponent)
            } else {
                0.0
            }
            candleConflictPenalty = opposingMove.coerceIn(0.0, 1.0)
        } else {
            candleComponent = 0.0
            candleConflictPenalty = 0.0
        }

        // Micro-pressure aggregation with fair dynamic rebalancing:
        // When candle or acceleration data is unavailable, weights are fairly redistributed
        val microPressure = when {
            isCandleDataUnavailable && historyInsufficient -> {
                (0.45 * levelComponent + 0.35 * velocityComponent + 0.20 * persistenceComponent)
            }
            isCandleDataUnavailable -> {
                (0.35 * levelComponent + 0.30 * velocityComponent + 0.20 * accelerationComponent + 0.15 * persistenceComponent)
            }
            historyInsufficient -> {
                (0.35 * levelComponent + 0.30 * velocityComponent + 0.20 * persistenceComponent + 0.15 * candleComponent)
            }
            else -> {
                (0.30 * levelComponent + 0.25 * velocityComponent + 0.15 * accelerationComponent + 0.15 * persistenceComponent + 0.15 * candleComponent)
            }
        }.coerceIn(-1.0, 1.0)

        return MicroPressureVector(
            levelComponent = levelComponent,
            velocityComponent = velocityComponent,
            accelerationComponent = accelerationComponent,
            persistenceComponent = persistenceComponent,
            candleComponent = candleComponent,
            microPressure = microPressure,
            historyInsufficient = historyInsufficient,
            isCandleDataUnavailable = isCandleDataUnavailable,
            candleConflictPenalty = candleConflictPenalty
        )
    }

    /**
     * Synthesizes inputs into a complete CanonicalDecision.
     */
    fun synthesize(input: EngineInput): CanonicalDecision {
        val val5m = input.val5m
        val val60m = input.val60m
        val val1d = input.val1d
        val matrixResult = input.matrixResult
        val history = input.history
        val elapsed = input.elapsedSeconds

        // Numeric validity check
        val isNumericValid = (val5m != null && !val5m.isNaN() && !val5m.isInfinite()) ||
                (val60m != null && !val60m.isNaN() && !val60m.isInfinite()) ||
                (val1d != null && !val1d.isNaN() && !val1d.isInfinite())

        val pVector = evaluateMicroPressure(val5m, val60m, val1d, history, elapsed)

        // Matrix evidence from MatrixEvaluationEngine (derived ONLY from active checked matrices)
        val matrixEvidence = matrixResult.matrixEvidence

        // Micro-Energy derivations
        val upEnergy = max(0.0, matrixEvidence) + max(0.0, pVector.microPressure) + max(0.0, pVector.candleComponent) + max(0.0, pVector.persistenceComponent)
        val downEnergy = max(0.0, -matrixEvidence) + max(0.0, -pVector.microPressure) + max(0.0, -pVector.candleComponent) + max(0.0, -pVector.persistenceComponent)
        val netEnergy = upEnergy - downEnergy
        val sumEnergy = upEnergy + downEnergy
        val energyDominance = if (sumEnergy > EPSILON) netEnergy / sumEnergy else 0.0

        // Conflict calculation
        val conflictRatio = if (sumEnergy > EPSILON) 1.0 - (abs(netEnergy) / sumEnergy) else 0.0
        val baseConflictIndex = if (matrixResult.isConflicting) max(conflictRatio, 0.80) else conflictRatio
        val totalConflictPenalty = min(1.0, baseConflictIndex * 0.60 + pVector.candleConflictPenalty * 0.40)

        // Factors
        var dataQualityFactor = when (input.dataQuality) {
            "VERIFIED" -> 1.00
            "PARTIALLY_VERIFIED" -> 0.85
            "APPROXIMATE" -> 0.70
            "AMBIGUOUS" -> 0.40
            else -> 0.20
        }
        if (pVector.isCandleDataUnavailable) {
            dataQualityFactor *= 0.90
        }
        if (pVector.historyInsufficient) {
            dataQualityFactor *= 0.85
        }

        val freshnessFactor = if (input.isStale) 0.20 else 1.00
        val persistenceFactor = (0.70 + 0.30 * abs(pVector.persistenceComponent)).coerceIn(0.5, 1.0)

        // Final Score & Adjusted Score with fair dynamic rebalancing when components are unavailable
        val wMatrix = if (abs(matrixEvidence) > 0.01) 0.45 else 0.0
        val wPressure = 0.35
        val wCandle = if (!pVector.isCandleDataUnavailable) 0.10 else 0.0
        val wPersistence = if (!pVector.historyInsufficient) 0.10 else 0.05
        val totalWeights = max(wMatrix + wPressure + wCandle + wPersistence, EPSILON)
        val finalScore = ((wMatrix * matrixEvidence + wPressure * pVector.microPressure + wCandle * pVector.candleComponent + wPersistence * pVector.persistenceComponent) / totalWeights).coerceIn(-1.0, 1.0)
        val adjustedScore = (finalScore * dataQualityFactor * freshnessFactor * persistenceFactor * (1.0 - totalConflictPenalty)).coerceIn(-1.0, 1.0)

        // Rolling volatility / MAD for adaptive noise floor
        val netSums = history.takeLast(10).map { it.netSum }
        val stats = calculateRobustStats(netSums)
        val adaptiveNoiseFloor = (0.05 + 0.02 * stats.mad).coerceIn(0.04, 0.15)

        // User-Checked Matrix Direct Priority:
        // When a user-checked (✔) non-cancelled quantitative matrix matches, its directional intent is authoritative
        val primaryChecked = matrixResult.primaryMatrix?.takeIf {
            MatrixUserStateRegistry.getUserState(it) == MatrixUserState.CHECKED &&
                    !it.warningOnly &&
                    it.direction != TradeDirection.NEUTRAL &&
                    matrixResult.eligibleMatrixIds.contains(it.id) &&
                    !matrixResult.cancelledMatrixIds.contains(it.id)
        }

        val direction = when {
            primaryChecked != null -> primaryChecked.direction
            adjustedScore > 0.0 -> TradeDirection.UP
            adjustedScore < 0.0 -> TradeDirection.DOWN
            (val5m ?: 0.0) > 0.0 -> TradeDirection.UP
            (val5m ?: 0.0) < 0.0 -> TradeDirection.DOWN
            (val60m ?: 0.0) > 0.0 -> TradeDirection.UP
            (val60m ?: 0.0) < 0.0 -> TradeDirection.DOWN
            else -> TradeDirection.NEUTRAL
        }
        val side = when (direction) {
            TradeDirection.UP -> "BUY"
            TradeDirection.DOWN -> "SELL"
            else -> "NONE"
        }

        // Percentage calculations (Sum is strictly 100.00)
        val scoreForPercentage = if (primaryChecked != null) {
            val dirMult = if (direction == TradeDirection.UP) 1.0 else -1.0
            val alignedScore = adjustedScore * dirMult
            if (alignedScore >= 0.15) adjustedScore else dirMult * 0.35
        } else {
            adjustedScore
        }
        val upPercentage = (50.0 + 50.0 * scoreForPercentage).coerceIn(1.0, 99.0)
        val downPercentage = 100.0 - upPercentage

        // Evidence Agreement factor
        val dirSign = when (direction) {
            TradeDirection.UP -> 1.0
            TradeDirection.DOWN -> -1.0
            else -> 1.0
        }
        val matrixAgrees = (matrixEvidence * dirSign) > 0.05
        val pressureAgrees = (pVector.microPressure * dirSign) > 0.05
        val evidenceAgreement = when {
            matrixAgrees && pressureAgrees -> 1.00
            matrixAgrees || pressureAgrees -> 0.75
            else -> 0.50
        }

        val strength = (100.0 * abs(adjustedScore) * evidenceAgreement * dataQualityFactor * persistenceFactor * (1.0 - totalConflictPenalty)).coerceIn(0.0, 100.0)

        val strengthLevel = when {
            strength >= 65.0 -> StrengthLevel.HIGH
            strength >= 40.0 -> StrengthLevel.MEDIUM
            else -> StrengthLevel.NORMAL
        }

        // Fingerprint generation for deterministic deduplication
        val fp5m = val5m?.let { String.format(Locale.US, "%.2f", it) } ?: "null"
        val fp60m = val60m?.let { String.format(Locale.US, "%.2f", it) } ?: "null"
        val fp1d = val1d?.let { String.format(Locale.US, "%.2f", it) } ?: "null"
        val primaryId = matrixResult.primaryMatrix?.id ?: "NONE"
        val fingerprint = "FP:${direction.name}:$side:${primaryId}:$fp5m:$fp60m:$fp1d"

        // Eligible checked matrices count
        val eligibleCount = matrixResult.eligibleMatrixIds.size
        val hasCheckedEligibleMatrix = eligibleCount > 0

        // Cancelled dominant matrix check
        val primaryIsCancelled = matrixResult.primaryMatrix?.let {
            matrixResult.cancelledMatrixIds.contains(it.id)
        } == true

        // Duplicate fingerprint check
        val isDuplicate = (fingerprint == input.lastDispatchedFingerprint)

        // Strict Hard Exclusion & Execution Eligibility Check
        // Mandatory user directive: Never miss an entry under all 165 matrices, whether UP or DOWN!
        val rejectionReasons = mutableListOf<String>()

        if (!isNumericValid) {
            rejectionReasons.add("সংখ্যাসূচক ডেটা অকার্যকর বা অনুপস্থিত।")
        }
        if (val5m == null || val60m == null) {
            rejectionReasons.add("কোর টাইমফ্রেম (5m বা 60m) অনুপস্থিত।")
        }
        if (isDuplicate) {
            rejectionReasons.add("একই সিগন্যাল ফিঙ্গারপ্রিন্ট পূর্বেই ডিসপ্যাচ করা হয়েছে।")
        }

        val executionEligibility = rejectionReasons.isEmpty()

        val explanation = if (executionEligibility) {
            val pTitle = matrixResult.primaryMatrix?.title ?: "মাল্টি-ম্যাট্রিক্স কনফ্লুয়েন্স"
            "স্বীকৃত $side সিগন্যাল: $pTitle (এভিডেন্স=${String.format(Locale.US, "%+.2f", matrixEvidence)}, প্রেশার=${String.format(Locale.US, "%+.2f", pVector.microPressure)}, শক্তি=${String.format(Locale.US, "%.1f", strength)}%)"
        } else {
            "অটো-ট্রেড স্থগিত: ${rejectionReasons.joinToString(" | ")}"
        }

        return CanonicalDecision(
            decisionId = java.util.UUID.randomUUID().toString(),
            direction = direction,
            side = side,
            upPercentage = upPercentage,
            downPercentage = downPercentage,
            strength = strength,
            netEnergy = netEnergy,
            matrixEvidence = matrixEvidence,
            microPressure = pVector.microPressure,
            candleConfirmation = pVector.candleComponent,
            conflictIndex = totalConflictPenalty,
            dataQuality = input.dataQuality,
            eligibleMatrixIds = matrixResult.eligibleMatrixIds,
            cancelledMatrixIds = matrixResult.cancelledMatrixIds,
            fingerprint = fingerprint,
            explanation = explanation,
            timestamp = System.currentTimeMillis(),
            executionEligibility = executionEligibility,

            // Legacy & auxiliary properties
            upPct = upPercentage,
            downPct = downPercentage,
            strengthScore = strength,
            strengthLevel = strengthLevel,
            noTrade = direction == TradeDirection.NEUTRAL || (matrixResult.isConflicting && primaryChecked == null),
            warningOnly = matrixResult.primaryMatrix?.warningOnly == true,
            confirmationCount = 1,
            requiredConfirmations = 1,
            confirmationStage = if (executionEligibility) "EXECUTABLE" else "BLOCKED",
            stale = input.isStale,
            approximate = input.isApproximate,
            conflict = matrixResult.isConflicting && primaryChecked == null,
            reason = explanation,
            authoritativeSource = "MicroPressureDecisionEngine",
            primaryMatrixId = matrixResult.primaryMatrix?.id,
            primaryMatrixTitle = matrixResult.primaryMatrix?.title,
            kineticVelocity = pVector.velocityComponent,
            kineticEnergy = abs(pVector.microPressure),
            netKineticForce = netEnergy,
            kineticUpWeight = upPercentage,
            kineticDownWeight = downPercentage,
            forensicPatternCode = matrixResult.primaryMatrix?.outputCode ?: "",
            formulaExplanation = "FinalScore = 0.45*E_m + 0.35*P_u + 0.10*C_conf + 0.10*P_hist",

            rawScore = finalScore,
            adjustedScore = adjustedScore,
            adaptiveNoiseFloor = adaptiveNoiseFloor,
            upEnergy = upEnergy,
            downEnergy = downEnergy,
            energyDominance = energyDominance,
            isCandleDataUnavailable = pVector.isCandleDataUnavailable,
            historyInsufficient = pVector.historyInsufficient,
            velocityConfirmation = pVector.velocityComponent,
            accelerationConfirmation = pVector.accelerationComponent,
            persistenceConfirmation = pVector.persistenceComponent
        )
    }
}
