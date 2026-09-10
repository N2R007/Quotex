package com.example.data.analyzer

import com.example.audio.AudioSignalEngine
import com.example.data.models.StrengthLevel
import com.example.data.models.TradeDirection
import com.example.data.models.TradingAnalysis
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object TradingOutputParser {

    private fun logDebug(message: String) {
        try {
            android.util.Log.d("TradingOutputParser", message)
        } catch (_: Throwable) {
            println("[TradingOutputParser] $message")
        }
    }

    private fun logWarn(message: String) {
        try {
            android.util.Log.w("TradingOutputParser", message)
        } catch (_: Throwable) {
            println("[TradingOutputParser WARN] $message")
        }
    }

    const val MAX_VALID_PERCENTAGE = 500.0 // Reject percentage changes exceeding ±500% as OCR noise/outliers
    const val EPSILON = 1e-9 // Floating-point neutral noise epsilon
    const val NO_TRADE_THRESHOLD = 0.10 // 0.10% dead market / no-trade zone boundary
    const val ALTERNATION_THRESHOLD = 0.05 // Meaningful net sign flip threshold: values > ALTERNATION_THRESHOLD to < -ALTERNATION_THRESHOLD (and vice versa) are flips, while values inside [-0.05, 0.05] are noise
    const val MAX_FINITE_PNL = 1e15 // Deterministic upper boundary cap for finite PnL display and safety

    /**
     * Shared finite and range validation policy for all trading metrics.
     * Rejects NaN, Infinity, and values exceeding MAX_VALID_PERCENTAGE (500.0).
     */
    fun isValidMetricValue(value: Double?): Boolean {
        return value != null && !value.isNaN() && !value.isInfinite() && abs(value) <= MAX_VALID_PERCENTAGE
    }

    fun parse(rawText: String, latencyMs: Long, history: List<com.example.data.models.MetricSnapshot> = emptyList()): TradingAnalysis {
        val cleanText = rawText.trim()

        if (cleanText.isEmpty()) {
            logWarn("Empty rawText passed to parser")
            return TradingAnalysis(
                isSuccess = false,
                isValid = false,
                errorMessage = "No data received (Empty response)",
                rawResponse = cleanText,
                latencyMs = latencyMs,
                audioEvent = AudioSignalEngine.SOUND_NONE,
                dataQuality = com.example.data.models.DataQualityState.UNAVAILABLE,
                direction = TradeDirection.NEUTRAL,
                isNoTradeZone = false
            )
        }

        // Check for explicit error responses
        if (cleanText.contains("ত্রুটি:", ignoreCase = true) || 
            cleanText.contains("isSuccess: false", ignoreCase = true) ||
            cleanText.contains("error", ignoreCase = true) && !cleanText.contains("গাণিতিক হিসাব")
        ) {
            return TradingAnalysis(
                isSuccess = false,
                isValid = false,
                errorMessage = "Error: Metrics could not be detected accurately.",
                rawResponse = cleanText,
                latencyMs = latencyMs,
                audioEvent = AudioSignalEngine.SOUND_NONE,
                dataQuality = com.example.data.models.DataQualityState.UNAVAILABLE,
                direction = TradeDirection.NEUTRAL,
                isNoTradeZone = false
            )
        }

        // Convert Bengali digits to Western digits and normalize unicode minus/dash characters
        val normalizedText = normalizeBengaliAndDashes(cleanText)
        logDebug("Parsing normalized text:\n$normalizedText")

        // TARGET A (5 min change): Extract value strictly from directly underneath or beside "5 min change"
        // Ignore lines belonging to "Traders' Sentiment", "Profit", "1 day change", "24h change", "1 month change", "1 year change", "ytd", etc.
        val filteredLines = normalizedText.lines().filter { line ->
            val lower = line.lowercase(Locale.US)
            !lower.contains("traders' sentiment") && 
            !lower.contains("sentiment") && 
            !lower.contains("profit") && 
            !lower.contains("1 day") &&
            !lower.contains("1day") &&
            !lower.contains("1 d ") &&
            !lower.contains("1d change") &&
            !lower.contains("24h") &&
            !lower.contains("24 hour") &&
            !lower.contains("১ দিন") &&
            !lower.contains("১ দিনের পরিবর্তন") &&
            !lower.contains("২৪ ঘণ্টা") &&
            !lower.contains("month") &&
            !lower.contains("year") &&
            !lower.contains("ytd") &&
            !lower.contains("মাস") &&
            !lower.contains("বছর")
        }.joinToString("\n")

        // Match regex supporting optional approximation marker (~, ≈), optional sign, and digits
        val change5mMatch = Regex(
            """(?:[৫5]\s*মিনিট[^\n\d]*|5\s*min(?:ute)?s?[^\n\d]*|5\s*m(?:in)?[^\n\d]*|5\s*ch(?:an)?g(?:e)?[^\n\d]*)[*:\s=]+([~≈]?)\s*([+\-]?)\s*(\d+(?:\.\d+)?)\s*%?""",
            RegexOption.IGNORE_CASE
        ).find(filteredLines)

        // TARGET B (60 min change / 1 hour change): Extract value strictly from 60 min or 1h line/header
        val change60mMatch = Regex(
            """(?:(?:৬০|60)\s*মিনিট[^\n\d]*|(?:১|1)\s*ঘণ্টা[^\n\d]*|(?:১|1)\s*ঘন্টা[^\n\d]*|60\s*min(?:ute)?s?[^\n\d]*|60\s*ch(?:an)?g(?:e)?[^\n\d]*|1\s*hour[^\n\d]*|1\s*h(?:our)?[^\n\d]*|1\s*hr[^\n\d]*|60\s*m[^\n\d]*)[*:\s=]+([~≈]?)\s*([+\-]?)\s*(\d+(?:\.\d+)?)\s*%?""",
            RegexOption.IGNORE_CASE
        ).find(filteredLines)

        var raw5mPair: Triple<String, String, String>? = change5mMatch?.let {
            Triple(it.groupValues[1], it.groupValues[2], it.groupValues[3])
        }
        var raw60mPair: Triple<String, String, String>? = change60mMatch?.let {
            Triple(it.groupValues[1], it.groupValues[2], it.groupValues[3])
        }

        // TARGET C (1 day change / 24h change):
        val change1dMatch = Regex(
            """(?:(?:১|1)\s*দিনের\s*পরিবর্তন[^\n\d]*|(?:১|1)\s*দিন[^\n\d]*|1\s*day\s*change[^\n\d]*|1\s*day[^\n\d]*|1d\s*change[^\n\d]*|1d[^\n\d]*)[*:\s=]+([~≈]?)\s*([+\-—–−]?)\s*(\d+(?:\.\d+)?)\s*%?""",
            RegexOption.IGNORE_CASE
        ).find(normalizedText)

        var is1dExplicitlyRejected = false
        var val1d: Double? = null

        var raw1dTriple: Triple<String, String, String>? = change1dMatch?.let {
            Triple(it.groupValues[1], it.groupValues[2], it.groupValues[3])
        }

        if (raw1dTriple == null) {
            for (line in normalizedText.lines()) {
                val lowerLine = line.lowercase(Locale.US)
                if (lowerLine.contains("1 day") || lowerLine.contains("1day") || lowerLine.contains("1d") || 
                    lowerLine.contains("১ দিন") || lowerLine.contains("১দিনের")) {
                    val tokenMatch = Regex("""([~≈]?)\s*([+\-—–−]?)\s*(\d+(?:\.\d+)?)\s*%?""").find(line.substringAfter("day").let { if (it.isEmpty()) line else it })
                        ?: Regex("""([~≈]?)\s*([+\-—–−]?)\s*(\d+(?:\.\d+)?)\s*%?""").find(line)
                    if (tokenMatch != null && tokenMatch.groupValues[3].isNotEmpty()) {
                        raw1dTriple = Triple(tokenMatch.groupValues[1], tokenMatch.groupValues[2], tokenMatch.groupValues[3])
                        break
                    }
                }
            }
        }

        if (raw1dTriple != null) {
            val approx = raw1dTriple.first
            val sign = raw1dTriple.second
            val num = raw1dTriple.third.toDoubleOrNull()

            if (approx.isNotEmpty()) {
                // Approximate 1D (~ or ≈) is explicitly rejected
                is1dExplicitlyRejected = true
                val1d = null
            } else if (sign.isEmpty() && num != null && num != 0.0) {
                // Non-zero unsigned 1D is explicitly rejected
                is1dExplicitlyRejected = true
                val1d = null
            } else if (num != null) {
                val isMinus = sign == "-" || sign == "—" || sign == "–" || sign == "−"
                val signedNum = if (isMinus) -abs(num) else abs(num)
                if (abs(signedNum) > 500.0) {
                    // Out-of-range (>500%) is explicitly rejected
                    is1dExplicitlyRejected = true
                    val1d = null
                } else {
                    is1dExplicitlyRejected = false
                    val1d = signedNum
                }
            }
        }

        // Fallback strategy 1: line-by-line inspection if any value missed
        if (raw5mPair == null || raw60mPair == null) {
            for (line in filteredLines.lines()) {
                val lowerLine = line.lowercase(Locale.US)
                val tokenMatch = Regex("""([~≈]?)\s*([+\-—–−]?)\s*(\d+(?:\.\d+)?)\s*%?""").find(line)
                if (tokenMatch != null) {
                    val approx = tokenMatch.groupValues[1]
                    val sign = tokenMatch.groupValues[2]
                    val digits = tokenMatch.groupValues[3]
                    if (raw5mPair == null && (lowerLine.contains("5") || lowerLine.contains("৫")) && 
                        (lowerLine.contains("min") || lowerLine.contains("মিনিট") || lowerLine.contains("5m") || lowerLine.contains("5chg") || lowerLine.contains("5 chg"))) {
                        raw5mPair = Triple(approx, sign, digits)
                    } else if (raw60mPair == null && 
                        (lowerLine.contains("60") || lowerLine.contains("৬০") || lowerLine.contains("1h") || 
                         lowerLine.contains("1 h") || lowerLine.contains("1 hour") || lowerLine.contains("ঘণ্টা") || 
                         lowerLine.contains("ঘন্টা") || lowerLine.contains("60m") || lowerLine.contains("60chg") || lowerLine.contains("60 chg"))) {
                        raw60mPair = Triple(approx, sign, digits)
                    }
                }
            }
        }

        val isApprox5m = raw5mPair?.first?.isNotEmpty() == true
        val sign5m = raw5mPair?.second ?: ""
        val num5m = raw5mPair?.third?.toDoubleOrNull()

        val isApprox60m = raw60mPair?.first?.isNotEmpty() == true
        val sign60m = raw60mPair?.second ?: ""
        val num60m = raw60mPair?.third?.toDoubleOrNull()

        // If explicit sign is missing on non-zero magnitude, mark AMBIGUOUS (never assume positive or negative)
        val isAmbiguous5m = raw5mPair != null && ((sign5m.isEmpty() && num5m != null && num5m != 0.0) || (isApprox5m && sign5m.isEmpty()))
        val isAmbiguous60m = raw60mPair != null && ((sign60m.isEmpty() && num60m != null && num60m != 0.0) || (isApprox60m && sign60m.isEmpty()))

        if (isAmbiguous5m || isAmbiguous60m) {
            logWarn("Ambiguous sign detected: 5m sign='$sign5m' num=$num5m approx=$isApprox5m, 60m sign='$sign60m' num=$num60m approx=$isApprox60m")
            return TradingAnalysis(
                isSuccess = false,
                isValid = false,
                errorMessage = "Ambiguous Sign or Approximation without explicit sign",
                rawResponse = cleanText,
                latencyMs = latencyMs,
                audioEvent = AudioSignalEngine.SOUND_NONE,
                dataQuality = com.example.data.models.DataQualityState.AMBIGUOUS,
                direction = TradeDirection.NEUTRAL,
                isNoTradeZone = false
            )
        }

        val parsed5m: Double? = if (num5m != null && !isAmbiguous5m) {
            val isMinus5m = sign5m == "-" || sign5m == "—" || sign5m == "–" || sign5m == "−"
            val v = if (isMinus5m) -abs(num5m) else abs(num5m)
            if (isValidMetricValue(v)) v else null
        } else null

        val parsed60m: Double? = if (num60m != null && !isAmbiguous60m) {
            val isMinus60m = sign60m == "-" || sign60m == "—" || sign60m == "–" || sign60m == "−"
            val v = if (isMinus60m) -abs(num60m) else abs(num60m)
            if (isValidMetricValue(v)) v else null
        } else null

        if (parsed5m == null || parsed60m == null) {
            logWarn("Hard Failure: Optical parsing requires both 5m and 60m metrics. raw5m=$raw5mPair, raw60m=$raw60mPair, val1d=$val1d")
            return TradingAnalysis(
                isSuccess = false,
                isValid = false,
                errorMessage = "No valid metrics (5m and 60m) clearly detected.",
                rawResponse = cleanText,
                latencyMs = latencyMs,
                audioEvent = AudioSignalEngine.SOUND_NONE,
                dataQuality = com.example.data.models.DataQualityState.UNAVAILABLE,
                direction = TradeDirection.NEUTRAL,
                isNoTradeZone = false
            )
        }

        return ReactiveMarketPressureEngine.buildTradingAnalysis(
            val5m = parsed5m,
            val60m = parsed60m,
            val1d = val1d,
            prefix5m = raw5mPair?.first ?: "",
            prefix60m = raw60mPair?.first ?: "",
            isApprox = isApprox5m || isApprox60m,
            is1dExplicitlyRejected = is1dExplicitlyRejected,
            latencyMs = latencyMs,
            engineSource = "Optical Parser",
            rawResponse = cleanText,
            history = history
        )
    }

    data class QuantAnalysisResult(
        val netSum: Double,
        val totalMagnitude: Double,
        val sensitivityRatio: Double,
        val magnitudeScore: Double = 0.0,
        val evidenceScore: Double = 0.0,
        val upPercentage: Double,
        val downPercentage: Double,
        val direction: TradeDirection,
        val strengthLevel: StrengthLevel,
        val audioEvent: String,
        val isNoTradeZone: Boolean,
        val isValid: Boolean = true
    )

    fun createInvalidCanonicalResult(): QuantAnalysisResult {
        return QuantAnalysisResult(
            netSum = Double.NaN,
            totalMagnitude = Double.NaN,
            sensitivityRatio = Double.NaN,
            magnitudeScore = 0.0,
            evidenceScore = 0.0,
            upPercentage = Double.NaN,
            downPercentage = Double.NaN,
            direction = TradeDirection.NEUTRAL,
            strengthLevel = StrengthLevel.NORMAL,
            audioEvent = AudioSignalEngine.SOUND_NONE,
            isNoTradeZone = false,
            isValid = false
        )
    }

    fun calculateCanonicalAnalysis(
        val5m: Double,
        val60m: Double,
        val1d: Double? = null,
        isConflicting: Boolean = false
    ): QuantAnalysisResult {
        if (!isValidMetricValue(val5m) || !isValidMetricValue(val60m)) {
            return createInvalidCanonicalResult()
        }

        val has1d = val1d != null && isValidMetricValue(val1d)
        val v1d = if (has1d) val1d!! else 0.0
        val abs1d = if (has1d) abs(v1d) else 0.0

        val netSum = val5m + val60m
        if (netSum.isNaN() || netSum.isInfinite()) {
            return createInvalidCanonicalResult()
        }

        val abs5m = abs(val5m)
        val abs60m = abs(val60m)
        val totalMagnitude = abs5m + abs60m
        if (totalMagnitude.isNaN() || totalMagnitude.isInfinite()) {
            return createInvalidCanonicalResult()
        }

        // Directional conflict interlock:
        // A severe conflict exists if explicitly flagged by matrix, or if 5m and 60m oppose each other with significant magnitude
        // and near-equal power (deadlock balance |netSum| < 0.25).
        // If one side has clearly dominant power (|netSum| >= 0.25 or asymmetric capacity), the stronger capacity wins (Pullback / Tactical dominance).
        val isOpposingSignificant = val5m * val60m < 0 && abs5m >= 0.35 && abs60m >= 0.35
        val isNearEqualDeadlock = isOpposingSignificant && abs(netSum) < 0.25
        val isSevereDirectionalConflict = isConflicting || isNearEqualDeadlock

        // NO TRADE ZONE (DEAD MARKET OR HARD CONFLICT):
        // If both 5m and 60m are within noise threshold, or totalMagnitude <= EPSILON, or conflict:
        val isNoTrade = (abs5m <= NO_TRADE_THRESHOLD && abs60m <= NO_TRADE_THRESHOLD) ||
                totalMagnitude <= EPSILON ||
                isSevereDirectionalConflict

        val rawSensitivityRatio = if (totalMagnitude <= EPSILON) 0.0 else (abs(netSum) / totalMagnitude) * 100.0
        if (rawSensitivityRatio.isNaN() || rawSensitivityRatio.isInfinite()) {
            return createInvalidCanonicalResult()
        }

        val sensitivityRatio = rawSensitivityRatio.coerceIn(0.0, 100.0)

        val magnitudeScore = (abs(netSum) / 1.00 * 100.0).coerceIn(0.0, 100.0)
        if (magnitudeScore.isNaN() || magnitudeScore.isInfinite()) {
            return createInvalidCanonicalResult()
        }

        val rawEvidence = minOf(sensitivityRatio, magnitudeScore)
        val evidenceScore = roundTo1Decimal(rawEvidence)
        if (evidenceScore.isNaN() || evidenceScore.isInfinite()) {
            return createInvalidCanonicalResult()
        }

        val baseScore = 50.0 + (sensitivityRatio / 2.0)
        if (baseScore.isNaN() || baseScore.isInfinite()) {
            return createInvalidCanonicalResult()
        }

        val (calcUp, calcDown, direction, audioEvent) = when {
            isNoTrade -> {
                Quad(50.0, 50.0, TradeDirection.NEUTRAL, AudioSignalEngine.SOUND_NONE)
            }
            abs(netSum) <= EPSILON -> {
                // Floating-point cancellation neutral resolution
                Quad(50.0, 50.0, TradeDirection.NEUTRAL, AudioSignalEngine.SOUND_NONE)
            }
            netSum > 0.0 -> {
                val up = roundTo1Decimal(baseScore).coerceIn(0.0, 100.0)
                val down = roundTo1Decimal(100.0 - up).coerceIn(0.0, 100.0)
                Quad(up, down, TradeDirection.UP, AudioSignalEngine.SOUND_UP_ALERT)
            }
            netSum < 0.0 -> {
                val down = roundTo1Decimal(baseScore).coerceIn(0.0, 100.0)
                val up = roundTo1Decimal(100.0 - down).coerceIn(0.0, 100.0)
                Quad(up, down, TradeDirection.DOWN, AudioSignalEngine.SOUND_DOWN_ALERT)
            }
            else -> {
                Quad(50.0, 50.0, TradeDirection.NEUTRAL, AudioSignalEngine.SOUND_NONE)
            }
        }

        if (calcUp.isNaN() || calcUp.isInfinite() || calcDown.isNaN() || calcDown.isInfinite() ||
            calcUp < 0.0 || calcUp > 100.0 || calcDown < 0.0 || calcDown > 100.0
        ) {
            return createInvalidCanonicalResult()
        }

        // Strength boundary rules:
        // abs(netSum) < 0.50 -> NORMAL (e.g. 0.4999 -> NORMAL)
        // abs(netSum) < 1.00 -> MEDIUM (e.g. 0.50 -> MEDIUM, 0.9999 -> MEDIUM)
        // >= 1.00 -> HIGH (e.g. 1.00 -> HIGH)
        val absNet = abs(netSum)
        val strength = when {
            absNet < 0.50 -> StrengthLevel.NORMAL
            absNet < 1.00 -> StrengthLevel.MEDIUM
            else -> StrengthLevel.HIGH
        }

        return QuantAnalysisResult(
            netSum = netSum,
            totalMagnitude = totalMagnitude,
            sensitivityRatio = sensitivityRatio,
            magnitudeScore = magnitudeScore,
            evidenceScore = evidenceScore,
            upPercentage = calcUp,
            downPercentage = calcDown,
            direction = direction,
            strengthLevel = strength,
            audioEvent = audioEvent,
            isNoTradeZone = isNoTrade,
            isValid = true
        )
    }

    /**
     * Single Source of Truth for Quantitative Strength and Direction Calculations.
     */
    fun calculateStrengthsAudited(val5m: Double, val60m: Double, val1d: Double? = null): QuantAnalysisResult {
        return calculateCanonicalAnalysis(val5m, val60m, val1d)
    }

    data class MtfCalculationResult(
        val mtfPercentage: Double,
        val upPercentage: Double,
        val downPercentage: Double,
        val direction: TradeDirection,
        val confluenceScore: Double,
        val isAligned: Boolean,
        val formulaExplanation: String
    )

    /**
     * Unified 5/60/1DAY Multi-Timeframe Confluence Percentage Calculation Engine.
     * Accurately integrates 5m (Immediate micro-trend), 60m (Hourly structure), and 1DAY (Macro anchor).
     *
     * Weights:
     * - When 1DAY is present and valid: 5m = 40%, 60m = 40%, 1DAY = 20%
     * - When 1DAY is absent or invalid: 5m = 50%, 60m = 50%
     *
     * Confluence Ratio: (|Weighted Net| / Weighted Magnitude) * 100%
     * Directional Probability: 50.0% + (Confluence / 2.0)
     */
    fun calculate5_60_1dPercentage(
        val5m: Double?,
        val60m: Double?,
        val1d: Double?
    ): MtfCalculationResult {
        if (val5m == null || val60m == null || !isValidMetricValue(val5m) || !isValidMetricValue(val60m)) {
            return MtfCalculationResult(
                mtfPercentage = 50.0,
                upPercentage = 50.0,
                downPercentage = 50.0,
                direction = TradeDirection.NEUTRAL,
                confluenceScore = 0.0,
                isAligned = false,
                formulaExplanation = "Values missing or out of bounds"
            )
        }

        val isDeadMarket = abs(val5m) <= NO_TRADE_THRESHOLD && abs(val60m) <= NO_TRADE_THRESHOLD
        if (isDeadMarket || (abs(val5m) + abs(val60m) + (val1d?.let { abs(it) } ?: 0.0)) <= EPSILON) {
            return MtfCalculationResult(
                mtfPercentage = 50.0,
                upPercentage = 50.0,
                downPercentage = 50.0,
                direction = TradeDirection.NEUTRAL,
                confluenceScore = 0.0,
                isAligned = false,
                formulaExplanation = "🚫 Dead Market / Zero Magnitude"
            )
        }

        val input = ReactiveMarketPressureEngine.PressureInput(
            current5m = val5m,
            previous5m = null,
            current60m = val60m,
            previous60m = null,
            current1d = val1d,
            previous1d = null,
            elapsedSeconds = 1.0,
            approximate = false
        )
        val decision = ReactiveMarketPressureEngine.calculate(input)

        val has1d = isValidMetricValue(val1d)
        val v1d = if (has1d) val1d!! else 0.0
        val isAllBullish = val5m > 0 && val60m > 0 && (!has1d || v1d > 0)
        val isAllBearish = val5m < 0 && val60m < 0 && (!has1d || v1d < 0)
        val isAligned = isAllBullish || isAllBearish

        val effectivePct = if (decision.direction == TradeDirection.DOWN) decision.downPercentage else decision.upPercentage

        return MtfCalculationResult(
            mtfPercentage = effectivePct,
            upPercentage = decision.upPercentage,
            downPercentage = decision.downPercentage,
            direction = decision.direction,
            confluenceScore = decision.pressureStrength,
            isAligned = isAligned,
            formulaExplanation = decision.explanation
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    /**
     * Standardized Rounding Helper with BigDecimal & RoundingMode.HALF_EVEN (Banker's Rounding)
     * Ensuring exact parity for all percentage boundary values and displayed sum == 100.0.
     */
    fun roundTo1Decimal(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return Double.NaN
        return BigDecimal.valueOf(value)
            .setScale(1, RoundingMode.HALF_EVEN)
            .toDouble()
    }

    fun formatWithSign(value: Double?): String {
        if (value == null || value.isNaN() || value.isInfinite()) return "--"
        val rounded = BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_EVEN)
            .toDouble()
        val sign = if (rounded > 0) "+" else ""
        val df = java.text.DecimalFormat("0.##", java.text.DecimalFormatSymbols(Locale.US))
        return "$sign${df.format(rounded)}%"
    }

    fun formatWithSign(value: Double): String {
        return formatWithSign(value as Double?)
    }

    fun formatOrUnavailable(value: Double?, fallback: String = "--"): String {
        if (value == null || value.isNaN() || value.isInfinite()) return fallback
        return formatWithSign(value)
    }

    const val STILL_DELTA = 0.005
    const val SHOCK_DELTA = 0.30
    const val DAILY_ACTIVE_THRESHOLD = 0.30
    const val DAILY_NEUTRAL_THRESHOLD = 0.10
    const val MAX_PAYOUT_PERCENTAGE = 500.0
    const val MAX_INVESTMENT_AMOUNT = 1_000_000_000.0

    enum class DailyContext {
        BULLISH_DAILY,
        BEARISH_DAILY,
        NEUTRAL
    }

    enum class DailyContextMatrix {
        STRONG_BULLISH,
        PULLBACK_BULLISH,
        CHOPPY_BULLISH,
        STRONG_BEARISH,
        PULLBACK_BEARISH,
        CHOPPY_BEARISH,
        NEUTRAL
    }

    fun evaluateDailyContext(val1d: Double?): DailyContext {
        if (!isValidMetricValue(val1d)) {
            return DailyContext.NEUTRAL
        }
        return when {
            val1d!! >= DAILY_ACTIVE_THRESHOLD -> DailyContext.BULLISH_DAILY
            val1d <= -DAILY_ACTIVE_THRESHOLD -> DailyContext.BEARISH_DAILY
            else -> DailyContext.NEUTRAL
        }
    }

    fun evaluateDailyContextMatrix(
        val1d: Double?,
        val5m: Double?,
        val60m: Double?,
        isChoppyOrDead: Boolean = false
    ): DailyContextMatrix {
        if (!isValidMetricValue(val1d) || abs(val1d!!) < DAILY_ACTIVE_THRESHOLD) {
            return DailyContextMatrix.NEUTRAL
        }
        if (val5m != null && !isValidMetricValue(val5m)) {
            return DailyContextMatrix.NEUTRAL
        }
        if (val60m != null && !isValidMetricValue(val60m)) {
            return DailyContextMatrix.NEUTRAL
        }
        if (val5m == null || val60m == null) {
            return DailyContextMatrix.NEUTRAL
        }

        val netShort = val5m + val60m
        if (netShort.isNaN() || netShort.isInfinite()) {
            return DailyContextMatrix.NEUTRAL
        }

        val isDead = isChoppyOrDead ||
                (abs(val5m) <= NO_TRADE_THRESHOLD && abs(val60m) <= NO_TRADE_THRESHOLD)

        return when {
            val1d >= DAILY_ACTIVE_THRESHOLD -> {
                when {
                    isDead -> DailyContextMatrix.CHOPPY_BULLISH
                    netShort > 0.10 -> DailyContextMatrix.STRONG_BULLISH
                    netShort < -0.10 -> DailyContextMatrix.PULLBACK_BULLISH
                    else -> DailyContextMatrix.CHOPPY_BULLISH
                }
            }
            val1d <= -DAILY_ACTIVE_THRESHOLD -> {
                when {
                    isDead -> DailyContextMatrix.CHOPPY_BEARISH
                    netShort < -0.10 -> DailyContextMatrix.STRONG_BEARISH
                    netShort > 0.10 -> DailyContextMatrix.PULLBACK_BEARISH
                    else -> DailyContextMatrix.CHOPPY_BEARISH
                }
            }
            else -> DailyContextMatrix.NEUTRAL
        }
    }

    data class BehaviorAnalysisResult(
        val code: String,
        val title: String,
        val subtitle: String,
        val tags: List<String> = emptyList(),
        val isWarningOnly: Boolean = false,
        val dailyContext: DailyContext = DailyContext.NEUTRAL,
        val dailyMatrix: DailyContextMatrix = DailyContextMatrix.NEUTRAL,
        val confirmationStage: ConfirmationStage = ConfirmationStage.UNKNOWN,
        val nextMovementBias: NextMovementBias = NextMovementBias.UNKNOWN
    )

    data class ActionCardText(
        val title: String,
        val subtitle: String,
        val isWarningOnly: Boolean = false
    )

    data class PnlCalculationResult(
        val grossProfit: Double,
        val grossLoss: Double,
        val netPnl: Double,
        val winRate: Double
    )

    fun calculatePnl(
        investmentAmount: Double,
        payoutPercentage: Double,
        profitCount: Int,
        lossCount: Int
    ): PnlCalculationResult {
        val safeInvestment = if (investmentAmount.isNaN() || investmentAmount.isInfinite() || investmentAmount < 0.0) {
            0.0
        } else {
            minOf(investmentAmount, MAX_INVESTMENT_AMOUNT)
        }
        val safePayout = if (payoutPercentage.isNaN() || payoutPercentage.isInfinite() || payoutPercentage < 0.0) {
            0.0
        } else {
            minOf(payoutPercentage, MAX_PAYOUT_PERCENTAGE)
        }
        val safeProfitCount = if (profitCount < 0) 0L else profitCount.toLong()
        val safeLossCount = if (lossCount < 0) 0L else lossCount.toLong()

        val rawGrossProfit = safeProfitCount.toDouble() * safeInvestment * (safePayout / 100.0)
        val rawGrossLoss = safeLossCount.toDouble() * safeInvestment
        val rawNetPnl = rawGrossProfit - rawGrossLoss

        val (grossProfit, grossLoss, netPnl) = if (rawGrossProfit.isNaN() || rawGrossLoss.isNaN()) {
            Triple(0.0, 0.0, 0.0)
        } else if (rawGrossProfit.isInfinite() || rawGrossLoss.isInfinite()) {
            val effProfit = safeProfitCount.toDouble() * (safePayout / 100.0)
            val effLoss = safeLossCount.toDouble()
            val effMax = maxOf(effProfit, effLoss, abs(effProfit - effLoss))
            if (effMax > 0.0 && effMax.isFinite()) {
                val scale = MAX_FINITE_PNL / effMax
                val scaledProfit = effProfit * scale
                val scaledLoss = effLoss * scale
                Triple(scaledProfit, scaledLoss, scaledProfit - scaledLoss)
            } else if (rawGrossProfit.isInfinite() && !rawGrossLoss.isInfinite()) {
                Triple(MAX_FINITE_PNL, 0.0, MAX_FINITE_PNL)
            } else if (!rawGrossProfit.isInfinite() && rawGrossLoss.isInfinite()) {
                Triple(0.0, MAX_FINITE_PNL, -MAX_FINITE_PNL)
            } else {
                Triple(MAX_FINITE_PNL, MAX_FINITE_PNL, 0.0)
            }
        } else if (rawNetPnl.isNaN()) {
            Triple(0.0, 0.0, 0.0)
        } else {
            val maxMagnitude = maxOf(rawGrossProfit, rawGrossLoss, abs(rawNetPnl))
            if (maxMagnitude > MAX_FINITE_PNL) {
                val scale = MAX_FINITE_PNL / maxMagnitude
                val scaledProfit = rawGrossProfit * scale
                val scaledLoss = rawGrossLoss * scale
                val scaledNet = scaledProfit - scaledLoss
                Triple(scaledProfit, scaledLoss, scaledNet)
            } else {
                Triple(rawGrossProfit, rawGrossLoss, rawNetPnl)
            }
        }

        val totalTrades = safeProfitCount + safeLossCount
        val winRate = if (totalTrades > 0L) {
            roundTo1Decimal((safeProfitCount.toDouble() / totalTrades.toDouble()) * 100.0)
        } else {
            0.0
        }
        return PnlCalculationResult(
            grossProfit = grossProfit,
            grossLoss = grossLoss,
            netPnl = netPnl,
            winRate = if (winRate.isNaN() || winRate.isInfinite()) 0.0 else winRate
        )
    }

    /**
     * Public deterministic helper returning exact behavior code based on strict priority order.
     */
    fun resolveBehaviorCode(
        val5m: Double?,
        val60m: Double?,
        val1d: Double? = null,
        history: List<com.example.data.models.MetricSnapshot> = emptyList(),
        isValid: Boolean = true,
        isApproximate: Boolean = false,
        errorMessage: String? = null,
        isDeadMarket: Boolean = false,
        isNoTradeZone: Boolean = false
    ): String {
        return classifyMovementBehavior(
            val5m = val5m,
            val60m = val60m,
            val1d = val1d,
            history = history,
            isValid = isValid,
            isApproximate = isApproximate,
            errorMessage = errorMessage,
            isDeadMarket = isDeadMarket,
            isNoTradeZone = isNoTradeZone
        ).code
    }

    /**
     * Deterministic Movement Behavior Engine.
     * Evaluates short-term pressure (5m), medium-term pressure (60m), daily context (1d),
     * and rolling history using a strict, unambiguous 12-level precedence chain.
     */
    fun classifyMovementBehavior(
        val5m: Double?,
        val60m: Double?,
        val1d: Double? = null,
        history: List<com.example.data.models.MetricSnapshot> = emptyList(),
        isValid: Boolean = true,
        isApproximate: Boolean = false,
        isProvisional: Boolean = false,
        dataQuality: String = com.example.data.models.DataQualityState.VERIFIED,
        errorMessage: String? = null,
        isDeadMarket: Boolean = false,
        isNoTradeZone: Boolean = false
    ): BehaviorAnalysisResult {
        val result = MovementClassificationEngine.classify(
            val5m = val5m,
            val60m = val60m,
            val1d = val1d,
            history = history,
            isValid = isValid,
            isApproximate = isApproximate,
            isProvisional = isProvisional,
            dataQuality = dataQuality,
            errorMessage = errorMessage,
            isDeadMarket = isDeadMarket,
            isNoTradeZone = isNoTradeZone
        )

        val dailyEnum = when (result.dailyContext) {
            DailyMovementContext.BULLISH_DAILY -> DailyContext.BULLISH_DAILY
            DailyMovementContext.BEARISH_DAILY -> DailyContext.BEARISH_DAILY
            else -> DailyContext.NEUTRAL
        }
        val matrix = evaluateDailyContextMatrix(val1d, val5m, val60m, isDeadMarket || isNoTradeZone)

        return BehaviorAnalysisResult(
            code = result.movementCode,
            title = result.titleBengali,
            subtitle = result.descriptionBengali,
            tags = result.supportingTags,
            isWarningOnly = result.isWarningOnly,
            dailyContext = dailyEnum,
            dailyMatrix = matrix,
            confirmationStage = result.confirmationStage,
            nextMovementBias = result.nextMovementBias
        )
    }

    /**
     * Resolves the user-facing action text strictly respecting Canonical Authority:
     * - Fakeout and momentum-loss rules provide warnings only and never contradict canonical direction
     * - No Trade Zone has highest priority for valid data
     * - Approximate data shows approximation badge and uncertainty, not active trade zone
     * - Invalid data shows Data Unavailable/Ambiguous, never No Trade or 0.0%
     */
    fun resolveActionCardTexts(
        direction: TradeDirection,
        signalType: com.example.data.models.SignalType = com.example.data.models.SignalType.NONE,
        isNoTrade: Boolean = false,
        isApproximate: Boolean = false,
        isValid: Boolean = true,
        errorMessage: String? = null,
        strength: StrengthLevel = StrengthLevel.NORMAL,
        isAligned: Boolean = false,
        v5m: Double? = null,
        v60m: Double? = null,
        isProvisional: Boolean = false,
        evidenceScore: Double? = null,
        formulaScore: Double? = null,
        v1d: Double? = null,
        history: List<com.example.data.models.MetricSnapshot> = emptyList(),
        behaviorTitle: String? = null,
        behaviorDescription: String? = null,
        behaviorWarningOnly: Boolean? = null
    ): ActionCardText {
        if (isProvisional) {
            return ActionCardText(
                title = "দ্রুত প্রিভিউ — যাচাই চলছে",
                subtitle = "পরবর্তী ফ্রেমে চূড়ান্ত নিশ্চিতকরণ প্রতীক্ষিত",
                isWarningOnly = true
            )
        }

        if (!isValid) {
            return ActionCardText(
                title = "তথ্য অনুপলব্ধ বা অস্পষ্ট",
                subtitle = errorMessage ?: "মান বা চিহ্ন শনাক্ত করা যায়নি — স্ক্যানের স্পষ্টতা নিশ্চিত করুন",
                isWarningOnly = true
            )
        }

        if (isApproximate) {
            return ActionCardText(
                title = "⚠️ আনুমানিক ডেটা — সতর্কতা",
                subtitle = "চিহ্ন বা মান অনিশ্চিত — সকল ট্রেড স্থগিত রাখুন",
                isWarningOnly = true
            )
        }

        if (isNoTrade) {
            return ActionCardText(
                title = "শান্ত ও স্থির বাজার — নো-ট্রেড জোন",
                subtitle = "৫মি ও ৬০মি উভয় চাপ দুর্বল (<= 0.10%); ধৈর্য ধরুন, ট্রেড ঝুঁকিপূর্ণ",
                isWarningOnly = false
            )
        }

        if (!behaviorTitle.isNullOrEmpty() && !behaviorDescription.isNullOrEmpty()) {
            return ActionCardText(
                title = behaviorTitle,
                subtitle = behaviorDescription,
                isWarningOnly = behaviorWarningOnly ?: false
            )
        }

        if (signalType == com.example.data.models.SignalType.TOP_FAKEOUT_SELL) {
            return ActionCardText(
                title = "সামান্য উপরে গিয়ে দ্রুত পতনের ঝুঁকি (Bull Trap)",
                subtitle = "৬০মি ঊর্ধ্বমুখী হলেও ৫মি নিম্নমুখী টান শুরু হয়েছে — সেল সিগন্যালে সতর্ক থাকুন",
                isWarningOnly = true
            )
        }

        if (signalType == com.example.data.models.SignalType.BOTTOM_FAKEOUT_BUY) {
            return ActionCardText(
                title = "সামান্য নিচে নেমে দ্রুত বাউন্সের সম্ভাবনা (Bear Trap)",
                subtitle = "৬০মি নিম্নমুখী হলেও ৫মি ঊর্ধ্বমুখী টান শুরু হয়েছে — বাউন্সের জন্য প্রস্তুত থাকুন",
                isWarningOnly = true
            )
        }

        val behavior = classifyMovementBehavior(
            val5m = v5m,
            val60m = v60m,
            val1d = v1d,
            history = history,
            isValid = true,
            isApproximate = false,
            errorMessage = errorMessage,
            isDeadMarket = isNoTrade,
            isNoTradeZone = isNoTrade
        )

        return ActionCardText(
            title = behavior.title,
            subtitle = behavior.subtitle,
            isWarningOnly = behavior.isWarningOnly
        )
    }

    fun normalizeBengaliAndDashes(input: String): String {
        val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        var result = input
            .replace("−", "-")
            .replace("–", "-")
            .replace("—", "-")
            .replace("‑", "-")
            .replace("﹣", "-")
            .replace("－", "-")
        for (i in 0 until 10) {
            result = result.replace(bengaliDigits[i], englishDigits[i])
        }
        result = result.replace(Regex("""(\d),(\d)"""), "$1.$2")
        return result
    }
}
