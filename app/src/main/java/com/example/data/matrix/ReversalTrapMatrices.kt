package com.example.data.matrix

import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Matrices M095 - M108: Reversal, Top/Bottom Fakeout & Liquidity Trap Quantitative Rules.
 * Detects zero-crossings and bull/bear trap patterns.
 */
object ReversalTrapMatrices {

    val matrices: List<QuantitativeMatrix> = listOf(
        // M095: Top Fakeout Initial Zero-Crossing Candidate
        QuantitativeMatrix(
            id = "M095",
            title = "Top Fakeout Candidate (Zero Crossing)",
            description = "60m is strong bullish (+0.50%+) but 5m flips negative (-0.05% to -0.15%). First sign of top trap.",
            priority = 750,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m >= 0.50 and val5m in -0.15..-0.05",
            outputCode = "TOP_FAKEOUT_CANDIDATE",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.10,
                val60m = 0.65,
                expectedMatched = true,
                expectedOutputCode = "TOP_FAKEOUT_CANDIDATE",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m >= 0.50 && ctx.val5m in -0.15..-0.05 }
        ),

        // M096: Top Fakeout Confirmed Sell Trigger
        QuantitativeMatrix(
            id = "M096",
            title = "Top Fakeout Confirmed Sell",
            description = "60m remains positive (+0.40%+) while 5m breaks down decisively (<= -0.20%). Reversal execution.",
            priority = 760,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m in 0.40..1.20 and val5m <= -0.20 and netSum in -0.10..0.30",
            outputCode = "TOP_FAKEOUT_CONFIRMED_SELL",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.30,
                val60m = 0.50,
                expectedMatched = true,
                expectedOutputCode = "TOP_FAKEOUT_CONFIRMED_SELL",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m in 0.40..1.20 && ctx.val5m <= -0.20 && ctx.netSum in -0.10..0.30 }
        ),

        // M097: Bottom Fakeout Initial Zero-Crossing Candidate
        QuantitativeMatrix(
            id = "M097",
            title = "Bottom Fakeout Candidate (Zero Crossing)",
            description = "60m is strong bearish (<= -0.50%) but 5m flips positive (+0.05% to +0.15%). First sign of bottom trap.",
            priority = 750,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m <= -0.50 and val5m in 0.05..0.15",
            outputCode = "BOTTOM_FAKEOUT_CANDIDATE",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.10,
                val60m = -0.65,
                expectedMatched = true,
                expectedOutputCode = "BOTTOM_FAKEOUT_CANDIDATE",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m <= -0.50 && ctx.val5m in 0.05..0.15 }
        ),

        // M098: Bottom Fakeout Confirmed Buy Trigger
        QuantitativeMatrix(
            id = "M098",
            title = "Bottom Fakeout Confirmed Buy",
            description = "60m remains negative (-1.20% to -0.40%) while 5m surges upward (>= +0.20%). Reversal execution.",
            priority = 760,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m in -1.20..-0.40 and val5m >= 0.20 and netSum in -0.30..0.10",
            outputCode = "BOTTOM_FAKEOUT_CONFIRMED_BUY",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.30,
                val60m = -0.50,
                expectedMatched = true,
                expectedOutputCode = "BOTTOM_FAKEOUT_CONFIRMED_BUY",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m in -1.20..-0.40 && ctx.val5m >= 0.20 && ctx.netSum in -0.30..0.10 }
        ),

        // M099: Bull Trap Liquidity Spike Collapse
        QuantitativeMatrix(
            id = "M099",
            title = "Bull Trap Spike Collapse",
            description = "Previous snapshot was strong 5m spike (>= +0.50%), followed immediately by negative flip (<= -0.15%).",
            priority = 770,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.isNotEmpty() and history[0].val5m >= 0.50 and val5m <= -0.15",
            outputCode = "BULL_TRAP_COLLAPSE",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = -0.20,
                val60m = 0.30,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = 0.55, val60m = 0.30)
                ),
                expectedMatched = true,
                expectedOutputCode = "BULL_TRAP_COLLAPSE",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                ctx.history[0].val5m >= 0.50 && ctx.val5m <= -0.15
            }
        ),

        // M100: Bear Trap Liquidity Flush Rebound
        QuantitativeMatrix(
            id = "M100",
            title = "Bear Trap Flush Rebound",
            description = "Previous snapshot was strong 5m flush (<= -0.50%), followed immediately by positive flip (>= +0.15%).",
            priority = 770,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.isNotEmpty() and history[0].val5m <= -0.50 and val5m >= 0.15",
            outputCode = "BEAR_TRAP_REBOUND",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.20,
                val60m = -0.30,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -0.55, val60m = -0.30)
                ),
                expectedMatched = true,
                expectedOutputCode = "BEAR_TRAP_REBOUND",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                ctx.history[0].val5m <= -0.50 && ctx.val5m >= 0.15
            }
        ),

        // M101: 60m Macro Exhaustion Divergence Bullish
        QuantitativeMatrix(
            id = "M101",
            title = "Macro Bullish Climax Reversal Warning",
            description = "60m is at extreme peak (+1.50%+) while 5m starts cascading down (<= -0.30%). Major trend peak.",
            priority = 765,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m >= 1.50 and val5m <= -0.30",
            outputCode = "MACRO_BULLISH_PEAK_EXHAUSTION",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.35,
                val60m = 1.60,
                expectedMatched = true,
                expectedOutputCode = "MACRO_BULLISH_PEAK_EXHAUSTION",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m >= 1.50 && ctx.val5m <= -0.30 }
        ),

        // M102: 60m Macro Exhaustion Divergence Bearish
        QuantitativeMatrix(
            id = "M102",
            title = "Macro Bearish Bottom Reversal Warning",
            description = "60m is at extreme bottom (<= -1.50%) while 5m surges upward (>= +0.30%). Major capitulation bottom.",
            priority = 765,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m <= -1.50 and val5m >= 0.30",
            outputCode = "MACRO_BEARISH_BOTTOM_EXHAUSTION",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.35,
                val60m = -1.60,
                expectedMatched = true,
                expectedOutputCode = "MACRO_BEARISH_BOTTOM_EXHAUSTION",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m <= -1.50 && ctx.val5m >= 0.30 }
        ),

        // M103: Fakeout Trap Failed Breakdown Retest
        QuantitativeMatrix(
            id = "M103",
            title = "Failed Breakdown Bullish Absorption",
            description = "5m dipped briefly to negative, but now re-crosses positive with higher 60m support.",
            priority = 745,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.size >= 2 and history[0].val5m < 0 and val5m > 0.15 and val60m > 0.20",
            outputCode = "FAILED_BREAKDOWN_ABSORPTION",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.25,
                val60m = 0.35,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -0.10, val60m = 0.30),
                    com.example.data.models.MetricSnapshot(val5m = 0.15, val60m = 0.30)
                ),
                expectedMatched = true,
                expectedOutputCode = "FAILED_BREAKDOWN_ABSORPTION",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx ->
                if (ctx.history.size < 2) return@QuantitativeMatrix false
                ctx.history[0].val5m < 0 && ctx.val5m > 0.15 && ctx.val60m > 0.20
            }
        ),

        // M104: Fakeout Trap Failed Breakout Rejection
        QuantitativeMatrix(
            id = "M104",
            title = "Failed Breakout Bearish Rejection",
            description = "5m spiked briefly positive, but now re-crosses negative with lower 60m resistance.",
            priority = 745,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.size >= 2 and history[0].val5m > 0 and val5m < -0.15 and val60m < -0.20",
            outputCode = "FAILED_BREAKOUT_REJECTION",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = -0.25,
                val60m = -0.35,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = 0.10, val60m = -0.30),
                    com.example.data.models.MetricSnapshot(val5m = -0.15, val60m = -0.30)
                ),
                expectedMatched = true,
                expectedOutputCode = "FAILED_BREAKOUT_REJECTION",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx ->
                if (ctx.history.size < 2) return@QuantitativeMatrix false
                ctx.history[0].val5m > 0 && ctx.val5m < -0.15 && ctx.val60m < -0.20
            }
        ),

        // M105: Reversal Failure Confirmation (Top Trap Invalidation)
        QuantitativeMatrix(
            id = "M105",
            title = "Top Trap Invalidation Re-Acceleration",
            description = "Potential top fakeout invalidated as 5m re-surges above +0.35% with 60m expanding.",
            priority = 740,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m >= 0.35 and val60m >= 0.40 and netSum >= 0.75",
            outputCode = "TOP_TRAP_INVALIDATED_UP",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.45,
                val60m = 0.50,
                expectedMatched = true,
                expectedOutputCode = "TOP_TRAP_INVALIDATED_UP",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m >= 0.35 && ctx.val60m >= 0.40 && ctx.netSum >= 0.75 }
        ),

        // M106: Reversal Failure Confirmation (Bottom Trap Invalidation)
        QuantitativeMatrix(
            id = "M106",
            title = "Bottom Trap Invalidation Re-Acceleration",
            description = "Potential bottom fakeout invalidated as 5m plunges below -0.35% with 60m expanding downward.",
            priority = 740,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m <= -0.35 and val60m <= -0.40 and netSum <= -0.75",
            outputCode = "BOTTOM_TRAP_INVALIDATED_DOWN",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.45,
                val60m = -0.50,
                expectedMatched = true,
                expectedOutputCode = "BOTTOM_TRAP_INVALIDATED_DOWN",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m <= -0.35 && ctx.val60m <= -0.40 && ctx.netSum <= -0.75 }
        ),

        // M107: Dual Horizon Reversal Convergence
        QuantitativeMatrix(
            id = "M107",
            title = "Dual Horizon Reversal Convergence (Bullish)",
            description = "Both 5m and 60m transition from negative history into positive territory simultaneously.",
            priority = 755,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.isNotEmpty() and history[0].netSum < 0 and val5m > 0.15 and val60m > 0.10",
            outputCode = "DUAL_HORIZON_REVERSAL_BULLISH",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.20,
                val60m = 0.15,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -0.10, val60m = -0.15, netSum = -0.25)
                ),
                expectedMatched = true,
                expectedOutputCode = "DUAL_HORIZON_REVERSAL_BULLISH",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                ctx.history[0].netSum < 0 && ctx.val5m > 0.15 && ctx.val60m > 0.10
            }
        ),

        // M108: Dual Horizon Reversal Convergence Bearish
        QuantitativeMatrix(
            id = "M108",
            title = "Dual Horizon Reversal Convergence (Bearish)",
            description = "Both 5m and 60m transition from positive history into negative territory simultaneously.",
            priority = 755,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.isNotEmpty() and history[0].netSum > 0 and val5m < -0.15 and val60m < -0.10",
            outputCode = "DUAL_HORIZON_REVERSAL_BEARISH",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = -0.20,
                val60m = -0.15,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = 0.10, val60m = 0.15, netSum = 0.25)
                ),
                expectedMatched = true,
                expectedOutputCode = "DUAL_HORIZON_REVERSAL_BEARISH",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                ctx.history[0].netSum > 0 && ctx.val5m < -0.15 && ctx.val60m < -0.10
            }
        )
    )
}
