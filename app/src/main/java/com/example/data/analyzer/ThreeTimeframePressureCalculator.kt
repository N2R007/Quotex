package com.example.data.analyzer

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.max
import kotlin.math.pow

/**
 * Source quality of the percentage inputs.
 */
enum class SourceQuality {
    VERIFIED,
    APPROXIMATE,
    AMBIGUOUS,
    INCOMPLETE
}

/**
 * Three-timeframe direction output.
 */
enum class PressureDirection {
    UP,
    DOWN,
    NO_SIGNAL
}

/**
 * Pressure band classification using non-overlapping ranges:
 * - net >= +80.0 -> VERY_STRONG_UP
 * - +60.0 <= net < +80.0 -> STRONG_UP
 * - +20.0 <= net < +60.0 -> MODERATE_UP
 * - -20.0 < net < +20.0 -> MIXED_OR_WEAK
 * - -60.0 < net <= -20.0 -> MODERATE_DOWN
 * - -80.0 < net <= -60.0 -> STRONG_DOWN
 * - net <= -80.0 -> VERY_STRONG_DOWN
 */
enum class PressureBand(
    val rangeText: String,
    val bengaliMeaning: String,
    val bengaliLabel: String = bengaliMeaning
) {
    VERY_STRONG_UP(
        rangeText = "+80% থেকে +100%",
        bengaliMeaning = "খুব শক্তিশালী ঊর্ধ্বমুখী চাপ—UP-এর সম্ভাব্য শক্তি খুব বেশি"
    ),
    STRONG_UP(
        rangeText = "+60% থেকে +80%",
        bengaliMeaning = "শক্তিশালী ঊর্ধ্বমুখী চাপ—UP signal জোরালো"
    ),
    MODERATE_UP(
        rangeText = "+20% থেকে +60%",
        bengaliMeaning = "মাঝারি ঊর্ধ্বমুখী চাপ—UP দিকে ঝোঁক আছে, তবে শক্তি সীমিত"
    ),
    MIXED_OR_WEAK(
        rangeText = "-20% থেকে +20%",
        bengaliMeaning = "চাপ দুর্বল বা মিশ্র—UP/DOWN পরিষ্কার নয়"
    ),
    MODERATE_DOWN(
        rangeText = "-60% থেকে -20%",
        bengaliMeaning = "মাঝারি নিম্নমুখী চাপ—DOWN দিকে ঝোঁক আছে"
    ),
    STRONG_DOWN(
        rangeText = "-80% থেকে -60%",
        bengaliMeaning = "শক্তিশালী নিম্নমুখী চাপ—DOWN signal জোরালো"
    ),
    VERY_STRONG_DOWN(
        rangeText = "-100% থেকে -80%",
        bengaliMeaning = "খুব শক্তিশালী নিম্নমুখী চাপ—DOWN-এর শক্তি খুব বেশি"
    )
}

/**
 * Immutable ResultSignature representing the canonical normalized triple and source quality.
 * Two signatures are equal if their canonical 2-decimal normalized values and quality match.
 */
data class ResultSignature(
    val normalizedP5m: Double?,
    val normalizedP60m: Double?,
    val normalizedP1d: Double?,
    val sourceQuality: SourceQuality,
    val isComplete: Boolean
) {
    val keyString: String = if (!isComplete) {
        "INCOMPLETE_${sourceQuality.name}"
    } else {
        "p5m=${String.format(Locale.US, "%.2f", normalizedP5m)};" +
        "p60m=${String.format(Locale.US, "%.2f", normalizedP60m)};" +
        "p1d=${String.format(Locale.US, "%.2f", normalizedP1d)};" +
        "sq=${sourceQuality.name}"
    }

    companion object {
        fun create(
            p5m: Double?,
            p60m: Double?,
            p1d: Double?,
            sourceQuality: SourceQuality = SourceQuality.VERIFIED
        ): ResultSignature {
            val n5 = ThreeTimeframePressureCalculator.normalizeCanonicalPercent(p5m)
            val n60 = ThreeTimeframePressureCalculator.normalizeCanonicalPercent(p60m)
            val n1d = ThreeTimeframePressureCalculator.normalizeCanonicalPercent(p1d)
            val isComplete = n5 != null && n60 != null && n1d != null && sourceQuality == SourceQuality.VERIFIED
            return ResultSignature(
                normalizedP5m = n5,
                normalizedP60m = n60,
                normalizedP1d = n1d,
                sourceQuality = sourceQuality,
                isComplete = isComplete
            )
        }
    }
}

/**
 * Immutable Result of Pure Three-Timeframe Pressure Calculation.
 */
data class ThreeTimeframePressureResult(
    val direction: PressureDirection,
    val netPressurePercent: Double,
    val upEnergy: Double,
    val downEnergy: Double,
    val upSharePercent: Double,
    val downSharePercent: Double,
    val totalEnergy: Double,
    val pressureBand: PressureBand,
    val timestamp: Long,
    val sourceQuality: SourceQuality,
    val p5m: Double?,
    val p60m: Double?,
    val p1d: Double?,
    val formulaExplanation: String,
    val isDataIncomplete: Boolean = false,
    val signature: ResultSignature = ResultSignature.create(p5m, p60m, p1d, sourceQuality)
)

/**
 * Pure, unit-testable Three-Timeframe Pressure Calculator.
 *
 * Implements deterministic directional pressure calculation across 5m, 60m, and 1D.
 * Formula:
 * upEnergy = (max(p5m, 0)^2 + max(p60m, 0)^2 + max(p1d, 0)^2) / 3.0
 * downEnergy = (max(-p5m, 0)^2 + max(-p60m, 0)^2 + max(-p1d, 0)^2) / 3.0
 * totalEnergy = upEnergy + downEnergy
 * netPressurePercent = ((upEnergy - downEnergy) / totalEnergy) * 100.0
 */
object ThreeTimeframePressureCalculator {

    const val CANONICAL_DECIMAL_PLACES = 2

    /**
     * Normalizes a raw timeframe percentage to canonical precision (2 decimal places, matching UI display).
     * Eliminates OCR micro-jitter (e.g. 0.011 vs 0.014 both map to 0.01).
     * Guards against null, NaN, infinite, and -0.0.
     */
    fun normalizeCanonicalPercent(value: Double?): Double? {
        if (value == null || value.isNaN() || value.isInfinite()) return null
        val bd = BigDecimal.valueOf(value).setScale(CANONICAL_DECIMAL_PLACES, RoundingMode.HALF_UP)
        val res = bd.toDouble()
        return if (res == -0.0) 0.0 else res
    }

    fun calculate(
        p5m: Double?,
        p60m: Double?,
        p1d: Double?,
        timestamp: Long = System.currentTimeMillis(),
        sourceQuality: SourceQuality = SourceQuality.VERIFIED
    ): ThreeTimeframePressureResult {
        val normP5m = normalizeCanonicalPercent(p5m)
        val normP60m = normalizeCanonicalPercent(p60m)
        val normP1d = normalizeCanonicalPercent(p1d)

        val signature = ResultSignature.create(normP5m, normP60m, normP1d, sourceQuality)

        // If any timeframe is null, NaN, infinite, approximate, or ambiguous, result is DATA_INCOMPLETE/NO_SIGNAL
        if (normP5m == null || normP60m == null || normP1d == null ||
            sourceQuality != SourceQuality.VERIFIED
        ) {
            val effQuality = if (sourceQuality != SourceQuality.VERIFIED) sourceQuality else SourceQuality.INCOMPLETE
            return ThreeTimeframePressureResult(
                direction = PressureDirection.NO_SIGNAL,
                netPressurePercent = 0.0,
                upEnergy = 0.0,
                downEnergy = 0.0,
                upSharePercent = 0.0,
                downSharePercent = 0.0,
                totalEnergy = 0.0,
                pressureBand = PressureBand.MIXED_OR_WEAK,
                timestamp = timestamp,
                sourceQuality = effQuality,
                p5m = normP5m,
                p60m = normP60m,
                p1d = normP1d,
                formulaExplanation = "তিনটি টাইমফ্রেমের সম্পূর্ণ ও নিশ্চিত ডেটা অনুপস্থিত (DATA_INCOMPLETE)",
                isDataIncomplete = true,
                signature = signature
            )
        }

        // Canonical calculation using normalized inputs:
        // upEnergy = (max(p5m, 0.0)^2 + max(p60m, 0.0)^2 + max(p1d, 0.0)^2) / 3.0
        val u5 = max(normP5m, 0.0).pow(2.0)
        val u60 = max(normP60m, 0.0).pow(2.0)
        val u1d = max(normP1d, 0.0).pow(2.0)
        val upEnergy = (u5 + u60 + u1d) / 3.0

        // downEnergy = (max(-p5m, 0.0)^2 + max(-p60m, 0.0)^2 + max(-p1d, 0.0)^2) / 3.0
        val d5 = max(-normP5m, 0.0).pow(2.0)
        val d60 = max(-normP60m, 0.0).pow(2.0)
        val d1d = max(-normP1d, 0.0).pow(2.0)
        val downEnergy = (d5 + d60 + d1d) / 3.0

        val totalEnergy = upEnergy + downEnergy

        val (netPressurePercent, upSharePercent, downSharePercent, direction) = if (totalEnergy == 0.0) {
            listOf(0.0, 0.0, 0.0, PressureDirection.NO_SIGNAL)
        } else {
            val net = ((upEnergy - downEnergy) / totalEnergy) * 100.0
            val upShare = (upEnergy / totalEnergy) * 100.0
            // Complete invariant: UP Share + DOWN Share = 100.00%
            val downShare = 100.0 - upShare
            val dir = when {
                net > 0.0 -> PressureDirection.UP
                net < 0.0 -> PressureDirection.DOWN
                else -> PressureDirection.NO_SIGNAL
            }
            listOf(net, upShare, downShare, dir)
        }

        val netVal = netPressurePercent as Double
        val upShareVal = upSharePercent as Double
        val downShareVal = downSharePercent as Double
        val dirVal = direction as PressureDirection

        // Pressure Band classification using non-overlapping rules:
        // net >= +80.0 -> VERY_STRONG_UP
        // +60.0 <= net < +80.0 -> STRONG_UP
        // +20.0 <= net < +60.0 -> MODERATE_UP
        // -20.0 < net < +20.0 -> MIXED_OR_WEAK
        // -60.0 < net <= -20.0 -> MODERATE_DOWN
        // -80.0 < net <= -60.0 -> STRONG_DOWN
        // net <= -80.0 -> VERY_STRONG_DOWN
        val band = classifyPressureBand(netVal)

        val explanation = "Up Energy: ${"%.6f".format(Locale.US, upEnergy)}, " +
                "Down Energy: ${"%.6f".format(Locale.US, downEnergy)}, " +
                "Net Directional Pressure: ${"%.2f".format(Locale.US, netVal)}%"

        return ThreeTimeframePressureResult(
            direction = dirVal,
            netPressurePercent = netVal,
            upEnergy = upEnergy,
            downEnergy = downEnergy,
            upSharePercent = upShareVal,
            downSharePercent = downShareVal,
            totalEnergy = totalEnergy,
            pressureBand = band,
            timestamp = timestamp,
            sourceQuality = sourceQuality,
            p5m = normP5m,
            p60m = normP60m,
            p1d = normP1d,
            formulaExplanation = explanation,
            isDataIncomplete = false,
            signature = signature
        )
    }

    /**
     * Non-overlapping boundary classification for pressure band.
     */
    fun classifyPressureBand(net: Double): PressureBand {
        return when {
            net >= 80.0 -> PressureBand.VERY_STRONG_UP
            net >= 60.0 && net < 80.0 -> PressureBand.STRONG_UP
            net >= 20.0 && net < 60.0 -> PressureBand.MODERATE_UP
            net > -20.0 && net < 20.0 -> PressureBand.MIXED_OR_WEAK
            net > -60.0 && net <= -20.0 -> PressureBand.MODERATE_DOWN
            net > -80.0 && net <= -60.0 -> PressureBand.STRONG_DOWN
            net <= -80.0 -> PressureBand.VERY_STRONG_DOWN
            else -> PressureBand.MIXED_OR_WEAK
        }
    }

    /**
     * Builds an immutable trigger identity key for the Three-Timeframe Result Section.
     * Bound strictly to ResultSignature and Direction.
     * Returns null if the result is incomplete or NO_SIGNAL.
     */
    fun buildAudioTriggerKey(result: ThreeTimeframePressureResult): String? {
        if (result.isDataIncomplete || !result.signature.isComplete || result.direction == PressureDirection.NO_SIGNAL) {
            return null
        }
        return "${result.signature.keyString};dir=${result.direction.name}"
    }

    /**
     * Deterministic sound mapping for Three-Timeframe Pressure Result:
     * - PressureDirection.UP -> AudioSignalEngine.SOUND_UP_ALERT only
     * - PressureDirection.DOWN -> AudioSignalEngine.SOUND_DOWN_ALERT only
     * - PressureDirection.NO_SIGNAL / incomplete -> AudioSignalEngine.SOUND_NONE
     */
    fun getDeterministicAudioEvent(direction: PressureDirection, isDataIncomplete: Boolean = false): String {
        if (isDataIncomplete) return com.example.audio.AudioSignalEngine.SOUND_NONE
        return when (direction) {
            PressureDirection.UP -> com.example.audio.AudioSignalEngine.SOUND_UP_ALERT
            PressureDirection.DOWN -> com.example.audio.AudioSignalEngine.SOUND_DOWN_ALERT
            PressureDirection.NO_SIGNAL -> com.example.audio.AudioSignalEngine.SOUND_NONE
        }
    }
}
