package com.example.data.matrix

import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Matrices M133 - M150: Micro-Volatility, Asymmetric Ratio, Harmonic Confluence & Squeeze Regimes.
 * Part of the Master Catalog of 165 total deterministic matrices (M001 - M165).
 */
object MicroVolatilityMatrices {

    val matrices: List<QuantitativeMatrix> = listOf(
        // M133: Asymmetric 5m Impulse Thrust with 60m Absorption (Bullish)
        QuantitativeMatrix(
            id = "M133",
            title = "Asymmetric Bullish Impulse Expansion",
            description = "Short-term 5m expands strongly (>= +0.45%) while 60m holds steady (+0.05% to +0.25%). Early stage institutional momentum surge.",
            priority = 760,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.45 and val60m in 0.05..0.25 and netSum >= 0.50",
            outputCode = "ASYMMETRIC_BULLISH_IMPULSE",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.52,
                val60m = 0.18,
                expectedMatched = true,
                expectedOutputCode = "ASYMMETRIC_BULLISH_IMPULSE",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.45 && ctx.val60m in 0.05..0.25 && ctx.netSum >= 0.50 }
        ),

        // M134: Asymmetric 5m Flush with 60m Absorption (Bearish)
        QuantitativeMatrix(
            id = "M134",
            title = "Asymmetric Bearish Flush Expansion",
            description = "Short-term 5m flushes strongly (<= -0.45%) while 60m holds negative steady (-0.25% to -0.05%). Early institutional sell cascade.",
            priority = 760,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.45 and val60m in -0.25..-0.05 and netSum <= -0.50",
            outputCode = "ASYMMETRIC_BEARISH_FLUSH",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.52,
                val60m = -0.18,
                expectedMatched = true,
                expectedOutputCode = "ASYMMETRIC_BEARISH_FLUSH",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.45 && ctx.val60m in -0.25..-0.05 && ctx.netSum <= -0.50 }
        ),

        // M135: 60m Anchor Absorption with Shallow 5m Retracement (Bullish Trend Defense)
        QuantitativeMatrix(
            id = "M135",
            title = "60m Anchor Trend Bullish Defense",
            description = "60m anchor trend is strong (>= +0.60%) while 5m pulls back shallowly (-0.15% to -0.01%). Net sum remains dominant positive (>= +0.45%).",
            priority = 745,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m >= 0.60 and val5m in -0.15..-0.01 and netSum >= 0.45",
            outputCode = "ANCHOR_TREND_BULLISH_DEFENSE",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.08,
                val60m = 0.75,
                expectedMatched = true,
                expectedOutputCode = "ANCHOR_TREND_BULLISH_DEFENSE",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m >= 0.60 && ctx.val5m in -0.15..-0.01 && ctx.netSum >= 0.45 }
        ),

        // M136: 60m Anchor Absorption with Shallow 5m Bounce (Bearish Trend Defense)
        QuantitativeMatrix(
            id = "M136",
            title = "60m Anchor Trend Bearish Defense",
            description = "60m anchor trend is heavy (<= -0.60%) while 5m bounces shallowly (+0.01% to +0.15%). Net sum remains dominant negative (<= -0.45%).",
            priority = 745,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m <= -0.60 and val5m in 0.01..0.15 and netSum <= -0.45",
            outputCode = "ANCHOR_TREND_BEARISH_DEFENSE",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.08,
                val60m = -0.75,
                expectedMatched = true,
                expectedOutputCode = "ANCHOR_TREND_BEARISH_DEFENSE",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m <= -0.60 && ctx.val5m in 0.01..0.15 && ctx.netSum <= -0.45 }
        ),

        // M137: High Precision Bullish Harmonic Parity
        QuantitativeMatrix(
            id = "M137",
            title = "Bullish Harmonic Horizon Parity",
            description = "5m and 60m advance in tight symmetry (both >= +0.20%, delta <= 0.05%). Perfectly balanced dual-horizon momentum.",
            priority = 680,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.20 and val60m >= 0.20 and abs(val5m - val60m) <= 0.05",
            outputCode = "BULLISH_HARMONIC_PARITY",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.32,
                val60m = 0.30,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_HARMONIC_PARITY",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.20 && ctx.val60m >= 0.20 && abs(ctx.val5m - ctx.val60m) <= 0.05 }
        ),

        // M138: High Precision Bearish Harmonic Parity
        QuantitativeMatrix(
            id = "M138",
            title = "Bearish Harmonic Horizon Parity",
            description = "5m and 60m decline in tight symmetry (both <= -0.20%, delta <= 0.05%). Perfectly balanced dual-horizon sell off.",
            priority = 680,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.20 and val60m <= -0.20 and abs(val5m - val60m) <= 0.05",
            outputCode = "BEARISH_HARMONIC_PARITY",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.32,
                val60m = -0.30,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_HARMONIC_PARITY",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.20 && ctx.val60m <= -0.20 && abs(ctx.val5m - ctx.val60m) <= 0.05 }
        ),

        // M139: Coiled Spring Micro-Compression Bullish Breakout
        QuantitativeMatrix(
            id = "M139",
            title = "Coiled Spring Bullish Breakout",
            description = "60m is tightly compressed in equilibrium (|60m| <= 0.05%) while 5m breaks out sharply (>= +0.30%). High potential kinetic release.",
            priority = 730,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.30 and abs(val60m) <= 0.05",
            outputCode = "COILED_SPRING_BULLISH_BREAKOUT",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.38,
                val60m = 0.02,
                expectedMatched = true,
                expectedOutputCode = "COILED_SPRING_BULLISH_BREAKOUT",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.30 && abs(ctx.val60m) <= 0.05 }
        ),

        // M140: Coiled Spring Micro-Compression Bearish Breakdown
        QuantitativeMatrix(
            id = "M140",
            title = "Coiled Spring Bearish Breakdown",
            description = "60m is tightly compressed in equilibrium (|60m| <= 0.05%) while 5m drops sharply (<= -0.30%). High potential breakdown release.",
            priority = 730,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.30 and abs(val60m) <= 0.05",
            outputCode = "COILED_SPRING_BEARISH_BREAKDOWN",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.38,
                val60m = -0.02,
                expectedMatched = true,
                expectedOutputCode = "COILED_SPRING_BEARISH_BREAKDOWN",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.30 && abs(ctx.val60m) <= 0.05 }
        ),

        // M141: Dual Horizon Parabolic Surge (Extreme Bull Expansion)
        QuantitativeMatrix(
            id = "M141",
            title = "Dual Horizon Parabolic Surge",
            description = "5m >= +0.80% and 60m >= +1.00%. Ultra-strong institutional buying across both horizons. High momentum continuation.",
            priority = 790,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.80 and val60m >= 1.00",
            outputCode = "DUAL_HORIZON_PARABOLIC_SURGE",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.95,
                val60m = 1.25,
                expectedMatched = true,
                expectedOutputCode = "DUAL_HORIZON_PARABOLIC_SURGE",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.80 && ctx.val60m >= 1.00 }
        ),

        // M142: Dual Horizon Cascading Capitulation (Extreme Bear Expansion)
        QuantitativeMatrix(
            id = "M142",
            title = "Dual Horizon Cascading Capitulation",
            description = "5m <= -0.80% and 60m <= -1.00%. Aggressive liquidations across short and medium horizons. Dominant downward impulse.",
            priority = 790,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.80 and val60m <= -1.00",
            outputCode = "DUAL_HORIZON_CASCADING_CAPITULATION",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.95,
                val60m = -1.25,
                expectedMatched = true,
                expectedOutputCode = "DUAL_HORIZON_CASCADING_CAPITULATION",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.80 && ctx.val60m <= -1.00 }
        ),

        // M143: Asymmetric Net Dominance Bullish (Sensitivity >= 80% & Net >= +0.35%)
        QuantitativeMatrix(
            id = "M143",
            title = "High Sensitivity Bullish Ratio Dominance",
            description = "Directional agreement sensitivity exceeds 80% with net sum >= +0.35% and aligned 5m flow. High conviction upward flow.",
            priority = 710,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.0 and val60m >= 0.0 and netSum >= 0.35 and (abs(netSum) / totalMagnitude) >= 0.80",
            outputCode = "HIGH_SENSITIVITY_BULLISH_DOMINANCE",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.28,
                val60m = 0.22,
                expectedMatched = true,
                expectedOutputCode = "HIGH_SENSITIVITY_BULLISH_DOMINANCE",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.0 && ctx.val60m >= 0.0 && ctx.netSum >= 0.35 && ctx.totalMagnitude > 0.001 && (abs(ctx.netSum) / ctx.totalMagnitude) >= 0.80 }
        ),

        // M144: Asymmetric Net Dominance Bearish (Sensitivity >= 80% & Net <= -0.35%)
        QuantitativeMatrix(
            id = "M144",
            title = "High Sensitivity Bearish Ratio Dominance",
            description = "Directional agreement sensitivity exceeds 80% with net sum <= -0.35% and aligned 5m flow. High conviction downward flow.",
            priority = 710,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= 0.0 and val60m <= 0.0 and netSum <= -0.35 and (abs(netSum) / totalMagnitude) >= 0.80",
            outputCode = "HIGH_SENSITIVITY_BEARISH_DOMINANCE",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.28,
                val60m = -0.22,
                expectedMatched = true,
                expectedOutputCode = "HIGH_SENSITIVITY_BEARISH_DOMINANCE",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= 0.0 && ctx.val60m <= 0.0 && ctx.netSum <= -0.35 && ctx.totalMagnitude > 0.001 && (abs(ctx.netSum) / ctx.totalMagnitude) >= 0.80 }
        ),

        // M145: Micro Squeeze Parabolic Exhaustion Warning (Bullish Caution)
        QuantitativeMatrix(
            id = "M145",
            title = "Micro Squeeze Parabolic Overextension",
            description = "5m spikes >= +1.20% while 60m is unsupportive (<= +0.10%). Unbalanced vertical spike vulnerable to sudden mean-reversion.",
            priority = 845,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 1.20 and val60m <= 0.10",
            outputCode = "MICRO_SQUEEZE_PARABOLIC_OVEREXTENSION",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 1.45,
                val60m = 0.05,
                expectedMatched = true,
                expectedOutputCode = "MICRO_SQUEEZE_PARABOLIC_OVEREXTENSION",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 1.20 && ctx.val60m <= 0.10 }
        ),

        // M146: Micro Dump Capitulation Exhaustion Warning (Bearish Caution)
        QuantitativeMatrix(
            id = "M146",
            title = "Micro Dump Flash Capitulation",
            description = "5m drops <= -1.20% while 60m is unsupportive (>= -0.10%). Flash drop susceptible to aggressive bounce liquidity trap.",
            priority = 845,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -1.20 and val60m >= -0.10",
            outputCode = "MICRO_DUMP_FLASH_CAPITULATION",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -1.45,
                val60m = -0.05,
                expectedMatched = true,
                expectedOutputCode = "MICRO_DUMP_FLASH_CAPITULATION",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -1.20 && ctx.val60m >= -0.10 }
        ),

        // M147: Triple Horizon Harmonic Bullish Confluence (5m, 60m aligned)
        QuantitativeMatrix(
            id = "M147",
            title = "Triple Horizon Harmonic Bullish Confluence",
            description = "5m >= +0.35% and 60m >= +0.35%. Harmonic institutional tailwind.",
            priority = 855,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.35 and val60m >= 0.35",
            outputCode = "TRIPLE_HORIZON_HARMONIC_BULLISH",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.40,
                val60m = 0.45,
                expectedMatched = true,
                expectedOutputCode = "TRIPLE_HORIZON_HARMONIC_BULLISH",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.35 && ctx.val60m >= 0.35 }
        ),

        // M148: Triple Horizon Harmonic Bearish Confluence (5m, 60m aligned)
        QuantitativeMatrix(
            id = "M148",
            title = "Triple Horizon Harmonic Bearish Confluence",
            description = "5m <= -0.35% and 60m <= -0.35%. Harmonic institutional headwind.",
            priority = 855,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.35 and val60m <= -0.35",
            outputCode = "TRIPLE_HORIZON_HARMONIC_BEARISH",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.40,
                val60m = -0.45,
                expectedMatched = true,
                expectedOutputCode = "TRIPLE_HORIZON_HARMONIC_BEARISH",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.35 && ctx.val60m <= -0.35 }
        ),

        // M149: Low-Magnitude Micro-Drift Warning
        QuantitativeMatrix(
            id = "M149",
            title = "Low Magnitude Micro Drift Warning",
            description = "Both 5m and 60m are barely above dead-market (|5m| in 0.11..0.18%, |60m| in 0.11..0.18%). Directional conviction is weak.",
            priority = 610,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val5m) in 0.11..0.18 and abs(val60m) in 0.11..0.18",
            outputCode = "LOW_MAGNITUDE_MICRO_DRIFT",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.MODERATE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.14,
                val60m = 0.13,
                expectedMatched = true,
                expectedOutputCode = "LOW_MAGNITUDE_MICRO_DRIFT",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> abs(ctx.val5m) in 0.11..0.18 && abs(ctx.val60m) in 0.11..0.18 }
        ),

        // M150: Multi-Horizon Equilibrium Interlock (High Conflicting Cross-Currents)
        QuantitativeMatrix(
            id = "M150",
            title = "Multi-Horizon Equilibrium Interlock",
            description = "High total activity (totalMagnitude >= 0.60%) but netSum cancels out to near zero (<= 0.03%). Opposing market forces in deadlock.",
            priority = 865,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(netSum) <= 0.03 and totalMagnitude >= 0.60",
            outputCode = "MULTI_HORIZON_EQUILIBRIUM_INTERLOCK",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.EXTREME,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.35,
                val60m = -0.36,
                expectedMatched = true,
                expectedOutputCode = "MULTI_HORIZON_EQUILIBRIUM_INTERLOCK",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> abs(ctx.netSum) <= 0.03 && ctx.totalMagnitude >= 0.60 }
        )
    )
}
