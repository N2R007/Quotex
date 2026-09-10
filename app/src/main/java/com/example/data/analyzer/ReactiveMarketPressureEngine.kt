package com.example.data.analyzer

import com.example.audio.AudioSignalEngine
import com.example.data.matrix.Authorized106MatrixEngine
import com.example.data.matrix.Directional206MatrixEngine
import com.example.data.matrix.MatrixEvaluationContext
import com.example.data.matrix.MatrixEvaluationEngine
import com.example.data.models.DataQualityState
import com.example.data.models.MetricSnapshot
import com.example.data.models.SignalType
import com.example.data.models.StrengthLevel
import com.example.data.models.TradeDirection
import com.example.data.models.TradingAnalysis
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh

/**
 * Quant Vision v62 — Reactive Market-Pressure Auto-Entry Engine.
 *
 * Implements a pure, deterministic pressure-following signal engine that follows the direction
 * observed from available multi-timeframe metrics (5m, 60m, 1D).
 * Single authority for final direction, movement, strength, and UP/DOWN pressure.
 */
object ReactiveMarketPressureEngine {

    const val ENGINE_VERSION = "v62"
    private const val EPSILON = 1e-9

    // Default scales for level normalization
    const val SCALE_5M = 1.0
    const val SCALE_60M = 1.0
    const val SCALE_1D = 2.0

    // Default scales for velocity normalization
    const val VELOCITY_SCALE_5M = 0.5
    const val VELOCITY_SCALE_60M = 0.5
    const val VELOCITY_SCALE_1D = 0.5

    // Fast-entry base weights
    const val BASE_WEIGHT_5M = 0.45
    const val BASE_WEIGHT_60M = 0.40
    const val BASE_WEIGHT_1D = 0.15

    enum class Timeframe { FIVE_MIN, SIXTY_MIN, ONE_DAY }

    data class PressureInput(
        val current5m: Double?,
        val previous5m: Double?,
        val current60m: Double?,
        val previous60m: Double?,
        val current1d: Double?,
        val previous1d: Double?,
        val elapsedSeconds: Double,
        val approximate: Boolean,
        val matrixDirectionalSupport: Double = 0.0,
        val matrixDirectionalConflict: Double = 0.0
    )

    data class PressureDecision(
        val direction: TradeDirection,
        val upPercentage: Double,
        val downPercentage: Double,
        val upPressure: Double,
        val downPressure: Double,
        val pressureStrength: Double,
        val movement: Double,
        val confidencePenalty: Double,
        val availableTimeframes: Set<Timeframe>,
        val fingerprint: String,
        val explanation: String
    )

    fun isValidMetricValue(value: Double?): Boolean {
        return value != null && value.isFinite() && !value.isNaN() && abs(value) <= TradingOutputParser.MAX_VALID_PERCENTAGE
    }

    fun mapStrengthLevel(pressureStrength: Double): com.example.data.models.StrengthLevel {
        return when {
            pressureStrength < 40.0 -> com.example.data.models.StrengthLevel.NORMAL
            pressureStrength < 70.0 -> com.example.data.models.StrengthLevel.MEDIUM
            else -> com.example.data.models.StrengthLevel.HIGH
        }
    }

    fun calculate(input: PressureInput): PressureDecision {
        val availableTfs = mutableSetOf<Timeframe>()
        val valid5m = if (isValidMetricValue(input.current5m)) input.current5m else null
        val valid60m = if (isValidMetricValue(input.current60m)) input.current60m else null
        val valid1d = if (isValidMetricValue(input.current1d)) input.current1d else null

        val prev5m = if (isValidMetricValue(input.previous5m)) input.previous5m else null
        val prev60m = if (isValidMetricValue(input.previous60m)) input.previous60m else null
        val prev1d = if (isValidMetricValue(input.previous1d)) input.previous1d else null

        if (valid5m != null) availableTfs.add(Timeframe.FIVE_MIN)
        if (valid60m != null) availableTfs.add(Timeframe.SIXTY_MIN)
        if (valid1d != null) availableTfs.add(Timeframe.ONE_DAY)

        val confidencePenalty = if (input.approximate) 0.15 else 0.00

        if (availableTfs.isEmpty()) {
            val fp = buildFingerprint(input, TradeDirection.NEUTRAL, 0.0, emptySet())
            return PressureDecision(
                direction = TradeDirection.NEUTRAL,
                upPercentage = 50.0,
                downPercentage = 50.0,
                upPressure = 0.0,
                downPressure = 0.0,
                pressureStrength = 0.0,
                movement = 0.0,
                confidencePenalty = confidencePenalty,
                availableTimeframes = emptySet(),
                fingerprint = fp,
                explanation = "No usable numeric metrics available."
            )
        }

        // 1. Calculate weights renormalized across available timeframes
        var sumBaseWeights = 0.0
        if (Timeframe.FIVE_MIN in availableTfs) sumBaseWeights += BASE_WEIGHT_5M
        if (Timeframe.SIXTY_MIN in availableTfs) sumBaseWeights += BASE_WEIGHT_60M
        if (Timeframe.ONE_DAY in availableTfs) sumBaseWeights += BASE_WEIGHT_1D

        val w5m = if (Timeframe.FIVE_MIN in availableTfs) BASE_WEIGHT_5M / sumBaseWeights else 0.0
        val w60m = if (Timeframe.SIXTY_MIN in availableTfs) BASE_WEIGHT_60M / sumBaseWeights else 0.0
        val w1d = if (Timeframe.ONE_DAY in availableTfs) BASE_WEIGHT_1D / sumBaseWeights else 0.0

        val elapsed = max(input.elapsedSeconds, 1.0)

        // 2. Calculate observed force per timeframe
        // observedForce_i = 0.65 * normalizedLevel_i + 0.35 * normalizedVelocity_i
        var upPressure = 0.0
        var downPressure = 0.0
        var netMovement = 0.0

        if (Timeframe.FIVE_MIN in availableTfs && valid5m != null) {
            val force5m = computeObservedForce(valid5m, prev5m, elapsed, SCALE_5M, VELOCITY_SCALE_5M)
            upPressure += w5m * max(force5m, 0.0)
            downPressure += w5m * max(-force5m, 0.0)
            netMovement += w5m * force5m
        }

        if (Timeframe.SIXTY_MIN in availableTfs && valid60m != null) {
            val force60m = computeObservedForce(valid60m, prev60m, elapsed, SCALE_60M, VELOCITY_SCALE_60M)
            upPressure += w60m * max(force60m, 0.0)
            downPressure += w60m * max(-force60m, 0.0)
            netMovement += w60m * force60m
        }

        if (Timeframe.ONE_DAY in availableTfs && valid1d != null) {
            val force1d = computeObservedForce(valid1d, prev1d, elapsed, SCALE_1D, VELOCITY_SCALE_1D)
            upPressure += w1d * max(force1d, 0.0)
            downPressure += w1d * max(-force1d, 0.0)
            netMovement += w1d * force1d
        }

        // 3. Dominance and continuous direction
        val isDeadZone = if (valid5m != null && valid60m != null) {
            abs(valid5m) <= 0.10 && abs(valid60m) <= 0.10
        } else {
            availableTfs.all { tf ->
                when (tf) {
                    Timeframe.FIVE_MIN -> abs(valid5m ?: 0.0) <= 0.10
                    Timeframe.SIXTY_MIN -> abs(valid60m ?: 0.0) <= 0.10
                    Timeframe.ONE_DAY -> abs(valid1d ?: 0.0) <= 0.10
                }
            }
        }
        val isOpposingNeutral = (valid1d == null && valid5m != null && valid60m != null && abs(valid5m + valid60m) < EPSILON)

        if (isDeadZone || isOpposingNeutral) {
            val direction = TradeDirection.NEUTRAL
            val upRounded = 50.0
            val downRounded = 50.0
            val roundedStrength = 0.0
            val roundedMovement = 0.0
            val fingerprint = buildFingerprint(input, direction, roundedStrength, availableTfs)
            return PressureDecision(
                direction = direction,
                upPercentage = upRounded,
                downPercentage = downRounded,
                upPressure = 0.0,
                downPressure = 0.0,
                pressureStrength = roundedStrength,
                movement = roundedMovement,
                confidencePenalty = confidencePenalty,
                availableTimeframes = availableTfs,
                fingerprint = fingerprint,
                explanation = if (isDeadZone) "Dead Market / No-Trade Zone (Noise threshold ±0.10%)." else "Neutral / Balanced Opposing Pressure."
            )
        }

        val pressureSum = upPressure + downPressure
        val dominance = if (pressureSum < EPSILON) {
            0.0
        } else {
            (upPressure - downPressure) / (pressureSum + EPSILON)
        }

        val direction = when {
            dominance > 0.0000001 -> TradeDirection.UP
            dominance < -0.0000001 -> TradeDirection.DOWN
            else -> TradeDirection.NEUTRAL
        }

        // 4. Percentages summing to exactly 100.0 after rounding
        val rawUpPercentage = 50.0 + 50.0 * dominance
        val upRounded = BigDecimal(rawUpPercentage).setScale(2, RoundingMode.HALF_UP).toDouble()
        val downRounded = BigDecimal(100.0 - upRounded).setScale(2, RoundingMode.HALF_UP).toDouble()

        // 5. Continuous Strength score with confidence penalty applied
        val evidence = min(1.0, pressureSum)
        val rawStrength = 100.0 * (0.70 * abs(dominance) + 0.30 * evidence)
        val penalizedStrength = rawStrength * (1.0 - confidencePenalty)
        val adjustedStrength = (penalizedStrength + input.matrixDirectionalSupport - input.matrixDirectionalConflict)
            .coerceIn(0.0, 100.0)

        val roundedStrength = BigDecimal(adjustedStrength).setScale(2, RoundingMode.HALF_UP).toDouble()
        val roundedMovement = BigDecimal(netMovement).setScale(4, RoundingMode.HALF_UP).toDouble()

        val fingerprint = buildFingerprint(input, direction, roundedStrength, availableTfs)

        val explanation = buildString {
            append("Pressure: $direction ($upRounded% UP / $downRounded% DOWN). ")
            append("Strength: $roundedStrength/100, NetMovement: $roundedMovement, ")
            append("Timeframes: ${availableTfs.joinToString { it.name }}. ")
            if (input.approximate) append("Penalty: -15% (Approximate data). ")
            if (input.matrixDirectionalSupport > 0.0) append("MatrixSupport: +${input.matrixDirectionalSupport}. ")
            if (input.matrixDirectionalConflict > 0.0) append("MatrixConflict: -${input.matrixDirectionalConflict}.")
        }.trim()

        return PressureDecision(
            direction = direction,
            upPercentage = upRounded,
            downPercentage = downRounded,
            upPressure = upPressure,
            downPressure = downPressure,
            pressureStrength = roundedStrength,
            movement = roundedMovement,
            confidencePenalty = confidencePenalty,
            availableTimeframes = availableTfs,
            fingerprint = fingerprint,
            explanation = explanation
        )
    }

    private fun computeObservedForce(
        current: Double,
        previous: Double?,
        elapsedSeconds: Double,
        scale: Double,
        velocityScale: Double
    ): Double {
        val normalizedLevel = tanh(current / scale)
        val normalizedVelocity = if (previous != null && isValidMetricValue(previous)) {
            val dt = max(elapsedSeconds, 1.0)
            val velocity = (current - previous) / dt
            tanh(velocity / velocityScale)
        } else {
            0.0
        }
        return 0.65 * normalizedLevel + 0.35 * normalizedVelocity
    }

    fun buildFingerprint(
        input: PressureInput,
        direction: TradeDirection,
        strength: Double,
        availableTfs: Set<Timeframe> = emptySet()
    ): String {
        fun fmt(v: Double?): String = if (v != null && isValidMetricValue(v)) "%.2f".format(Locale.US, v) else "none"
        val tfsStr = availableTfs.map { it.name }.sorted().joinToString(",")
        return "RMP-$ENGINE_VERSION" +
                "|dir=$direction" +
                "|str=%.2f".format(Locale.US, strength) +
                "|5m=${fmt(input.current5m)},p5m=${fmt(input.previous5m)}" +
                "|60m=${fmt(input.current60m)},p60m=${fmt(input.previous60m)}" +
                "|1d=${fmt(input.current1d)},p1d=${fmt(input.previous1d)}" +
                "|tfs=[$tfsStr]" +
                "|supp=%.2f".format(Locale.US, input.matrixDirectionalSupport) +
                "|conf=%.2f".format(Locale.US, input.matrixDirectionalConflict) +
                "|app=${input.approximate}"
    }

    /**
     * Unified authority for constructing canonical TradingAnalysis directly from ReactiveMarketPressureEngine.
     * Guarantees that UI, parser, local OCR, matrix trace, ViewModel, and auto-entry share the identical decision.
     */
    fun buildTradingAnalysis(
        val5m: Double?,
        val60m: Double?,
        val1d: Double?,
        prefix5m: String = "",
        prefix60m: String = "",
        isApprox: Boolean = false,
        is1dExplicitlyRejected: Boolean = false,
        latencyMs: Long = 0L,
        engineSource: String = "On-Device OCR",
        rawResponse: String = "",
        history: List<MetricSnapshot> = emptyList()
    ): TradingAnalysis {
        val valid5m = if (isValidMetricValue(val5m)) val5m else null
        val valid60m = if (isValidMetricValue(val60m)) val60m else null
        val valid1d = if (isValidMetricValue(val1d)) val1d else null

        if (valid5m == null || valid60m == null) {
            val missingMsg = when {
                valid5m == null && valid60m == null -> "5m এবং 60m উভয় কোর টাইমফ্রেম অনুপস্থিত (No core metrics)"
                valid5m == null -> "5m টাইমফ্রেম অনুপস্থিত (কোর ইনপুট আবশ্যক)"
                else -> "60m টাইমফ্রেম অনুপস্থিত (কোর ইনপুট আবশ্যক)"
            }
            return TradingAnalysis(
                isSuccess = false,
                isValid = false,
                errorMessage = missingMsg,
                rawResponse = rawResponse.ifEmpty { missingMsg },
                latencyMs = latencyMs,
                engineSource = "$engineSource ($ENGINE_VERSION)",
                audioEvent = AudioSignalEngine.SOUND_NONE,
                dataQuality = if (valid5m == null && valid60m == null) DataQualityState.UNAVAILABLE else DataQualityState.INCOMPLETE,
                direction = TradeDirection.NEUTRAL,
                upPercentage = 50.0,
                downPercentage = 50.0,
                calculatedPercentage = 50.0,
                strengthLevel = StrengthLevel.NORMAL,
                isNoTradeZone = true,
                change5m = valid5m?.let { String.format(Locale.US, "%s%.2f%%", prefix5m, it) } ?: "--",
                change5mValue = valid5m,
                change60m = valid60m?.let { String.format(Locale.US, "%s%.2f%%", prefix60m, it) } ?: "--",
                change60mValue = valid60m,
                change1d = valid1d?.let { String.format(Locale.US, "%.2f%%", it) } ?: "--",
                change1dValue = valid1d
            )
        }

        val prevSnapshot = history.firstOrNull()
        val prev5m = prevSnapshot?.val5m?.let { if (isValidMetricValue(it)) it else null }
        val prev60m = prevSnapshot?.val60m?.let { if (isValidMetricValue(it)) it else null }
        val prev1d = prevSnapshot?.val1d?.let { if (isValidMetricValue(it)) it else null }
        val elapsedSec = if (prevSnapshot != null) {
            max((System.currentTimeMillis() - prevSnapshot.timestamp) / 1000.0, 1.0)
        } else {
            1.0
        }

        val pressureInput = PressureInput(
            current5m = valid5m,
            previous5m = prev5m,
            current60m = valid60m,
            previous60m = prev60m,
            current1d = valid1d,
            previous1d = prev1d,
            elapsedSeconds = elapsedSec,
            approximate = isApprox,
            matrixDirectionalSupport = 0.0,
            matrixDirectionalConflict = 0.0
        )
        val initialDecision = calculate(pressureInput)

        val totalMagnitude = (valid5m?.let { abs(it) } ?: 0.0) +
                (valid60m?.let { abs(it) } ?: 0.0)
        val isDeadMarket = (initialDecision.direction == TradeDirection.NEUTRAL)
        val dataQuality = if (isApprox) DataQualityState.APPROXIMATE else DataQualityState.VERIFIED

        val rawNet = (valid5m ?: 0.0) + (valid60m ?: 0.0)
        val matrixContext = MatrixEvaluationContext(
            val5m = valid5m ?: 0.0,
            val60m = valid60m ?: 0.0,
            val1d = valid1d,
            rawNetSum = rawNet,
            netSum = rawNet,
            normalizedMovement = initialDecision.movement,
            totalMagnitude = totalMagnitude,
            dataQuality = dataQuality,
            isNoTradeZone = isDeadMarket,
            history = history,
            is5mPresent = valid5m != null,
            is60mPresent = valid60m != null,
            is1dPresent = valid1d != null
        )
        val matrixResult = MatrixEvaluationEngine.evaluate(matrixContext)

        val activeDirectional = matrixResult.matchedMatrices.filter {
            !it.warningOnly && (it.direction == TradeDirection.UP || it.direction == TradeDirection.DOWN)
        }
        val matrixSupport = when (initialDecision.direction) {
            TradeDirection.UP -> min(15.0, activeDirectional.count { it.direction == TradeDirection.UP } * 2.0)
            TradeDirection.DOWN -> min(15.0, activeDirectional.count { it.direction == TradeDirection.DOWN } * 2.0)
            else -> 0.0
        }
        val matrixConflict = when (initialDecision.direction) {
            TradeDirection.UP -> min(15.0, activeDirectional.count { it.direction == TradeDirection.DOWN } * 3.0)
            TradeDirection.DOWN -> min(15.0, activeDirectional.count { it.direction == TradeDirection.UP } * 3.0)
            else -> 0.0
        }

        val finalDecision = if (matrixSupport > 0.0 || matrixConflict > 0.0) {
            calculate(
                pressureInput.copy(
                    matrixDirectionalSupport = matrixSupport,
                    matrixDirectionalConflict = matrixConflict
                )
            )
        } else {
            initialDecision
        }

        val behavior = MovementClassificationEngine.classify(
            val5m = valid5m,
            val60m = valid60m,
            val1d = valid1d,
            history = history,
            isValid = true,
            isApproximate = isApprox,
            isDeadMarket = isDeadMarket,
            isNoTradeZone = isDeadMarket
        )

        val signalType: SignalType = when {
            behavior.code == "TOP_FAKEOUT_RISK" || behavior.code == "TOP_FAKEOUT_SELL" -> SignalType.TOP_FAKEOUT_SELL
            behavior.code == "BOTTOM_FAKEOUT_RISK" || behavior.code == "BOTTOM_FAKEOUT_BUY" -> SignalType.BOTTOM_FAKEOUT_BUY
            behavior.code.contains("MOMENTUM") -> SignalType.MOMENTUM_LOSS
            finalDecision.direction == TradeDirection.UP || finalDecision.direction == TradeDirection.DOWN -> SignalType.STANDARD_SIGNAL
            else -> SignalType.NONE
        }

        val formatted5m = if (valid5m != null) prefix5m + TradingOutputParser.formatWithSign(valid5m) else "--"
        val formatted60m = if (valid60m != null) prefix60m + TradingOutputParser.formatWithSign(valid60m) else "--"
        val formatted1d = valid1d?.let { TradingOutputParser.formatWithSign(it) } ?: "--"
        val formattedNet = TradingOutputParser.formatWithSign(finalDecision.movement)
        val strength = mapStrengthLevel(finalDecision.pressureStrength)

        val audioEvent = if (isApprox || isDeadMarket) AudioSignalEngine.SOUND_NONE else when (finalDecision.direction) {
            TradeDirection.UP -> AudioSignalEngine.SOUND_UP_ALERT
            TradeDirection.DOWN -> AudioSignalEngine.SOUND_DOWN_ALERT
            else -> AudioSignalEngine.SOUND_NONE
        }

        val kineticResult = MicroKineticVectorEngine.calculate(
            val5m = valid5m,
            val60m = valid60m,
            val1d = valid1d,
            history = history,
            isApproximate = isApprox
        )

        val canonicalDecision = CanonicalDecisionEngine.evaluate(
            val5m = valid5m,
            val60m = valid60m,
            val1d = valid1d,
            history = history,
            isApproximate = isApprox,
            rmpDecision = finalDecision,
            matrixResult = matrixResult,
            kineticResult = kineticResult,
            behavior = behavior
        )

        val canonicalAnalysis = if (valid5m != null && valid60m != null) {
            TradingOutputParser.calculateCanonicalAnalysis(
                val5m = valid5m,
                val60m = valid60m,
                val1d = valid1d,
                isConflicting = matrixResult.isConflicting
            )
        } else {
            TradingOutputParser.createInvalidCanonicalResult()
        }

        val isShortTermStrengthMode = (CanonicalDecisionEngine.currentDecisionMode == com.example.data.models.DecisionMode.SHORT_TERM_STRENGTH)
        val isThreeTimeframePressureMode = (CanonicalDecisionEngine.currentDecisionMode == com.example.data.models.DecisionMode.THREE_TIMEFRAME_PRESSURE)
        val isDirectMode = isShortTermStrengthMode || isThreeTimeframePressureMode
        val isHardConflictOrHold = if (isDirectMode) {
            false
        } else {
            matrixResult.isConflicting || canonicalDecision.conflict
        }

        val resolvedNetSumValue = canonicalAnalysis.netSum
        val resolvedFormattedNet = if (canonicalAnalysis.isValid) {
            TradingOutputParser.formatWithSign(resolvedNetSumValue)
        } else {
            formattedNet
        }
        val resolvedTotalMagnitude = canonicalAnalysis.totalMagnitude
        val resolvedSensitivityRatio = canonicalAnalysis.sensitivityRatio
        val resolvedAlignmentScore = canonicalAnalysis.sensitivityRatio
        val resolvedMagnitudeScore = canonicalAnalysis.magnitudeScore
        val resolvedEvidenceScore = canonicalAnalysis.evidenceScore
        val resolvedStrengthLevel = canonicalAnalysis.strengthLevel

        // Direction and percentages resolution
        val resolvedDirection = if (isHardConflictOrHold || (!isDirectMode && isDeadMarket)) {
            TradeDirection.NEUTRAL
        } else {
            canonicalDecision.direction
        }

        val resolvedUpPercentage = if (isHardConflictOrHold || resolvedDirection == TradeDirection.NEUTRAL) {
            50.0
        } else {
            canonicalDecision.upPercentage
        }

        val resolvedDownPercentage = if (isHardConflictOrHold || resolvedDirection == TradeDirection.NEUTRAL) {
            50.0
        } else {
            canonicalDecision.downPercentage
        }

        val baseIsNoTrade = if (isDirectMode) {
            canonicalDecision.noTrade || resolvedDirection == TradeDirection.NEUTRAL
        } else {
            isDeadMarket || isHardConflictOrHold || resolvedDirection == TradeDirection.NEUTRAL
        }

        val matched206 = Directional206MatrixEngine.evaluate(valid5m, valid60m, null, history)
        val finalResolvedDirection = if (isDeadMarket || isHardConflictOrHold || resolvedDirection == TradeDirection.NEUTRAL) {
            TradeDirection.NEUTRAL
        } else {
            matched206?.direction ?: resolvedDirection
        }
        val finalIsNoTrade = if (isDeadMarket || isHardConflictOrHold || finalResolvedDirection == TradeDirection.NEUTRAL) {
            true
        } else {
            baseIsNoTrade
        }

        val resolvedAudioEvent = if (isApprox || finalIsNoTrade) {
            AudioSignalEngine.SOUND_NONE
        } else when (finalResolvedDirection) {
            TradeDirection.UP -> AudioSignalEngine.SOUND_UP_ALERT
            TradeDirection.DOWN -> AudioSignalEngine.SOUND_DOWN_ALERT
            else -> AudioSignalEngine.SOUND_NONE
        }
        val resolvedDailyContext = "NEUTRAL"

        val matchedId = matched206?.id ?: matrixResult.primaryMatrix?.id ?: "NONE"
        val matchedCode = matched206?.outputCode ?: matrixResult.primaryMatrix?.outputCode ?: "NO_MATCH"
        val dirStr = when (finalResolvedDirection) {
            TradeDirection.UP -> "UP"
            TradeDirection.DOWN -> "DOWN"
            else -> "HOLD"
        }

        val summaryText = buildString {
            appendLine("গাণিতিক হিসাব:")
            appendLine("৫ মিনিটের পরিবর্তন: $formatted5m")
            appendLine("৬০ মিনিটের পরিবর্তন: $formatted60m")
            appendLine()
            appendLine("ম্যাট্রিক্স স্ট্যাটাস:")
            appendLine("ম্যাচড ম্যাট্রিক্স ID: $matchedId")
            appendLine("আউটপুট কোড: $matchedCode")
            appendLine()
            appendLine("ফলাফল:")
            append("দিক: $dirStr")
        }

        val finalUpPercentage = resolvedUpPercentage
        val finalDownPercentage = resolvedDownPercentage

        return TradingAnalysis(
            change5m = formatted5m,
            change5mValue = valid5m,
            change60m = formatted60m,
            change60mValue = valid60m,
            change1d = formatted1d,
            change1dValue = valid1d,
            netSum = resolvedFormattedNet,
            netSumValue = resolvedNetSumValue,
            totalMagnitude = resolvedTotalMagnitude,
            sensitivityRatio = resolvedSensitivityRatio,
            alignmentScore = resolvedAlignmentScore,
            magnitudeScore = resolvedMagnitudeScore,
            evidenceScore = resolvedEvidenceScore,
            strengthLevel = resolvedStrengthLevel,
            direction = finalResolvedDirection,
            calculatedPercentage = if (finalResolvedDirection == TradeDirection.DOWN) finalDownPercentage else finalUpPercentage,
            upPercentage = finalUpPercentage,
            downPercentage = finalDownPercentage,
            audioEvent = resolvedAudioEvent,
            rawResponse = rawResponse.ifEmpty { summaryText },
            timestamp = System.currentTimeMillis(),
            isSuccess = true,
            isValid = true,
            errorMessage = null,
            latencyMs = latencyMs,
            engineSource = "$engineSource ($ENGINE_VERSION)",
            isNoTradeZone = finalIsNoTrade,
            dataQuality = dataQuality,
            isApproximate = isApprox,
            signalType = signalType,
            behaviorCode = behavior.code,
            behaviorTitle = behavior.title,
            behaviorDescription = behavior.subtitle,
            behaviorTags = behavior.tags,
            isWarningOnly = if (isShortTermStrengthMode) canonicalDecision.warningOnly else behavior.isWarningOnly,
            dailyContext = resolvedDailyContext,
            is1dExplicitlyRejected = is1dExplicitlyRejected,
            confirmationStage = behavior.confirmationStage,
            nextMovementBias = behavior.nextMovementBias,
            mtfCalculatedPercentage = if (finalDecision.direction == TradeDirection.DOWN) finalDecision.downPercentage else finalDecision.upPercentage,
            mtfUpPercentage = finalDecision.upPercentage,
            mtfDownPercentage = finalDecision.downPercentage,
            totalMatricesCount = matrixResult.totalMatricesCount,
            evaluatedMatricesCount = matrixResult.evaluatedMatricesCount,
            skippedMatricesCount = matrixResult.skippedMatricesCount,
            failedMatricesCount = matrixResult.failedMatricesCount,
            failureTraces = matrixResult.failureTraces,
            matchedMatrixIds = ((if (matched206 != null) listOf(matched206.id) else emptyList()) + matrixResult.matchedMatrices.map { it.id }).distinct(),
            primaryMatrixId = matched206?.id ?: matrixResult.primaryMatrix?.id ?: canonicalDecision.primaryMatrixId,
            primaryMatrixTitle = matched206?.title ?: matrixResult.primaryMatrix?.title ?: canonicalDecision.primaryMatrixTitle,
            primaryMatrixDescription = matched206?.conditionDescription ?: matrixResult.primaryMatrix?.description,
            primaryMatrixRiskLevel = matrixResult.primaryMatrix?.riskLevel?.name ?: (if (matched206 != null) "HIGH_CONFIDENCE" else "NORMAL"),
            pressureFingerprint = canonicalDecision.fingerprint,
            decisionTrace = matrixResult.decisionTrace + listOf(
                "Engine: ${finalDecision.explanation}",
                "Fingerprint: ${finalDecision.fingerprint}",
                "Kinetic: v=${String.format(Locale.US, "%.4f", kineticResult.microVelocity)}%/s, E_k=${String.format(Locale.US, "%.4f", kineticResult.kineticBaseEnergy)}, Footprint=${kineticResult.footprint.code}"
            ),
            rejectedMatrixNotes = matrixResult.rejectedMatrixNotes,
            kineticBaseEnergy = kineticResult.kineticBaseEnergy,
            microVelocity = kineticResult.microVelocity,
            netKineticForce = kineticResult.netKineticForce,
            kineticUpWeight = kineticResult.kineticUpWeight,
            kineticDownWeight = kineticResult.kineticDownWeight,
            forensicPatternCode = kineticResult.footprint.code,
            forensicPatternTitle = kineticResult.footprint.title,
            forensicDiagnosis = kineticResult.footprint.diagnosis,
            isBrakeInertiaPullback = kineticResult.footprint.isBrakeInertiaPullback,
            canonicalDecision = if (matched206 != null) canonicalDecision.copy(
                primaryMatrixId = matched206.id,
                primaryMatrixTitle = matched206.title
            ) else canonicalDecision,
            matrixRecords = matrixResult.matrixRecords
        )
    }
}
