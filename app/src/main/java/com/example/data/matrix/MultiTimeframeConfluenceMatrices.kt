package com.example.data.matrix

import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Matrices M151 - M165: Multi-Timeframe Confluence, Harmonic Resonance,
 * Macro-Anchor Absorption & Cross-Horizon Quantitative Decision Regimes.
 * Extends the Quantitative Decision Engine to 165 total deterministic matrices.
 * Synchronized to 5m & 60m timeframes (1D removed per strict specification).
 */
object MultiTimeframeConfluenceMatrices {

    val matrices: List<QuantitativeMatrix> = listOf(
        // M151: Triple-Horizon Bullish Harmonic Confluence
        QuantitativeMatrix(
            id = "M151",
            title = "Triple-Horizon Bullish Harmonic Confluence",
            description = "Harmonic bullish momentum across 5m (>= +0.25%) and 60m (>= +0.35%). Full directional agreement.",
            priority = 770,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.25 and val60m >= 0.35",
            outputCode = "TRIPLE_HORIZON_BULLISH_HARMONIC",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.30,
                val60m = 0.45,
                expectedMatched = true,
                expectedOutputCode = "TRIPLE_HORIZON_BULLISH_HARMONIC",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.25 && ctx.val60m >= 0.35 }
        ),

        // M152: Triple-Horizon Bearish Harmonic Confluence
        QuantitativeMatrix(
            id = "M152",
            title = "Triple-Horizon Bearish Harmonic Confluence",
            description = "Harmonic bearish cascade across 5m (<= -0.25%) and 60m (<= -0.35%). Full sell agreement.",
            priority = 770,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.25 and val60m <= -0.35",
            outputCode = "TRIPLE_HORIZON_BEARISH_HARMONIC",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.30,
                val60m = -0.45,
                expectedMatched = true,
                expectedOutputCode = "TRIPLE_HORIZON_BEARISH_HARMONIC",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.25 && ctx.val60m <= -0.35 }
        ),

        // M153: Daily Macro Anchor Bullish Dip Buy
        QuantitativeMatrix(
            id = "M153",
            title = "Daily Macro Anchor Bullish Dip Buy",
            description = "Hourly trend holding (+0.20%..+0.80%) while 5m pulls back temporarily (-0.30%..-0.05%). High-probability discount entry.",
            priority = 765,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m in 0.20..0.80 and val5m in -0.30..-0.05",
            outputCode = "DAILY_MACRO_ANCHOR_BULLISH_DIP",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.15,
                val60m = 0.40,
                expectedMatched = true,
                expectedOutputCode = "DAILY_MACRO_ANCHOR_BULLISH_DIP",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m in 0.20..0.80 && ctx.val5m in -0.30..-0.05 }
        ),

        // M154: Daily Macro Anchor Bearish Relief Fade
        QuantitativeMatrix(
            id = "M154",
            title = "Daily Macro Anchor Bearish Relief Fade",
            description = "Hourly trend down (-0.80%..-0.20%) while 5m experiences short-squeeze counter-bounce (+0.05%..+0.30%). High-probability fade.",
            priority = 765,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m in -0.80..-0.20 and val5m in 0.05..0.30",
            outputCode = "DAILY_MACRO_ANCHOR_BEARISH_FADE",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.15,
                val60m = -0.40,
                expectedMatched = true,
                expectedOutputCode = "DAILY_MACRO_ANCHOR_BEARISH_FADE",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m in -0.80..-0.20 && ctx.val5m in 0.05..0.30 }
        ),

        // M155: Macro Overextension Reversal Exhaustion
        QuantitativeMatrix(
            id = "M155",
            title = "Macro Overextension Reversal Exhaustion",
            description = "Aggressive hourly distribution (60m <= -0.40%) and 5m confirmation (<= -0.25%). Breakdown continuation.",
            priority = 790,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m <= -0.40 and val5m <= -0.25",
            outputCode = "MACRO_OVEREXTENSION_EXHAUSTION_SHORT",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.35,
                val60m = -0.55,
                expectedMatched = true,
                expectedOutputCode = "MACRO_OVEREXTENSION_EXHAUSTION_SHORT",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m <= -0.40 && ctx.val5m <= -0.25 }
        ),

        // M156: Macro Capitulation Reversal Spring
        QuantitativeMatrix(
            id = "M156",
            title = "Macro Capitulation Reversal Spring",
            description = "Sharp hourly accumulation bounce (60m >= +0.40%) and 5m confirmation (>= +0.25%). Bottom reversal spring.",
            priority = 790,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m >= 0.40 and val5m >= 0.25",
            outputCode = "MACRO_CAPITULATION_SPRING_LONG",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.35,
                val60m = 0.55,
                expectedMatched = true,
                expectedOutputCode = "MACRO_CAPITULATION_SPRING_LONG",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m >= 0.40 && ctx.val5m >= 0.25 }
        ),

        // M157: Multi-Horizon Volatility Coiling Compression
        QuantitativeMatrix(
            id = "M157",
            title = "Multi-Horizon Volatility Coiling Compression",
            description = "Extreme low volatility in both 5m (abs <= 0.06%) and 60m (abs <= 0.06%). Coiled spring awaiting expansion.",
            priority = 815,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val5m) <= 0.06 and abs(val60m) <= 0.06",
            outputCode = "MULTI_HORIZON_COILING_COMPRESSION",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.03,
                val60m = -0.04,
                expectedMatched = true,
                expectedOutputCode = "MULTI_HORIZON_COILING_COMPRESSION",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> abs(ctx.val5m) <= 0.06 && abs(ctx.val60m) <= 0.06 }
        ),

        // M158: Cross-Horizon Bullish Velocity Cascading
        QuantitativeMatrix(
            id = "M158",
            title = "Cross-Horizon Bullish Velocity Cascading",
            description = "Steadily accelerating velocity: 60m is strong (+0.40%..+0.90%) and 5m is explosive (>= +0.55%). Multi-timeframe acceleration.",
            priority = 775,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m in 0.40..0.90 and val5m >= 0.55",
            outputCode = "CROSS_HORIZON_BULLISH_VELOCITY",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.65,
                val60m = 0.50,
                expectedMatched = true,
                expectedOutputCode = "CROSS_HORIZON_BULLISH_VELOCITY",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m in 0.40..0.90 && ctx.val5m >= 0.55 }
        ),

        // M159: Cross-Horizon Bearish Velocity Cascading
        QuantitativeMatrix(
            id = "M159",
            title = "Cross-Horizon Bearish Velocity Cascading",
            description = "Steadily accelerating selloff velocity: 60m is sharper (-0.90%..-0.40%) and 5m is cascading (<= -0.55%). Multi-timeframe dump.",
            priority = 775,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m in -0.90..-0.40 and val5m <= -0.55",
            outputCode = "CROSS_HORIZON_BEARISH_VELOCITY",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.65,
                val60m = -0.50,
                expectedMatched = true,
                expectedOutputCode = "CROSS_HORIZON_BEARISH_VELOCITY",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m in -0.90..-0.40 && ctx.val5m <= -0.55 }
        ),

        // M160: Hourly Range Consolidation 5m Breakout Expansion
        QuantitativeMatrix(
            id = "M160",
            title = "Hourly Range Consolidation 5m Breakout Expansion",
            description = "60m is tightly consolidated (-0.08%..+0.08%) while 5m breaks out dynamically (>= +0.38%).",
            priority = 760,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val60m) <= 0.08 and val5m >= 0.38",
            outputCode = "HOURLY_CONSOLIDATION_5M_BREAKOUT_UP",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.42,
                val60m = 0.04,
                expectedMatched = true,
                expectedOutputCode = "HOURLY_CONSOLIDATION_5M_BREAKOUT_UP",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> abs(ctx.val60m) <= 0.08 && ctx.val5m >= 0.38 }
        ),

        // M161: Hourly Range Consolidation 5m Breakdown Expansion
        QuantitativeMatrix(
            id = "M161",
            title = "Hourly Range Consolidation 5m Breakdown Expansion",
            description = "60m is tightly consolidated (-0.08%..+0.08%) while 5m breaks down dynamically (<= -0.38%).",
            priority = 760,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val60m) <= 0.08 and val5m <= -0.38",
            outputCode = "HOURLY_CONSOLIDATION_5M_BREAKDOWN_DOWN",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.42,
                val60m = -0.04,
                expectedMatched = true,
                expectedOutputCode = "HOURLY_CONSOLIDATION_5M_BREAKDOWN_DOWN",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> abs(ctx.val60m) <= 0.08 && ctx.val5m <= -0.38 }
        ),

        // M162: Macro Structural Demand Absorption
        QuantitativeMatrix(
            id = "M162",
            title = "Macro Structural Demand Absorption",
            description = "60m exhibits minor healthy resting (-0.15%..+0.05%) and 5m resumes upward thrust (>= +0.30%).",
            priority = 768,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m in -0.15..0.05 and val5m >= 0.30",
            outputCode = "MACRO_STRUCTURAL_DEMAND_ABSORPTION",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.35,
                val60m = -0.05,
                expectedMatched = true,
                expectedOutputCode = "MACRO_STRUCTURAL_DEMAND_ABSORPTION",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m in -0.15..0.05 && ctx.val5m >= 0.30 }
        ),

        // M163: Macro Structural Supply Distribution
        QuantitativeMatrix(
            id = "M163",
            title = "Macro Structural Supply Distribution",
            description = "60m exhibits weak dead-cat resting (-0.05%..+0.15%) and 5m resumes downward slide (<= -0.30%).",
            priority = 768,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m in -0.05..0.15 and val5m <= -0.30",
            outputCode = "MACRO_STRUCTURAL_SUPPLY_DISTRIBUTION",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.35,
                val60m = 0.05,
                expectedMatched = true,
                expectedOutputCode = "MACRO_STRUCTURAL_SUPPLY_DISTRIBUTION",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m in -0.05..0.15 && ctx.val5m <= -0.30 }
        ),

        // M164: Total Multi-Horizon Resonance Squeeze (Bullish)
        QuantitativeMatrix(
            id = "M164",
            title = "Total Multi-Horizon Resonance Squeeze (Bullish)",
            description = "Absolute multi-timeframe resonance: 5m >= +0.50% and 60m >= +0.50%. High-conviction accumulation squeeze.",
            priority = 780,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.50 and val60m >= 0.50",
            outputCode = "TOTAL_HORIZON_RESONANCE_BULLISH",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.55,
                val60m = 0.60,
                expectedMatched = true,
                expectedOutputCode = "TOTAL_HORIZON_RESONANCE_BULLISH",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.50 && ctx.val60m >= 0.50 }
        ),

        // M165: Total Multi-Horizon Resonance Cascade (Bearish)
        QuantitativeMatrix(
            id = "M165",
            title = "Total Multi-Horizon Resonance Cascade (Bearish)",
            description = "Absolute multi-timeframe resonance cascade: 5m <= -0.50% and 60m <= -0.50%. High-conviction distribution cascade.",
            priority = 780,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.50 and val60m <= -0.50",
            outputCode = "TOTAL_HORIZON_RESONANCE_BEARISH",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.55,
                val60m = -0.60,
                expectedMatched = true,
                expectedOutputCode = "TOTAL_HORIZON_RESONANCE_BEARISH",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.50 && ctx.val60m <= -0.50 }
        )
    )
}
