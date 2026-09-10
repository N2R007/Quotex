package com.example.data.matrix

import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Matrices M121 - M132: Macro Confluence, Triple Horizon & Cross-Matrix Arbitration Rules.
 * Integrates 1D context and resolves high-level matrix confluence.
 */
object MacroConfluenceMatrices {

    val matrices: List<QuantitativeMatrix> = listOf(
        // M121: Triple Horizon Strong Bullish Confluence
        QuantitativeMatrix(
            id = "M121",
            title = "Triple Horizon Strong Bullish Confluence",
            description = "5m >= +0.30% and 60m >= +0.30%. Complete institutional bull alignment.",
            priority = 850,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.30 and val60m >= 0.30",
            outputCode = "TRIPLE_HORIZON_STRONG_BULLISH",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.45,
                val60m = 0.50,
                expectedMatched = true,
                expectedOutputCode = "TRIPLE_HORIZON_STRONG_BULLISH",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.30 && ctx.val60m >= 0.30 }
        ),

        // M122: Triple Horizon Strong Bearish Confluence
        QuantitativeMatrix(
            id = "M122",
            title = "Triple Horizon Strong Bearish Confluence",
            description = "5m <= -0.30% and 60m <= -0.30%. Complete institutional bear alignment.",
            priority = 850,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.30 and val60m <= -0.30",
            outputCode = "TRIPLE_HORIZON_STRONG_BEARISH",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.45,
                val60m = -0.50,
                expectedMatched = true,
                expectedOutputCode = "TRIPLE_HORIZON_STRONG_BEARISH",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.30 && ctx.val60m <= -0.30 }
        ),

        // M123: 1D Counter-Trend Headwind Bullish Warning
        QuantitativeMatrix(
            id = "M123",
            title = "Daily Macro Headwind Bullish Warning",
            description = "Short-term 5m and 60m are bullish (> +0.20%).",
            priority = 840,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m > 0.20 and val60m > 0.20",
            outputCode = "DAILY_HEADWIND_BULLISH_WARNING",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.35,
                val60m = 0.40,
                expectedMatched = true,
                expectedOutputCode = "DAILY_HEADWIND_BULLISH_WARNING",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m > 0.20 && ctx.val60m > 0.20 }
        ),

        // M124: 1D Counter-Trend Headwind Bearish Warning
        QuantitativeMatrix(
            id = "M124",
            title = "Daily Macro Headwind Bearish Warning",
            description = "Short-term 5m and 60m are bearish (< -0.20%).",
            priority = 840,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m < -0.20 and val60m < -0.20",
            outputCode = "DAILY_HEADWIND_BEARISH_WARNING",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.35,
                val60m = -0.40,
                expectedMatched = true,
                expectedOutputCode = "DAILY_HEADWIND_BEARISH_WARNING",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m < -0.20 && ctx.val60m < -0.20 }
        ),

        // M125: 1D Neutral Flat Background
        QuantitativeMatrix(
            id = "M125",
            title = "Neutral 1D Macro Context",
            description = "1D is completely flat (between -0.20% and +0.20%). Intra-day setups take pure precedence.",
            priority = 820,
            requiredInputs = setOf(InputType.CHANGE_1D),
            conditionDescription = "val1d != null and abs(val1d) <= 0.20",
            outputCode = "NEUTRAL_1D_CONTEXT",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.LOW,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.30,
                val60m = 0.30,
                val1d = 0.05,
                expectedMatched = true,
                expectedOutputCode = "NEUTRAL_1D_CONTEXT",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.val1d != null && abs(ctx.val1d) <= 0.20 }
        ),

        // M126: Multi-Day Breakout Surge Confluence
        QuantitativeMatrix(
            id = "M126",
            title = "Multi-Day Breakout Surge Confluence",
            description = "5m >= +0.40% and 60m >= +0.20%. Powerful breakout surge.",
            priority = 845,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.40 and val60m >= 0.20",
            outputCode = "MULTI_DAY_BREAKOUT_SURGE",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.50,
                val60m = 0.40,
                expectedMatched = true,
                expectedOutputCode = "MULTI_DAY_BREAKOUT_SURGE",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.40 && ctx.val60m >= 0.20 }
        ),

        // M127: Multi-Day Breakdown Cascade Confluence
        QuantitativeMatrix(
            id = "M127",
            title = "Multi-Day Breakdown Cascade Confluence",
            description = "5m <= -0.40% and 60m <= -0.20%. Powerful breakdown cascade.",
            priority = 845,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.40 and val60m <= -0.20",
            outputCode = "MULTI_DAY_BREAKDOWN_CASCADE",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.50,
                val60m = -0.40,
                expectedMatched = true,
                expectedOutputCode = "MULTI_DAY_BREAKDOWN_CASCADE",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.40 && ctx.val60m <= -0.20 }
        ),

        // M128: 1D Parabolic Overextension Warning Bullish
        QuantitativeMatrix(
            id = "M128",
            title = "Daily Parabolic Overextension Warning",
            description = "5m >= +0.60% and 60m >= +0.30%. Overextension warning.",
            priority = 855,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.60 and val60m >= 0.30",
            outputCode = "DAILY_PARABOLIC_OVEREXTENSION_UP",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.65,
                val60m = 0.40,
                expectedMatched = true,
                expectedOutputCode = "DAILY_PARABOLIC_OVEREXTENSION_UP",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.60 && ctx.val60m >= 0.30 }
        ),

        // M129: 1D Parabolic Overextension Warning Bearish
        QuantitativeMatrix(
            id = "M129",
            title = "Daily Panic Capitulation Overextension Warning",
            description = "5m <= -0.60% and 60m <= -0.30%. Capitulation overextension warning.",
            priority = 855,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.60 and val60m <= -0.30",
            outputCode = "DAILY_CAPITULATION_OVEREXTENSION_DOWN",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.65,
                val60m = -0.40,
                expectedMatched = true,
                expectedOutputCode = "DAILY_CAPITULATION_OVEREXTENSION_DOWN",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.60 && ctx.val60m <= -0.30 }
        ),

        // M130: Conflicting Evidence Master Arbitration
        QuantitativeMatrix(
            id = "M130",
            title = "Conflicting Evidence Master Safety Interlock",
            description = "Short-term thrust opposes 60m trend while net sum is within ±0.08%. Zero active trade recommendation.",
            priority = 860,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "(val5m * val60m < 0) and abs(netSum) <= 0.08 and totalMagnitude >= 0.40",
            outputCode = "CONFLICTING_EVIDENCE_SAFETY_INTERLOCK",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.EXTREME,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.40,
                val60m = -0.45,
                expectedMatched = true,
                expectedOutputCode = "CONFLICTING_EVIDENCE_SAFETY_INTERLOCK",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> (ctx.val5m * ctx.val60m < 0) && abs(ctx.netSum) <= 0.08 && ctx.totalMagnitude >= 0.40 }
        ),

        // M131: Low Confidence Data Quality Degradation Interlock
        QuantitativeMatrix(
            id = "M131",
            title = "Data Quality Degradation Safety Interlock",
            description = "Data quality is not strictly VERIFIED. Forces warning-only state.",
            priority = 930,
            requiredInputs = setOf(InputType.DATA_QUALITY),
            conditionDescription = "dataQuality != VERIFIED",
            outputCode = "DATA_QUALITY_DEGRADATION_INTERLOCK",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.35,
                val60m = 0.35,
                dataQuality = "PARTIALLY_VERIFIED",
                expectedMatched = true,
                expectedOutputCode = "DATA_QUALITY_DEGRADATION_INTERLOCK",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.dataQuality != "VERIFIED" }
        ),

        // M132: Default Quantitative Neutral / Unmatched Fallback
        QuantitativeMatrix(
            id = "M132",
            title = "Deterministic Neutral Fallback",
            description = "Baseline fallback rule when no specific directional setup reaches conviction threshold.",
            priority = 1,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "Always active baseline fallback",
            outputCode = "DETERMINISTIC_NEUTRAL_BASELINE",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.0,
                val60m = 0.0,
                expectedMatched = true,
                expectedOutputCode = "DETERMINISTIC_NEUTRAL_BASELINE",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { _ -> true }
        )
    )
}
