package com.example.data.matrix

import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Matrices M109 - M120: Alternation, Whipsaw & Choppy Market Regime Matrices.
 * Detects frequent sign reversals and unstable persistence.
 */
object AlternationChopMatrices {

    val matrices: List<QuantitativeMatrix> = listOf(
        // M109: Sign Flip-Flop Whipsaw (Scan N opposes N-1)
        QuantitativeMatrix(
            id = "M109",
            title = "Two-Scan Sign Whipsaw",
            description = "5m flipped polarity compared to previous scan (e.g. +0.25% to -0.25%). Unstable liquidity.",
            priority = 800,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.isNotEmpty() and (val5m * history[0].val5m < 0) and abs(val5m) >= 0.15",
            outputCode = "TWO_SCAN_SIGN_WHIPSAW",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.EXTREME,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = -0.22,
                val60m = 0.10,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = 0.25, val60m = 0.10)
                ),
                expectedMatched = true,
                expectedOutputCode = "TWO_SCAN_SIGN_WHIPSAW",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                (ctx.val5m * ctx.history[0].val5m < 0) && abs(ctx.val5m) >= 0.15
            }
        ),

        // M110: Three-Scan Alternation Ping-Pong
        QuantitativeMatrix(
            id = "M110",
            title = "Three-Scan Alternation Ping-Pong",
            description = "Sign has alternated positive-negative-positive across 3 consecutive scans. Violent chop.",
            priority = 810,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.size >= 2 and (val5m * history[0].val5m < 0) and (history[0].val5m * history[1].val5m < 0)",
            outputCode = "THREE_SCAN_PING_PONG_CHOP",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.EXTREME,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.20,
                val60m = 0.05,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -0.20, val60m = 0.05),
                    com.example.data.models.MetricSnapshot(val5m = 0.25, val60m = 0.05)
                ),
                expectedMatched = true,
                expectedOutputCode = "THREE_SCAN_PING_PONG_CHOP",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx ->
                if (ctx.history.size < 2) return@QuantitativeMatrix false
                val h0 = ctx.history[0].val5m
                val h1 = ctx.history[1].val5m
                (ctx.val5m * h0 < 0) && (h0 * h1 < 0)
            }
        ),

        // M111: Narrow Range Oscillation (Bound within ±0.20%)
        QuantitativeMatrix(
            id = "M111",
            title = "Narrow Range Chop Band",
            description = "All recent scans remain trapped inside ±0.20% with changing signs. No trend direction.",
            priority = 795,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.size >= 2 and abs(val5m) <= 0.20 and abs(history[0].val5m) <= 0.20 and abs(history[1].val5m) <= 0.20",
            outputCode = "NARROW_RANGE_CHOP_BAND",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.15,
                val60m = 0.08,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -0.12, val60m = 0.08),
                    com.example.data.models.MetricSnapshot(val5m = 0.18, val60m = 0.08)
                ),
                expectedMatched = true,
                expectedOutputCode = "NARROW_RANGE_CHOP_BAND",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx ->
                if (ctx.history.size < 2) return@QuantitativeMatrix false
                abs(ctx.val5m) <= 0.20 && abs(ctx.history[0].val5m) <= 0.20 && abs(ctx.history[1].val5m) <= 0.20
            }
        ),

        // M112: Net Sum Instability Variance
        QuantitativeMatrix(
            id = "M112",
            title = "Net Sum Variance Instability",
            description = "Net sum swings wildly by > 0.80% in single interval without fundamental shift.",
            priority = 805,
            requiredInputs = setOf(InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.isNotEmpty() and abs(netSum - history[0].netSum) >= 0.80",
            outputCode = "NET_SUM_VARIANCE_INSTABILITY",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.50,
                val60m = 0.20,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -0.30, val60m = 0.10, netSum = -0.20)
                ),
                expectedMatched = true,
                expectedOutputCode = "NET_SUM_VARIANCE_INSTABILITY",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                abs(ctx.netSum - ctx.history[0].netSum) >= 0.80
            }
        ),

        // M113: Low Persistence Rate Warning (< 40%)
        QuantitativeMatrix(
            id = "M113",
            title = "Low Trend Persistence Regimes",
            description = "Less than 40% of recent snapshots agree with current direction sign.",
            priority = 790,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.size >= 3 and persistenceRatio < 0.40",
            outputCode = "LOW_TREND_PERSISTENCE",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.25,
                val60m = 0.10,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -0.20, val60m = 0.10),
                    com.example.data.models.MetricSnapshot(val5m = -0.30, val60m = 0.10),
                    com.example.data.models.MetricSnapshot(val5m = -0.25, val60m = 0.10)
                ),
                expectedMatched = true,
                expectedOutputCode = "LOW_TREND_PERSISTENCE",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx ->
                if (ctx.history.size < 3) return@QuantitativeMatrix false
                val sameSignCount = ctx.history.count { (it.val5m > 0 && ctx.val5m > 0) || (it.val5m < 0 && ctx.val5m < 0) }
                (sameSignCount.toDouble() / ctx.history.size) < 0.40
            }
        ),

        // M114: Directional Flip with Inverted 60m
        QuantitativeMatrix(
            id = "M114",
            title = "Inverted Directional Cross Chop",
            description = "5m and 60m cross each other's thresholds in reverse order.",
            priority = 785,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m in 0.15..0.30 and val60m in -0.30..-0.15",
            outputCode = "INVERTED_DIRECTIONAL_CHOP",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.22,
                val60m = -0.22,
                expectedMatched = true,
                expectedOutputCode = "INVERTED_DIRECTIONAL_CHOP",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.val5m in 0.15..0.30 && ctx.val60m in -0.30..-0.15 }
        ),

        // M115: High-Frequency Sign Alternation Filter
        QuantitativeMatrix(
            id = "M115",
            title = "High Frequency Sign Alternation",
            description = "4 or more sign flips in last 5 snapshots. Directional trading blocked.",
            priority = 815,
            requiredInputs = setOf(InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.size >= 4 and signFlips >= 3",
            outputCode = "HIGH_FREQ_ALTERNATION_BLOCKED",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.EXTREME,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.15,
                val60m = 0.05,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -0.15, val60m = 0.05),
                    com.example.data.models.MetricSnapshot(val5m = 0.12, val60m = 0.05),
                    com.example.data.models.MetricSnapshot(val5m = -0.18, val60m = 0.05),
                    com.example.data.models.MetricSnapshot(val5m = 0.14, val60m = 0.05)
                ),
                expectedMatched = true,
                expectedOutputCode = "HIGH_FREQ_ALTERNATION_BLOCKED",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx ->
                if (ctx.history.size < 4) return@QuantitativeMatrix false
                var flips = 0
                var lastVal = ctx.val5m
                for (h in ctx.history) {
                    if (h.val5m * lastVal < 0) flips++
                    lastVal = h.val5m
                }
                flips >= 3
            }
        ),

        // M116: Spread Noise Dominated Regime
        QuantitativeMatrix(
            id = "M116",
            title = "Spread Noise Dominated Regime",
            description = "Changes are tiny (<= 0.15%) and alternating without breakout.",
            priority = 780,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val5m) <= 0.15 and abs(val60m) <= 0.15 and (val5m * val60m < 0)",
            outputCode = "SPREAD_NOISE_REGIME",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.12,
                val60m = -0.13,
                expectedMatched = true,
                expectedOutputCode = "SPREAD_NOISE_REGIME",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> abs(ctx.val5m) <= 0.15 && abs(ctx.val60m) <= 0.15 && (ctx.val5m * ctx.val60m < 0) }
        ),

        // M117: Unstable Horizon Ratio Discrepancy
        QuantitativeMatrix(
            id = "M117",
            title = "Unstable Horizon Discrepancy",
            description = "5m is jumping unpredictably while 60m stays dead flat (<= 0.05%).",
            priority = 775,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val5m) >= 0.40 and abs(val60m) <= 0.05",
            outputCode = "UNSTABLE_HORIZON_DISCREPANCY",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.45,
                val60m = 0.03,
                expectedMatched = true,
                expectedOutputCode = "UNSTABLE_HORIZON_DISCREPANCY",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> abs(ctx.val5m) >= 0.40 && abs(ctx.val60m) <= 0.05 }
        ),

        // M118: Random Walk Drift Verification
        QuantitativeMatrix(
            id = "M118",
            title = "Random Walk Drift Regime",
            description = "Net sum shifts randomly around zero without directional drift.",
            priority = 770,
            requiredInputs = setOf(InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.size >= 2 and abs(netSum) <= 0.10 and abs(history[0].netSum) <= 0.10",
            outputCode = "RANDOM_WALK_DRIFT",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.05,
                val60m = -0.04,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -0.06, val60m = 0.05, netSum = -0.01)
                ),
                expectedMatched = true,
                expectedOutputCode = "RANDOM_WALK_DRIFT",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                abs(ctx.netSum) <= 0.10 && abs(ctx.history[0].netSum) <= 0.10
            }
        ),

        // M119: Choppy Alternation Breakout Candidate
        QuantitativeMatrix(
            id = "M119",
            title = "Chop Band Breakout Candidate",
            description = "After prolonged chop, 5m pushes to +0.35% with 60m beginning to lift.",
            priority = 765,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.isNotEmpty() and abs(history[0].val5m) <= 0.15 and val5m >= 0.35 and val60m >= 0.15",
            outputCode = "CHOP_BREAKOUT_CANDIDATE_UP",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.38,
                val60m = 0.18,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = 0.10, val60m = 0.10)
                ),
                expectedMatched = true,
                expectedOutputCode = "CHOP_BREAKOUT_CANDIDATE_UP",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                abs(ctx.history[0].val5m) <= 0.15 && ctx.val5m >= 0.35 && ctx.val60m >= 0.15
            }
        ),

        // M120: Choppy Alternation Breakdown Candidate
        QuantitativeMatrix(
            id = "M120",
            title = "Chop Band Breakdown Candidate",
            description = "After prolonged chop, 5m collapses to -0.35% with 60m beginning to drop.",
            priority = 765,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.isNotEmpty() and abs(history[0].val5m) <= 0.15 and val5m <= -0.35 and val60m <= -0.15",
            outputCode = "CHOP_BREAKDOWN_CANDIDATE_DOWN",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.LOW,
            warningOnly = false,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = -0.38,
                val60m = -0.18,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -0.10, val60m = -0.10)
                ),
                expectedMatched = true,
                expectedOutputCode = "CHOP_BREAKDOWN_CANDIDATE_DOWN",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                abs(ctx.history[0].val5m) <= 0.15 && ctx.val5m <= -0.35 && ctx.val60m <= -0.15
            }
        )
    )
}
