package com.example.data.matrix

import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Matrices M025 - M044: Aligned Bullish Dual-Horizon Momentum Matrices.
 * Conditions: 5m > 0 and 60m > 0 with verified data, outside Dead Zone.
 */
object AlignedBullishMatrices {

    val matrices: List<QuantitativeMatrix> = listOf(
        // M025: Early Stage Micro Bullish Breakout
        QuantitativeMatrix(
            id = "M025",
            title = "Early Stage Bullish Breakout",
            description = "5m (+0.12% - +0.25%) and 60m (+0.11% - +0.25%) both fresh out of dead zone.",
            priority = 500,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m in 0.11..0.25 and val60m in 0.11..0.25",
            outputCode = "ALIGNED_BULLISH_EARLY_BREAKOUT",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.18,
                val60m = 0.15,
                expectedMatched = true,
                expectedOutputCode = "ALIGNED_BULLISH_EARLY_BREAKOUT",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m in 0.11..0.25 && ctx.val60m in 0.11..0.25 }
        ),

        // M026: Aligned Bullish Normal Flow (< 0.50% Net Sum)
        QuantitativeMatrix(
            id = "M026",
            title = "Aligned Bullish Normal Flow",
            description = "Dual positive horizons, net sum < 0.50%. Standard continuous upward drift.",
            priority = 510,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m > 0.10 and val60m > 0.10 and netSum < 0.50",
            outputCode = "ALIGNED_BULLISH_NORMAL",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.20,
                val60m = 0.22,
                expectedMatched = true,
                expectedOutputCode = "ALIGNED_BULLISH_NORMAL",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m > 0.10 && ctx.val60m > 0.10 && ctx.netSum < 0.50 }
        ),

        // M027: Bullish Impulse Thrust (5m Leading 60m)
        QuantitativeMatrix(
            id = "M027",
            title = "Bullish Impulse Thrust (Short Horizon Lead)",
            description = "5m (+0.35% - +0.60%) is more than double the 60m (+0.12% - +0.25%), indicating fresh buyer entry.",
            priority = 520,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.35 and val60m in 0.11..0.25 and val5m > val60m * 1.5",
            outputCode = "BULLISH_IMPULSE_5M_LEAD",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.45,
                val60m = 0.18,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_IMPULSE_5M_LEAD",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.35 && ctx.val60m in 0.11..0.25 && ctx.val5m > ctx.val60m * 1.5 }
        ),

        // M028: Aligned Bullish Medium Strength (0.50% - 0.99% Net Sum)
        QuantitativeMatrix(
            id = "M028",
            title = "Aligned Bullish Medium Strength",
            description = "Dual positive horizons, net sum between 0.50% and 0.99%. High-conviction continuation.",
            priority = 530,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m > 0.10 and val60m > 0.10 and netSum in 0.50..0.99",
            outputCode = "ALIGNED_BULLISH_MEDIUM",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.35,
                val60m = 0.40,
                expectedMatched = true,
                expectedOutputCode = "ALIGNED_BULLISH_MEDIUM",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m > 0.10 && ctx.val60m > 0.10 && ctx.netSum in 0.50..0.99 }
        ),

        // M029: Macro-Anchored Bullish Continuation (60m Leading 5m)
        QuantitativeMatrix(
            id = "M029",
            title = "Macro-Anchored Bullish Continuation",
            description = "60m is strong (+0.50%+) while 5m continues upward (+0.15% - +0.35%). Powerful macro wave.",
            priority = 535,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m >= 0.50 and val5m in 0.15..0.45",
            outputCode = "BULLISH_MACRO_ANCHORED_EXPANSION",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.25,
                val60m = 0.65,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_MACRO_ANCHORED_EXPANSION",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m >= 0.50 && ctx.val5m in 0.15..0.45 }
        ),

        // M030: Aligned Bullish High Strength (>= 1.00% Net Sum)
        QuantitativeMatrix(
            id = "M030",
            title = "Aligned Bullish High Strength",
            description = "Dual positive horizons, net sum >= 1.00%. Strong institutional momentum.",
            priority = 540,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m > 0.10 and val60m > 0.10 and netSum in 1.00..1.99",
            outputCode = "ALIGNED_BULLISH_HIGH",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.55,
                val60m = 0.65,
                expectedMatched = true,
                expectedOutputCode = "ALIGNED_BULLISH_HIGH",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m > 0.10 && ctx.val60m > 0.10 && ctx.netSum in 1.00..1.99 }
        ),

        // M031: Extreme Bullish Momentum (Net Sum >= 2.00%)
        QuantitativeMatrix(
            id = "M031",
            title = "Extreme Bullish Momentum Surge",
            description = "Dual positive horizons with combined net sum >= 2.00%. Heavy upward volume.",
            priority = 550,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m > 0.20 and val60m > 0.20 and netSum in 2.00..3.49",
            outputCode = "BULLISH_EXTREME_SURGE",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 1.10,
                val60m = 1.25,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_EXTREME_SURGE",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m > 0.20 && ctx.val60m > 0.20 && ctx.netSum in 2.00..3.49 }
        ),

        // M032: Parabolic Bullish Climax Warning (Net Sum >= 3.50%)
        QuantitativeMatrix(
            id = "M032",
            title = "Parabolic Bullish Climax Extension",
            description = "Net sum exceeds 3.50%. Direction is UP but risk transitions to HIGH due to exhaustion/reversal risk.",
            priority = 560,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m > 0.50 and val60m > 0.50 and netSum >= 3.50",
            outputCode = "BULLISH_PARABOLIC_CLIMAX",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.HIGH,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 2.00,
                val60m = 1.80,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_PARABOLIC_CLIMAX",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m > 0.50 && ctx.val60m > 0.50 && ctx.netSum >= 3.50 }
        ),

        // M033: Balanced Dual Bullish Symmetry
        QuantitativeMatrix(
            id = "M033",
            title = "Balanced Dual Bullish Symmetry",
            description = "5m and 60m are virtually identical in magnitude (within 0.05% of each other, e.g. +0.30% vs +0.32%).",
            priority = 525,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m > 0.15 and val60m > 0.15 and abs(val5m - val60m) <= 0.05",
            outputCode = "BULLISH_BALANCED_SYMMETRY",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.35,
                val60m = 0.33,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_BALANCED_SYMMETRY",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m > 0.15 && ctx.val60m > 0.15 && abs(ctx.val5m - ctx.val60m) <= 0.05 }
        ),

        // M034: Bullish Breakout from Prior Compression
        QuantitativeMatrix(
            id = "M034",
            title = "Bullish Volatility Breakout",
            description = "5m leaps above +0.40% while 60m is moderately positive (+0.15% - +0.30%).",
            priority = 522,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.40 and val60m in 0.15..0.30",
            outputCode = "BULLISH_VOLATILITY_EXPANSION",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.48,
                val60m = 0.22,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_VOLATILITY_EXPANSION",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.40 && ctx.val60m in 0.15..0.30 }
        ),

        // M035: Low-Volatility Bullish Drift (0.12% - 0.20% each)
        QuantitativeMatrix(
            id = "M035",
            title = "Low-Volatility Bullish Drift",
            description = "Both horizons drift gently upward between 0.12% and 0.20%. Orderly trend, low noise.",
            priority = 505,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m in 0.12..0.20 and val60m in 0.12..0.20",
            outputCode = "BULLISH_LOW_VOLATILITY_DRIFT",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.16,
                val60m = 0.17,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_LOW_VOLATILITY_DRIFT",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m in 0.12..0.20 && ctx.val60m in 0.12..0.20 }
        ),

        // M036: High-Sensitivity Bullish Alignment (100% Sensitivity Ratio)
        QuantitativeMatrix(
            id = "M036",
            title = "Pure Bullish Alignment Ratio",
            description = "Both horizons are strictly positive, ensuring sensitivity ratio is exactly 100.0%.",
            priority = 515,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m > 0.10 and val60m > 0.10 and totalMagnitude > 0.30",
            outputCode = "BULLISH_PURE_ALIGNMENT_RATIO",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.28,
                val60m = 0.32,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_PURE_ALIGNMENT_RATIO",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m > 0.10 && ctx.val60m > 0.10 && ctx.totalMagnitude > 0.30 }
        ),

        // M037: Rapid Bullish Acceleration Over 60m Trend
        QuantitativeMatrix(
            id = "M037",
            title = "Bullish Velocity Acceleration",
            description = "5m (+0.60%+) is over triple the 60m (+0.15% - +0.25%). Massive short-term influx.",
            priority = 538,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.60 and val60m in 0.15..0.25 and val5m > val60m * 2.5",
            outputCode = "BULLISH_VELOCITY_ACCELERATION",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.75,
                val60m = 0.20,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_VELOCITY_ACCELERATION",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.60 && ctx.val60m in 0.15..0.25 && ctx.val5m > ctx.val60m * 2.5 }
        ),

        // M038: Steady State Bullish Trend
        QuantitativeMatrix(
            id = "M038",
            title = "Steady State Bullish Trend",
            description = "5m between +0.25% and +0.45%, 60m between +0.25% and +0.45%. Ideal trend structure.",
            priority = 528,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m in 0.25..0.45 and val60m in 0.25..0.45",
            outputCode = "BULLISH_STEADY_STATE_TREND",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.32,
                val60m = 0.38,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_STEADY_STATE_TREND",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m in 0.25..0.45 && ctx.val60m in 0.25..0.45 }
        ),

        // M039: Robust Bullish Expansion (Net Sum 0.70% - 0.90%)
        QuantitativeMatrix(
            id = "M039",
            title = "Robust Bullish Expansion",
            description = "Both horizons strong, producing a net sum between +0.70% and +0.90%.",
            priority = 532,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m > 0.20 and val60m > 0.20 and netSum in 0.70..0.90",
            outputCode = "BULLISH_ROBUST_EXPANSION",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.40,
                val60m = 0.45,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_ROBUST_EXPANSION",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m > 0.20 && ctx.val60m > 0.20 && ctx.netSum in 0.70..0.90 }
        ),

        // M040: Heavy Institutional Bullish Continuation
        QuantitativeMatrix(
            id = "M040",
            title = "Heavy Institutional Bullish Flow",
            description = "60m is >= +0.80% and 5m maintains solid positive thrust (+0.30%+).",
            priority = 545,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m >= 0.80 and val5m >= 0.30",
            outputCode = "BULLISH_HEAVY_INSTITUTIONAL",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.45,
                val60m = 0.95,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_HEAVY_INSTITUTIONAL",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m >= 0.80 && ctx.val5m >= 0.30 }
        ),

        // M041: Bullish Trend with 1D Confluence
        QuantitativeMatrix(
            id = "M041",
            title = "Triple Bullish Confluence",
            description = "5m and 60m are both positive (> 0.15%), creating alignment across short horizons.",
            priority = 555,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m > 0.15 and val60m > 0.15",
            outputCode = "BULLISH_TRIPLE_CONFLUENCE",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.35,
                val60m = 0.45,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_TRIPLE_CONFLUENCE",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m > 0.15 && ctx.val60m > 0.15 }
        ),

        // M042: High Velocity Bullish Ramp
        QuantitativeMatrix(
            id = "M042",
            title = "High Velocity Bullish Ramp",
            description = "Net sum between 1.50% and 2.50% with both horizons strictly > +0.50%.",
            priority = 548,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.50 and val60m >= 0.50 and netSum in 1.50..2.50",
            outputCode = "BULLISH_HIGH_VELOCITY_RAMP",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.85,
                val60m = 0.95,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_HIGH_VELOCITY_RAMP",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.50 && ctx.val60m >= 0.50 && ctx.netSum in 1.50..2.50 }
        ),

        // M043: Modest Bullish Step (5m 0.20% - 0.35%, 60m 0.15% - 0.25%)
        QuantitativeMatrix(
            id = "M043",
            title = "Modest Bullish Progression",
            description = "Stable upward step with net sum in 0.35% - 0.60%.",
            priority = 512,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m in 0.20..0.35 and val60m in 0.15..0.25",
            outputCode = "BULLISH_MODEST_PROGRESSION",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.28,
                val60m = 0.20,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_MODEST_PROGRESSION",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m in 0.20..0.35 && ctx.val60m in 0.15..0.25 }
        ),

        // M044: Bullish Breakout Sustained High
        QuantitativeMatrix(
            id = "M044",
            title = "Sustained Bullish High Alignment",
            description = "Net sum >= 1.20%, both horizons > 0.40%, no warning flags.",
            priority = 542,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m > 0.40 and val60m > 0.40 and netSum >= 1.20",
            outputCode = "BULLISH_SUSTAINED_HIGH_ALIGNMENT",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.62,
                val60m = 0.68,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_SUSTAINED_HIGH_ALIGNMENT",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m > 0.40 && ctx.val60m > 0.40 && ctx.netSum >= 1.20 }
        )
    )
}
