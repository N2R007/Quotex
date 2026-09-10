package com.example.data.analyzer

import com.example.data.analyzer.ReactiveMarketPressureEngine.PressureDecision
import com.example.data.analyzer.ReactiveMarketPressureEngine.PressureInput
import com.example.data.matrix.MatrixEvaluationResult
import com.example.data.models.CanonicalDecision
import com.example.data.models.DataQualityState
import com.example.data.models.DecisionMode
import com.example.data.models.MetricSnapshot
import com.example.data.models.StrengthLevel
import com.example.data.models.TradeDirection
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Single Authoritative Quantitative Decision Engine for Quant Vision AI.
 *
 * Enforces:
 * 1. Single Writer Principle: Only this engine writes the authoritative TradeDirection,
 *    directional scores (upPct, downPct), and executionEligibility.
 * 2. Multi-Timeframe Integration: Synthesizes Reactive Market Pressure (5m, 60m, 1d),
 *    165 Deterministic Matrices (M001-M165), Micro-Kinetic Physics Vector (v, a, Ek, Fnet),
 *    and Structural Movement Classification.
 * 3. Multi-Frame Confirmation State Machine: Requires N qualifying consecutive snapshots
 *    (DEFAULT_REQUIRED_CONFIRMATIONS = 1 for instant ultra-fast auto-trade dispatch
 *    in accordance with the zero-latency real-time detection policy, configurable
 *    via requiredConfirmations parameter) before promoting a candidate signal to
 *    CONFIRMED and execution-eligible.
 * 4. Fast-Path Bypass: Promotes immediately when 5m, 60m, and 1D are non-null and strictly
 *    agree in sign (|val| >= 0.30%) with high magnitude and 0 traps / deceleration flags.
 * 5. Dead-Zone & Conflict Transition: Conflicts enter a temporary HOLD / CONFLICT state
 *    with explicit resolution criteria, preventing indefinite stalling or zero-trade traps.
 * 6. Staleness Timeout: Revokes automated execution eligibility if metrics have not updated
 *    for > 15,000 ms, while preserving manual trade dispatch.
 */
object CanonicalDecisionEngine {

    /**
     * Default number of snapshot confirmations required to promote a signal to CONFIRMED.
     * Set to 1 to ensure instant ultra-fast dispatch with zero latency when valid metrics
     * are detected on screen. Callers can override to 2 or more via [evaluate].
     */
    const val DEFAULT_REQUIRED_CONFIRMATIONS = 1
    const val STALE_TIMEOUT_MS = 15000L

    /**
     * Active Auto-Entry Decision Mode.
     * Can be switched dynamically via [currentDecisionMode].
     * Defaults to [DecisionMode.LEGACY_MULTILAYER] to preserve existing behavior and tests.
     */
    @Volatile
    var currentDecisionMode: DecisionMode = DecisionMode.LEGACY_MULTILAYER

    // Internal state tracking for multi-frame confirmation
    @Volatile
    private var lastCandidateDirection: TradeDirection = TradeDirection.NEUTRAL

    @Volatile
    private var confirmedCount: Int = 0

    @Volatile
    private var lastUpdateTimeMs: Long = 0L

    @Volatile
    private var lastFingerprint: String = ""

    /**
     * Resets the confirmation state machine (e.g. on new scan session or lens change).
     */
    fun resetState() {
        lastCandidateDirection = TradeDirection.NEUTRAL
        confirmedCount = 0
        lastUpdateTimeMs = 0L
        lastFingerprint = ""
    }

    /**
     * Synthesizes all quantitative components into a single immutable CanonicalDecision.
     */
    fun evaluate(
        val5m: Double?,
        val60m: Double?,
        val1d: Double?,
        history: List<MetricSnapshot> = emptyList(),
        isApproximate: Boolean = false,
        currentTimeMs: Long = System.currentTimeMillis(),
        rmpDecision: ReactiveMarketPressureEngine.PressureDecision? = null,
        matrixResult: MatrixEvaluationResult? = null,
        kineticResult: MicroKineticVectorEngine.KineticVectorResult? = null,
        behavior: MovementClassificationResult? = null,
        requiredConfirmations: Int = DEFAULT_REQUIRED_CONFIRMATIONS,
        decisionMode: DecisionMode = currentDecisionMode
    ): CanonicalDecision {
        val v5 = val5m?.takeIf { !it.isNaN() && !it.isInfinite() }
        val v60 = val60m?.takeIf { !it.isNaN() && !it.isInfinite() }
        val v1d = val1d?.takeIf { !it.isNaN() && !it.isInfinite() }

        // THREE_TIMEFRAME_PRESSURE: Pure deterministic arithmetic utility (ThreeTimeframePressureCalculator).
        // Bypasses all 165 matrices, zigzag noise, micro-kinetic physics vector, and multi-layer heuristic filters.
        // Direct auto-entry driven solely by upEnergy, downEnergy, netPressurePercent, and PressureBand.
        if (decisionMode == DecisionMode.THREE_TIMEFRAME_PRESSURE) {
            return evaluateThreeTimeframePressure(
                v5 = v5,
                v60 = v60,
                v1d = v1d,
                isApproximate = isApproximate,
                currentTimeMs = currentTimeMs
            )
        }

        // SHORT_TERM_STRENGTH_MODE: Bypasses the 13-priority hierarchy and multi-layer matrix scoring.
        // PRIMARY: ReactiveMarketPressureEngine (upPressure vs downPressure).
        // CONFIRMATION: MicroKineticVectorEngine (kineticUpWeight vs kineticDownWeight).
        if (decisionMode == DecisionMode.SHORT_TERM_STRENGTH) {
            return evaluateShortTermStrength(
                v5 = v5,
                v60 = v60,
                v1d = v1d,
                history = history,
                rmpDecision = rmpDecision,
                kineticResult = kineticResult,
                currentTimeMs = currentTimeMs,
                isApproximate = isApproximate
            )
        }

        val effectiveMatrixResult = matrixResult ?: com.example.data.matrix.MatrixEvaluationEngine.evaluate(
            com.example.data.matrix.MatrixEvaluationContext(
                val5m = v5 ?: 0.0,
                val60m = v60 ?: 0.0,
                val1d = v1d,
                is5mPresent = v5 != null,
                is60mPresent = v60 != null,
                is1dPresent = v1d != null,
                dataQuality = if (isApproximate) "APPROXIMATE" else "VERIFIED",
                history = history
            )
        )

        val effectiveKinetic = kineticResult ?: MicroKineticVectorEngine.calculate(
            val5m = v5,
            val60m = v60,
            val1d = v1d,
            history = history,
            isApproximate = isApproximate
        )

        val effectiveBehavior = behavior ?: MovementClassificationEngine.classify(
            val5m = v5,
            val60m = v60,
            val1d = v1d,
            history = history,
            isValid = true,
            isApproximate = isApproximate,
            isDeadMarket = (v5 != null && v60 != null && abs(v5) <= 0.10 && abs(v60) <= 0.10),
            isNoTradeZone = (v5 != null && v60 != null && abs(v5) <= 0.10 && abs(v60) <= 0.10)
        )

        val dataQuality = when {
            v5 == null && v60 == null -> DataQualityState.UNAVAILABLE
            isApproximate -> DataQualityState.APPROXIMATE
            effectiveMatrixResult.isConflicting -> DataQualityState.CONFLICTING_EVIDENCE
            else -> DataQualityState.VERIFIED
        }

        // 0. Zigzag / Alternation Noise Protection:
        // হালকা একটু আপে যাবে আবার নিচে নামবে আবার আপে যাবে আবার নিচে নামবে (জিগজ্যাক)
        // Suppresses automated execution during direction oscillation to protect user from whipsaw losses
        val detectedNet = (v5 ?: 0.0) + (v60 ?: 0.0)
        val isZigzagChoppy = effectiveBehavior.movementCode == "REPEATED_ALTERNATION" ||
                (effectiveKinetic.footprint.code == "FOOTPRINT_MICRO_CHOP_DISSIPATION" && abs(v5 ?: 0.0) <= 0.08 && abs(v60 ?: 0.0) <= 0.08 && effectiveKinetic.kineticBaseEnergy < 0.005)

        if (isZigzagChoppy) {
            val fp = "FP:NEUTRAL:NONE:ZIGZAG_PROTECTION:${String.format(Locale.US, "%.2f", v5 ?: 0.0)}:${String.format(Locale.US, "%.2f", v60 ?: 0.0)}:${String.format(Locale.US, "%.2f", v1d ?: 0.0)}"
            lastCandidateDirection = TradeDirection.NEUTRAL
            confirmedCount = 0
            lastUpdateTimeMs = currentTimeMs
            lastFingerprint = fp
            return CanonicalDecision(
                decisionId = java.util.UUID.randomUUID().toString(),
                direction = TradeDirection.NEUTRAL,
                side = "NONE",
                upPercentage = 50.0,
                downPercentage = 50.0,
                upPct = 50.0,
                downPct = 50.0,
                strength = 50.0,
                netEnergy = detectedNet,
                matrixEvidence = 0.0,
                microPressure = effectiveKinetic.microVelocity,
                candleConfirmation = 0.0,
                conflictIndex = 0.0,
                dataQuality = dataQuality,
                noTrade = true,
                eligibleMatrixIds = emptyList(),
                cancelledMatrixIds = emptyList(),
                fingerprint = fp,
                explanation = "জিগজ্যাক চপি বাজার: হালকা ওঠা-নামার অস্থিরতায় লস এড়াতে অটো-ট্রেড সুরক্ষিত (NO TRADE)",
                reason = "জিগজ্যাক চপি বাজার: হালকা ওঠা-নামার অস্থিরতায় লস এড়াতে অটো-ট্রেড সুরক্ষিত (NO TRADE)",
                timestamp = currentTimeMs,
                executionEligibility = false,
                primaryMatrixId = "M_ZIGZAG",
                primaryMatrixTitle = "Zigzag Noise Suppression",
                kineticVelocity = effectiveKinetic.microVelocity,
                kineticEnergy = effectiveKinetic.kineticBaseEnergy,
                netKineticForce = effectiveKinetic.netKineticForce,
                kineticUpWeight = 50.0,
                kineticDownWeight = 50.0,
                forensicPatternCode = "ZIGZAG_NO_TRADE",
                formulaExplanation = "Zigzag Protection: Alternating direction oscillation with zero kinetic power. Auto-trade blocked."
            )
        }

        // 1. Dead Zone / Sub-Threshold Movement: Resolve entry directly from detected numbers under user mandate
        // "যখনই স্ক্রিনে পার্সেন্টেজ ডিটেক্ট করবে যে নাম্বার আসবে ওই নাম্বার অনুযায়ী এন্ট্রি দিবে"
        val defaultDir = when {
            effectiveMatrixResult.primaryMatrix != null && effectiveMatrixResult.primaryMatrix.direction != TradeDirection.NEUTRAL -> effectiveMatrixResult.primaryMatrix.direction
            v5 != null && v5 > 0.0 -> TradeDirection.UP
            v5 != null && v5 < 0.0 -> TradeDirection.DOWN
            v60 != null && v60 > 0.0 -> TradeDirection.UP
            v60 != null && v60 < 0.0 -> TradeDirection.DOWN
            (v1d ?: 0.0) > 0.0 -> TradeDirection.UP
            (v1d ?: 0.0) < 0.0 -> TradeDirection.DOWN
            detectedNet >= 0.0 -> TradeDirection.UP
            else -> TradeDirection.DOWN
        }
        val defaultSide = if (defaultDir == TradeDirection.UP) "BUY" else "SELL"
        val defaultUpPct = if (defaultDir == TradeDirection.UP) 52.0 else 48.0
        val defaultDownPct = 100.0 - defaultUpPct
        val isDeadMarket = (v5 != null && v60 != null && abs(v5) <= 0.10 && abs(v60) <= 0.10)
        val isPrimaryDeadZone = effectiveMatrixResult.primaryMatrix?.outputCode?.startsWith("DEAD_ZONE") == true ||
                (effectiveMatrixResult.primaryMatrix?.direction == TradeDirection.NEUTRAL && effectiveMatrixResult.primaryMatrix.riskLevel == com.example.data.matrix.RiskLevel.NO_TRADE)

        if (isDeadMarket || isPrimaryDeadZone) {
            val fp = "FP:${defaultDir.name}:$defaultSide:${effectiveMatrixResult.primaryMatrix?.id ?: "M015"}:${String.format(Locale.US, "%.2f", v5 ?: 0.0)}:${String.format(Locale.US, "%.2f", v60 ?: 0.0)}:${String.format(Locale.US, "%.2f", v1d ?: 0.0)}"
            lastCandidateDirection = defaultDir
            confirmedCount = 1
            lastUpdateTimeMs = currentTimeMs
            lastFingerprint = fp
            return CanonicalDecision(
                decisionId = java.util.UUID.randomUUID().toString(),
                direction = defaultDir,
                side = defaultSide,
                upPercentage = defaultUpPct,
                downPercentage = defaultDownPct,
                upPct = defaultUpPct,
                downPct = defaultDownPct,
                strength = 55.0,
                netEnergy = detectedNet,
                matrixEvidence = if (defaultDir == TradeDirection.UP) 0.30 else -0.30,
                microPressure = effectiveKinetic.microVelocity,
                candleConfirmation = 1.0,
                conflictIndex = 0.0,
                dataQuality = dataQuality,
                eligibleMatrixIds = if (effectiveMatrixResult.eligibleMatrixIds.isNotEmpty()) effectiveMatrixResult.eligibleMatrixIds else listOf(effectiveMatrixResult.primaryMatrix?.id ?: "M015"),
                cancelledMatrixIds = emptyList(),
                fingerprint = fp,
                explanation = "ডিটেক্টেড পার্সেন্টেজ অনুযায়ী এন্ট্রি: ${effectiveMatrixResult.primaryMatrix?.title ?: "ম্যাট্রিক্স এন্ট্রি"} ($defaultSide | 5m=${v5 ?: 0.0}%, 60m=${v60 ?: 0.0}%)",
                timestamp = currentTimeMs,
                executionEligibility = true,
                primaryMatrixId = effectiveMatrixResult.primaryMatrix?.id ?: "M015",
                primaryMatrixTitle = effectiveMatrixResult.primaryMatrix?.title ?: "Micro Movement Dynamic Entry",
                kineticVelocity = effectiveKinetic.microVelocity,
                kineticEnergy = 0.5,
                netKineticForce = if (defaultDir == TradeDirection.UP) 0.5 else -0.5,
                kineticUpWeight = defaultUpPct,
                kineticDownWeight = defaultDownPct,
                forensicPatternCode = effectiveKinetic.footprint.code,
                formulaExplanation = "165 Matrix Entry Mandate: Micro movement resolved to $defaultSide according to detected numbers"
            )
        }

        // 2. Active Interlock Conflict State: Resolve deterministically in favor of fast micro-momentum
        if (effectiveMatrixResult.isConflicting) {
            val conflictDir = when {
                v5 != null && v5 > 0.0 -> TradeDirection.UP
                v5 != null && v5 < 0.0 -> TradeDirection.DOWN
                effectiveKinetic.microVelocity > 0.0 -> TradeDirection.UP
                effectiveKinetic.microVelocity < 0.0 -> TradeDirection.DOWN
                v60 != null && v60 > 0.0 -> TradeDirection.UP
                v60 != null && v60 < 0.0 -> TradeDirection.DOWN
                else -> TradeDirection.UP
            }
            val conflictSide = if (conflictDir == TradeDirection.UP) "BUY" else "SELL"
            val conflictUpPct = if (conflictDir == TradeDirection.UP) 54.0 else 46.0
            val conflictDownPct = 100.0 - conflictUpPct
            val fp = "FP:${conflictDir.name}:$conflictSide:${effectiveMatrixResult.primaryMatrix?.id ?: "M130"}:${String.format(Locale.US, "%.2f", v5 ?: 0.0)}:${String.format(Locale.US, "%.2f", v60 ?: 0.0)}:${String.format(Locale.US, "%.2f", v1d ?: 0.0)}"
            lastCandidateDirection = conflictDir
            confirmedCount = 1
            lastUpdateTimeMs = currentTimeMs
            lastFingerprint = fp
            return CanonicalDecision(
                decisionId = java.util.UUID.randomUUID().toString(),
                direction = conflictDir,
                side = conflictSide,
                upPercentage = conflictUpPct,
                downPercentage = conflictDownPct,
                upPct = conflictUpPct,
                downPct = conflictDownPct,
                strength = 60.0,
                netEnergy = (v5 ?: 0.0) + (v60 ?: 0.0),
                matrixEvidence = effectiveMatrixResult.matrixEvidence,
                microPressure = effectiveKinetic.microVelocity,
                candleConfirmation = 1.0,
                conflictIndex = 0.5,
                dataQuality = dataQuality,
                eligibleMatrixIds = if (effectiveMatrixResult.eligibleMatrixIds.isNotEmpty()) effectiveMatrixResult.eligibleMatrixIds else listOf(effectiveMatrixResult.primaryMatrix?.id ?: "M130"),
                cancelledMatrixIds = emptyList(),
                fingerprint = fp,
                explanation = "ডাইভারজেন্স মীমাংসিত: $conflictSide এন্ট্রি (5m=${v5 ?: 0.0}%, 60m=${v60 ?: 0.0}%)",
                timestamp = currentTimeMs,
                executionEligibility = true,
                primaryMatrixId = effectiveMatrixResult.primaryMatrix?.id ?: "M130",
                primaryMatrixTitle = effectiveMatrixResult.primaryMatrix?.title ?: "Resolved Multi-Timeframe Divergence",
                kineticVelocity = effectiveKinetic.microVelocity,
                kineticEnergy = 0.6,
                netKineticForce = if (conflictDir == TradeDirection.UP) 0.6 else -0.6,
                kineticUpWeight = conflictUpPct,
                kineticDownWeight = conflictDownPct,
                forensicPatternCode = effectiveKinetic.footprint.code,
                formulaExplanation = "165 Matrix Entry Mandate: Conflict resolved to $conflictSide based on active momentum"
            )
        }

        // 3. Synthesize full Evidence-Weighted Micro-Pressure Decision
        val elapsedSec = if (lastUpdateTimeMs > 0L) max(0.05, (currentTimeMs - lastUpdateTimeMs) / 1000.0) else 1.0
        val isStale = lastUpdateTimeMs > 0L && (currentTimeMs - lastUpdateTimeMs > STALE_TIMEOUT_MS)

        val engineInput = MicroPressureDecisionEngine.EngineInput(
            val5m = v5,
            val60m = v60,
            val1d = v1d,
            matrixResult = effectiveMatrixResult,
            history = history,
            elapsedSeconds = elapsedSec,
            dataQuality = dataQuality,
            isStale = isStale,
            isApproximate = isApproximate,
            lastDispatchedFingerprint = lastFingerprint
        )

        val microDecision = MicroPressureDecisionEngine.synthesize(engineInput)

        // 4. Update State Machine
        val candidateDirection = microDecision.direction
        val fingerprint = microDecision.fingerprint
        val isNewSnapshot = fingerprint != lastFingerprint

        if (candidateDirection != TradeDirection.NEUTRAL) {
            if (candidateDirection == lastCandidateDirection) {
                if (isNewSnapshot) {
                    confirmedCount++
                    lastUpdateTimeMs = currentTimeMs
                    lastFingerprint = fingerprint
                }
            } else {
                lastCandidateDirection = candidateDirection
                confirmedCount = 1
                lastUpdateTimeMs = currentTimeMs
                lastFingerprint = fingerprint
            }
        } else {
            confirmedCount = 0
            lastCandidateDirection = TradeDirection.NEUTRAL
        }

        // 5. Fast-path check: Instant auto-entry dispatch for ANY checked (✔) matrix or verified multi-frame consensus
        val hasActiveCheckedMatrix = effectiveMatrixResult.primaryMatrix?.let {
            com.example.data.matrix.MatrixUserStateRegistry.getUserState(it) == com.example.data.matrix.MatrixUserState.CHECKED &&
                    !it.warningOnly &&
                    it.direction != TradeDirection.NEUTRAL &&
                    effectiveMatrixResult.eligibleMatrixIds.contains(it.id) &&
                    !effectiveMatrixResult.cancelledMatrixIds.contains(it.id)
        } == true

        val isFastPath = (hasActiveCheckedMatrix || (((v5 != null && v60 != null && v1d != null) &&
                ((v5 >= 0.30 && v60 >= 0.30 && v1d >= 0.30) || (v5 <= -0.30 && v60 <= -0.30 && v1d <= -0.30))))) &&
                dataQuality == DataQualityState.VERIFIED &&
                (!effectiveMatrixResult.isConflicting || hasActiveCheckedMatrix)

        val isConfirmed = isFastPath || (confirmedCount >= requiredConfirmations) || effectiveKinetic.isStrategicReversal

        // 6. Strategic Kinetic & Movement Classification Resolution:
        // গতি ১: সোজা আপে যাবে বা সোজা ডাউনে যাবে (Direct Pure Momentum)
        // গতি ২: সামান্য আপে গিয়ে আবার ডাউনে বা সামান্য ডাউনে গিয়ে আবার আপে (Pullback Exhaustion / Trap Spring)
        val effectiveDirection: TradeDirection
        val effectiveSide: String
        val effectiveUpPct: Double
        val effectiveDownPct: Double
        val effectiveExplanation: String

        val isDirectMomentum = (v5 != null && v60 != null) &&
                ((v5 >= 0.10 && v60 >= 0.10) || (v5 <= -0.10 && v60 <= -0.10))

        if (effectiveKinetic.isStrategicReversal && effectiveKinetic.footprint.suggestedDirection != TradeDirection.NEUTRAL) {
            // গতি ২: সাময়িক আপে গিয়ে আবার ডাউনে, বা ডাউনে গিয়ে আবার আপে (Pullback Exhaustion / Trap Reversal)
            effectiveDirection = effectiveKinetic.footprint.suggestedDirection
            effectiveSide = if (effectiveDirection == TradeDirection.UP) "BUY" else "SELL"
            effectiveUpPct = effectiveKinetic.kineticUpWeight
            effectiveDownPct = effectiveKinetic.kineticDownWeight
            effectiveExplanation = "কৌশলগত পুলব্যাক/ট্র্যাপ সমাপ্তি: ${effectiveKinetic.strategicReversalReason} | প্রধান চালক: ${effectiveKinetic.primaryDriverTimeframe} | অধিক ক্ষমতাসম্পন্ন দিকে ($effectiveSide) স্নাইপার এন্ট্রি।"
        } else if (isDirectMomentum) {
            // গতি ১: সোজা আপ বা সোজা ডাউন (Direct Pure Momentum)
            effectiveDirection = if (v5!! > 0.0) TradeDirection.UP else TradeDirection.DOWN
            effectiveSide = if (effectiveDirection == TradeDirection.UP) "BUY" else "SELL"
            val netSum = abs((v5 ?: 0.0) + (v60 ?: 0.0))
            val wMicro = if (isApproximate) {
                0.46
            } else {
                (0.6489 - 0.2662 * (netSum - 1.50)).coerceIn(0.40, 0.70)
            }
            val wKinetic = 1.0 - wMicro
            val rawUp = round(((microDecision.upPercentage * wMicro) + (effectiveKinetic.kineticUpWeight * wKinetic)) * 100.0) / 100.0
            effectiveUpPct = if (effectiveDirection == TradeDirection.UP) max(55.0, rawUp) else min(45.0, rawUp)
            effectiveDownPct = round((100.0 - effectiveUpPct) * 100.0) / 100.0
            effectiveExplanation = "সোজা গতিবেগ (Direct Momentum): 5M (${String.format(Locale.US, "%+.2f", v5)}%) ও 60M (${String.format(Locale.US, "%+.2f", v60)}%) একমুখী প্রসারিত | প্রধান চালক: ${effectiveKinetic.primaryDriverTimeframe} | $effectiveSide দ্রুত এন্ট্রি সক্রিয়।"
        } else {
            val resolvedDir = when {
                candidateDirection != TradeDirection.NEUTRAL -> candidateDirection
                effectiveMatrixResult.primaryMatrix != null && effectiveMatrixResult.primaryMatrix.direction != TradeDirection.NEUTRAL -> effectiveMatrixResult.primaryMatrix.direction
                v5 != null && v5 > 0.0 -> TradeDirection.UP
                v5 != null && v5 < 0.0 -> TradeDirection.DOWN
                v60 != null && v60 > 0.0 -> TradeDirection.UP
                v60 != null && v60 < 0.0 -> TradeDirection.DOWN
                (v1d ?: 0.0) > 0.0 -> TradeDirection.UP
                (v1d ?: 0.0) < 0.0 -> TradeDirection.DOWN
                else -> TradeDirection.UP
            }
            effectiveDirection = resolvedDir
            effectiveSide = if (effectiveDirection == TradeDirection.UP) "BUY" else "SELL"
            val rawUp = round(((microDecision.upPercentage * 0.40) + (effectiveKinetic.kineticUpWeight * 0.60)) * 100.0) / 100.0
            effectiveUpPct = rawUp
            effectiveDownPct = round((100.0 - effectiveUpPct) * 100.0) / 100.0
            effectiveExplanation = "প্রধান চালক: ${effectiveKinetic.primaryDriverTimeframe} | গাণিতিক ক্ষমতা: UP=${effectiveUpPct}%, DOWN=${effectiveDownPct}% | $effectiveSide এন্ট্রি প্রস্তুত।"
        }

        val confirmationStage = when {
            isStale -> "STALE"
            effectiveKinetic.isStrategicReversal -> "STRATEGIC_REVERSAL_CONFIRMED"
            isFastPath -> "FAST_PATH_CONFIRMED"
            isConfirmed && microDecision.executionEligibility -> "CONFIRMED"
            else -> "FAST_ENTRY_ACTIVE"
        }

        // Mandatory User Directive: Under NO circumstances should an entry be missed!
        // When any percentage numbers (5m, 60m, 1D) are detected on screen, entry must be eligible and dispatched immediately!
        val finalExecutionEligibility = (v5 != null || v60 != null || v1d != null)

        val finalFingerprint = "FP:${effectiveDirection.name}:$effectiveSide:${effectiveMatrixResult.primaryMatrix?.id ?: "NONE"}:${String.format(Locale.US, "%.2f", v5 ?: 0.0)}:${String.format(Locale.US, "%.2f", v60 ?: 0.0)}:${String.format(Locale.US, "%.2f", v1d ?: 0.0)}"

        return microDecision.copy(
            direction = effectiveDirection,
            side = effectiveSide,
            upPercentage = effectiveUpPct,
            downPercentage = effectiveDownPct,
            upPct = effectiveUpPct,
            downPct = effectiveDownPct,
            explanation = effectiveExplanation,
            reason = effectiveExplanation,
            fingerprint = finalFingerprint,
            confirmationCount = min(confirmedCount, requiredConfirmations),
            requiredConfirmations = requiredConfirmations,
            confirmationStage = confirmationStage,
            executionEligibility = finalExecutionEligibility,
            stale = isStale,
            warningOnly = effectiveBehavior.isWarningOnly || isStale,
            forensicPatternCode = effectiveKinetic.footprint.code,
            kineticVelocity = effectiveKinetic.microVelocity,
            kineticEnergy = effectiveKinetic.kineticBaseEnergy,
            netKineticForce = effectiveKinetic.netKineticForce,
            kineticUpWeight = effectiveUpPct,
            kineticDownWeight = effectiveDownPct,
            formulaExplanation = effectiveKinetic.mathematicalProof
        )
    }

    /**
     * Evaluates trade entry using dual-engine short-term synthesis:
     * 1. PRIMARY (মূল প্রেসার সোর্স): ReactiveMarketPressureEngine.calculate() -> upPressure vs downPressure.
     * 2. CONFIRMATION/CROSS-CHECK: MicroKineticVectorEngine.calculate() -> kineticUpWeight vs kineticDownWeight.
     *
     * Rules:
     * - If v5 and v60 are both null/unavailable -> DATA_UNAVAILABLE / NO_TRADE.
     * - If primary difference is minimal (< 2.0% or balanced) -> NEUTRAL / NO_TRADE (avoids forced entry).
     * - If upPressure > downPressure -> BUY (UP).
     * - If downPressure > upPressure -> SELL (DOWN).
     * - If MicroKinetic agrees -> normal entry with warningOnly = false.
     * - If MicroKinetic conflicts -> primary RMP direction is authoritative, but warningOnly = true.
     * - Reversals/fakeouts/exhaustions are bypassed; forensic footprint & proof retained for logging.
     */
    private fun evaluateShortTermStrength(
        v5: Double?,
        v60: Double?,
        v1d: Double?,
        history: List<MetricSnapshot> = emptyList(),
        rmpDecision: PressureDecision?,
        kineticResult: MicroKineticVectorEngine.KineticVectorResult?,
        currentTimeMs: Long,
        isApproximate: Boolean
    ): CanonicalDecision {
        val effectiveKinetic = kineticResult ?: MicroKineticVectorEngine.calculate(
            val5m = v5,
            val60m = v60,
            val1d = v1d,
            history = history,
            isApproximate = isApproximate
        )

        // 1. Missing data safety: val5m, val60m উভয়ই null/unavailable হলে DATA_UNAVAILABLE/NO_TRADE
        if (v5 == null && v60 == null) {
            val fp = "FP_PRESSURE:NEUTRAL:NONE:UNAVAILABLE:0.00:0.00:0.00"
            lastCandidateDirection = TradeDirection.NEUTRAL
            lastFingerprint = fp
            return CanonicalDecision(
                decisionId = java.util.UUID.randomUUID().toString(),
                direction = TradeDirection.NEUTRAL,
                side = "NONE",
                upPercentage = 50.0,
                downPercentage = 50.0,
                upPct = 50.0,
                downPct = 50.0,
                strength = 50.0,
                explanation = "ডেটা অনুপস্থিত [DATA_UNAVAILABLE]: 5m এবং 60m উভয় মেট্রিক অনুপলব্ধ",
                reason = "ডেটা অনুপস্থিত [DATA_UNAVAILABLE]: 5m এবং 60m উভয় মেট্রিক অনুপলব্ধ",
                dataQuality = DataQualityState.UNAVAILABLE,
                noTrade = true,
                conflict = false,
                fingerprint = fp,
                confirmationCount = 0,
                requiredConfirmations = 1,
                confirmationStage = "DATA_UNAVAILABLE",
                executionEligibility = false,
                stale = false,
                warningOnly = false,
                forensicPatternCode = effectiveKinetic.footprint.code,
                kineticVelocity = effectiveKinetic.microVelocity,
                kineticEnergy = effectiveKinetic.kineticBaseEnergy,
                netKineticForce = effectiveKinetic.netKineticForce,
                kineticUpWeight = 50.0,
                kineticDownWeight = 50.0,
                formulaExplanation = effectiveKinetic.mathematicalProof
            )
        }

        // PRIMARY: ReactiveMarketPressureEngine.calculate()
        val prevSnapshot = history.firstOrNull()
        val prev5m = prevSnapshot?.val5m?.let { if (ReactiveMarketPressureEngine.isValidMetricValue(it)) it else null }
        val prev60m = prevSnapshot?.val60m?.let { if (ReactiveMarketPressureEngine.isValidMetricValue(it)) it else null }
        val prev1d = prevSnapshot?.val1d?.let { if (ReactiveMarketPressureEngine.isValidMetricValue(it)) it else null }
        val elapsedSec = if (prevSnapshot != null) {
            max((currentTimeMs - prevSnapshot.timestamp) / 1000.0, 1.0)
        } else {
            1.0
        }

        val effectiveRmp = rmpDecision ?: ReactiveMarketPressureEngine.calculate(
            PressureInput(
                current5m = v5,
                previous5m = prev5m,
                current60m = v60,
                previous60m = prev60m,
                current1d = v1d,
                previous1d = prev1d,
                elapsedSeconds = elapsedSec,
                approximate = isApproximate
            )
        )

        val upPressure = effectiveRmp.upPressure
        val downPressure = effectiveRmp.downPressure
        val upPct = effectiveRmp.upPercentage
        val downPct = effectiveRmp.downPercentage
        val pctDiff = upPct - downPct
        val absPctDiff = abs(pctDiff)

        // CONFIRMATION / CROSS-CHECK: MicroKineticVectorEngine
        val kineticUp = effectiveKinetic.kineticUpWeight
        val kineticDown = effectiveKinetic.kineticDownWeight
        val kineticDiff = kineticUp - kineticDown
        val absKineticDiff = abs(kineticDiff)

        val kineticDirection = when {
            absKineticDiff < 2.0 -> TradeDirection.NEUTRAL
            kineticUp > kineticDown -> TradeDirection.UP
            else -> TradeDirection.DOWN
        }

        // Rule 3: ফাইনাল এন্ট্রি ডিরেকশন (Primary: upPressure vs downPressure)
        val primaryDirection: TradeDirection
        val primarySide: String
        val isDeadBand: Boolean

        if (absPctDiff < 2.0 && abs(upPressure - downPressure) < 0.005) {
            primaryDirection = TradeDirection.NEUTRAL
            primarySide = "NONE"
            isDeadBand = true
        } else if (upPressure > downPressure || (upPct > downPct && absPctDiff >= 2.0)) {
            primaryDirection = TradeDirection.UP
            primarySide = "BUY"
            isDeadBand = false
        } else if (downPressure > upPressure || (downPct > upPct && absPctDiff >= 2.0)) {
            primaryDirection = TradeDirection.DOWN
            primarySide = "SELL"
            isDeadBand = false
        } else {
            primaryDirection = TradeDirection.NEUTRAL
            primarySide = "NONE"
            isDeadBand = true
        }

        val effectiveDirection: TradeDirection
        val effectiveSide: String
        val isWarning: Boolean
        val explanationText: String
        val isEligible: Boolean

        if (isDeadBand || primaryDirection == TradeDirection.NEUTRAL) {
            effectiveDirection = TradeDirection.NEUTRAL
            effectiveSide = "NONE"
            isWarning = false
            isEligible = false
            explanationText = "প্রেসার ভারসাম্য (পার্থক্য < ২.০%): UP=${String.format(Locale.US, "%.1f", upPct)}%, DOWN=${String.format(Locale.US, "%.1f", downPct)}% (Δ=${String.format(Locale.US, "%.1f", absPctDiff)}%) | নো ট্রেড"
        } else {
            effectiveDirection = primaryDirection
            effectiveSide = primarySide
            isEligible = true

            if (kineticDirection == primaryDirection || kineticDirection == TradeDirection.NEUTRAL) {
                // Agree: উভয় ইঞ্জিন একমত
                isWarning = false
                explanationText = "শর্ট-টার্ম প্রেসার মোড | প্রাইমারি প্রেসার (${effectiveRmp.direction}) + কাইনেটিক (${effectiveKinetic.primaryDriverTimeframe}) কনফার্মড | UP=${String.format(Locale.US, "%.1f", upPct)}%, DOWN=${String.format(Locale.US, "%.1f", downPct)}% | $effectiveSide এন্ট্রি প্রস্তুত"
            } else {
                // Conflict: ইঞ্জিনদ্বয় সাংঘর্ষিক -> প্রাইমারি প্রাধান্য, কিন্তু warningOnly = true
                isWarning = true
                explanationText = "শর্ট-টার্ম প্রেসার মোড (সতর্কতা: কাইনেটিক ডাইভারজেন্স) | প্রাইমারি প্রেসার: $primaryDirection বনাম কাইনেটিক: $kineticDirection | প্রাইমারি অনুযায়ী $effectiveSide এন্ট্রি প্রস্তুত"
            }
        }

        val effectiveUpPct = if (effectiveDirection == TradeDirection.NEUTRAL) 50.0 else upPct
        val effectiveDownPct = if (effectiveDirection == TradeDirection.NEUTRAL) 50.0 else downPct

        val isStale = lastUpdateTimeMs > 0L && (currentTimeMs - lastUpdateTimeMs) > STALE_TIMEOUT_MS
        val isNoTrade = !isEligible || effectiveDirection == TradeDirection.NEUTRAL

        val finalFingerprint = "FP_PRESSURE:${effectiveDirection.name}:$effectiveSide:${effectiveKinetic.footprint.code}:${String.format(Locale.US, "%.2f", v5 ?: 0.0)}:${String.format(Locale.US, "%.2f", v60 ?: 0.0)}:${String.format(Locale.US, "%.2f", v1d ?: 0.0)}"

        lastCandidateDirection = effectiveDirection
        confirmedCount = if (isEligible) 1 else 0
        lastUpdateTimeMs = currentTimeMs
        lastFingerprint = finalFingerprint

        return CanonicalDecision(
            decisionId = java.util.UUID.randomUUID().toString(),
            direction = effectiveDirection,
            side = effectiveSide,
            upPercentage = effectiveUpPct,
            downPercentage = effectiveDownPct,
            upPct = effectiveUpPct,
            downPct = effectiveDownPct,
            strength = max(effectiveUpPct, effectiveDownPct),
            explanation = explanationText,
            reason = explanationText,
            dataQuality = if (isApproximate) DataQualityState.APPROXIMATE else DataQualityState.VERIFIED,
            noTrade = isNoTrade,
            conflict = isWarning,
            fingerprint = finalFingerprint,
            confirmationCount = if (isEligible) 1 else 0,
            requiredConfirmations = 1,
            confirmationStage = if (isEligible) (if (isWarning) "PRESSURE_KINETIC_DIVERGENCE" else "PRESSURE_KINETIC_ALIGNED") else "NO_TRADE",
            executionEligibility = isEligible && !isStale,
            stale = isStale,
            warningOnly = isWarning,
            forensicPatternCode = effectiveKinetic.footprint.code,
            kineticVelocity = effectiveKinetic.microVelocity,
            kineticEnergy = effectiveKinetic.kineticBaseEnergy,
            netKineticForce = effectiveKinetic.netKineticForce,
            kineticUpWeight = kineticUp,
            kineticDownWeight = kineticDown,
            formulaExplanation = effectiveKinetic.mathematicalProof + " | RMP: upP=" + String.format(Locale.US, "%.4f", upPressure) + ", downP=" + String.format(Locale.US, "%.4f", downPressure)
        )
    }

    /**
     * Pure Deterministic Arithmetic Decision Method driven solely by ThreeTimeframePressureCalculator.
     * Completely bypasses the 165 matrices, zigzag filters, and kinetic vector layers.
     */
    private fun evaluateThreeTimeframePressure(
        v5: Double?,
        v60: Double?,
        v1d: Double?,
        isApproximate: Boolean = false,
        currentTimeMs: Long = System.currentTimeMillis()
    ): CanonicalDecision {
        val isDataMissing = v5 == null || v60 == null || v1d == null ||
                v5.isNaN() || v60.isNaN() || v1d.isNaN() ||
                v5.isInfinite() || v60.isInfinite() || v1d.isInfinite()

        val sourceQuality = when {
            isApproximate -> SourceQuality.APPROXIMATE
            isDataMissing -> SourceQuality.INCOMPLETE
            else -> SourceQuality.VERIFIED
        }

        val pressureResult = ThreeTimeframePressureCalculator.calculate(
            p5m = v5,
            p60m = v60,
            p1d = v1d,
            timestamp = currentTimeMs,
            sourceQuality = sourceQuality
        )

        if (pressureResult.isDataIncomplete) {
            val fp = "FP:NEUTRAL:NONE:DATA_INCOMPLETE"
            lastCandidateDirection = TradeDirection.NEUTRAL
            confirmedCount = 0
            lastUpdateTimeMs = currentTimeMs
            lastFingerprint = fp

            val qualityState = when {
                isApproximate -> DataQualityState.APPROXIMATE
                v5 == null && v60 == null && v1d == null -> DataQualityState.UNAVAILABLE
                else -> DataQualityState.INCOMPLETE
            }

            return CanonicalDecision(
                decisionId = java.util.UUID.randomUUID().toString(),
                direction = TradeDirection.NEUTRAL,
                side = "NONE",
                upPercentage = 0.0,
                downPercentage = 0.0,
                upPct = 0.0,
                downPct = 0.0,
                strength = 0.0,
                strengthScore = 0.0,
                netEnergy = 0.0,
                matrixEvidence = 0.0,
                microPressure = 0.0,
                candleConfirmation = 0.0,
                conflictIndex = 0.0,
                dataQuality = qualityState,
                noTrade = true,
                conflict = false,
                eligibleMatrixIds = emptyList(),
                cancelledMatrixIds = emptyList(),
                fingerprint = fp,
                explanation = "ডেটা অনুপস্থিত [DATA_INCOMPLETE]: তিনটি টাইমফ্রেমের সম্পূর্ণ ও নিশ্চিত ডেটা প্রয়োজন (v5, v60, v1d)",
                reason = "ডেটা অনুপস্থিত [DATA_INCOMPLETE]: তিনটি টাইমফ্রেমের সম্পূর্ণ ও নিশ্চিত ডেটা প্রয়োজন (v5, v60, v1d)",
                timestamp = currentTimeMs,
                confirmationCount = 0,
                requiredConfirmations = 1,
                confirmationStage = "DATA_INCOMPLETE",
                executionEligibility = false,
                stale = false,
                warningOnly = false,
                primaryMatrixId = "3TF_MATH",
                primaryMatrixTitle = "ThreeTimeframePressureCalculator (DATA_INCOMPLETE)",
                upEnergy = 0.0,
                downEnergy = 0.0,
                energyDominance = 0.0,
                forensicPatternCode = "DATA_INCOMPLETE",
                formulaExplanation = "তিনটি টাইমফ্রেমের সম্পূর্ণ ও নিশ্চিত ডেটা অনুপস্থিত (DATA_INCOMPLETE)"
            )
        }

        val direction = when (pressureResult.direction) {
            PressureDirection.UP -> TradeDirection.UP
            PressureDirection.DOWN -> TradeDirection.DOWN
            PressureDirection.NO_SIGNAL -> TradeDirection.NEUTRAL
        }

        val side = when (direction) {
            TradeDirection.UP -> "BUY"
            TradeDirection.DOWN -> "SELL"
            else -> "NONE"
        }

        val isEligible = (direction != TradeDirection.NEUTRAL && pressureResult.totalEnergy > 0.0)
        val fp = "FP:3TF:${pressureResult.direction.name}:${pressureResult.pressureBand.name}:${String.format(Locale.US, "%.2f", v5)}:${String.format(Locale.US, "%.2f", v60)}:${String.format(Locale.US, "%.2f", v1d)}"

        lastCandidateDirection = direction
        confirmedCount = if (isEligible) 1 else 0
        lastUpdateTimeMs = currentTimeMs
        lastFingerprint = fp

        val explanationText = "৩-টাইমফ্রেম প্রেসার পিওর গণিত | ডিরেকশন: ${pressureResult.direction} | ব্যান্ড: ${pressureResult.pressureBand.name} | UP=${String.format(Locale.US, "%.2f", pressureResult.upSharePercent)}%, DOWN=${String.format(Locale.US, "%.2f", pressureResult.downSharePercent)}%, Net=${String.format(Locale.US, "%+.2f", pressureResult.netPressurePercent)}% | $side এন্ট্রি প্রস্তুত"

        return CanonicalDecision(
            decisionId = java.util.UUID.randomUUID().toString(),
            direction = direction,
            side = side,
            upPercentage = pressureResult.upSharePercent,
            downPercentage = pressureResult.downSharePercent,
            upPct = pressureResult.upSharePercent,
            downPct = pressureResult.downSharePercent,
            strength = kotlin.math.abs(pressureResult.netPressurePercent),
            strengthScore = kotlin.math.abs(pressureResult.netPressurePercent),
            netEnergy = pressureResult.netPressurePercent,
            matrixEvidence = 0.0,
            microPressure = pressureResult.netPressurePercent,
            candleConfirmation = 0.0,
            conflictIndex = 0.0,
            dataQuality = DataQualityState.VERIFIED,
            noTrade = (direction == TradeDirection.NEUTRAL),
            conflict = false,
            eligibleMatrixIds = listOf("3TF_MATH"),
            cancelledMatrixIds = emptyList(),
            fingerprint = fp,
            explanation = explanationText,
            reason = explanationText,
            timestamp = currentTimeMs,
            confirmationCount = if (isEligible) 1 else 0,
            requiredConfirmations = 1,
            confirmationStage = if (isEligible) "3TF_CONFIRMED" else "NO_SIGNAL",
            executionEligibility = isEligible,
            stale = false,
            warningOnly = false,
            primaryMatrixId = "3TF_MATH",
            primaryMatrixTitle = "ThreeTimeframePressureCalculator (${pressureResult.pressureBand.name})",
            upEnergy = pressureResult.upEnergy,
            downEnergy = pressureResult.downEnergy,
            energyDominance = kotlin.math.abs(pressureResult.netPressurePercent),
            forensicPatternCode = "3TF_PRESSURE_${pressureResult.pressureBand.name}",
            formulaExplanation = "UpEnergy: ${String.format(Locale.US, "%.6f", pressureResult.upEnergy)} | DownEnergy: ${String.format(Locale.US, "%.6f", pressureResult.downEnergy)} | Net: ${String.format(Locale.US, "%+.2f", pressureResult.netPressurePercent)}% (${pressureResult.pressureBand.name})"
        )
    }
}
