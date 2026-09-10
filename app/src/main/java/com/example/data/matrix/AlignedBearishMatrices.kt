package com.example.data.matrix

import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Matrices M045 - M064: Aligned Bearish Dual-Horizon Momentum Matrices.
 * Conditions: 5m < 0 and 60m < 0 with verified data, outside Dead Zone.
 */
object AlignedBearishMatrices {

    val matrices: List<QuantitativeMatrix> = listOf(
        // M045: Early Stage Micro Bearish Breakdown
        QuantitativeMatrix(
            id = "M045",
            title = "Early Stage Bearish Breakdown",
            description = "5m (-0.12% to -0.25%) and 60m (-0.11% to -0.25%) both fresh below dead zone.",
            priority = 500,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m in -0.25..-0.11 and val60m in -0.25..-0.11",
            outputCode = "ALIGNED_BEARISH_EARLY_BREAKDOWN",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.18,
                val60m = -0.15,
                expectedMatched = true,
                expectedOutputCode = "ALIGNED_BEARISH_EARLY_BREAKDOWN",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m in -0.25..-0.11 && ctx.val60m in -0.25..-0.11 }
        ),

        // M046: Aligned Bearish Normal Flow (< 0.50% Net Magnitude)
        QuantitativeMatrix(
            id = "M046",
            title = "Aligned Bearish Normal Flow",
            description = "Dual negative horizons, net sum > -0.50%. Standard downward drift.",
            priority = 510,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m < -0.10 and val60m < -0.10 and netSum > -0.50",
            outputCode = "ALIGNED_BEARISH_NORMAL",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.20,
                val60m = -0.22,
                expectedMatched = true,
                expectedOutputCode = "ALIGNED_BEARISH_NORMAL",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m < -0.10 && ctx.val60m < -0.10 && ctx.netSum > -0.50 }
        ),

        // M047: Bearish Impulse Thrust (5m Leading 60m)
        QuantitativeMatrix(
            id = "M047",
            title = "Bearish Impulse Thrust (Short Horizon Lead)",
            description = "5m (-0.35% to -0.60%) is more than double the negative magnitude of 60m (-0.12% to -0.25%).",
            priority = 520,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.35 and val60m in -0.25..-0.11 and abs(val5m) > abs(val60m) * 1.5",
            outputCode = "BEARISH_IMPULSE_5M_LEAD",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.45,
                val60m = -0.18,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_IMPULSE_5M_LEAD",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.35 && ctx.val60m in -0.25..-0.11 && abs(ctx.val5m) > abs(ctx.val60m) * 1.5 }
        ),

        // M048: Aligned Bearish Medium Strength (-0.50% to -0.99% Net Sum)
        QuantitativeMatrix(
            id = "M048",
            title = "Aligned Bearish Medium Strength",
            description = "Dual negative horizons, net sum between -0.50% and -0.99%. Strong downward continuation.",
            priority = 530,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m < -0.10 and val60m < -0.10 and netSum in -0.99..-0.50",
            outputCode = "ALIGNED_BEARISH_MEDIUM",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.35,
                val60m = -0.40,
                expectedMatched = true,
                expectedOutputCode = "ALIGNED_BEARISH_MEDIUM",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m < -0.10 && ctx.val60m < -0.10 && ctx.netSum in -0.99..-0.50 }
        ),

        // M049: Macro-Anchored Bearish Continuation (60m Leading 5m)
        QuantitativeMatrix(
            id = "M049",
            title = "Macro-Anchored Bearish Continuation",
            description = "60m is heavily negative (< -0.50%) while 5m continues downward (-0.15% to -0.45%).",
            priority = 535,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m <= -0.50 and val5m in -0.45..-0.15",
            outputCode = "BEARISH_MACRO_ANCHORED_EXPANSION",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.25,
                val60m = -0.65,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_MACRO_ANCHORED_EXPANSION",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m <= -0.50 && ctx.val5m in -0.45..-0.15 }
        ),

        // M050: Aligned Bearish High Strength (<= -1.00% Net Sum)
        QuantitativeMatrix(
            id = "M050",
            title = "Aligned Bearish High Strength",
            description = "Dual negative horizons, net sum between -1.00% and -1.99%. High-conviction institutional selling.",
            priority = 540,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m < -0.10 and val60m < -0.10 and netSum in -1.99..-1.00",
            outputCode = "ALIGNED_BEARISH_HIGH",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.55,
                val60m = -0.65,
                expectedMatched = true,
                expectedOutputCode = "ALIGNED_BEARISH_HIGH",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m < -0.10 && ctx.val60m < -0.10 && ctx.netSum in -1.99..-1.00 }
        ),

        // M051: Extreme Bearish Momentum (Net Sum <= -2.00%)
        QuantitativeMatrix(
            id = "M051",
            title = "Extreme Bearish Selloff Surge",
            description = "Dual negative horizons with combined net sum between -2.00% and -3.49%. Aggressive panic selling.",
            priority = 550,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m < -0.20 and val60m < -0.20 and netSum in -3.49..-2.00",
            outputCode = "BEARISH_EXTREME_SURGE",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -1.10,
                val60m = -1.25,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_EXTREME_SURGE",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m < -0.20 && ctx.val60m < -0.20 && ctx.netSum in -3.49..-2.00 }
        ),

        // M052: Parabolic Bearish Climax Waterfall (Net Sum <= -3.50%)
        QuantitativeMatrix(
            id = "M052",
            title = "Parabolic Bearish Waterfall Climax",
            description = "Net sum <= -3.50%. Direction is DOWN but risk transitions to HIGH due to potential short squeeze/snapback.",
            priority = 560,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m < -0.50 and val60m < -0.50 and netSum <= -3.50",
            outputCode = "BEARISH_PARABOLIC_WATERFALL",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.HIGH,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -2.00,
                val60m = -1.80,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_PARABOLIC_WATERFALL",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m < -0.50 && ctx.val60m < -0.50 && ctx.netSum <= -3.50 }
        ),

        // M053: Balanced Dual Bearish Symmetry
        QuantitativeMatrix(
            id = "M053",
            title = "Balanced Dual Bearish Symmetry",
            description = "5m and 60m are virtually identical in negative magnitude (within 0.05% of each other).",
            priority = 525,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m < -0.15 and val60m < -0.15 and abs(val5m - val60m) <= 0.05",
            outputCode = "BEARISH_BALANCED_SYMMETRY",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.35,
                val60m = -0.33,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_BALANCED_SYMMETRY",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m < -0.15 && ctx.val60m < -0.15 && abs(ctx.val5m - ctx.val60m) <= 0.05 }
        ),

        // M054: Bearish Breakdown from Prior Compression
        QuantitativeMatrix(
            id = "M054",
            title = "Bearish Volatility Expansion",
            description = "5m drops below -0.40% while 60m is moderately negative (-0.15% to -0.30%).",
            priority = 522,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.40 and val60m in -0.30..-0.15",
            outputCode = "BEARISH_VOLATILITY_EXPANSION",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.48,
                val60m = -0.22,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_VOLATILITY_EXPANSION",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.40 && ctx.val60m in -0.30..-0.15 }
        ),

        // M055: Low-Volatility Bearish Drift (-0.12% to -0.20% each)
        QuantitativeMatrix(
            id = "M055",
            title = "Low-Volatility Bearish Drift",
            description = "Both horizons drift downward between -0.12% and -0.20%. Orderly downtrend.",
            priority = 505,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m in -0.20..-0.12 and val60m in -0.20..-0.12",
            outputCode = "BEARISH_LOW_VOLATILITY_DRIFT",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.16,
                val60m = -0.17,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_LOW_VOLATILITY_DRIFT",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m in -0.20..-0.12 && ctx.val60m in -0.20..-0.12 }
        ),

        // M056: High-Sensitivity Bearish Alignment (100% Sensitivity Ratio)
        QuantitativeMatrix(
            id = "M056",
            title = "Pure Bearish Alignment Ratio",
            description = "Both horizons strictly negative, sensitivity ratio is exactly 100.0%.",
            priority = 515,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m < -0.10 and val60m < -0.10 and totalMagnitude > 0.30",
            outputCode = "BEARISH_PURE_ALIGNMENT_RATIO",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.28,
                val60m = -0.32,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_PURE_ALIGNMENT_RATIO",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m < -0.10 && ctx.val60m < -0.10 && ctx.totalMagnitude > 0.30 }
        ),

        // M057: Rapid Bearish Acceleration Over 60m Trend
        QuantitativeMatrix(
            id = "M057",
            title = "Bearish Velocity Acceleration",
            description = "5m (< -0.60%) is over triple the negative magnitude of 60m (-0.15% to -0.25%).",
            priority = 538,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.60 and val60m in -0.25..-0.15 and abs(val5m) > abs(val60m) * 2.5",
            outputCode = "BEARISH_VELOCITY_ACCELERATION",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.75,
                val60m = -0.20,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_VELOCITY_ACCELERATION",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.60 && ctx.val60m in -0.25..-0.15 && abs(ctx.val5m) > abs(ctx.val60m) * 2.5 }
        ),

        // M058: Steady State Bearish Trend
        QuantitativeMatrix(
            id = "M058",
            title = "Steady State Bearish Trend",
            description = "5m between -0.25% and -0.45%, 60m between -0.25% and -0.45%. Ideal downtrend structure.",
            priority = 528,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m in -0.45..-0.25 and val60m in -0.45..-0.25",
            outputCode = "BEARISH_STEADY_STATE_TREND",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.32,
                val60m = -0.38,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_STEADY_STATE_TREND",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m in -0.45..-0.25 && ctx.val60m in -0.45..-0.25 }
        ),

        // M059: Robust Bearish Expansion (Net Sum -0.70% to -0.90%)
        QuantitativeMatrix(
            id = "M059",
            title = "Robust Bearish Expansion",
            description = "Both horizons negative, producing a net sum between -0.70% and -0.90%.",
            priority = 532,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m < -0.20 and val60m < -0.20 and netSum in -0.90..-0.70",
            outputCode = "BEARISH_ROBUST_EXPANSION",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.40,
                val60m = -0.45,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_ROBUST_EXPANSION",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m < -0.20 && ctx.val60m < -0.20 && ctx.netSum in -0.90..-0.70 }
        ),

        // M060: Heavy Institutional Bearish Flow
        QuantitativeMatrix(
            id = "M060",
            title = "Heavy Institutional Bearish Flow",
            description = "60m is <= -0.80% and 5m maintains solid negative thrust (<= -0.30%).",
            priority = 545,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m <= -0.80 and val5m <= -0.30",
            outputCode = "BEARISH_HEAVY_INSTITUTIONAL",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.45,
                val60m = -0.95,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_HEAVY_INSTITUTIONAL",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m <= -0.80 && ctx.val5m <= -0.30 }
        ),

        // M061: Bearish Trend with 1D Confluence
        QuantitativeMatrix(
            id = "M061",
            title = "Triple Bearish Confluence",
            description = "5m and 60m are both negative (< -0.15%), creating alignment across short horizons.",
            priority = 555,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m < -0.15 and val60m < -0.15",
            outputCode = "BEARISH_TRIPLE_CONFLUENCE",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.35,
                val60m = -0.45,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_TRIPLE_CONFLUENCE",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m < -0.15 && ctx.val60m < -0.15 }
        ),

        // M062: High Velocity Bearish Slide
        QuantitativeMatrix(
            id = "M062",
            title = "High Velocity Bearish Slide",
            description = "Net sum between -1.50% and -2.50% with both horizons strictly <= -0.50%.",
            priority = 548,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.50 and val60m <= -0.50 and netSum in -2.50..-1.50",
            outputCode = "BEARISH_HIGH_VELOCITY_SLIDE",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.85,
                val60m = -0.95,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_HIGH_VELOCITY_SLIDE",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.50 && ctx.val60m <= -0.50 && ctx.netSum in -2.50..-1.50 }
        ),

        // M063: Modest Bearish Step
        QuantitativeMatrix(
            id = "M063",
            title = "Modest Bearish Progression",
            description = "Stable downward step with net sum in -0.60% to -0.35%.",
            priority = 512,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m in -0.35..-0.20 and val60m in -0.25..-0.15",
            outputCode = "BEARISH_MODEST_PROGRESSION",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.28,
                val60m = -0.20,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_MODEST_PROGRESSION",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m in -0.35..-0.20 && ctx.val60m in -0.25..-0.15 }
        ),

        // M064: Bearish Breakdown Sustained High
        QuantitativeMatrix(
            id = "M064",
            title = "Sustained Bearish High Alignment",
            description = "Net sum <= -1.20%, both horizons < -0.40%, no warning flags.",
            priority = 542,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m < -0.40 and val60m < -0.40 and netSum <= -1.20",
            outputCode = "BEARISH_SUSTAINED_HIGH_ALIGNMENT",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.62,
                val60m = -0.68,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_SUSTAINED_HIGH_ALIGNMENT",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m < -0.40 && ctx.val60m < -0.40 && ctx.netSum <= -1.20 }
        )
    )
}
