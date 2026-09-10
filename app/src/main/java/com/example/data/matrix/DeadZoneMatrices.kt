package com.example.data.matrix

import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Matrices M015 - M024: Dead Market, Spread Trap & No-Trade Zone Quantitative Rules.
 * Canonical Mandate: Any market state with abs(5m) <= 0.10% and abs(60m) <= 0.10% is NO_TRADE.
 */
object DeadZoneMatrices {

    val matrices: List<QuantitativeMatrix> = listOf(
        // M015: Exact Double Zero Dead Market
        QuantitativeMatrix(
            id = "M015",
            title = "Exact Double Zero Dead Zone",
            description = "Both 5m and 60m are exactly 0.00%. Zero liquidity and zero directional momentum.",
            priority = 910,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m == 0.0 and val60m == 0.0",
            outputCode = "DEAD_ZONE_DOUBLE_ZERO",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.0,
                val60m = 0.0,
                expectedMatched = true,
                expectedOutputCode = "DEAD_ZONE_DOUBLE_ZERO",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val5m.isNaN() && !ctx.val60m.isNaN() && abs(ctx.val5m) <= 0.0001 && abs(ctx.val60m) <= 0.0001 }
        ),

        // M016: Micro-Fluctuation Flat Band (0.01% - 0.04%)
        QuantitativeMatrix(
            id = "M016",
            title = "Micro-Fluctuation Flat Band",
            description = "Absolute values in 0.01% - 0.04% range. Pure exchange spread and broker noise.",
            priority = 905,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val5m) <= 0.04 and abs(val60m) <= 0.04",
            outputCode = "MICRO_FLUCTUATION_FLAT_BAND",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.02,
                val60m = -0.03,
                expectedMatched = true,
                expectedOutputCode = "MICRO_FLUCTUATION_FLAT_BAND",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val5m.isNaN() && !ctx.val60m.isNaN() && abs(ctx.val5m) <= 0.04 && abs(ctx.val60m) <= 0.04 }
        ),

        // M017: Canonical No-Trade Zone (abs(5m) <= 0.10% and abs(60m) <= 0.10%)
        QuantitativeMatrix(
            id = "M017",
            title = "Canonical No-Trade Zone Threshold",
            description = "Strict canonical rule: both horizons are <= 0.10%. Directional trading is mathematically blocked.",
            priority = 900,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val5m) <= 0.10 and abs(val60m) <= 0.10",
            outputCode = "NO_TRADE_ZONE_CANONICAL",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.08,
                val60m = 0.07,
                expectedMatched = true,
                expectedOutputCode = "NO_TRADE_ZONE_CANONICAL",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val5m.isNaN() && !ctx.val60m.isNaN() && abs(ctx.val5m) <= 0.10 && abs(ctx.val60m) <= 0.10 }
        ),

        // M018: Flat 5m with Micro Positive 60m
        QuantitativeMatrix(
            id = "M018",
            title = "Flat Short-Term with Micro Bullish Tilt",
            description = "5m is flat (0.00% - 0.03%) while 60m is micro-positive (0.04% - 0.10%). Inside dead zone.",
            priority = 890,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val5m) <= 0.03 and val60m in 0.04..0.10",
            outputCode = "DEAD_ZONE_MICRO_BULLISH_TILT",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.01,
                val60m = 0.06,
                expectedMatched = true,
                expectedOutputCode = "DEAD_ZONE_MICRO_BULLISH_TILT",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val5m.isNaN() && !ctx.val60m.isNaN() && abs(ctx.val5m) <= 0.03 && ctx.val60m in 0.04..0.10 }
        ),

        // M019: Flat 5m with Micro Negative 60m
        QuantitativeMatrix(
            id = "M019",
            title = "Flat Short-Term with Micro Bearish Tilt",
            description = "5m is flat (0.00% - 0.03%) while 60m is micro-negative (-0.10% to -0.04%). Inside dead zone.",
            priority = 890,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val5m) <= 0.03 and val60m in -0.10..-0.04",
            outputCode = "DEAD_ZONE_MICRO_BEARISH_TILT",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.01,
                val60m = -0.07,
                expectedMatched = true,
                expectedOutputCode = "DEAD_ZONE_MICRO_BEARISH_TILT",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val5m.isNaN() && !ctx.val60m.isNaN() && abs(ctx.val5m) <= 0.03 && ctx.val60m in -0.10..-0.04 }
        ),

        // M020: Dead Zone with Long-Term Macro Compression
        QuantitativeMatrix(
            id = "M020",
            title = "Multi-Horizon Volatility Compression",
            description = "All horizons including 1D (abs <= 0.15%) are compressed. Volatility squeeze.",
            priority = 895,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M, InputType.CHANGE_1D),
            conditionDescription = "abs(val5m) <= 0.10 and abs(val60m) <= 0.10 and abs(val1d) <= 0.15",
            outputCode = "MULTI_HORIZON_COMPRESSION",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.05,
                val60m = -0.05,
                val1d = 0.10,
                expectedMatched = true,
                expectedOutputCode = "MULTI_HORIZON_COMPRESSION",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val5m.isNaN() && !ctx.val60m.isNaN() && abs(ctx.val5m) <= 0.10 && abs(ctx.val60m) <= 0.10 && ctx.val1d != null && abs(ctx.val1d) <= 0.15 }
        ),

        // M021: Micro Net Sum Dead Zone Cancelation
        QuantitativeMatrix(
            id = "M021",
            title = "Micro Divergence Net Zero Deadlock",
            description = "5m and 60m oppose each other by micro amounts (e.g. +0.07% vs -0.07%), net sum ~ 0.00%.",
            priority = 885,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val5m) <= 0.10 and abs(val60m) <= 0.10 and abs(netSum) <= 0.02",
            outputCode = "MICRO_DIVERGENCE_NET_DEADLOCK",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.08,
                val60m = -0.07,
                expectedMatched = true,
                expectedOutputCode = "MICRO_DIVERGENCE_NET_DEADLOCK",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val5m.isNaN() && !ctx.val60m.isNaN() && abs(ctx.val5m) <= 0.10 && abs(ctx.val60m) <= 0.10 && abs(ctx.netSum) <= 0.02 }
        ),

        // M022: Spread Trap Boundary Threshold
        QuantitativeMatrix(
            id = "M022",
            title = "Spread Trap Boundary (0.09% - 0.10%)",
            description = "Magnitude sits right at the boundary of no-trade zone. High probability of chop loss.",
            priority = 880,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "maxOf(abs(val5m), abs(val60m)) in 0.09..0.10 and minOf(abs(val5m), abs(val60m)) <= 0.10",
            outputCode = "SPREAD_TRAP_BOUNDARY",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.10,
                val60m = 0.09,
                expectedMatched = true,
                expectedOutputCode = "SPREAD_TRAP_BOUNDARY",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx ->
                if (ctx.val5m.isNaN() || ctx.val60m.isNaN()) return@QuantitativeMatrix false
                val maxMag = maxOf(abs(ctx.val5m), abs(ctx.val60m))
                val minMag = minOf(abs(ctx.val5m), abs(ctx.val60m))
                maxMag in 0.09..0.10 && minMag <= 0.10
            }
        ),

        // M023: Sub-Threshold Volatility After Sudden Stop
        QuantitativeMatrix(
            id = "M023",
            title = "Exhausted Sub-Threshold Drift",
            description = "Sub-threshold volatility after prior movement. Velocity has collapsed to near zero.",
            priority = 875,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val5m) <= 0.06 and abs(val60m) <= 0.10 and abs(netSum) <= 0.05",
            outputCode = "EXHAUSTED_SUB_THRESHOLD_DRIFT",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.04,
                val60m = -0.05,
                expectedMatched = true,
                expectedOutputCode = "EXHAUSTED_SUB_THRESHOLD_DRIFT",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val5m.isNaN() && !ctx.val60m.isNaN() && abs(ctx.val5m) <= 0.06 && abs(ctx.val60m) <= 0.10 && abs(ctx.netSum) <= 0.05 }
        ),

        // M024: Dead Zone Transition Boundary
        QuantitativeMatrix(
            id = "M024",
            title = "Dead Zone Transition Standby",
            description = "One horizon is marginally above 0.10% (0.11% - 0.12%) but other is 0.00%. Awaiting breakout confirmation.",
            priority = 870,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val5m) in 0.11..0.12 and abs(val60m) <= 0.05",
            outputCode = "DEAD_ZONE_TRANSITION_STANDBY",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.11,
                val60m = 0.02,
                expectedMatched = true,
                expectedOutputCode = "DEAD_ZONE_TRANSITION_STANDBY",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val5m.isNaN() && !ctx.val60m.isNaN() && abs(ctx.val5m) in 0.11..0.12 && abs(ctx.val60m) <= 0.05 }
        )
    )
}
