package com.example.data.matrix

import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Matrices M065 - M080: Counter-Trend Divergence & Pullback Quantitative Rules.
 * Conditions: 5m and 60m have opposing polarities (one positive, one negative).
 */
object CounterTrendPullbackMatrices {

    val matrices: List<QuantitativeMatrix> = listOf(
        // M065: Bullish Trend Normal Dip Pullback
        QuantitativeMatrix(
            id = "M065",
            title = "Bullish Trend Normal Pullback (Dip)",
            description = "60m >= +0.50% while 5m in -0.30%..-0.12% with netSum > 0.20%. Macro uptrend dominates.",
            priority = 600,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m >= 0.50 and val5m in -0.30..-0.12 and netSum > 0.20",
            outputCode = "BULLISH_TREND_NORMAL_DIP",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.20,
                val60m = 0.65,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_TREND_NORMAL_DIP",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m >= 0.50 && ctx.val5m in -0.30..-0.12 && ctx.netSum > 0.20 }
        ),

        // M066: Bearish Trend Counter Rally (Dead-Cat Bounce)
        QuantitativeMatrix(
            id = "M066",
            title = "Bearish Trend Dead-Cat Counter Rally",
            description = "60m <= -0.50% while 5m in +0.12%..+0.30% with netSum < -0.20%. Macro downtrend dominates.",
            priority = 600,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m <= -0.50 and val5m in 0.12..0.30 and netSum < -0.20",
            outputCode = "BEARISH_TREND_COUNTER_RALLY",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.20,
                val60m = -0.65,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_TREND_COUNTER_RALLY",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m <= -0.50 && ctx.val5m in 0.12..0.30 && ctx.netSum < -0.20 }
        ),

        // M067: Bullish Trend Deep Pullback Warning
        QuantitativeMatrix(
            id = "M067",
            title = "Deep Pullback Testing Bullish Trend",
            description = "val60m in 0.40..0.80 and val5m in -0.60..-0.35 and netSum in 0.05..0.25.",
            priority = 610,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m in 0.40..0.80 and val5m in -0.60..-0.35 and netSum in 0.05..0.25",
            outputCode = "BULLISH_TREND_DEEP_PULLBACK_WARNING",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.45,
                val60m = 0.60,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_TREND_DEEP_PULLBACK_WARNING",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m in 0.40..0.80 && ctx.val5m in -0.60..-0.35 && ctx.netSum in 0.05..0.25 }
        ),

        // M068: Bearish Trend Deep Rally Warning
        QuantitativeMatrix(
            id = "M068",
            title = "Aggressive Counter-Rally in Downtrend",
            description = "val60m in -0.80..-0.40 and val5m in 0.35..0.60 and netSum in -0.25..-0.05.",
            priority = 610,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m in -0.80..-0.40 and val5m in 0.35..0.60 and netSum in -0.25..-0.05",
            outputCode = "BEARISH_TREND_DEEP_RALLY_WARNING",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.45,
                val60m = -0.60,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_TREND_DEEP_RALLY_WARNING",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m in -0.80..-0.40 && ctx.val5m in 0.35..0.60 && ctx.netSum in -0.25..-0.05 }
        ),

        // M069: Direct Divergence Net Zero Deadlock
        QuantitativeMatrix(
            id = "M069",
            title = "Dual Horizon Direct Divergence Deadlock",
            description = "5m and 60m are in direct opposition and cancel each other out (net sum between -0.05% and +0.05%).",
            priority = 630,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "(val5m * val60m < 0) and abs(netSum) <= 0.05 and totalMagnitude >= 0.30",
            outputCode = "DIVERGENCE_NET_DEADLOCK",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.EXTREME,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.35,
                val60m = -0.36,
                expectedMatched = true,
                expectedOutputCode = "DIVERGENCE_NET_DEADLOCK",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> (ctx.val5m * ctx.val60m < 0) && abs(ctx.netSum) <= 0.05 && ctx.totalMagnitude >= 0.30 }
        ),

        // M070: Aggressive Short-Term Bullish Reversal Overtake
        QuantitativeMatrix(
            id = "M070",
            title = "Short-Term Bullish Momentum Overtake",
            description = "5m (+0.60%+) is over double the negative magnitude of 60m (-0.25%), overpowering the prior downtrend.",
            priority = 620,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.60 and val60m in -0.35..-0.10 and netSum >= 0.25",
            outputCode = "BULLISH_SHORT_TERM_OVERTAKE",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.70,
                val60m = -0.25,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_SHORT_TERM_OVERTAKE",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.60 && ctx.val60m in -0.35..-0.10 && ctx.netSum >= 0.25 }
        ),

        // M071: Aggressive Short-Term Bearish Reversal Overtake
        QuantitativeMatrix(
            id = "M071",
            title = "Short-Term Bearish Momentum Overtake",
            description = "5m (<= -0.60%) is over double the positive magnitude of 60m (+0.25%), overpowering the prior uptrend.",
            priority = 620,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.60 and val60m in 0.10..0.35 and netSum <= -0.25",
            outputCode = "BEARISH_SHORT_TERM_OVERTAKE",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.70,
                val60m = 0.25,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_SHORT_TERM_OVERTAKE",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.60 && ctx.val60m in 0.10..0.35 && ctx.netSum <= -0.25 }
        ),

        // M072: Divergent Micro-Pullback Bullish
        QuantitativeMatrix(
            id = "M072",
            title = "Divergent Micro-Dip in Bullish Expansion",
            description = "val60m in 0.30..0.50 and val5m in -0.15..-0.10.",
            priority = 605,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m in 0.30..0.50 and val5m in -0.15..-0.10",
            outputCode = "DIVERGENT_MICRO_PULLBACK_UP",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.12,
                val60m = 0.38,
                expectedMatched = true,
                expectedOutputCode = "DIVERGENT_MICRO_PULLBACK_UP",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m in 0.30..0.50 && ctx.val5m in -0.15..-0.10 }
        ),

        // M073: Divergent Micro-Bounce Bearish
        QuantitativeMatrix(
            id = "M073",
            title = "Divergent Micro-Bounce in Bearish Decline",
            description = "val60m in -0.50..-0.30 and val5m in 0.10..0.15.",
            priority = 605,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m in -0.50..-0.30 and val5m in 0.10..0.15",
            outputCode = "DIVERGENT_MICRO_BOUNCE_DOWN",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.12,
                val60m = -0.38,
                expectedMatched = true,
                expectedOutputCode = "DIVERGENT_MICRO_BOUNCE_DOWN",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m in -0.50..-0.30 && ctx.val5m in 0.10..0.15 }
        ),

        // M074: Asymmetric Pullback Up
        QuantitativeMatrix(
            id = "M074",
            title = "Asymmetric Pullback Up",
            description = "val60m >= 0.70 and val5m in -0.20..-0.05 and netSum >= 0.50.",
            priority = 615,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m >= 0.70 and val5m in -0.20..-0.05 and netSum >= 0.50",
            outputCode = "ASYMMETRIC_PULLBACK_UP",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.10,
                val60m = 0.75,
                expectedMatched = true,
                expectedOutputCode = "ASYMMETRIC_PULLBACK_UP",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m >= 0.70 && ctx.val5m in -0.20..-0.05 && ctx.netSum >= 0.50 }
        ),

        // M075: Asymmetric Counter Bounce Down
        QuantitativeMatrix(
            id = "M075",
            title = "Asymmetric Counter Bounce Down",
            description = "val60m <= -0.70 and val5m in 0.05..0.20 and netSum <= -0.50.",
            priority = 615,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m <= -0.70 and val5m in 0.05..0.20 and netSum <= -0.50",
            outputCode = "ASYMMETRIC_COUNTER_BOUNCE_DOWN",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.10,
                val60m = -0.75,
                expectedMatched = true,
                expectedOutputCode = "ASYMMETRIC_COUNTER_BOUNCE_DOWN",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m <= -0.70 && ctx.val5m in 0.05..0.20 && ctx.netSum <= -0.50 }
        ),

        // M076: Shallow Confluence Absorption Up
        QuantitativeMatrix(
            id = "M076",
            title = "Shallow Confluence Absorption Up",
            description = "val60m >= 0.40 and val5m in -0.08..-0.02 and (val1d ?: 0.0) >= 0.10.",
            priority = 635,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m >= 0.40 and val5m in -0.08..-0.02 and (val1d ?: 0.0) >= 0.10",
            outputCode = "SHALLOW_CONFLUENCE_ABSORPTION_UP",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.05,
                val60m = 0.50,
                val1d = 0.20,
                expectedMatched = true,
                expectedOutputCode = "SHALLOW_CONFLUENCE_ABSORPTION_UP",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m >= 0.40 && ctx.val5m in -0.08..-0.02 && (ctx.val1d ?: 0.0) >= 0.10 }
        ),

        // M077: Shallow Resistance Absorption Down
        QuantitativeMatrix(
            id = "M077",
            title = "Shallow Resistance Absorption Down",
            description = "val60m <= -0.40 and val5m in 0.02..0.08 and (val1d ?: 0.0) <= -0.10.",
            priority = 612,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m <= -0.40 and val5m in 0.02..0.08 and (val1d ?: 0.0) <= -0.10",
            outputCode = "SHALLOW_RESISTANCE_ABSORPTION_DOWN",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.05,
                val60m = -0.50,
                val1d = -0.20,
                expectedMatched = true,
                expectedOutputCode = "SHALLOW_RESISTANCE_ABSORPTION_DOWN",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m <= -0.40 && ctx.val5m in 0.02..0.08 && (ctx.val1d ?: 0.0) <= -0.10 }
        ),

        // M078: Counter-Trend Exhaustion Pullback Downward
        QuantitativeMatrix(
            id = "M078",
            title = "Counter-Trend Exhaustion Pullback (Bearish Side)",
            description = "5m is negative (-0.30% to -0.15%) but 60m is heavily positive (>= +0.90%).",
            priority = 612,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m in -0.30..-0.15 and val60m >= 0.90",
            outputCode = "COUNTER_TREND_EXHAUSTION_DIP",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.22,
                val60m = 1.10,
                expectedMatched = true,
                expectedOutputCode = "COUNTER_TREND_EXHAUSTION_DIP",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m in -0.30..-0.15 && ctx.val60m >= 0.90 }
        ),

        // M079: Opposing Horizons Low Sensitivity Ratio (< 30.0%)
        QuantitativeMatrix(
            id = "M079",
            title = "Low Sensitivity Divergence Ambiguity",
            description = "Sensitivity ratio is < 30.0% due to near cancellation of 5m and 60m.",
            priority = 625,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "(val5m * val60m < 0) and (abs(netSum) / totalMagnitude) < 0.30",
            outputCode = "LOW_SENSITIVITY_DIVERGENCE_AMBIGUITY",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.25,
                val60m = -0.30,
                expectedMatched = true,
                expectedOutputCode = "LOW_SENSITIVITY_DIVERGENCE_AMBIGUITY",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx ->
                if (ctx.totalMagnitude <= 0.0001) return@QuantitativeMatrix false
                (ctx.val5m * ctx.val60m < 0) && (abs(ctx.netSum) / ctx.totalMagnitude) < 0.30
            }
        ),

        // M080: Opposing Horizons Moderate Sensitivity (30.0% - 60.0%)
        QuantitativeMatrix(
            id = "M080",
            title = "Moderate Sensitivity Horizon Divergence",
            description = "Polarities oppose, sensitivity ratio between 30.0% and 60.0%. Direction follows net sum with moderate risk.",
            priority = 618,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "(val5m * val60m < 0) and (abs(netSum) / totalMagnitude) in 0.30..0.60",
            outputCode = "MODERATE_SENSITIVITY_HORIZON_DIVERGENCE",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.MODERATE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.20,
                val60m = -0.50,
                expectedMatched = true,
                expectedOutputCode = "MODERATE_SENSITIVITY_HORIZON_DIVERGENCE",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx ->
                if (ctx.totalMagnitude <= 0.0001) return@QuantitativeMatrix false
                val ratio = abs(ctx.netSum) / ctx.totalMagnitude
                (ctx.val5m * ctx.val60m < 0) && ratio in 0.30..0.60
            }
        )
    )
}
