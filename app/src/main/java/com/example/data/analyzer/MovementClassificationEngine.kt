package com.example.data.analyzer

import com.example.data.models.DataQualityState
import com.example.data.models.MetricSnapshot
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

/**
 * Direction state for an individual timeframe (5m, 60m, 1D).
 */
enum class TimeframeDirection {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
    UNAVAILABLE
}

/**
 * Internal confirmation lifecycle stage for movement classification.
 */
enum class ConfirmationStage {
    PRELIMINARY,
    DEVELOPING,
    CONFIRMED,
    EXHAUSTING,
    WARNING,
    UNKNOWN
}

/**
 * Next-movement analytical bias.
 * Note: Represents a deterministic statistical/flow bias, NOT a guaranteed prediction.
 */
enum class NextMovementBias {
    UP,
    DOWN,
    NEUTRAL,
    UNKNOWN
}

/**
 * Broader market daily context (1-day percentage).
 */
enum class DailyMovementContext {
    BULLISH_DAILY,
    BEARISH_DAILY,
    NEUTRAL,
    UNAVAILABLE
}

/**
 * Primary Movement Classification Result.
 * Contains single primary movement code, Bengali title and explanation,
 * supporting tags, warning state, confirmation stage, daily context, and next-movement bias.
 */
data class MovementClassificationResult(
    val movementCode: String,
    val titleBengali: String,
    val descriptionBengali: String,
    val supportingTags: List<String>,
    val isWarningOnly: Boolean,
    val confirmationStage: ConfirmationStage,
    val dailyContext: DailyMovementContext,
    val nextMovementBias: NextMovementBias
) {
    // Backwards compatibility property mapping for TradingOutputParser.BehaviorAnalysisResult
    val code: String get() = movementCode
    val title: String get() = titleBengali
    val subtitle: String get() = descriptionBengali
    val tags: List<String> get() = supportingTags
}

/**
 * Deterministic Multi-Timeframe Movement-Classification Engine.
 *
 * Implements 20 primary movement families using:
 * - 5-minute percentage: immediate short-term movement
 * - 60-minute percentage: dominant trend context
 * - 1-day percentage: broader market context (context only, never overrides 5m/60m)
 * - Confirmed historical snapshots: persistence, acceleration, reversal, fakeout, alternation
 */
object MovementClassificationEngine {

    // --- NAMED THRESHOLD CONSTANTS ---

    /**
     * ০.১০% বা তার কম পরিবর্তন হলে শান্ত বা স্থির বাজার হিসেবে গণ্য (No Trade Zone Boundary).
     */
    const val NO_TRADE_THRESHOLD = 0.10

    /**
     * ০.০৫% থ্রেশহোল্ড — এর চেয়ে বেশি পরিবর্তন হলে কার্যকর দিকবদল (Meaningful alternation flip).
     */
    const val ALTERNATION_THRESHOLD = 0.05

    /**
     * ০.১৫% — স্পাইকের পূর্বে পূর্ববর্তী শান্ত অবস্থার সর্বোচ্চ সীমা (Spike base stillness threshold).
     */
    const val SPIKE_BASE_STILL_THRESHOLD = 0.15

    /**
     * ০.৪০% — হঠাৎ স্পাইকের ন্যূনতম পরম মান (Sudden spike absolute threshold).
     */
    const val SPIKE_ABSOLUTE_THRESHOLD = 0.40

    /**
     * ০.৩৫% — পূর্ববর্তী ফ্রেমের তুলনায় হঠাৎ উল্লম্ফন (Sudden spike delta threshold).
     */
    const val SPIKE_DELTA_THRESHOLD = 0.35

    /**
     * ০.২০% — ফেকআউট (বুল/বেয়ার ট্র্যাপ) যাচাইয়ে ৬০ মিনিটের ন্যূনতম শক্তি (Fakeout 60m threshold).
     */
    const val FAKEOUT_60M_THRESHOLD = 0.20

    /**
     * ০.১৫% — মোমেন্টাম ক্লান্তি শনাক্তে ৬০ মিনিটের প্রাথমিক শক্তি (Momentum loss 60m threshold).
     */
    const val MOMENTUM_LOSS_60M_THRESHOLD = 0.15

    /**
     * ০.১০% — মোমেন্টাম হ্রাসে ৫ মিনিটের দুর্বল সীমানা (Momentum loss 5m bound).
     */
    const val MOMENTUM_LOSS_5M_BOUND = 0.10

    /**
     * ০.২০% — নিশ্চিত রিভার্সাল নির্ধারণে নেট মুভমেন্ট (5m + 60m) অতিক্রম করার থ্রেশহোল্ড (Reversal net threshold).
     */
    const val REVERSAL_NET_THRESHOLD = 0.20

    /**
     * ৩টি পরপর নিশ্চিত স্ন্যাপশট — কোনো একক ফ্রেমে রিভার্সাল নিশ্চিত করা নিষিদ্ধ (Min confirmed reversal snapshots).
     */
    const val MIN_REVERSAL_CONFIRMED_SNAPSHOTS = 3

    /**
     * ০.৫০% — ৫ মিনিটের শতাংশ মোমেন্টাম ব্রেক থ্রেশহোল্ড (Momentum break 5m threshold).
     */
    const val MOMENTUM_BREAK_5M_THRESHOLD = 0.50

    /**
     * ০.৬০% — নেট শতাংশ মোমেন্টাম ব্রেক থ্রেশহোল্ড (Momentum break net threshold).
     */
    const val MOMENTUM_BREAK_NET_THRESHOLD = 0.60

    /**
     * ০.৩০% — ১ দিনের সক্রিয় দৈনিক ট্রেন্ড থ্রেশহোল্ড (Daily active trend threshold).
     */
    const val DAILY_ACTIVE_THRESHOLD = 0.30

    /**
     * ০.১০% — ১ দিনের নিরপেক্ষ জোন থ্রেশহোল্ড (Daily neutral threshold).
     */
    const val DAILY_NEUTRAL_THRESHOLD = 0.10

    /**
     * ±৫০০% — চরম আউটলায়ার ফিল্টারিং (Maximum valid percentage boundary).
     */
    const val MAX_VALID_PERCENTAGE = 500.0

    /**
     * ফ্লোটিং পয়েন্ট গোলযোগ দূরীকরণ (Floating-point noise epsilon).
     */
    const val EPSILON = 1e-9

    /**
     * Classifies raw timeframe direction for 5m, 60m, or 1D.
     */
    fun classifyTimeframeDirection(value: Double?): TimeframeDirection {
        if (value == null || value.isNaN() || value.isInfinite() || abs(value) > MAX_VALID_PERCENTAGE) {
            return TimeframeDirection.UNAVAILABLE
        }
        return when {
            value > NO_TRADE_THRESHOLD -> TimeframeDirection.POSITIVE
            value < -NO_TRADE_THRESHOLD -> TimeframeDirection.NEGATIVE
            else -> TimeframeDirection.NEUTRAL
        }
    }

    /**
     * Evaluates broader 1D market context.
     * The 1D context acts as ambient market bias and NEVER overrides 5m/60m movement classification.
     */
    fun evaluateDailyContext(val1d: Double?): DailyMovementContext {
        if (val1d == null || val1d.isNaN() || val1d.isInfinite() || abs(val1d) > MAX_VALID_PERCENTAGE) {
            return DailyMovementContext.UNAVAILABLE
        }
        return when {
            val1d >= DAILY_ACTIVE_THRESHOLD -> DailyMovementContext.BULLISH_DAILY
            val1d <= -DAILY_ACTIVE_THRESHOLD -> DailyMovementContext.BEARISH_DAILY
            else -> DailyMovementContext.NEUTRAL
        }
    }

    /**
     * Evaluates daily context tags and subtitle annotations.
     */
    private fun buildDailyAnnotations(
        dailyCtx: DailyMovementContext,
        val1d: Double?,
        netShort: Double
    ): Triple<List<String>, String, Boolean> {
        val tags = mutableListOf<String>()
        var extraNote = ""
        var addWarning = false

        when (dailyCtx) {
            DailyMovementContext.BULLISH_DAILY -> {
                tags.add("DAILY_BULLISH")
                tags.add("DAILY_UP")
                val formatted1d = formatDailyString(val1d)
                if (netShort > 0.10) {
                    tags.add("DAILY_CONFIRMED")
                    extraNote = " • দৈনিক ট্রেন্ড নিশ্চিতকরণ ($formatted1d)"
                } else if (netShort < -0.10) {
                    tags.add("DAILY_CONFLICT")
                    extraNote = " ⚠️ দৈনিক ট্রেন্ডের সাথে দ্বন্দ্ব (Daily Conflict: $formatted1d)"
                    addWarning = true
                } else {
                    extraNote = " • দৈনিক ট্রেন্ড: ঊর্ধ্বমুখী ($formatted1d)"
                }
            }
            DailyMovementContext.BEARISH_DAILY -> {
                tags.add("DAILY_BEARISH")
                tags.add("DAILY_DOWN")
                val formatted1d = formatDailyString(val1d)
                if (netShort < -0.10) {
                    tags.add("DAILY_CONFIRMED")
                    extraNote = " • দৈনিক ট্রেন্ড নিশ্চিতকরণ ($formatted1d)"
                } else if (netShort > 0.10) {
                    tags.add("DAILY_CONFLICT")
                    extraNote = " ⚠️ দৈনিক ট্রেন্ডের সাথে দ্বন্দ্ব (Daily Conflict: $formatted1d)"
                    addWarning = true
                } else {
                    extraNote = " • দৈনিক ট্রেন্ড: নিম্নমুখী ($formatted1d)"
                }
            }
            DailyMovementContext.NEUTRAL -> {
                tags.add("DAILY_NEUTRAL")
            }
            DailyMovementContext.UNAVAILABLE -> {
                // No daily tag added if unavailable
            }
        }

        return Triple(tags, extraNote, addWarning)
    }

    private fun formatDailyString(val1d: Double?): String {
        if (val1d == null || val1d.isNaN() || val1d.isInfinite()) return "--"
        val rounded = BigDecimal.valueOf(val1d).setScale(2, RoundingMode.HALF_EVEN).toDouble()
        val sign = if (rounded > 0) "+" else ""
        val df = java.text.DecimalFormat("0.##", java.text.DecimalFormatSymbols(Locale.US))
        return "$sign${df.format(rounded)}%"
    }

    /**
     * Deterministic Multi-Timeframe Movement Classifier.
     *
     * Strict Priority Order:
     * 1. Invalid, ambiguous, incomplete, or unavailable data
     * 2. Approximate/provisional data
     * 3. Still/no-trade (STILL_NO_TRADE)
     * 4. Repeated alternation (REPEATED_ALTERNATION)
     * 5. Sudden spike (SUDDEN_SPIKE_UP, SUDDEN_SPIKE_DOWN)
     * 6. Fakeout (TOP_FAKEOUT_RISK, BOTTOM_FAKEOUT_RISK)
     * 7. Confirmed reversal (CONFIRMED_BULLISH_REVERSAL, CONFIRMED_BEARISH_REVERSAL)
     * 8. Momentum loss (MOMENTUM_LOSS_UP, MOMENTUM_LOSS_DOWN)
     * 9. Pullback/reversal attempt (DOWNTREND_PULLBACK_UP, UPTREND_PULLBACK_DOWN, BULLISH_REVERSAL_ATTEMPT, BEARISH_REVERSAL_ATTEMPT)
     * 10. Momentum break (UPWARD_MOMENTUM_BREAK, DOWNWARD_MOMENTUM_BREAK)
     * 11. Steady aligned trend (STEADY_ALIGNED_UP, STEADY_ALIGNED_DOWN)
     * 12. Direction conflict (DIRECTION_CONFLICT)
     * 13. Neutral sideways (NEUTRAL_SIDEWAYS)
     */
    fun classify(
        val5m: Double?,
        val60m: Double?,
        val1d: Double? = null,
        history: List<MetricSnapshot> = emptyList(),
        isValid: Boolean = true,
        isApproximate: Boolean = false,
        isProvisional: Boolean = false,
        dataQuality: String = DataQualityState.VERIFIED,
        errorMessage: String? = null,
        isDeadMarket: Boolean = false,
        isNoTradeZone: Boolean = false
    ): MovementClassificationResult {

        val dir5m = classifyTimeframeDirection(val5m)
        val dir60m = classifyTimeframeDirection(val60m)
        val dailyCtx = evaluateDailyContext(val1d)

        // Helper to construct timeframe direction tags
        val tfTags = mutableListOf<String>()
        when (dir5m) {
            TimeframeDirection.POSITIVE -> tfTags.add("5M_UP")
            TimeframeDirection.NEGATIVE -> tfTags.add("5M_DOWN")
            TimeframeDirection.NEUTRAL -> tfTags.add("5M_NEUTRAL")
            TimeframeDirection.UNAVAILABLE -> {}
        }
        when (dir60m) {
            TimeframeDirection.POSITIVE -> tfTags.add("60M_UP")
            TimeframeDirection.NEGATIVE -> tfTags.add("60M_DOWN")
            TimeframeDirection.NEUTRAL -> tfTags.add("60M_NEUTRAL")
            TimeframeDirection.UNAVAILABLE -> {}
        }

        // =========================================================================
        // PRIORITY 1: Invalid, ambiguous, or unavailable data
        // =========================================================================
        if (!isValid || dataQuality == DataQualityState.UNAVAILABLE) {
            return MovementClassificationResult(
                movementCode = "DATA_UNAVAILABLE",
                titleBengali = "ডেটা অনুপলব্ধ বা অস্পষ্ট",
                descriptionBengali = errorMessage ?: "সঠিক চিহ্ন বা মান শনাক্ত করা যায়নি — স্ক্যান নিশ্চিত করুন",
                supportingTags = listOf("DATA_UNAVAILABLE", "WARNING"),
                isWarningOnly = true,
                confirmationStage = ConfirmationStage.UNKNOWN,
                dailyContext = dailyCtx,
                nextMovementBias = NextMovementBias.UNKNOWN
            )
        }

        if (dataQuality == DataQualityState.AMBIGUOUS) {
            return MovementClassificationResult(
                movementCode = "DATA_AMBIGUOUS",
                titleBengali = "চিহ্ন বা মান অস্পষ্ট",
                descriptionBengali = errorMessage ?: "চিহ্নের দ্ব্যর্থকতা রয়েছে — নিশ্চিত মানের অপেক্ষা করুন",
                supportingTags = listOf("AMBIGUOUS_DATA", "WARNING"),
                isWarningOnly = true,
                confirmationStage = ConfirmationStage.UNKNOWN,
                dailyContext = dailyCtx,
                nextMovementBias = NextMovementBias.UNKNOWN
            )
        }

        if (val5m == null || val60m == null || val5m.isNaN() || val60m.isNaN() ||
            val5m.isInfinite() || val60m.isInfinite() ||
            abs(val5m) > MAX_VALID_PERCENTAGE || abs(val60m) > MAX_VALID_PERCENTAGE
        ) {
            return MovementClassificationResult(
                movementCode = "INCOMPLETE_DATA",
                titleBengali = "ডেটা অসম্পূর্ণ বা অবৈধ",
                descriptionBengali = "৫ মিনিট ও ৬০ মিনিটের উভয় মান পাওয়া যায়নি বা পরিসীমা অতিক্রম করেছে",
                supportingTags = listOf("INCOMPLETE_DATA", "WARNING"),
                isWarningOnly = true,
                confirmationStage = ConfirmationStage.UNKNOWN,
                dailyContext = dailyCtx,
                nextMovementBias = NextMovementBias.UNKNOWN
            )
        }

        // =========================================================================
        // PRIORITY 2: Approximate / provisional data
        // =========================================================================
        if (isProvisional) {
            return MovementClassificationResult(
                movementCode = "PROVISIONAL_DATA",
                titleBengali = "দ্রুত প্রিভিউ — যাচাই চলছে",
                descriptionBengali = "পরবর্তী ফ্রেমে চূড়ান্ত নিশ্চিতকরণ অপেক্ষমাণ",
                supportingTags = listOf("PROVISIONAL", "PREVIEW", "WARNING", "প্রাথমিক ইঙ্গিত"),
                isWarningOnly = true,
                confirmationStage = ConfirmationStage.PRELIMINARY,
                dailyContext = dailyCtx,
                nextMovementBias = NextMovementBias.UNKNOWN
            )
        }

        if (isApproximate || dataQuality == DataQualityState.APPROXIMATE) {
            val tags = mutableListOf("APPROXIMATE_DATA", "WARNING", "প্রাথমিক ইঙ্গিত")
            tags.addAll(tfTags)
            return MovementClassificationResult(
                movementCode = "APPROXIMATE_DATA",
                titleBengali = "⚠️ আনুমানিক ডেটা — সতর্কতা",
                descriptionBengali = "চিহ্ন বা মান সম্পূর্ণ নিশ্চিত নয় — ট্রেড স্থগিত রাখুন",
                supportingTags = tags,
                isWarningOnly = true,
                confirmationStage = ConfirmationStage.PRELIMINARY,
                dailyContext = dailyCtx,
                nextMovementBias = NextMovementBias.UNKNOWN
            )
        }

        // Canonical math for movement analysis
        val abs5m = abs(val5m)
        val abs60m = abs(val60m)
        val netShort = BigDecimal.valueOf(val5m).add(BigDecimal.valueOf(val60m)).toDouble()
        val totalMagnitude = abs5m + abs60m

        // Filter confirmed valid snapshots from history (newest first, index 0 is previous frame)
        val confirmedHistory = history.filter { snapshot ->
            snapshot.isValid && !snapshot.isApproximate &&
                !snapshot.val5m.isNaN() && !snapshot.val5m.isInfinite() &&
                !snapshot.val60m.isNaN() && !snapshot.val60m.isInfinite() &&
                abs(snapshot.val5m) <= MAX_VALID_PERCENTAGE &&
                abs(snapshot.val60m) <= MAX_VALID_PERCENTAGE
        }
        val prev5m = confirmedHistory.firstOrNull()?.val5m

        // Daily context annotations helper
        val (dailyTags, dailyNote, dailyConflictWarn) = buildDailyAnnotations(dailyCtx, val1d, netShort)

        // Helper to assemble final MovementClassificationResult with daily tags & note
        fun finalizeResult(
            code: String,
            title: String,
            subtitle: String,
            tags: List<String>,
            isWarningOnly: Boolean,
            stage: ConfirmationStage,
            bias: NextMovementBias
        ): MovementClassificationResult {
            val allTags = mutableListOf<String>()
            allTags.addAll(tags)
            allTags.addAll(tfTags)
            allTags.addAll(dailyTags)

            val stagePhrase = when (stage) {
                ConfirmationStage.PRELIMINARY -> "প্রাথমিক ইঙ্গিত"
                ConfirmationStage.DEVELOPING -> "বিকাশমান, নিশ্চিতকরণ অপেক্ষমাণ"
                ConfirmationStage.CONFIRMED -> "একাধিক ফ্রেমে নিশ্চিত"
                ConfirmationStage.EXHAUSTING -> "মোমেন্টাম দুর্বল হচ্ছে"
                ConfirmationStage.WARNING -> "সতর্কতা"
                ConfirmationStage.UNKNOWN -> null
            }
            if (stagePhrase != null && !allTags.contains(stagePhrase)) {
                allTags.add(stagePhrase)
            }

            if (isWarningOnly && !allTags.contains("WARNING")) {
                allTags.add("WARNING")
            }

            val baseWithDaily = if (dailyNote.isNotEmpty()) subtitle + dailyNote else subtitle
            val biasSentence = when (bias) {
                NextMovementBias.UP -> "পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী — নিশ্চিত পূর্বাভাস নয়"
                NextMovementBias.DOWN -> "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী — নিশ্চিত পূর্বাভাস নয়"
                NextMovementBias.NEUTRAL -> "পরবর্তী সম্ভাব্য দিক: নিরপেক্ষ বা অনিশ্চিত"
                NextMovementBias.UNKNOWN -> null
            }

            val finalSubtitle = if (biasSentence != null) {
                val trimmed = baseWithDaily.trim()
                val sep = when {
                    trimmed.endsWith("।") || trimmed.endsWith(".") || trimmed.endsWith("!") -> " "
                    trimmed.endsWith(";") || trimmed.endsWith(":") || trimmed.endsWith(",") -> " "
                    else -> "। "
                }
                "$trimmed$sep$biasSentence"
            } else {
                baseWithDaily
            }

            val finalWarning = isWarningOnly || dailyConflictWarn

            return MovementClassificationResult(
                movementCode = code,
                titleBengali = title,
                descriptionBengali = finalSubtitle,
                supportingTags = allTags.distinct(),
                isWarningOnly = finalWarning,
                confirmationStage = stage,
                dailyContext = dailyCtx,
                nextMovementBias = bias
            )
        }

        // =========================================================================
        // PRIORITY 3: Still / no-trade (STILL_NO_TRADE)
        // =========================================================================
        val isStillMarket = isDeadMarket || isNoTradeZone ||
            (abs5m <= NO_TRADE_THRESHOLD && abs60m <= NO_TRADE_THRESHOLD) ||
            totalMagnitude <= EPSILON

        if (isStillMarket) {
            val isExtremeCompression = abs5m <= 0.05 && abs60m <= 0.08 && totalMagnitude <= 0.10
            val tags = if (isExtremeCompression) {
                listOf("NO_TRADE", "STILL_MARKET", "COMPRESSION_COIL", "BREAKOUT_PENDING")
            } else {
                listOf("NO_TRADE", "STILL_MARKET")
            }
            return finalizeResult(
                code = "STILL_NO_TRADE",
                title = if (isExtremeCompression) "শান্ত ও স্থির বাজার — চরম কম্প্রেশন জোন" else "শান্ত ও স্থির বাজার — নো-ট্রেড জোন",
                subtitle = if (isExtremeCompression) {
                    "৫মি ও ৬০মি উভয় টাইমফ্রেম চরম সংকুচিত; স্প্রিংয়ের মতো চাপ জমা হচ্ছে, যেকোনো দিকে বড় মুভমেন্টের সম্ভাবনা"
                } else {
                    "৫ মিনিট ও ৬০ মিনিটের চাপ দুটোই দুর্বল (<= ০.১০%); শান্ত বাজার অঞ্চল"
                },
                tags = tags,
                isWarningOnly = false,
                stage = ConfirmationStage.CONFIRMED,
                bias = NextMovementBias.NEUTRAL
            )
        }

        // =========================================================================
        // PRIORITY 4: Repeated Alternation (REPEATED_ALTERNATION)
        // Choppy market / direction flips
        // =========================================================================
        val hasAlternation = if (confirmedHistory.size >= 3) {
            val s0 = netShort
            val s1 = confirmedHistory[0].val5m + confirmedHistory[0].val60m
            val s2 = confirmedHistory[1].val5m + confirmedHistory[1].val60m
            val s3 = confirmedHistory[2].val5m + confirmedHistory[2].val60m

            fun isMeaningfulSignFlip(a: Double, b: Double): Boolean =
                (a > ALTERNATION_THRESHOLD && b < -ALTERNATION_THRESHOLD) ||
                (a < -ALTERNATION_THRESHOLD && b > ALTERNATION_THRESHOLD)

            var flips = 0
            if (isMeaningfulSignFlip(s0, s1)) flips++
            if (isMeaningfulSignFlip(s1, s2)) flips++
            if (isMeaningfulSignFlip(s2, s3)) flips++
            flips >= 2
        } else false

        if (hasAlternation) {
            return finalizeResult(
                code = "REPEATED_ALTERNATION",
                title = "উপর-নিচ-উপর-নিচ তীব্র টানাপোড়েন — চপি মার্কেট",
                subtitle = "পরপর ক্যান্ডেলে দিক পরিবর্তন হচ্ছে; পরিষ্কার ট্রেন্ড নেই, ট্রেড এড়িয়ে চলুন",
                tags = listOf("CHOPPY", "ALTERNATION", "CONFLICT"),
                isWarningOnly = true,
                stage = ConfirmationStage.WARNING,
                bias = NextMovementBias.NEUTRAL
            )
        }

        // =========================================================================
        // PRIORITY 5: Sudden Spike (SUDDEN_SPIKE_UP, SUDDEN_SPIKE_DOWN)
        // Shock moves following stillness
        // =========================================================================
        val hasTwoStillPrevious = confirmedHistory.size >= 2 &&
            abs(confirmedHistory[0].val5m) <= SPIKE_BASE_STILL_THRESHOLD &&
            abs(confirmedHistory[1].val5m) <= SPIKE_BASE_STILL_THRESHOLD

        val isSpikeUp = hasTwoStillPrevious &&
            ((val5m >= SPIKE_ABSOLUTE_THRESHOLD) || ((val5m - confirmedHistory[0].val5m) >= SPIKE_DELTA_THRESHOLD))
        if (isSpikeUp) {
            return finalizeResult(
                code = "SUDDEN_SPIKE_UP",
                title = "হঠাৎ উপরের ধাক্কা — দ্রুত অস্থিরতার ঝুঁকি",
                subtitle = "পূর্বের শান্ত অবস্থার পর হঠাৎ বড় স্পাইক এসেছে; ট্রেন্ড নিশ্চিত হওয়ার আগে সতর্ক থাকুন",
                tags = listOf("SUDDEN_SPIKE", "UP_SHOCK", "VOLATILITY"),
                isWarningOnly = true,
                stage = ConfirmationStage.WARNING,
                bias = NextMovementBias.UP
            )
        }

        val isSpikeDown = hasTwoStillPrevious &&
            ((val5m <= -SPIKE_ABSOLUTE_THRESHOLD) || ((val5m - confirmedHistory[0].val5m) <= -SPIKE_DELTA_THRESHOLD))
        if (isSpikeDown) {
            return finalizeResult(
                code = "SUDDEN_SPIKE_DOWN",
                title = "হঠাৎ নিচের ধাক্কা — দ্রুত পতনের ঝুঁকি",
                subtitle = "পূর্বের শান্ত অবস্থার পর হঠাৎ বড় নিম্নমুখী স্পাইক এসেছে; সতর্ক থাকুন",
                tags = listOf("SUDDEN_SPIKE", "DOWN_SHOCK", "VOLATILITY"),
                isWarningOnly = true,
                stage = ConfirmationStage.WARNING,
                bias = NextMovementBias.DOWN
            )
        }

        // =========================================================================
        // PRIORITY 6: Fakeout (TOP_FAKEOUT_RISK, BOTTOM_FAKEOUT_RISK)
        // =========================================================================
        // Top fakeout (Bull Trap): prev5m > 0, current val5m < 0, val60m >= FAKEOUT_60M_THRESHOLD (0.20)
        val isTopFakeout = prev5m != null && prev5m > 0.0 && val5m < 0.0 && val60m >= FAKEOUT_60M_THRESHOLD
        if (isTopFakeout) {
            return finalizeResult(
                code = "TOP_FAKEOUT_RISK",
                title = "সামান্য উপরে গিয়ে দ্রুত পতনের ঝুঁকি (Bull Trap)",
                subtitle = "৬০ মিনিট ঊর্ধ্বমুখী হলেও ৫ মিনিটে নিম্নমুখী ধাক্কা শুরু হয়েছে — সেল সিগন্যালে সতর্কতা রাখুন",
                tags = listOf("BULL_TRAP", "TOP_FAKEOUT", "SELL_WARNING"),
                isWarningOnly = true,
                stage = ConfirmationStage.WARNING,
                bias = NextMovementBias.DOWN
            )
        }

        // Bottom fakeout (Bear Trap): prev5m < 0, current val5m > 0, val60m <= -FAKEOUT_60M_THRESHOLD (-0.20)
        val isBottomFakeout = prev5m != null && prev5m < 0.0 && val5m > 0.0 && val60m <= -FAKEOUT_60M_THRESHOLD
        if (isBottomFakeout) {
            return finalizeResult(
                code = "BOTTOM_FAKEOUT_RISK",
                title = "সামান্য নিচে নেমে দ্রুত বাউন্সের সম্ভাবনা (Bear Trap)",
                subtitle = "৬০ মিনিট নিম্নমুখী হলেও ৫ মিনিটে ঊর্ধ্বমুখী রিবাউন্ড শুরু হয়েছে — বাউন্সের প্রস্তুতি নিন",
                tags = listOf("BEAR_TRAP", "BOTTOM_FAKEOUT", "BUY_ALERT"),
                isWarningOnly = true,
                stage = ConfirmationStage.WARNING,
                bias = NextMovementBias.UP
            )
        }

        // =========================================================================
        // PRIORITY 7: Confirmed Reversal (CONFIRMED_BULLISH_REVERSAL, CONFIRMED_BEARISH_REVERSAL)
        // Require multiple confirmed snapshots and a documented net-movement threshold crossing.
        // Never classify one positive/negative frame as a confirmed reversal.
        // =========================================================================
        val hasConfirmedBullishReversal = run {
            // Need at least MIN_REVERSAL_CONFIRMED_SNAPSHOTS (3) consecutive positive 5m snapshots:
            // current val5m > 0 + at least 2 consecutive positive 5m in confirmedHistory
            if (val5m > 0.0 && confirmedHistory.size >= 2) {
                val p1 = confirmedHistory[0].val5m
                val p2 = confirmedHistory[1].val5m
                val consecutivePositive = p1 > 0.0 && p2 > 0.0
                // Net movement must cross the positive threshold
                val netCrossed = netShort >= REVERSAL_NET_THRESHOLD
                // Prior or current larger context (60m) was negative or in recovery
                val contextWasBearish = val60m < 0.0 || (confirmedHistory.any { it.val60m < 0.0 })
                consecutivePositive && netCrossed && contextWasBearish
            } else false
        }

        if (hasConfirmedBullishReversal) {
            return finalizeResult(
                code = "CONFIRMED_BULLISH_REVERSAL",
                title = "নিশ্চিত ঊর্ধ্বমুখী রিভার্সাল — ট্রেন্ড বদল",
                subtitle = "একাধিক ফ্রেমে ইতিবাচক চাপ ও নেট মোমেন্টাম স্তর (+০.২০%) অতিক্রম করেছে; ট্রেন্ড ঘুরে দাঁড়িয়েছে",
                tags = listOf("CONFIRMED_REVERSAL", "BULLISH_REVERSAL", "TREND_SHIFT"),
                isWarningOnly = false,
                stage = ConfirmationStage.CONFIRMED,
                bias = NextMovementBias.UP
            )
        }

        val hasConfirmedBearishReversal = run {
            // Need at least MIN_REVERSAL_CONFIRMED_SNAPSHOTS (3) consecutive negative 5m snapshots:
            // current val5m < 0 + at least 2 consecutive negative 5m in confirmedHistory
            if (val5m < 0.0 && confirmedHistory.size >= 2) {
                val p1 = confirmedHistory[0].val5m
                val p2 = confirmedHistory[1].val5m
                val consecutiveNegative = p1 < 0.0 && p2 < 0.0
                // Net movement must cross the negative threshold
                val netCrossed = netShort <= -REVERSAL_NET_THRESHOLD
                // Prior or current larger context (60m) was positive or in decline
                val contextWasBullish = val60m > 0.0 || (confirmedHistory.any { it.val60m > 0.0 })
                consecutiveNegative && netCrossed && contextWasBullish
            } else false
        }

        if (hasConfirmedBearishReversal) {
            return finalizeResult(
                code = "CONFIRMED_BEARISH_REVERSAL",
                title = "নিশ্চিত নিম্নমুখী রিভার্সাল — ট্রেন্ড বদল",
                subtitle = "একাধিক ফ্রেমে নেতিবাচক চাপ ও নেট মোমেন্টাম স্তর (-০.২০%) অতিক্রম করেছে; ট্রেন্ড নিম্নমুখী হয়েছে",
                tags = listOf("CONFIRMED_REVERSAL", "BEARISH_REVERSAL", "TREND_SHIFT"),
                isWarningOnly = false,
                stage = ConfirmationStage.CONFIRMED,
                bias = NextMovementBias.DOWN
            )
        }

        // =========================================================================
        // PRIORITY 8: Momentum Loss (MOMENTUM_LOSS_UP, MOMENTUM_LOSS_DOWN)
        // Upward or downward exhaustion
        // =========================================================================
        val isMomentumLossUp = val60m >= MOMENTUM_LOSS_60M_THRESHOLD &&
            prev5m != null && prev5m >= 0.15 &&
            val5m in 0.0..MOMENTUM_LOSS_5M_BOUND &&
            val5m < prev5m

        if (isMomentumLossUp) {
            return finalizeResult(
                code = "MOMENTUM_LOSS_UP",
                title = "উপরে ওঠার গতি কমছে — সাময়িক বিরতি বা পতনের সম্ভাবনা",
                subtitle = "UP চাপ হ্রাস পাচ্ছে; যেকোনো সময় সাময়িক DOWN পুলব্যাক বা রিভার্সাল ঝুঁকি রয়েছে",
                tags = listOf("MOMENTUM_LOSS", "REVERSAL_RISK", "SELL_BIAS"),
                isWarningOnly = true,
                stage = ConfirmationStage.EXHAUSTING,
                bias = NextMovementBias.DOWN
            )
        }

        val isMomentumLossDown = val60m <= -MOMENTUM_LOSS_60M_THRESHOLD &&
            prev5m != null && prev5m <= -0.15 &&
            val5m in -MOMENTUM_LOSS_5M_BOUND..0.0 &&
            val5m > prev5m

        if (isMomentumLossDown) {
            return finalizeResult(
                code = "MOMENTUM_LOSS_DOWN",
                title = "নিচে নামার গতি কমছে — সাময়িক বিরতি বা বাউন্সের সম্ভাবনা",
                subtitle = "DOWN চাপ হ্রাস পাচ্ছে; যেকোনো সময় সাময়িক UP বাউন্স বা রিভার্সাল ঝুঁকি রয়েছে",
                tags = listOf("MOMENTUM_LOSS", "BOUNCE_RISK", "BUY_BIAS"),
                isWarningOnly = true,
                stage = ConfirmationStage.EXHAUSTING,
                bias = NextMovementBias.UP
            )
        }

        // =========================================================================
        // PRIORITY 9: Pullback & Reversal Attempts
        // (DOWNTREND_PULLBACK_UP, UPTREND_PULLBACK_DOWN, BULLISH_REVERSAL_ATTEMPT, BEARISH_REVERSAL_ATTEMPT, AGGRESSIVE_UP_BREAK, AGGRESSIVE_DOWN_BREAK)
        // =========================================================================
        // Downtrend Pullback Up or Bullish Reversal Attempt: 60m is negative, 5m is positive
        if (val60m < -NO_TRADE_THRESHOLD && val5m > 0.0) {
            // Check for aggressive breakout attempt against downtrend
            val isAggressiveBreak = val5m >= 0.35 && val5m > abs(val60m) * 1.2
            if (isAggressiveBreak) {
                return finalizeResult(
                    code = "AGGRESSIVE_UP_BREAK",
                    title = "আগ্রাসী UP মোমেন্টাম ব্রেক — ডাউনট্রেন্ড ব্রেকআউট প্রচেষ্টা",
                    subtitle = "৬০মি নিম্নমুখী চাপকে ছাপিয়ে ৫ মিনিটে তীব্র UP গতি প্রবেশ করেছে; ডাউনট্রেন্ড ব্রেকআউটের লক্ষণ",
                    tags = listOf("BREAKOUT_ATTEMPT", "AGGRESSIVE_UP", "TREND_BREAK"),
                    isWarningOnly = true,
                    stage = ConfirmationStage.DEVELOPING,
                    bias = NextMovementBias.UP
                )
            }

            val isWeakBounce = abs(val5m) < abs(val60m) * 0.45

            // Check if confirmed history shows early upward recovery (reversal attempt)
            // e.g. 1 positive snapshot in history, or 5m showing progressive upward steps
            val hasEarlyBullishRecovery = confirmedHistory.isNotEmpty() &&
                !isWeakBounce &&
                ((confirmedHistory[0].val5m >= 0.15 && abs(confirmedHistory[0].val5m) >= abs(val60m) * 0.35) || (prev5m != null && val5m > prev5m && val5m >= 0.15))

            if (hasEarlyBullishRecovery) {
                val bouncePct = if (abs(val60m) > 0.001) abs(val5m) / abs(val60m) * 100.0 else 0.0
                val bounceStr = String.format(Locale.US, "%.1f", bouncePct)
                return finalizeResult(
                    code = "BULLISH_REVERSAL_ATTEMPT",
                    title = "ঊর্ধ্বমুখী রিভার্সাল প্রচেষ্টা — যাচাই চলছে",
                    subtitle = "৬০মি নিম্নমুখী (${String.format(Locale.US, "%.2f", val60m)}%) হলেও ৫ মিনিটে বড় পুনরুদ্ধার +${String.format(Locale.US, "%.2f", val5m)}% (${bounceStr}% বাউন্স টান); পরবর্তী ১–২ মিনিটে বায়ারদের চাপ অব্যাহত থাকলে UP রিভার্সাল নিশ্চিত হবে",
                    tags = listOf("REVERSAL_ATTEMPT", "REVERSAL_PENDING"),
                    isWarningOnly = true,
                    stage = ConfirmationStage.DEVELOPING,
                    bias = NextMovementBias.UP
                )
            } else {
                val bouncePct = if (abs(val60m) > 0.001) abs(val5m) / abs(val60m) * 100.0 else 0.0
                val sellerControlPct = maxOf(0.0, 100.0 - bouncePct)
                val bouncePctStr = String.format(Locale.US, "%.1f", bouncePct)
                val sellerControlStr = String.format(Locale.US, "%.1f", sellerControlPct)

                val title = if (isWeakBounce) {
                    "ডাউনট্রেন্ডে দুর্বল UP বাউন্স — রেজিস্ট্যান্স রিজেকশনে DOWN পতনের ঝুঁকি"
                } else {
                    "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন"
                }
                val desc = if (isWeakBounce) {
                    "৬০মি পতন (${String.format(Locale.US, "%.2f", val60m)}%) শক্তিশালী, ৫মি বৃদ্ধি মাত্র +${String.format(Locale.US, "%.2f", val5m)}% (মাত্র ${bouncePctStr}% বাউন্স, সেলারদের নিয়ন্ত্রণ ${sellerControlStr}%); পরবর্তী ১–৩ মিনিটে ৬০মি রেজিস্ট্যান্সে সেলারদের চাপে ক্যান্ডেল পুনরায় DOWN পতনে ফেরার সর্বোচ্চ সম্ভাবনা"
                } else {
                    "৬০ মিনিট নিম্নমুখী হলেও ৫ মিনিটে ঊর্ধ্বমুখী টান এসেছে; এটি বড় পতনের মাঝে সাময়িক পুলব্যাক"
                }
                val tags = if (isWeakBounce) {
                    listOf("PULLBACK", "PULLBACK_PRIMARY_SIGNAL", "DOWNTREND_PULLBACK", "RESISTANCE_REJECTION", "DOWN_CONTINUATION")
                } else {
                    listOf("PULLBACK", "DOWNTREND_PULLBACK")
                }
                val bias = if (isWeakBounce) NextMovementBias.DOWN else NextMovementBias.UP

                return finalizeResult(
                    code = "DOWNTREND_PULLBACK_UP",
                    title = title,
                    subtitle = desc,
                    tags = tags,
                    isWarningOnly = true,
                    stage = ConfirmationStage.DEVELOPING,
                    bias = bias
                )
            }
        }

        // Uptrend Pullback Down or Bearish Reversal Attempt: 60m is positive, 5m is negative
        if (val60m > NO_TRADE_THRESHOLD && val5m < 0.0) {
            // Check for aggressive breakdown attempt against uptrend
            val isAggressiveBreak = val5m <= -0.35 && abs(val5m) > abs(val60m) * 1.2
            if (isAggressiveBreak) {
                return finalizeResult(
                    code = "AGGRESSIVE_DOWN_BREAK",
                    title = "আগ্রাসী DOWN মোমেন্টাম ব্রেক — আপট্রেন্ড ব্রেকডাউন প্রচেষ্টা",
                    subtitle = "৬০মি ঊর্ধ্বমুখী চাপকে ছাপিয়ে ৫ মিনিটে তীব্র DOWN গতি প্রবেশ করেছে; আপট্রেন্ড ব্রেকডাউনের লক্ষণ",
                    tags = listOf("BREAKDOWN_ATTEMPT", "AGGRESSIVE_DOWN", "TREND_BREAK"),
                    isWarningOnly = true,
                    stage = ConfirmationStage.DEVELOPING,
                    bias = NextMovementBias.DOWN
                )
            }

            val isWeakPullback = abs(val5m) < abs(val60m) * 0.45

            // Check if confirmed history shows early downward pressure (reversal attempt)
            val hasEarlyBearishDecline = confirmedHistory.isNotEmpty() &&
                !isWeakPullback &&
                ((confirmedHistory[0].val5m <= -0.15 && abs(confirmedHistory[0].val5m) >= abs(val60m) * 0.35) || (prev5m != null && val5m < prev5m && val5m <= -0.15))

            if (hasEarlyBearishDecline) {
                val retracementPct = if (abs(val60m) > 0.001) abs(val5m) / abs(val60m) * 100.0 else 0.0
                val retracementStr = String.format(Locale.US, "%.1f", retracementPct)
                return finalizeResult(
                    code = "BEARISH_REVERSAL_ATTEMPT",
                    title = "নিম্নমুখী রিভার্সাল প্রচেষ্টা — যাচাই চলছে",
                    subtitle = "৬০মি বৃদ্ধি (+${String.format(Locale.US, "%.2f", val60m)}%) সত্ত্বেও ৫ মিনিটে বড় পতন ${String.format(Locale.US, "%.2f", val5m)}% (${retracementStr}% রিভার্সাল টান); পরবর্তী ১–২ মিনিটে সেলারদের চাপ অব্যাহত থাকলে DOWN রিভার্সাল নিশ্চিত হবে",
                    tags = listOf("REVERSAL_ATTEMPT", "REVERSAL_PENDING"),
                    isWarningOnly = true,
                    stage = ConfirmationStage.DEVELOPING,
                    bias = NextMovementBias.DOWN
                )
            } else {
                val retracementPct = if (abs(val60m) > 0.001) abs(val5m) / abs(val60m) * 100.0 else 0.0
                val buyerControlPct = maxOf(0.0, 100.0 - retracementPct)
                val retracementStr = String.format(Locale.US, "%.1f", retracementPct)
                val buyerControlStr = String.format(Locale.US, "%.1f", buyerControlPct)

                val title = if (isWeakPullback) {
                    "আপট্রেন্ডে দুর্বল DOWN পুলব্যাক — সাপোর্ট বাউন্সে UP উত্থানের সম্ভাবনা"
                } else {
                    "আপট্রেন্ডে নিম্নমুখী পুলব্যাক — সংশোধন"
                }
                val desc = if (isWeakPullback) {
                    "৬০মি ঊর্ধ্বমুখী গতি (+${String.format(Locale.US, "%.2f", val60m)}%) অত্যন্ত শক্তিশালী, ৫মি পতন মাত্র ${String.format(Locale.US, "%.2f", val5m)}% (মাত্র ${retracementStr}% রিট্রেসমেন্ট, বায়ারদের নিয়ন্ত্রণ ${buyerControlStr}%); পরবর্তী ১–৩ মিনিটে ৬০মি সাপোর্টে বায়ারদের বাউন্সে ক্যান্ডেল পুনরায় UP মুভমেন্টে ফেরার সর্বোচ্চ সম্ভাবনা"
                } else {
                    "৬০ মিনিট ঊর্ধ্বমুখী হলেও ৫ মিনিটে নিম্নমুখী ধাক্কা এসেছে; এটি বড় আপট্রেন্ডের মাঝে সাময়িক সংশোধন"
                }
                val tags = if (isWeakPullback) {
                    listOf("PULLBACK", "PULLBACK_PRIMARY_SIGNAL", "UPTREND_PULLBACK", "SUPPORT_BOUNCE", "UP_CONTINUATION")
                } else {
                    listOf("PULLBACK", "UPTREND_PULLBACK")
                }
                val bias = if (isWeakPullback) NextMovementBias.UP else NextMovementBias.DOWN

                return finalizeResult(
                    code = "UPTREND_PULLBACK_DOWN",
                    title = title,
                    subtitle = desc,
                    tags = tags,
                    isWarningOnly = true,
                    stage = ConfirmationStage.DEVELOPING,
                    bias = bias
                )
            }
        }

        // =========================================================================
        // PRIORITY 10: Momentum Break (UPWARD_MOMENTUM_BREAK, DOWNWARD_MOMENTUM_BREAK)
        // Percentage movement crosses documented threshold and persists
        // =========================================================================
        val isUpwardMomentumBreak = val5m >= MOMENTUM_BREAK_5M_THRESHOLD &&
            netShort >= MOMENTUM_BREAK_NET_THRESHOLD &&
            (confirmedHistory.isNotEmpty() && (prev5m != null && prev5m >= 0.20 || confirmedHistory[0].val5m + confirmedHistory[0].val60m > 0.0))

        if (isUpwardMomentumBreak) {
            return finalizeResult(
                code = "UPWARD_MOMENTUM_BREAK",
                title = "ঊর্ধ্বমুখী মোমেন্টাম ব্রেক — তীব্র চাপ",
                subtitle = "শতাংশের গতি নির্ধারিত মোমেন্টাম স্তর (+০.৬০%) অতিক্রম করেছে এবং স্থায়িত্ব পেয়েছে",
                tags = listOf("MOMENTUM_BREAK", "UP_MOMENTUM", "HIGH_PRESSURE"),
                isWarningOnly = false,
                stage = ConfirmationStage.CONFIRMED,
                bias = NextMovementBias.UP
            )
        }

        val isDownwardMomentumBreak = val5m <= -MOMENTUM_BREAK_5M_THRESHOLD &&
            netShort <= -MOMENTUM_BREAK_NET_THRESHOLD &&
            (confirmedHistory.isNotEmpty() && (prev5m != null && prev5m <= -0.20 || confirmedHistory[0].val5m + confirmedHistory[0].val60m < 0.0))

        if (isDownwardMomentumBreak) {
            return finalizeResult(
                code = "DOWNWARD_MOMENTUM_BREAK",
                title = "নিম্নমুখী মোমেন্টাম ব্রেক — তীব্র পতন",
                subtitle = "শতাংশের গতি নির্ধারিত মোমেন্টাম স্তর (-০.৬০%) অতিক্রম করেছে এবং স্থায়িত্ব পেয়েছে",
                tags = listOf("MOMENTUM_BREAK", "DOWN_MOMENTUM", "HIGH_PRESSURE"),
                isWarningOnly = false,
                stage = ConfirmationStage.CONFIRMED,
                bias = NextMovementBias.DOWN
            )
        }

        // =========================================================================
        // PRIORITY 11: Multi-Timeframe Aligned & Speed-Ratio Movements
        // (STEADY_ALIGNED_UP, STEADY_ALIGNED_DOWN with acceleration, deceleration, triple-confluence & daily context)
        // =========================================================================
        if (val5m > 0.0 && val60m > 0.0) {
            val isTripleStrongUp = val1d != null && val1d >= 0.20 && val5m >= 0.20 && val60m >= 0.20
            val isDailyBearishSqueeze = val1d != null && val1d <= -0.30
            val isUpAcceleration = val5m > val60m * 1.25 && val5m >= 0.20
            val isUpDeceleration = val5m < val60m * 0.75 && val60m >= 0.15

            val title = when {
                isTripleStrongUp -> "ত্রি-টাইমফ্রেম সুসংহত তীব্র উত্থান — ফুল বুলিশ প্রবাহ"
                isDailyBearishSqueeze -> "দৈনিক ডাউনট্রেন্ডে শর্ট স্কুইজ — রিজেকশন ঝুঁকি"
                isUpAcceleration -> "তীব্র আপ গতি বৃদ্ধি — উত্থান ত্বরান্বিত হচ্ছে"
                isUpDeceleration -> "উপরে ওঠার গতি মন্থর — UP চাপ হ্রাস ও পুলব্যাক ঝুঁকি"
                else -> "স্থিতিশীল গতিতে উপরে ওঠার ট্রেন্ড"
            }
            val subtitle = when {
                isTripleStrongUp -> "৫মি, ৬০মি এবং ১দিন তিন টাইমফ্রেমেই একযোগে শক্তিশালী UP প্রেসার; মার্কেট সম্পূর্ণরূপে ঊর্ধ্বমুখী ধারায় রয়েছে"
                isDailyBearishSqueeze -> "১দিনের ট্রেন্ড ভারী DOWN হলেও স্বল্পমেয়াদে সাময়িক আপ স্কুইজ চলছে; মেজরের রেজিস্ট্যান্সে বাধা পেয়ে পুনরায় তীব্র পতনের ঝুঁকি"
                isUpAcceleration -> "৬০ মিনিটের তুলনায় ৫ মিনিটে বৃদ্ধির বেগ আরও বৃদ্ধি পেয়েছে; ক্রেতাদের আগ্রাসী বাই প্রেসারে দ্রুত উপরে উঠছে"
                isUpDeceleration -> "৬০ মিনিটের তুলনায় ৫ মিনিটে বৃদ্ধির বেগ কমে এসেছে; যেকোনো সময় সাময়িক DOWN পুলব্যাক বা রিভার্সাল হতে পারে"
                else -> "৫ মিনিট ও ৬০ মিনিটের চাপ একই দিকে; বর্তমান ঊর্ধ্বমুখী গতি বজায় রয়েছে"
            }
            val tags = mutableListOf("STEADY_UP", "UPTREND", "ALIGNED_UP")
            if (isTripleStrongUp) tags.add("TRIPLE_ALIGNED_STRONG_UP")
            if (isDailyBearishSqueeze) tags.add("DAILY_BEARISH_SQUEEZE")
            if (isUpAcceleration) tags.add("ACCELERATION_UP")
            if (isUpDeceleration) {
                tags.add("DECELERATION_UP")
                tags.add("PULLBACK_RISK")
            }

            return finalizeResult(
                code = "STEADY_ALIGNED_UP",
                title = title,
                subtitle = subtitle,
                tags = tags,
                isWarningOnly = false,
                stage = ConfirmationStage.CONFIRMED,
                bias = NextMovementBias.UP
            )
        }

        if (val5m < 0.0 && val60m < 0.0) {
            val isTripleStrongDown = val1d != null && val1d <= -0.20 && val5m <= -0.20 && val60m <= -0.20
            val isDailyBullishDip = val1d != null && val1d >= 0.30
            val isDownAcceleration = abs5m > abs60m * 1.25 && abs5m >= 0.20
            val isDownDeceleration = abs5m < abs60m * 0.75 && abs60m >= 0.15

            val title = when {
                isTripleStrongDown -> "ত্রি-টাইমফ্রেম সুসংহত তীব্র পতন — ফুল বিয়ারিশ প্রবাহ"
                isDailyBullishDip -> "দৈনিক আপট্রেন্ডে ডিপ কারেকশন — রিবাউন্ড সম্ভাবনা"
                isDownAcceleration -> "তীব্র ডাউন গতি বৃদ্ধি — পতন ত্বরান্বিত হচ্ছে"
                isDownDeceleration -> "নিচে নামার গতি মন্থর — DOWN চাপ হ্রাস ও বাউন্স ঝুঁকি"
                else -> "স্থিতিশীল গতিতে নিচে নামার ট্রেন্ড"
            }
            val subtitle = when {
                isTripleStrongDown -> "৫মি, ৬০মি এবং ১দিন তিন টাইমফ্রেমেই একযোগে শক্তিশালী DOWN প্রেসার; মার্কেট সম্পূর্ণরূপে নিম্নমুখী ধারায় রয়েছে"
                isDailyBullishDip -> "১দিনের ট্রেন্ড শক্তিশালী UP হলেও ইন্ট্রাডে (৫মি ও ৬০মি) ডিপ কারেকশনে রয়েছে; মেজরের সাপোর্টে রিবাউন্ডের সম্ভাবনা"
                isDownAcceleration -> "৬০ মিনিটের তুলনায় ৫ মিনিটে পতনের বেগ আরও বৃদ্ধি পেয়েছে; বিক্রেতাদের আগ্রাসী সেল প্রেসারে দ্রুত নিচে নামছে"
                isDownDeceleration -> "৬০ মিনিটের তুলনায় ৫ মিনিটে পতনের বেগ কমে এসেছে; যেকোনো সময় সাময়িক UP বাউন্স বা রিভার্সাল হতে পারে"
                else -> "৫ মিনিট ও ৬০ মিনিটের চাপ একই দিকে; বর্তমান নিম্নমুখী পতন বজায় রয়েছে"
            }
            val tags = mutableListOf("STEADY_DOWN", "DOWNTREND", "ALIGNED_DOWN")
            if (isTripleStrongDown) tags.add("TRIPLE_ALIGNED_STRONG_DOWN")
            if (isDailyBullishDip) tags.add("DAILY_BULLISH_DIP")
            if (isDownAcceleration) tags.add("ACCELERATION_DOWN")
            if (isDownDeceleration) {
                tags.add("DECELERATION_DOWN")
                tags.add("BOUNCE_RISK")
            }

            return finalizeResult(
                code = "STEADY_ALIGNED_DOWN",
                title = title,
                subtitle = subtitle,
                tags = tags,
                isWarningOnly = false,
                stage = ConfirmationStage.CONFIRMED,
                bias = NextMovementBias.DOWN
            )
        }

        // =========================================================================
        // PRIORITY 12: Direction Conflict (DIRECTION_CONFLICT)
        // =========================================================================
        if (val5m * val60m < 0.0 || (abs(netShort) < 0.15 && totalMagnitude > 0.20)) {
            return finalizeResult(
                code = "DIRECTION_CONFLICT",
                title = "বিপরীতমুখী প্রবণতা — টাইমফ্রেমের দ্বন্দ্ব",
                subtitle = "৫ মিনিট ও ৬০ মিনিটের চাপ বিপরীতমুখী; বাজার এখনো কোনো এক দিকে স্থির হয়নি",
                tags = listOf("VOLATILE", "CHOPPY", "CONFLICT"),
                isWarningOnly = true,
                stage = ConfirmationStage.WARNING,
                bias = NextMovementBias.NEUTRAL
            )
        }

        // =========================================================================
        // PRIORITY 13: Neutral Sideways (NEUTRAL_SIDEWAYS)
        // =========================================================================
        return finalizeResult(
            code = "NEUTRAL_SIDEWAYS",
            title = "নিরপেক্ষ সাইডওয়েজ মার্কেট — স্পষ্ট মুভমেন্ট নেই",
            subtitle = "বাজার কোনো স্পষ্ট দিকে যাচ্ছে না; সাইডওয়েজ বা অনিশ্চিত রেঞ্জ",
            tags = listOf("NEUTRAL", "SIDEWAYS"),
            isWarningOnly = false,
            stage = ConfirmationStage.CONFIRMED,
            bias = NextMovementBias.NEUTRAL
        )
    }
}
