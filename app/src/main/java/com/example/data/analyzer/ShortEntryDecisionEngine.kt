package com.example.data.analyzer

import com.example.data.models.DataQualityState
import com.example.data.models.MetricSnapshot
import com.example.data.models.TradeDirection
import com.example.data.models.TradingAnalysis
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic entry states for trade execution decision layer.
 * Strictly avoids claiming guaranteed win rates or certainty.
 */
enum class EntryState(val labelEnglish: String, val labelBengali: String) {
    SHORT_ENTRY_READY("SHORT READY", "শর্ট এন্ট্রি প্রস্তুত"),
    SHORT_SETUP("SHORT SETUP", "শর্ট সেটআপ"),
    WAIT_CONFIRMATION("WAIT", "কনফার্মেশনের অপেক্ষা"),
    NO_TRADE("NO TRADE", "নো ট্রেড"),
    INVALID_DATA("INVALID DATA", "ত্রুটিপূর্ণ ডেটা"),
    SIGNAL_EXPIRED("EXPIRED", "সিগন্যালের মেয়াদ শেষ"),
    HOLD("HOLD", "অপেক্ষা করুন")
}

/**
 * Risk flags providing deterministic transparency into signal conditions.
 */
enum class ShortRiskFlag(val priority: Int, val description: String) {
    INVALID_DATA(1, "Incomplete, NaN or unverified OCR values"),
    NO_TRADE_ZONE(2, "Market in low-volatility dead zone or flatline"),
    APPROXIMATE_OCR(3, "OCR reading marked approximate or provisional"),
    CHOPPY_MARKET(4, "Frequent directional alternation / chop detected"),
    DIRECTION_CONFLICT(5, "Timeframes or higher-level biases conflict"),
    DAILY_CONFLICT(6, "Higher timeframe 1-day trend opposes short direction"),
    SPIKE_NOT_CONFIRMED(7, "Sudden downward spike lacks multi-frame continuation"),
    PULLBACK_NOT_FINISHED(8, "Bearish bounce ongoing; 5m has not re-broken downward"),
    SINGLE_CONFIRMATION(9, "Only one distinct snapshot observed; needs multi-frame confirmation"),
    STALE_SIGNAL(10, "Signal exceeded maximum validity duration"),
    WARNING_ONLY(11, "Classification behavior is marked warning-only")
}

/**
 * Deterministic Short Entry Decision result model.
 */
data class ShortEntryDecision(
    val state: EntryState,
    val directionLabel: String,
    val strengthScore: Double?,
    val confirmationCount: Int,
    val requiredConfirmations: Int,
    val reason: String,
    val riskFlags: List<String>,
    val patternCode: String,
    val generatedAtMs: Long,
    val isApproximate: Boolean,
    val isWarningOnly: Boolean
) {
    val primaryRiskFlag: String?
        get() = riskFlags.firstOrNull()
}

/**
 * Pure, deterministic Short-Entry Decision Layer.
 *
 * Implements strict safety gates:
 * 1. Data Validity & Verification (Reject NaN, Infinite, Missing 5m/60m, Approximate, Provisional)
 * 2. No-Trade & Dead Market Suppression
 * 3. Freshness / TTL Checks
 * 4. Multi-Timeframe Confluence (60m meaningfully negative, 5m confirming)
 * 5. Distinct Snapshot Confirmation (repetition of identical frames is not a new confirmation)
 * 6. Honest Score Bounding (0.0 to 100.0, deterministic mathematical strength, NOT a win rate)
 */
object ShortEntryDecisionEngine {

    /** Default time-to-live before an unrefreshed signal is considered expired */
    const val SIGNAL_TTL_MS: Long = 15_000L // 15 seconds

    /** Meaningful minimum negative 60m threshold for short entry (-0.05%) */
    const val MIN_60M_BEARISH_THRESHOLD: Double = -0.05

    /** Minimum required distinct verified confirmations */
    const val REQUIRED_CONFIRMATIONS: Int = 2

    /** Maximum valid percentage to filter out OCR outliers */
    const val MAX_VALID_PERCENTAGE: Double = 500.0

    /** Epsilon for floating comparison */
    const val EPSILON: Double = 1e-6

    /**
     * Converts a TradingAnalysis to a MetricSnapshot for historical tracking.
     */
    fun TradingAnalysis.toMetricSnapshot(): MetricSnapshot = MetricSnapshot(
        val5m = change5mValue ?: 0.0,
        val60m = change60mValue ?: 0.0,
        val1d = change1dValue,
        netSum = netSumValue ?: ((change5mValue ?: 0.0) + (change60mValue ?: 0.0)),
        timestamp = timestamp,
        isValid = isValid && isSuccess,
        isApproximate = isApproximate
    )

    /**
     * Evaluates a TradingAnalysis against strict deterministic gates using TradingAnalysis history.
     */
    @JvmName("evaluateShortEntryFromTradingAnalysisList")
    fun evaluateShortEntry(
        analysis: TradingAnalysis?,
        history: List<TradingAnalysis>,
        currentTimeMs: Long = System.currentTimeMillis()
    ): ShortEntryDecision = evaluateShortEntry(
        analysis = analysis,
        history = history.map { it.toMetricSnapshot() },
        currentTimeMs = currentTimeMs
    )

    /**
     * Evaluates a TradingAnalysis against strict deterministic gates.
     *
     * @param analysis The latest TradingAnalysis produced by OCR or parser
     * @param history Prior verified metric snapshots (max 8-30)
     * @param currentTimeMs Current timestamp (monotonic or wall clock)
     */
    fun evaluateShortEntry(
        analysis: TradingAnalysis?,
        history: List<MetricSnapshot> = emptyList(),
        currentTimeMs: Long = System.currentTimeMillis()
    ): ShortEntryDecision {
        val riskFlags = mutableListOf<String>()

        // GATE 1: Analysis existence, success, and validation
        if (analysis == null || !analysis.isSuccess || !analysis.isValid) {
            riskFlags.add(ShortRiskFlag.INVALID_DATA.name)
            return ShortEntryDecision(
                state = EntryState.INVALID_DATA,
                directionLabel = "HOLD",
                strengthScore = null,
                confirmationCount = 0,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "Analysis data is missing or marked unsuccessful",
                riskFlags = riskFlags,
                patternCode = analysis?.behaviorCode ?: "INVALID",
                generatedAtMs = currentTimeMs,
                isApproximate = analysis?.isApproximate ?: false,
                isWarningOnly = analysis?.isWarningOnly ?: false
            )
        }

        // GATE 2: Core 5m and 60m values present and finite
        val v5m = analysis.change5mValue
        val v60m = analysis.change60mValue
        val v1d = analysis.change1dValue

        if (v5m == null || v60m == null ||
            v5m.isNaN() || v60m.isNaN() ||
            v5m.isInfinite() || v60m.isInfinite() ||
            abs(v5m) > MAX_VALID_PERCENTAGE || abs(v60m) > MAX_VALID_PERCENTAGE
        ) {
            riskFlags.add(ShortRiskFlag.INVALID_DATA.name)
            return ShortEntryDecision(
                state = EntryState.INVALID_DATA,
                directionLabel = "HOLD",
                strengthScore = null,
                confirmationCount = 0,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "5m or 60m metric value is missing, NaN, or out of range",
                riskFlags = riskFlags,
                patternCode = analysis.behaviorCode,
                generatedAtMs = currentTimeMs,
                isApproximate = analysis.isApproximate,
                isWarningOnly = analysis.isWarningOnly
            )
        }

        // GATE 3: Approximate or Provisional OCR
        if (analysis.isApproximate || analysis.isProvisional || analysis.dataQuality != DataQualityState.VERIFIED) {
            riskFlags.add(ShortRiskFlag.APPROXIMATE_OCR.name)
        }

        // GATE 4: Signal freshness / TTL check
        val signalAgeMs = if (analysis.timestamp > 0) currentTimeMs - analysis.timestamp else 0L
        val isStale = signalAgeMs > SIGNAL_TTL_MS
        if (isStale) {
            riskFlags.add(ShortRiskFlag.STALE_SIGNAL.name)
        }

        // GATE 5: No-trade zone, dead market, or neutral direction
        val isDead = analysis.isDeadMarket || analysis.isNoTradeZone ||
                (abs(v5m) <= 0.10 && abs(v60m) <= 0.10)
        if (isDead) {
            riskFlags.add(ShortRiskFlag.NO_TRADE_ZONE.name)
        }

        // GATE 6: Choppy / Alternating market
        val isChoppy = analysis.behaviorCode == "ALTERNATION_CHOP" ||
                analysis.behaviorCode == "UNCONFIRMED_DIRECTION_FLIP" ||
                analysis.behaviorTags.contains("চপি মার্কেট") ||
                analysis.behaviorTags.contains("টানাপোড়েন")
        if (isChoppy) {
            riskFlags.add(ShortRiskFlag.CHOPPY_MARKET.name)
        }

        // GATE 7: Daily context conflict (1D is strongly positive when trying to short)
        if (v1d != null && v1d > 0.30) {
            riskFlags.add(ShortRiskFlag.DAILY_CONFLICT.name)
        }

        // Calculate distinct verified snapshot confirmations
        val distinctConfirmations = countDistinctBearishConfirmations(v5m, v60m, history)

        // Calculate deterministic strength score (0.0 to 100.0)
        val strengthScore = calculateDeterministicStrength(analysis, v5m, v60m, v1d)

        // Route state deterministically
        val bCode = analysis.behaviorCode

        // If data is approximate or unverified: NEVER ready for entry
        if (analysis.isApproximate || analysis.isProvisional || analysis.dataQuality != DataQualityState.VERIFIED) {
            return ShortEntryDecision(
                state = EntryState.WAIT_CONFIRMATION,
                directionLabel = "WAIT",
                strengthScore = strengthScore,
                confirmationCount = distinctConfirmations,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "Approximate OCR reading. Verified optical clarity required.",
                riskFlags = riskFlags,
                patternCode = bCode,
                generatedAtMs = currentTimeMs,
                isApproximate = true,
                isWarningOnly = analysis.isWarningOnly
            )
        }

        // If stale: expire signal
        if (isStale) {
            return ShortEntryDecision(
                state = EntryState.SIGNAL_EXPIRED,
                directionLabel = "HOLD",
                strengthScore = strengthScore,
                confirmationCount = distinctConfirmations,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "Signal expired after ${signalAgeMs / 1000}s without fresh update",
                riskFlags = riskFlags,
                patternCode = bCode,
                generatedAtMs = currentTimeMs,
                isApproximate = false,
                isWarningOnly = analysis.isWarningOnly
            )
        }

        // If no trade or dead market
        if (isDead) {
            return ShortEntryDecision(
                state = EntryState.NO_TRADE,
                directionLabel = "HOLD",
                strengthScore = 50.0,
                confirmationCount = 0,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "Market is in low-volatility dead zone or no-trade regime",
                riskFlags = riskFlags,
                patternCode = bCode,
                generatedAtMs = currentTimeMs,
                isApproximate = false,
                isWarningOnly = analysis.isWarningOnly
            )
        }

        // If choppy market
        if (isChoppy) {
            return ShortEntryDecision(
                state = EntryState.NO_TRADE,
                directionLabel = "HOLD",
                strengthScore = 50.0,
                confirmationCount = 0,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "Choppy alternation detected. Avoid short entries during range chop.",
                riskFlags = riskFlags,
                patternCode = bCode,
                generatedAtMs = currentTimeMs,
                isApproximate = false,
                isWarningOnly = analysis.isWarningOnly
            )
        }

        // Non-down directions
        if (analysis.direction == TradeDirection.NEUTRAL) {
            return ShortEntryDecision(
                state = EntryState.HOLD,
                directionLabel = "HOLD",
                strengthScore = 50.0,
                confirmationCount = 0,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "Neutral market direction without directional edge",
                riskFlags = riskFlags,
                patternCode = bCode,
                generatedAtMs = currentTimeMs,
                isApproximate = false,
                isWarningOnly = analysis.isWarningOnly
            )
        }

        if (analysis.direction == TradeDirection.UP) {
            // Uptrend - short is blocked
            riskFlags.add(ShortRiskFlag.DIRECTION_CONFLICT.name)
            return ShortEntryDecision(
                state = EntryState.HOLD,
                directionLabel = "UP ↗",
                strengthScore = strengthScore,
                confirmationCount = 0,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "Market currently aligned in bullish UP direction",
                riskFlags = riskFlags,
                patternCode = bCode,
                generatedAtMs = currentTimeMs,
                isApproximate = false,
                isWarningOnly = analysis.isWarningOnly
            )
        }

        // Now analyzing TradeDirection.DOWN setups and entries:

        // Case A: Bearish Pullback / Exhaustion Rally in Downtrend (DOWNTREND_PULLBACK_UP)
        // 60m is negative, but 5m is currently positive (pullback bounce)
        if (bCode == "DOWNTREND_PULLBACK_UP" || (v60m < -0.10 && v5m > 0.0)) {
            riskFlags.add(ShortRiskFlag.PULLBACK_NOT_FINISHED.name)
            return ShortEntryDecision(
                state = EntryState.SHORT_SETUP,
                directionLabel = "DOWN ↘",
                strengthScore = strengthScore,
                confirmationCount = distinctConfirmations,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "Bearish pullback detected. Wait for verified 5m downside re-break confirmation.",
                riskFlags = riskFlags,
                patternCode = bCode,
                generatedAtMs = currentTimeMs,
                isApproximate = false,
                isWarningOnly = true
            )
        }

        // Case B: Top Fakeout / Bull Trap
        if (bCode == "TOP_FAKEOUT_RISK" || bCode == "BULL_TRAP" || bCode == "TOP_FAKEOUT_SELL") {
            // If 5m is still positive, it's a setup / warning only
            if (v5m >= 0.0) {
                riskFlags.add(ShortRiskFlag.WARNING_ONLY.name)
                return ShortEntryDecision(
                    state = EntryState.SHORT_SETUP,
                    directionLabel = "DOWN ↘",
                    strengthScore = strengthScore,
                    confirmationCount = distinctConfirmations,
                    requiredConfirmations = REQUIRED_CONFIRMATIONS,
                    reason = "Top fakeout risk detected. Awaiting verified downward follow-through.",
                    riskFlags = riskFlags,
                    patternCode = bCode,
                    generatedAtMs = currentTimeMs,
                    isApproximate = false,
                    isWarningOnly = true
                )
            }
            // If 5m turned negative but we only have 1 confirmation
            if (distinctConfirmations < REQUIRED_CONFIRMATIONS) {
                riskFlags.add(ShortRiskFlag.SINGLE_CONFIRMATION.name)
                return ShortEntryDecision(
                    state = EntryState.WAIT_CONFIRMATION,
                    directionLabel = "DOWN ↘",
                    strengthScore = strengthScore,
                    confirmationCount = distinctConfirmations,
                    requiredConfirmations = REQUIRED_CONFIRMATIONS,
                    reason = "Top fakeout downward follow-through requires 2 distinct snapshot confirmations ($distinctConfirmations/$REQUIRED_CONFIRMATIONS).",
                    riskFlags = riskFlags,
                    patternCode = bCode,
                    generatedAtMs = currentTimeMs,
                    isApproximate = false,
                    isWarningOnly = false
                )
            }
            // All confirmed!
            return ShortEntryDecision(
                state = EntryState.SHORT_ENTRY_READY,
                directionLabel = "DOWN ↘",
                strengthScore = strengthScore,
                confirmationCount = distinctConfirmations,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "Top fakeout confirmed with verified downward 5m transition.",
                riskFlags = riskFlags,
                patternCode = bCode,
                generatedAtMs = currentTimeMs,
                isApproximate = false,
                isWarningOnly = false
            )
        }

        // Case C: Sudden Spike Down (SUDDEN_SPIKE_DOWN)
        if (bCode == "SUDDEN_SPIKE_DOWN") {
            if (distinctConfirmations < REQUIRED_CONFIRMATIONS) {
                riskFlags.add(ShortRiskFlag.SPIKE_NOT_CONFIRMED.name)
                return ShortEntryDecision(
                    state = EntryState.WAIT_CONFIRMATION,
                    directionLabel = "DOWN ↘",
                    strengthScore = strengthScore,
                    confirmationCount = distinctConfirmations,
                    requiredConfirmations = REQUIRED_CONFIRMATIONS,
                    reason = "Sudden downward spike requires stabilization confirmation ($distinctConfirmations/$REQUIRED_CONFIRMATIONS).",
                    riskFlags = riskFlags,
                    patternCode = bCode,
                    generatedAtMs = currentTimeMs,
                    isApproximate = false,
                    isWarningOnly = false
                )
            }
        }

        // Case D: Warning-only classification
        if (analysis.isWarningOnly) {
            riskFlags.add(ShortRiskFlag.WARNING_ONLY.name)
            return ShortEntryDecision(
                state = EntryState.SHORT_SETUP,
                directionLabel = "DOWN ↘",
                strengthScore = strengthScore,
                confirmationCount = distinctConfirmations,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "Pattern flagged as preliminary or warning-only. Awaiting confirmed stage.",
                riskFlags = riskFlags,
                patternCode = bCode,
                generatedAtMs = currentTimeMs,
                isApproximate = false,
                isWarningOnly = true
            )
        }

        // Case E: Standard Down / Confirmed Down
        // Check 60m meaningful threshold
        if (v60m > MIN_60M_BEARISH_THRESHOLD) {
            // 60m is barely negative or flat (e.g. -0.01% to -0.04%)
            riskFlags.add(ShortRiskFlag.DIRECTION_CONFLICT.name)
            return ShortEntryDecision(
                state = EntryState.WAIT_CONFIRMATION,
                directionLabel = "DOWN ↘",
                strengthScore = strengthScore,
                confirmationCount = distinctConfirmations,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "60m trend ($v60m%) is not meaningfully negative (needs <= $MIN_60M_BEARISH_THRESHOLD%).",
                riskFlags = riskFlags,
                patternCode = bCode,
                generatedAtMs = currentTimeMs,
                isApproximate = false,
                isWarningOnly = false
            )
        }

        // Check 5m is strictly non-positive
        if (v5m > 0.0) {
            riskFlags.add(ShortRiskFlag.PULLBACK_NOT_FINISHED.name)
            return ShortEntryDecision(
                state = EntryState.SHORT_SETUP,
                directionLabel = "DOWN ↘",
                strengthScore = strengthScore,
                confirmationCount = distinctConfirmations,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "5m change ($v5m%) is positive during downtrend. Awaiting downward continuation.",
                riskFlags = riskFlags,
                patternCode = bCode,
                generatedAtMs = currentTimeMs,
                isApproximate = false,
                isWarningOnly = false
            )
        }

        // Check confirmation count
        if (distinctConfirmations < REQUIRED_CONFIRMATIONS) {
            riskFlags.add(ShortRiskFlag.SINGLE_CONFIRMATION.name)
            return ShortEntryDecision(
                state = EntryState.WAIT_CONFIRMATION,
                directionLabel = "DOWN ↘",
                strengthScore = strengthScore,
                confirmationCount = distinctConfirmations,
                requiredConfirmations = REQUIRED_CONFIRMATIONS,
                reason = "Awaiting second distinct verified snapshot confirmation ($distinctConfirmations/$REQUIRED_CONFIRMATIONS).",
                riskFlags = riskFlags,
                patternCode = bCode,
                generatedAtMs = currentTimeMs,
                isApproximate = false,
                isWarningOnly = false
            )
        }

        // All gates passed!
        return ShortEntryDecision(
            state = EntryState.SHORT_ENTRY_READY,
            directionLabel = "DOWN ↘",
            strengthScore = strengthScore,
            confirmationCount = distinctConfirmations,
            requiredConfirmations = REQUIRED_CONFIRMATIONS,
            reason = "Multi-timeframe downward trend verified with $distinctConfirmations distinct snapshot confirmations.",
            riskFlags = riskFlags,
            patternCode = bCode,
            generatedAtMs = currentTimeMs,
            isApproximate = false,
            isWarningOnly = false
        )
    }

    /**
     * Counts distinct verified snapshots supporting the bearish direction.
     * Identical repeated frames (same 5m and 60m values within EPSILON) are strictly NOT counted as new confirmations.
     */
    fun countDistinctBearishConfirmations(
        current5m: Double,
        current60m: Double,
        history: List<MetricSnapshot>
    ): Int {
        var count = 0
        if (current5m <= 0.0 && current60m < 0.0) {
            count = 1
        } else if (current60m < -0.10) {
            count = 1
        }

        if (history.isEmpty()) return count

        var lastRecorded5m = current5m
        var lastRecorded60m = current60m

        for (snapshot in history) {
            if (!snapshot.isValid || snapshot.isApproximate) continue
            // Only count if this snapshot was genuinely bearish
            if (snapshot.val60m < 0.0) {
                val isDistinct = abs(snapshot.val5m - lastRecorded5m) > 0.005 ||
                        abs(snapshot.val60m - lastRecorded60m) > 0.005
                if (isDistinct) {
                    count++
                    lastRecorded5m = snapshot.val5m
                    lastRecorded60m = snapshot.val60m
                }
            }
            if (count >= 3) break
        }

        return count
    }

    /**
     * Calculates a bounded, deterministic strength score (0.0 - 100.0).
     * Strictly avoids treating this as an absolute win probability.
     */
    fun calculateDeterministicStrength(
        analysis: TradingAnalysis,
        v5m: Double,
        v60m: Double,
        v1d: Double?
    ): Double {
        val bCode = analysis.behaviorCode
        // 1. Weak bounce / Bearish pullback in downtrend
        if (bCode == "DOWNTREND_PULLBACK_UP" || (v60m < -0.10 && v5m > 0.0 && abs(v5m) < abs(v60m) * 0.45)) {
            val bouncePct = if (abs(v60m) > 0.001) abs(v5m) / abs(v60m) * 100.0 else 0.0
            val baseSellerPower = max(50.0, min(99.9, 100.0 - bouncePct))
            val dailyBoost = if (v1d != null && v1d < 0.0) min(5.0, abs(v1d) * 2.0) else max(-4.0, (v1d ?: 0.0) * -2.0)
            return max(52.0, min(99.9, baseSellerPower + dailyBoost * 0.4))
        }

        // 2. Weak pullback in uptrend
        if (bCode == "UPTREND_PULLBACK_DOWN" || (v60m > 0.10 && v5m < 0.0 && abs(v5m) < abs(v60m) * 0.45)) {
            val retracementPct = if (abs(v60m) > 0.001) abs(v5m) / abs(v60m) * 100.0 else 0.0
            val baseBuyerPower = max(50.0, min(99.9, 100.0 - retracementPct))
            val dailyBoost = if (v1d != null && v1d > 0.0) min(5.0, v1d * 2.0) else max(-4.0, (v1d ?: 0.0) * 2.0)
            return max(52.0, min(99.9, baseBuyerPower + dailyBoost * 0.4))
        }

        // 3. Directional base score from canonical analysis
        val primaryScore = when {
            analysis.direction == TradeDirection.DOWN && analysis.downPercentage.isFinite() && analysis.downPercentage > 50.0 ->
                analysis.downPercentage
            analysis.direction == TradeDirection.UP && analysis.upPercentage.isFinite() && analysis.upPercentage > 50.0 ->
                analysis.upPercentage
            analysis.calculatedPercentage.isFinite() && analysis.calculatedPercentage > 50.0 ->
                analysis.calculatedPercentage
            else -> {
                // Deterministic formula from magnitude of 5m and 60m
                val totalMag = abs(v5m) + abs(v60m)
                val base = 50.0 + min(35.0, totalMag * 20.0)
                max(50.0, min(95.0, base))
            }
        }

        // Add multi-timeframe confluence boost
        val v5Boost = when {
            analysis.direction == TradeDirection.DOWN -> if (v5m < 0.0) min(4.0, abs(v5m) * 4.0) else max(-6.0, -v5m * 4.0)
            analysis.direction == TradeDirection.UP -> if (v5m > 0.0) min(4.0, v5m * 4.0) else max(-6.0, v5m * 4.0)
            else -> 0.0
        }

        val v60Boost = when {
            analysis.direction == TradeDirection.DOWN -> if (v60m < 0.0) min(3.0, abs(v60m) * 2.0) else max(-5.0, -v60m * 2.0)
            analysis.direction == TradeDirection.UP -> if (v60m > 0.0) min(3.0, v60m * 2.0) else max(-5.0, v60m * 2.0)
            else -> 0.0
        }

        val finalScore = primaryScore + (v5Boost * 0.6 + v60Boost * 0.4)
        return max(50.0, min(99.9, finalScore))
    }
}
