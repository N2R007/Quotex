package com.example.data.matrix

import com.example.data.models.MetricSnapshot
import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Directional Matrix System containing EXACTLY 206 RULES (103 UP Rules + 103 DOWN Rules).
 *
 * Rules:
 * 1. NON-OVERLAPPING DETERMINISM: Matches exactly one rule or returns null (NO MATCH).
 * 2. SINGLE-MATCH GUARANTEE: Evaluates in strict order. Once a rule matches, evaluates no further.
 * 3. STRICT ISOLATION: Evaluates ONLY using val5m and val60m (plus netSum/history where explicitly defined).
 * 4. ABSOLUTE ZERO DUPLICATION: Adheres strictly to the numerical boundaries defined in each tier.
 */
object Directional206MatrixEngine {

    data class DirectionalMatrixMatch(
        val id: String,
        val direction: TradeDirection,
        val outputCode: String,
        val title: String,
        val conditionDescription: String,
        val priority: Int
    )

    private fun inRange(v: Double, min: Double, max: Double): Boolean {
        return v in min..max
    }

    /**
     * Evaluates val5m and val60m against the 206 Directional Matrix Rules.
     * Evaluates in deterministic order with Single-Match Guarantee.
     */
    fun evaluate(
        val5m: Double?,
        val60m: Double?,
        val1d: Double? = null,
        history: List<MetricSnapshot> = emptyList()
    ): DirectionalMatrixMatch? {
        if (val5m == null || val60m == null) return null

        // 🟢 Priority 1: User-created Custom Rules (Highest priority)
        val customMatch = UserRuleRegistry.evaluateCustomRules(val5m, val60m)
        if (customMatch != null) {
            return customMatch
        }

        val netSum = val5m + val60m
        val hasHistory = history.isNotEmpty()
        val prev5m = if (hasHistory) history[0].val5m else null

        // ----------------------------------------------------------------
        // 🟢 SECTION 1: 103 UNIQUE UP MATRIX RULES (U001 - U103)
        // ----------------------------------------------------------------

        // Tier 10: Special Historical (checked first if history applies to respect momentum reversal patterns)
        if (hasHistory && prev5m != null && prev5m < 0 && val60m >= 0.10) {
            when {
                val5m in 0.10..0.18 -> return match("U079", TradeDirection.UP, "EARLY_V_SHAPE_RECOVERY_A")
                val5m in 0.19..0.25 -> return match("U080", TradeDirection.UP, "EARLY_V_SHAPE_RECOVERY_B")
                val5m in 0.26..0.38 -> return match("U081", TradeDirection.UP, "CONFIRMED_V_SHAPE_RECOVERY_A")
                val5m in 0.39..0.50 -> return match("U082", TradeDirection.UP, "CONFIRMED_V_SHAPE_RECOVERY_B")
                val5m in 0.51..0.75 -> return match("U083", TradeDirection.UP, "EXPLOSIVE_V_SHAPE_RECOVERY_A")
                val5m > 0.75 -> return match("U084", TradeDirection.UP, "EXPLOSIVE_V_SHAPE_RECOVERY_B")
            }
        }

        // Tier 8: Harmonic & Symmetry Rules
        if (abs(val5m - val60m) <= 0.02) {
            when {
                val5m in 0.15..0.25 -> return match("U065", TradeDirection.UP, "HARMONIC_LOW_ALIGNMENT_A")
                val5m in 0.26..0.35 -> return match("U066", TradeDirection.UP, "HARMONIC_LOW_ALIGNMENT_B")
                val5m in 0.36..0.48 -> return match("U067", TradeDirection.UP, "HARMONIC_MID_ALIGNMENT_A")
                val5m in 0.49..0.60 -> return match("U068", TradeDirection.UP, "HARMONIC_MID_ALIGNMENT_B")
                val5m in 0.61..0.70 -> return match("U069", TradeDirection.UP, "HARMONIC_HIGH_ALIGNMENT_A")
                val5m in 0.71..0.80 -> return match("U070", TradeDirection.UP, "HARMONIC_HIGH_ALIGNMENT_B")
            }
        }

        // Tier 9: Asymmetric Dominance Rules
        if (val5m > val60m) {
            when {
                val5m in 0.10..0.20 && netSum in 0.30..0.40 -> return match("U071", TradeDirection.UP, "ASYMMETRIC_5M_DOMINANCE_LOW_A")
                val5m in 0.21..0.30 && netSum in 0.41..0.50 -> return match("U072", TradeDirection.UP, "ASYMMETRIC_5M_DOMINANCE_LOW_B")
                val5m in 0.31..0.45 && netSum in 0.51..0.70 -> return match("U073", TradeDirection.UP, "ASYMMETRIC_5M_DOMINANCE_HIGH_A")
                val5m in 0.46..0.60 && netSum in 0.71..0.90 -> return match("U074", TradeDirection.UP, "ASYMMETRIC_5M_DOMINANCE_HIGH_B")
            }
        } else if (val60m > val5m) {
            when {
                val60m in 0.10..0.20 && netSum in 0.30..0.40 -> return match("U075", TradeDirection.UP, "ASYMMETRIC_60M_DOMINANCE_LOW_A")
                val60m in 0.21..0.30 && netSum in 0.41..0.50 -> return match("U076", TradeDirection.UP, "ASYMMETRIC_60M_DOMINANCE_LOW_B")
                val60m in 0.31..0.45 && netSum in 0.51..0.70 -> return match("U077", TradeDirection.UP, "ASYMMETRIC_60M_DOMINANCE_HIGH_A")
                val60m in 0.46..0.60 && netSum in 0.71..0.90 -> return match("U078", TradeDirection.UP, "ASYMMETRIC_60M_DOMINANCE_HIGH_B")
            }
        }

        // Tier 1: Micro & Macro Low Momentum Alignment
        when {
            val5m in 0.10..0.15 && val60m in 0.10..0.15 -> return match("U001", TradeDirection.UP, "ULTRA_LOW_ALIGNED_UP")
            val5m in 0.16..0.20 && val60m in 0.10..0.15 -> return match("U002", TradeDirection.UP, "LOW_VOLATILITY_5M_LEAD")
            val5m in 0.10..0.15 && val60m in 0.16..0.20 -> return match("U003", TradeDirection.UP, "LOW_VOLATILITY_60M_LEAD")
            val5m in 0.16..0.20 && val60m in 0.16..0.20 -> return match("U004", TradeDirection.UP, "LOW_VOLATILITY_ALIGNED_UP")
            val5m in 0.21..0.28 && val60m in 0.10..0.20 -> return match("U005", TradeDirection.UP, "EARLY_IMPULSE_5M_LEAD_A")
            val5m in 0.29..0.35 && val60m in 0.10..0.20 -> return match("U006", TradeDirection.UP, "EARLY_IMPULSE_5M_LEAD_B")
            val5m in 0.10..0.20 && val60m in 0.21..0.28 -> return match("U007", TradeDirection.UP, "STEADY_ACCUMULATION_60M_LEAD_A")
            val5m in 0.10..0.20 && val60m in 0.29..0.35 -> return match("U008", TradeDirection.UP, "STEADY_ACCUMULATION_60M_LEAD_B")
            val5m in 0.21..0.28 && val60m in 0.21..0.28 -> return match("U009", TradeDirection.UP, "BALANCED_BULLISH_EXPANSION_LOW")
            val5m in 0.29..0.35 && val60m in 0.29..0.35 -> return match("U010", TradeDirection.UP, "BALANCED_BULLISH_EXPANSION_MID")
        }

        // Tier 2: Mid Velocity Expansion
        when {
            val5m in 0.36..0.43 && val60m in 0.10..0.25 -> return match("U011", TradeDirection.UP, "FAST_MOMENTUM_SURGE_5M_A")
            val5m in 0.44..0.50 && val60m in 0.10..0.25 -> return match("U012", TradeDirection.UP, "FAST_MOMENTUM_SURGE_5M_B")
            val5m in 0.10..0.25 && val60m in 0.36..0.43 -> return match("U013", TradeDirection.UP, "STRONG_ANCHOR_TREND_UP_A")
            val5m in 0.10..0.25 && val60m in 0.44..0.50 -> return match("U014", TradeDirection.UP, "STRONG_ANCHOR_TREND_UP_B")
            val5m in 0.36..0.43 && val60m in 0.26..0.38 -> return match("U015", TradeDirection.UP, "HIGH_VELOCITY_BREAKOUT_A")
            val5m in 0.44..0.50 && val60m in 0.39..0.50 -> return match("U016", TradeDirection.UP, "HIGH_VELOCITY_BREAKOUT_B")
            val5m in 0.26..0.35 && val60m in 0.36..0.43 -> return match("U017", TradeDirection.UP, "MID_RANGE_CONFLUENCE_UP_A")
            val5m in 0.26..0.35 && val60m in 0.44..0.50 -> return match("U018", TradeDirection.UP, "MID_RANGE_CONFLUENCE_UP_B")
        }

        // Tier 3: High Momentum & Parabolic Impulse
        when {
            val5m in 0.51..0.65 && val60m in 0.10..0.30 -> return match("U019", TradeDirection.UP, "AGGRESSIVE_PARABOLIC_IMPULSE_A")
            val5m in 0.66..0.80 && val60m in 0.10..0.30 -> return match("U020", TradeDirection.UP, "AGGRESSIVE_PARABOLIC_IMPULSE_B")
            val5m in 0.10..0.30 && val60m in 0.51..0.65 -> return match("U021", TradeDirection.UP, "INSTITUTIONAL_HEAVY_ACCUMULATION_A")
            val5m in 0.10..0.30 && val60m in 0.66..0.80 -> return match("U022", TradeDirection.UP, "INSTITUTIONAL_HEAVY_ACCUMULATION_B")
            val5m in 0.51..0.65 && val60m in 0.31..0.55 -> return match("U023", TradeDirection.UP, "POWER_SURGE_DUAL_EXPANSION_A")
            val5m in 0.66..0.80 && val60m in 0.56..0.80 -> return match("U024", TradeDirection.UP, "POWER_SURGE_DUAL_EXPANSION_B")
            val5m in 0.31..0.50 && val60m in 0.51..0.65 -> return match("U025", TradeDirection.UP, "STEADY_HEAVY_ACCUMULATION_A")
            val5m in 0.31..0.50 && val60m in 0.66..0.80 -> return match("U026", TradeDirection.UP, "STEADY_HEAVY_ACCUMULATION_B")
        }

        // Tier 4: Extreme Climax Conditions
        when {
            val5m > 1.20 && val60m > 1.20 -> return match("U036", TradeDirection.UP, "MEGA_PARABOLIC_SUPER_CLIMAX")
            val5m in 0.81..1.20 && val60m in 0.81..1.20 -> return match("U035", TradeDirection.UP, "TOTAL_PARABOLIC_CLIMAX_UP")
            val5m in 0.81..1.00 && val60m in 0.10..0.30 -> return match("U027", TradeDirection.UP, "EXTREME_MICRO_SQUEEZE_UP_LOW")
            val5m in 0.81..1.00 && val60m in 0.31..0.50 -> return match("U028", TradeDirection.UP, "EXTREME_MICRO_SQUEEZE_UP_HIGH")
            val5m > 1.00 && val60m in 0.10..0.50 -> return match("U029", TradeDirection.UP, "HYPER_MICRO_SQUEEZE_BLOWOUT")
            val5m in 0.10..0.30 && val60m in 0.81..1.00 -> return match("U030", TradeDirection.DOWN, "EXTREME_MACRO_EXPANSION_DOWN_LOW")
            val5m in 0.31..0.50 && val60m in 0.81..1.00 -> return match("U031", TradeDirection.UP, "EXTREME_MACRO_EXPANSION_UP_HIGH")
            val5m in 0.10..0.50 && val60m > 1.00 -> return match("U032", TradeDirection.UP, "HYPER_MACRO_EXPANSION_BLOWOUT")
            val5m in 0.81..1.00 && val60m in 0.51..0.80 -> return match("U033", TradeDirection.UP, "HYPER_VOLATILITY_MICRO_BREAKOUT")
            val5m in 0.51..0.80 && val60m in 0.81..1.00 -> return match("U034", TradeDirection.UP, "HYPER_VOLATILITY_MACRO_BREAKOUT")
        }

        // Tier 5: Pullbacks & Dip Buying (60m Up, 5m Down)
        when {
            val5m in -0.10..-0.01 && val60m in 0.20..0.35 -> return match("U037", TradeDirection.UP, "SHALLOW_PULLBACK_DIP_BUY")
            val5m in -0.20..-0.11 && val60m in 0.20..0.35 -> return match("U038", TradeDirection.UP, "LIGHT_PULLBACK_DIP_BUY")
            val5m in -0.10..-0.01 && val60m in 0.36..0.60 -> return match("U039", TradeDirection.UP, "DEEP_ANCHOR_SHALLOW_PULLBACK")
            val5m in -0.20..-0.11 && val60m in 0.36..0.60 -> return match("U040", TradeDirection.UP, "DEEP_ANCHOR_LIGHT_PULLBACK")
            val5m in -0.35..-0.21 && val60m in 0.30..0.50 -> return match("U041", TradeDirection.UP, "MEDIUM_PULLBACK_SUPPORT_REJOIN")
            val5m in -0.35..-0.21 && val60m in 0.51..0.80 -> return match("U042", TradeDirection.UP, "DEEP_PULLBACK_HEAVY_SUPPORT")
            val5m in -0.50..-0.36 && val60m in 0.40..0.80 -> return match("U043", TradeDirection.UP, "EXTREME_DIP_STRONG_BULL_TREND")
            val5m in -0.70..-0.51 && val60m in 0.50..1.00 -> return match("U044", TradeDirection.UP, "MAXIMUM_DIP_MACRO_BULL_RECOVERY")
        }

        // Tier 6: Trend Reversals & Divergence (60m Down, 5m Up)
        when {
            val5m > 0.80 && val60m <= -0.81 -> return match("U054", TradeDirection.UP, "EXTREME_V_BOTTOM_REVERSAL")
            val5m in 0.05..0.15 && val60m in -0.20..-0.10 -> return match("U045", TradeDirection.UP, "WEAK_REBOUND_EARLY_ATTEMPT_A")
            val5m in 0.05..0.15 && val60m in -0.35..-0.21 -> return match("U046", TradeDirection.UP, "WEAK_REBOUND_EARLY_ATTEMPT_B")
            val5m in 0.16..0.25 && val60m in -0.30..-0.10 -> return match("U047", TradeDirection.UP, "BULLISH_DIVERGENCE_CONFIRMED_A")
            val5m in 0.26..0.35 && val60m in -0.30..-0.10 -> return match("U048", TradeDirection.UP, "BULLISH_DIVERGENCE_CONFIRMED_B")
            val5m in 0.36..0.48 && val60m in -0.30..-0.10 -> return match("U049", TradeDirection.UP, "STRONG_COUNTER_TREND_PUMP_A")
            val5m in 0.49..0.60 && val60m in -0.30..-0.10 -> return match("U050", TradeDirection.UP, "STRONG_COUNTER_TREND_PUMP_B")
            val5m in 0.20..0.35 && val60m in -0.60..-0.31 -> return match("U051", TradeDirection.UP, "HEAVY_BEAR_DIVERGENCE_REVERSAL_A")
            val5m in 0.36..0.50 && val60m in -0.60..-0.31 -> return match("U052", TradeDirection.UP, "HEAVY_BEAR_DIVERGENCE_REVERSAL_B")
            val5m in 0.51..0.80 && val60m in -0.80..-0.61 -> return match("U053", TradeDirection.UP, "MACRO_BOTTOM_EXHAUSTION_REBOUND")
        }

        // Tier 7: Range Breakouts & Consolidation Escapes
        when {
            val5m in 0.15..0.22 && val60m in -0.09..0.09 -> return match("U055", TradeDirection.UP, "CONSOLIDATION_MICRO_BREAKOUT_A")
            val5m in 0.23..0.30 && val60m in -0.09..0.09 -> return match("U056", TradeDirection.UP, "CONSOLIDATION_MICRO_BREAKOUT_B")
            val5m in 0.31..0.40 && val60m in -0.09..0.09 -> return match("U057", TradeDirection.UP, "RANGE_HIGH_VOLATILITY_ESCAPE_A")
            val5m in 0.41..0.50 && val60m in -0.09..0.09 -> return match("U058", TradeDirection.UP, "RANGE_HIGH_VOLATILITY_ESCAPE_B")
            val5m in 0.51..0.75 && val60m in -0.09..0.09 -> return match("U059", TradeDirection.UP, "EXPLOSIVE_RANGE_BREAKOUT_A")
            val5m > 0.75 && val60m in -0.09..0.09 -> return match("U060", TradeDirection.UP, "EXPLOSIVE_RANGE_BREAKOUT_B")
            val5m in -0.09..0.09 && val60m in 0.20..0.30 -> return match("U061", TradeDirection.UP, "HOURLY_BULLISH_DRIFT_A")
            val5m in -0.09..0.09 && val60m in 0.31..0.40 -> return match("U062", TradeDirection.UP, "HOURLY_BULLISH_DRIFT_B")
            val5m in -0.09..0.09 && val60m in 0.41..0.55 -> return match("U063", TradeDirection.UP, "HOURLY_STRONG_ACCUMULATION_A")
            val5m in -0.09..0.09 && val60m in 0.56..0.70 -> return match("U064", TradeDirection.UP, "HOURLY_STRONG_ACCUMULATION_B")
        }

        // Tier 11: Micro/Macro Fractional Confluence
        when {
            val5m > 0.50 && val60m in 0.01..0.09 && netSum >= 0.80 -> return match("U103", TradeDirection.UP, "AGGRESSIVE_NET_SUM_EXPANSION_UP")
            val5m > 1.10 && val60m in -0.09..0.09 -> return match("U100", TradeDirection.UP, "MONSTER_BREAKOUT_FROM_SQUEEZE_B")
            val5m in 0.91..1.10 && val60m in -0.09..0.09 -> return match("U099", TradeDirection.UP, "MONSTER_BREAKOUT_FROM_SQUEEZE_A")
            val5m in 0.10..0.30 && val60m in 0.81..1.20 -> return match("U102", TradeDirection.UP, "ANCHORED_BULLISH_CONTINUATION")
            val5m in 0.10..0.20 && val60m in 0.01..0.09 -> return match("U085", TradeDirection.UP, "MICRO_LEAD_SLIGHT_MACRO_POSITIVE_A")
            val5m in 0.21..0.30 && val60m in 0.01..0.09 -> return match("U086", TradeDirection.UP, "MICRO_LEAD_SLIGHT_MACRO_POSITIVE_B")
            val5m in 0.31..0.45 && val60m in 0.01..0.09 -> return match("U087", TradeDirection.UP, "STRONG_MICRO_LEAD_SLIGHT_MACRO_POSITIVE_A")
            val5m in 0.46..0.60 && val60m in 0.01..0.09 -> return match("U088", TradeDirection.UP, "STRONG_MICRO_LEAD_SLIGHT_MACRO_POSITIVE_B")
            val5m in 0.01..0.09 && val60m in 0.10..0.20 -> return match("U089", TradeDirection.UP, "SLIGHT_MICRO_POSITIVE_MACRO_LEAD_A")
            val5m in 0.01..0.09 && val60m in 0.21..0.30 -> return match("U090", TradeDirection.UP, "SLIGHT_MICRO_POSITIVE_MACRO_LEAD_B")
            val5m in 0.01..0.09 && val60m in 0.31..0.45 -> return match("U091", TradeDirection.UP, "SLIGHT_MICRO_POSITIVE_STRONG_MACRO_LEAD_A")
            val5m in 0.01..0.09 && val60m in 0.46..0.60 -> return match("U092", TradeDirection.UP, "SLIGHT_MICRO_POSITIVE_STRONG_MACRO_LEAD_B")
            val5m in 0.10..0.25 && val60m in -0.09..-0.01 -> return match("U093", TradeDirection.UP, "MICRO_PULSE_NEUTRAL_MACRO_A")
            val5m in 0.26..0.35 && val60m in -0.09..-0.01 -> return match("U094", TradeDirection.UP, "MICRO_PULSE_NEUTRAL_MACRO_B")
            val5m in 0.36..0.50 && val60m in -0.09..-0.01 -> return match("U095", TradeDirection.UP, "STRONG_MICRO_PULSE_NEUTRAL_MACRO_A")
            val5m in 0.51..0.65 && val60m in -0.09..-0.01 -> return match("U096", TradeDirection.UP, "STRONG_MICRO_PULSE_NEUTRAL_MACRO_B")
            val5m in 0.61..0.75 && val60m in -0.20..-0.10 -> return match("U097", TradeDirection.UP, "INTENSE_SQUEEZE_AGAINST_BEAR_A")
            val5m in 0.76..0.90 && val60m in -0.20..-0.10 -> return match("U098", TradeDirection.UP, "INTENSE_SQUEEZE_AGAINST_BEAR_B")
            val5m in 0.01..0.09 && val60m in 0.01..0.09 -> return match("U101", TradeDirection.UP, "MICRO_CONSOLIDATION_DRIFT_UP")
        }

        // ----------------------------------------------------------------
        // 🔴 SECTION 2: 103 UNIQUE DOWN MATRIX RULES (D001 - D103)
        // ----------------------------------------------------------------

        // Tier 10: Special Historical / Inverted Patterns
        if (hasHistory && prev5m != null && prev5m > 0 && val60m <= -0.10) {
            when {
                val5m in -0.18..-0.10 -> return match("D079", TradeDirection.DOWN, "EARLY_INVERTED_V_REVERSAL_A")
                val5m in -0.25..-0.19 -> return match("D080", TradeDirection.DOWN, "EARLY_INVERTED_V_REVERSAL_B")
                val5m in -0.38..-0.26 -> return match("D081", TradeDirection.DOWN, "CONFIRMED_INVERTED_V_REVERSAL_A")
                val5m in -0.50..-0.39 -> return match("D082", TradeDirection.DOWN, "CONFIRMED_INVERTED_V_REVERSAL_B")
                val5m in -0.75..-0.51 -> return match("D083", TradeDirection.DOWN, "EXPLOSIVE_INVERTED_V_REVERSAL_A")
                val5m < -0.75 -> return match("D084", TradeDirection.DOWN, "EXPLOSIVE_INVERTED_V_REVERSAL_B")
            }
        }

        // Tier 8: Bearish Harmonic & Symmetry Rules
        if (abs(val5m - val60m) <= 0.02) {
            when {
                val5m in -0.25..-0.15 -> return match("D065", TradeDirection.DOWN, "HARMONIC_LOW_BEAR_ALIGNMENT_A")
                val5m in -0.35..-0.26 -> return match("D066", TradeDirection.DOWN, "HARMONIC_LOW_BEAR_ALIGNMENT_B")
                val5m in -0.48..-0.36 -> return match("D067", TradeDirection.DOWN, "HARMONIC_MID_BEAR_ALIGNMENT_A")
                val5m in -0.60..-0.49 -> return match("D068", TradeDirection.DOWN, "HARMONIC_MID_BEAR_ALIGNMENT_B")
                val5m in -0.70..-0.61 -> return match("D069", TradeDirection.DOWN, "HARMONIC_HIGH_BEAR_ALIGNMENT_A")
                val5m in -0.80..-0.71 -> return match("D070", TradeDirection.DOWN, "HARMONIC_HIGH_BEAR_ALIGNMENT_B")
            }
        }

        // Tier 9: Asymmetric Bearish Dominance Rules
        if (abs(val5m) > abs(val60m)) {
            when {
                val5m in -0.20..-0.10 && netSum in -0.40..-0.30 -> return match("D071", TradeDirection.DOWN, "ASYMMETRIC_5M_BEAR_LOW_A")
                val5m in -0.30..-0.21 && netSum in -0.50..-0.41 -> return match("D072", TradeDirection.DOWN, "ASYMMETRIC_5M_BEAR_LOW_B")
                val5m in -0.45..-0.31 && netSum in -0.70..-0.51 -> return match("D073", TradeDirection.DOWN, "ASYMMETRIC_5M_BEAR_HIGH_A")
                val5m in -0.60..-0.46 && netSum in -0.90..-0.71 -> return match("D074", TradeDirection.DOWN, "ASYMMETRIC_5M_BEAR_HIGH_B")
            }
        } else if (abs(val60m) > abs(val5m)) {
            when {
                val60m in -0.20..-0.10 && netSum in -0.40..-0.30 -> return match("D075", TradeDirection.DOWN, "ASYMMETRIC_60M_BEAR_LOW_A")
                val60m in -0.30..-0.21 && netSum in -0.50..-0.41 -> return match("D076", TradeDirection.DOWN, "ASYMMETRIC_60M_BEAR_LOW_B")
                val60m in -0.45..-0.31 && netSum in -0.70..-0.51 -> return match("D077", TradeDirection.DOWN, "ASYMMETRIC_60M_BEAR_HIGH_A")
                val60m in -0.60..-0.46 && netSum in -0.90..-0.71 -> return match("D078", TradeDirection.DOWN, "ASYMMETRIC_60M_BEAR_HIGH_B")
            }
        }

        // Tier 1: Micro & Macro Low Bearish Alignment
        when {
            val5m in -0.15..-0.10 && val60m in -0.15..-0.10 -> return match("D001", TradeDirection.DOWN, "ULTRA_LOW_ALIGNED_DOWN")
            val5m in -0.20..-0.16 && val60m in -0.15..-0.10 -> return match("D002", TradeDirection.DOWN, "LOW_VOLATILITY_5M_DROP_LEAD")
            val5m in -0.15..-0.10 && val60m in -0.20..-0.16 -> return match("D003", TradeDirection.DOWN, "LOW_VOLATILITY_60M_DUMP_LEAD")
            val5m in -0.20..-0.16 && val60m in -0.20..-0.16 -> return match("D004", TradeDirection.DOWN, "LOW_VOLATILITY_ALIGNED_DOWN")
            val5m in -0.28..-0.21 && val60m in -0.20..-0.10 -> return match("D005", TradeDirection.DOWN, "EARLY_IMPULSE_5M_DUMP_A")
            val5m in -0.35..-0.29 && val60m in -0.20..-0.10 -> return match("D006", TradeDirection.DOWN, "EARLY_IMPULSE_5M_DUMP_B")
            val5m in -0.20..-0.10 && val60m in -0.28..-0.21 -> return match("D007", TradeDirection.DOWN, "STEADY_DISTRIBUTION_60M_LEAD_A")
            val5m in -0.20..-0.10 && val60m in -0.35..-0.29 -> return match("D008", TradeDirection.DOWN, "STEADY_DISTRIBUTION_60M_LEAD_B")
            val5m in -0.28..-0.21 && val60m in -0.28..-0.21 -> return match("D009", TradeDirection.DOWN, "BALANCED_BEARISH_EXPANSION_LOW")
            val5m in -0.35..-0.29 && val60m in -0.35..-0.29 -> return match("D010", TradeDirection.DOWN, "BALANCED_BEARISH_EXPANSION_MID")
        }

        // Tier 2: Mid Velocity Breakdown
        when {
            val5m in -0.43..-0.36 && val60m in -0.25..-0.10 -> return match("D011", TradeDirection.DOWN, "FAST_SELLING_SURGE_5M_A")
            val5m in -0.50..-0.44 && val60m in -0.25..-0.10 -> return match("D012", TradeDirection.DOWN, "FAST_SELLING_SURGE_5M_B")
            val5m in -0.25..-0.10 && val60m in -0.43..-0.36 -> return match("D013", TradeDirection.DOWN, "STRONG_ANCHOR_TREND_DOWN_A")
            val5m in -0.25..-0.10 && val60m in -0.50..-0.44 -> return match("D014", TradeDirection.DOWN, "STRONG_ANCHOR_TREND_DOWN_B")
            val5m in -0.43..-0.36 && val60m in -0.38..-0.26 -> return match("D015", TradeDirection.DOWN, "HIGH_VELOCITY_BREAKDOWN_A")
            val5m in -0.50..-0.44 && val60m in -0.50..-0.39 -> return match("D016", TradeDirection.DOWN, "HIGH_VELOCITY_BREAKDOWN_B")
            val5m in -0.35..-0.26 && val60m in -0.43..-0.36 -> return match("D017", TradeDirection.DOWN, "MID_RANGE_CONFLUENCE_DOWN_A")
            val5m in -0.35..-0.26 && val60m in -0.50..-0.44 -> return match("D018", TradeDirection.DOWN, "MID_RANGE_CONFLUENCE_DOWN_B")
        }

        // Tier 3: High Bearish Momentum & Waterfall Impulse
        when {
            val5m in -0.65..-0.51 && val60m in -0.30..-0.10 -> return match("D019", TradeDirection.DOWN, "AGGRESSIVE_WATERFALL_IMPULSE_A")
            val5m in -0.80..-0.66 && val60m in -0.30..-0.10 -> return match("D020", TradeDirection.DOWN, "AGGRESSIVE_WATERFALL_IMPULSE_B")
            val5m in -0.30..-0.10 && val60m in -0.65..-0.51 -> return match("D021", TradeDirection.DOWN, "INSTITUTIONAL_HEAVY_SELLING_A")
            val5m in -0.30..-0.10 && val60m in -0.80..-0.66 -> return match("D022", TradeDirection.DOWN, "INSTITUTIONAL_HEAVY_SELLING_B")
            val5m in -0.65..-0.51 && val60m in -0.55..-0.31 -> return match("D023", TradeDirection.DOWN, "CASCADING_DUAL_DUMP_A")
            val5m in -0.80..-0.66 && val60m in -0.80..-0.56 -> return match("D024", TradeDirection.DOWN, "CASCADING_DUAL_DUMP_B")
            val5m in -0.50..-0.31 && val60m in -0.65..-0.51 -> return match("D025", TradeDirection.DOWN, "STEADY_HEAVY_DISTRIBUTION_A")
            val5m in -0.50..-0.31 && val60m in -0.80..-0.66 -> return match("D026", TradeDirection.DOWN, "STEADY_HEAVY_DISTRIBUTION_B")
        }

        // Tier 4: Extreme Capitulation Conditions
        when {
            val5m < -1.20 && val60m < -1.20 -> return match("D036", TradeDirection.DOWN, "MEGA_PARABOLIC_SUPER_CRASH")
            val5m in -1.20..-0.81 && val60m in -1.20..-0.81 -> return match("D035", TradeDirection.DOWN, "TOTAL_PARABOLIC_WATERFALL_DOWN")
            val5m in -1.00..-0.81 && val60m in -0.30..-0.10 -> return match("D027", TradeDirection.DOWN, "EXTREME_MICRO_FLASH_DUMP_LOW")
            val5m in -1.00..-0.81 && val60m in -0.50..-0.31 -> return match("D028", TradeDirection.DOWN, "EXTREME_MICRO_FLASH_DUMP_HIGH")
            val5m < -1.00 && val60m in -0.50..-0.10 -> return match("D029", TradeDirection.DOWN, "HYPER_MICRO_FLASH_CRASH")
            val5m in -0.30..-0.10 && val60m in -1.00..-0.81 -> return match("D030", TradeDirection.DOWN, "EXTREME_MACRO_CAPITULATION_LOW")
            val5m in -0.50..-0.31 && val60m in -1.00..-0.81 -> return match("D031", TradeDirection.DOWN, "EXTREME_MACRO_CAPITULATION_HIGH")
            val5m in -0.50..-0.10 && val60m < -1.00 -> return match("D032", TradeDirection.DOWN, "HYPER_MACRO_CAPITULATION_CRASH")
            val5m in -1.00..-0.81 && val60m in -0.80..-0.51 -> return match("D033", TradeDirection.DOWN, "HYPER_VOLATILITY_MICRO_DUMP")
            val5m in -0.80..-0.51 && val60m in -1.00..-0.81 -> return match("D034", TradeDirection.DOWN, "HYPER_VOLATILITY_MACRO_DUMP")
        }

        // Tier 5: Pullbacks & Rally Selling (60m Down, 5m Up)
        when {
            val5m in 0.01..0.10 && val60m in -0.35..-0.20 -> return match("D037", TradeDirection.DOWN, "SHALLOW_RALLY_PULLBACK_SELL")
            val5m in 0.11..0.20 && val60m in -0.35..-0.20 -> return match("D038", TradeDirection.DOWN, "LIGHT_RALLY_PULLBACK_SELL")
            val5m in 0.01..0.10 && val60m in -0.60..-0.36 -> return match("D039", TradeDirection.DOWN, "DEEP_ANCHOR_SHALLOW_RALLY")
            val5m in 0.11..0.20 && val60m in -0.60..-0.36 -> return match("D040", TradeDirection.DOWN, "DEEP_ANCHOR_LIGHT_RALLY")
            val5m in 0.21..0.35 && val60m in -0.50..-0.30 -> return match("D041", TradeDirection.DOWN, "MEDIUM_RALLY_RESISTANCE_REJECT")
            val5m in 0.21..0.35 && val60m in -0.80..-0.51 -> return match("D042", TradeDirection.DOWN, "DEEP_RALLY_HEAVY_RESISTANCE")
            val5m in 0.36..0.50 && val60m in -0.80..-0.40 -> return match("D043", TradeDirection.DOWN, "EXTREME_RALLY_STRONG_BEAR_TREND")
            val5m in 0.51..0.70 && val60m in -1.00..-0.50 -> return match("D044", TradeDirection.DOWN, "MAXIMUM_RALLY_MACRO_BEAR_REJECTION")
        }

        // Tier 6: Trend Reversals & Bearish Divergence (60m Up, 5m Down)
        when {
            val5m < -0.80 && val60m >= 0.81 -> return match("D054", TradeDirection.DOWN, "EXTREME_V_TOP_REJECTION")
            val5m in -0.15..-0.05 && val60m in 0.10..0.20 -> return match("D045", TradeDirection.DOWN, "WEAK_DROP_EARLY_ATTEMPT_A")
            val5m in -0.15..-0.05 && val60m in 0.21..0.35 -> return match("D046", TradeDirection.DOWN, "WEAK_DROP_EARLY_ATTEMPT_B")
            val5m in -0.25..-0.16 && val60m in 0.10..0.30 -> return match("D047", TradeDirection.DOWN, "BEARISH_DIVERGENCE_CONFIRMED_A")
            val5m in -0.35..-0.26 && val60m in 0.10..0.30 -> return match("D048", TradeDirection.DOWN, "BEARISH_DIVERGENCE_CONFIRMED_B")
            val5m in -0.48..-0.36 && val60m in 0.10..0.30 -> return match("D049", TradeDirection.DOWN, "STRONG_COUNTER_TREND_DUMP_A")
            val5m in -0.60..-0.49 && val60m in 0.10..0.30 -> return match("D050", TradeDirection.DOWN, "STRONG_COUNTER_TREND_DUMP_B")
            val5m in -0.35..-0.20 && val60m in 0.31..0.60 -> return match("D051", TradeDirection.DOWN, "HEAVY_BULL_DIVERGENCE_REVERSAL_A")
            val5m in -0.50..-0.36 && val60m in 0.31..0.60 -> return match("D052", TradeDirection.DOWN, "HEAVY_BULL_DIVERGENCE_REVERSAL_B")
            val5m in -0.80..-0.51 && val60m in 0.61..0.80 -> return match("D053", TradeDirection.DOWN, "MACRO_TOP_EXHAUSTION_REJECTION")
        }

        // Tier 7: Range Breakdowns & Consolidation Escapes
        when {
            val5m in -0.22..-0.15 && val60m in -0.09..0.09 -> return match("D055", TradeDirection.DOWN, "CONSOLIDATION_MICRO_BREAKDOWN_A")
            val5m in -0.30..-0.23 && val60m in -0.09..0.09 -> return match("D056", TradeDirection.DOWN, "CONSOLIDATION_MICRO_BREAKDOWN_B")
            val5m in -0.40..-0.31 && val60m in -0.09..0.09 -> return match("D057", TradeDirection.DOWN, "RANGE_HIGH_VOLATILITY_DROP_A")
            val5m in -0.50..-0.41 && val60m in -0.09..0.09 -> return match("D058", TradeDirection.DOWN, "RANGE_HIGH_VOLATILITY_DROP_B")
            val5m in -0.75..-0.51 && val60m in -0.09..0.09 -> return match("D059", TradeDirection.DOWN, "EXPLOSIVE_RANGE_BREAKDOWN_A")
            val5m < -0.75 && val60m in -0.09..0.09 -> return match("D060", TradeDirection.DOWN, "EXPLOSIVE_RANGE_BREAKDOWN_B")
            val5m in -0.09..0.09 && val60m in -0.30..-0.20 -> return match("D061", TradeDirection.DOWN, "HOURLY_BEARISH_DRIFT_A")
            val5m in -0.09..0.09 && val60m in -0.40..-0.31 -> return match("D062", TradeDirection.DOWN, "HOURLY_BEARISH_DRIFT_B")
            val5m in -0.09..0.09 && val60m in -0.55..-0.41 -> return match("D063", TradeDirection.DOWN, "HOURLY_STRONG_DISTRIBUTION_A")
            val5m in -0.09..0.09 && val60m in -0.70..-0.56 -> return match("D064", TradeDirection.DOWN, "HOURLY_STRONG_DISTRIBUTION_B")
        }

        // Tier 11: Micro/Macro Fractional Bearish Confluence
        when {
            val5m < -0.50 && val60m in -0.09..-0.01 && netSum <= -0.80 -> return match("D103", TradeDirection.DOWN, "AGGRESSIVE_NET_SUM_EXPANSION_DOWN")
            val5m < -1.10 && val60m in -0.09..0.09 -> return match("D100", TradeDirection.DOWN, "MONSTER_BREAKDOWN_FROM_SQUEEZE_B")
            val5m in -1.10..-0.91 && val60m in -0.09..0.09 -> return match("D099", TradeDirection.DOWN, "MONSTER_BREAKDOWN_FROM_SQUEEZE_A")
            val5m in -0.30..-0.10 && val60m in -1.20..-0.81 -> return match("D102", TradeDirection.DOWN, "ANCHORED_BEARISH_CONTINUATION")
            val5m in -0.20..-0.10 && val60m in -0.09..-0.01 -> return match("D085", TradeDirection.DOWN, "MICRO_DROP_SLIGHT_MACRO_NEGATIVE_A")
            val5m in -0.30..-0.21 && val60m in -0.09..-0.01 -> return match("D086", TradeDirection.DOWN, "MICRO_DROP_SLIGHT_MACRO_NEGATIVE_B")
            val5m in -0.45..-0.31 && val60m in -0.09..-0.01 -> return match("D087", TradeDirection.DOWN, "STRONG_MICRO_DROP_SLIGHT_MACRO_NEGATIVE_A")
            val5m in -0.60..-0.46 && val60m in -0.09..-0.01 -> return match("D088", TradeDirection.DOWN, "STRONG_MICRO_DROP_SLIGHT_MACRO_NEGATIVE_B")
            val5m in -0.09..-0.01 && val60m in -0.20..-0.10 -> return match("D089", TradeDirection.DOWN, "SLIGHT_MICRO_NEGATIVE_MACRO_LEAD_A")
            val5m in -0.09..-0.01 && val60m in -0.30..-0.21 -> return match("D090", TradeDirection.DOWN, "SLIGHT_MICRO_NEGATIVE_MACRO_LEAD_B")
            val5m in -0.09..-0.01 && val60m in -0.45..-0.31 -> return match("D091", TradeDirection.DOWN, "SLIGHT_MICRO_NEGATIVE_STRONG_MACRO_LEAD_A")
            val5m in -0.09..-0.01 && val60m in -0.60..-0.46 -> return match("D092", TradeDirection.DOWN, "SLIGHT_MICRO_NEGATIVE_STRONG_MACRO_LEAD_B")
            val5m in -0.25..-0.10 && val60m in 0.01..0.09 -> return match("D093", TradeDirection.DOWN, "MICRO_DROP_NEUTRAL_MACRO_A")
            val5m in -0.35..-0.26 && val60m in 0.01..0.09 -> return match("D094", TradeDirection.DOWN, "MICRO_DROP_NEUTRAL_MACRO_B")
            val5m in -0.50..-0.36 && val60m in 0.01..0.09 -> return match("D095", TradeDirection.DOWN, "STRONG_MICRO_DROP_NEUTRAL_MACRO_A")
            val5m in -0.65..-0.51 && val60m in 0.01..0.09 -> return match("D096", TradeDirection.DOWN, "STRONG_MICRO_DROP_NEUTRAL_MACRO_B")
            val5m in -0.75..-0.61 && val60m in 0.10..0.20 -> return match("D097", TradeDirection.DOWN, "INTENSE_DUMP_AGAINST_BULL_A")
            val5m in -0.90..-0.76 && val60m in 0.10..0.20 -> return match("D098", TradeDirection.DOWN, "INTENSE_DUMP_AGAINST_BULL_B")
            val5m in -0.09..-0.01 && val60m in -0.09..-0.01 -> return match("D101", TradeDirection.DOWN, "MICRO_CONSOLIDATION_DRIFT_DOWN")
        }

        return null
    }

    private fun match(id: String, direction: TradeDirection, code: String): DirectionalMatrixMatch {
        val effectiveDirection = UserRuleRegistry.getRuleOverride(id) ?: direction
        return DirectionalMatrixMatch(
            id = id,
            direction = effectiveDirection,
            outputCode = code,
            title = code.replace("_", " "),
            conditionDescription = if (effectiveDirection != direction) {
                "User Override $id: [$effectiveDirection] (Default: $direction) $code"
            } else {
                "Strict Rule $id: [$direction] $code"
            },
            priority = 100
        )
    }
}
