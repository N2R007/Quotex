package com.example.data.matrix

import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Matrices M081 - M094: Momentum Deceleration & Velocity Decay Quantitative Rules.
 * Evaluates rate of change decay across current values and rolling snapshot history.
 */
object MomentumDecelerationMatrices {

    val matrices: List<QuantitativeMatrix> = listOf(
        // M081: Bullish Momentum Deceleration Warning (Instantaneous Ratio)
        QuantitativeMatrix(
            id = "M081",
            title = "Bullish Momentum Fade Warning",
            description = "60m is strongly bullish (>= +0.80%) but 5m velocity has decayed to +0.12% - +0.20%. Buying pressure cooling.",
            priority = 700,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m >= 0.80 and val5m in 0.11..0.22",
            outputCode = "BULLISH_MOMENTUM_FADE_WARNING",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.MODERATE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.15,
                val60m = 0.90,
                expectedMatched = true,
                expectedOutputCode = "BULLISH_MOMENTUM_FADE_WARNING",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m >= 0.80 && ctx.val5m in 0.11..0.22 }
        ),

        // M082: Bearish Momentum Deceleration Warning (Instantaneous Ratio)
        QuantitativeMatrix(
            id = "M082",
            title = "Bearish Momentum Fade Warning",
            description = "60m is strongly bearish (<= -0.80%) but 5m selling velocity has decayed to -0.22% to -0.11%. Sellers cooling.",
            priority = 700,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m <= -0.80 and val5m in -0.22..-0.11",
            outputCode = "BEARISH_MOMENTUM_FADE_WARNING",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.MODERATE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.15,
                val60m = -0.90,
                expectedMatched = true,
                expectedOutputCode = "BEARISH_MOMENTUM_FADE_WARNING",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m <= -0.80 && ctx.val5m in -0.22..-0.11 }
        ),

        // M083: Sequential Bullish Decay Over History
        QuantitativeMatrix(
            id = "M083",
            title = "Sequential Bullish Decay Across Snapshots",
            description = "Rolling history shows 5m values strictly decreasing across 3 consecutive scans while positive.",
            priority = 710,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.size >= 2 and h[0].val5m > h[1].val5m > val5m > 0",
            outputCode = "SEQUENTIAL_BULLISH_DECAY",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.MODERATE,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.25,
                val60m = 0.50,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = 0.60, val60m = 0.50),
                    com.example.data.models.MetricSnapshot(val5m = 0.40, val60m = 0.50)
                ),
                expectedMatched = true,
                expectedOutputCode = "SEQUENTIAL_BULLISH_DECAY",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx ->
                if (ctx.history.size < 2) return@QuantitativeMatrix false
                val h0 = ctx.history[0].val5m
                val h1 = ctx.history[1].val5m
                h0 > h1 && h1 > ctx.val5m && ctx.val5m > 0
            }
        ),

        // M084: Sequential Bearish Decay Over History
        QuantitativeMatrix(
            id = "M084",
            title = "Sequential Bearish Decay Across Snapshots",
            description = "Rolling history shows negative 5m values shrinking towards zero across 3 consecutive scans.",
            priority = 710,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.size >= 2 and h[0].val5m < h[1].val5m < val5m < 0",
            outputCode = "SEQUENTIAL_BEARISH_DECAY",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.MODERATE,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = -0.25,
                val60m = -0.50,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -0.60, val60m = -0.50),
                    com.example.data.models.MetricSnapshot(val5m = -0.40, val60m = -0.50)
                ),
                expectedMatched = true,
                expectedOutputCode = "SEQUENTIAL_BEARISH_DECAY",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx ->
                if (ctx.history.size < 2) return@QuantitativeMatrix false
                val h0 = ctx.history[0].val5m
                val h1 = ctx.history[1].val5m
                h0 < h1 && h1 < ctx.val5m && ctx.val5m < 0
            }
        ),

        // M085: Sudden Velocity Drop > 50%
        QuantitativeMatrix(
            id = "M085",
            title = "Sudden Velocity Drop (> 50% Falloff)",
            description = "Previous 5m snapshot was > +0.60%, current 5m dropped by more than half (e.g. to +0.25%).",
            priority = 720,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.isNotEmpty() and history[0].val5m >= 0.60 and val5m <= history[0].val5m * 0.5",
            outputCode = "SUDDEN_VELOCITY_COLLAPSE",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.28,
                val60m = 0.60,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = 0.65, val60m = 0.60)
                ),
                expectedMatched = true,
                expectedOutputCode = "SUDDEN_VELOCITY_COLLAPSE",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                val prev = ctx.history[0].val5m
                prev >= 0.60 && ctx.val5m <= prev * 0.5 && ctx.val5m > 0
            }
        ),

        // M086: Sudden Downward Velocity Drop > 50%
        QuantitativeMatrix(
            id = "M086",
            title = "Sudden Downward Selling Velocity Drop",
            description = "Previous negative 5m snapshot was <= -0.60%, current dropped by more than half.",
            priority = 720,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.isNotEmpty() and history[0].val5m <= -0.60 and val5m >= history[0].val5m * 0.5",
            outputCode = "SUDDEN_SELLING_VELOCITY_COLLAPSE",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = -0.28,
                val60m = -0.60,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -0.65, val60m = -0.60)
                ),
                expectedMatched = true,
                expectedOutputCode = "SUDDEN_SELLING_VELOCITY_COLLAPSE",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                val prev = ctx.history[0].val5m
                prev <= -0.60 && ctx.val5m >= prev * 0.5 && ctx.val5m < 0
            }
        ),

        // M087: Exhaustion Climax Step-Down
        QuantitativeMatrix(
            id = "M087",
            title = "Bullish Climax Step-Down",
            description = "After an extreme spike (> +1.50% net sum), current scan steps down by >= 0.40%.",
            priority = 725,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.isNotEmpty() and history[0].netSum >= 1.50 and (history[0].netSum - netSum) >= 0.40",
            outputCode = "BULLISH_CLIMAX_STEP_DOWN",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.50,
                val60m = 0.60,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = 1.00, val60m = 0.60, netSum = 1.60)
                ),
                expectedMatched = true,
                expectedOutputCode = "BULLISH_CLIMAX_STEP_DOWN",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                val prevNet = ctx.history[0].netSum
                prevNet >= 1.50 && (prevNet - ctx.netSum) >= 0.40
            }
        ),

        // M088: Bearish Climax Step-Up (Selling Relief)
        QuantitativeMatrix(
            id = "M088",
            title = "Bearish Climax Relief Step-Up",
            description = "After an extreme selloff (<= -1.50% net sum), current scan recovers by >= 0.40%.",
            priority = 725,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.isNotEmpty() and history[0].netSum <= -1.50 and (netSum - history[0].netSum) >= 0.40",
            outputCode = "BEARISH_CLIMAX_RELIEF_STEP_UP",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = -0.50,
                val60m = -0.60,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = -1.00, val60m = -0.60, netSum = -1.60)
                ),
                expectedMatched = true,
                expectedOutputCode = "BEARISH_CLIMAX_RELIEF_STEP_UP",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx ->
                if (ctx.history.isEmpty()) return@QuantitativeMatrix false
                val prevNet = ctx.history[0].netSum
                prevNet <= -1.50 && (ctx.netSum - prevNet) >= 0.40
            }
        ),

        // M089: Micro Momentum Stall Before Dead Zone
        QuantitativeMatrix(
            id = "M089",
            title = "Micro Momentum Stall Near Boundary",
            description = "5m has decayed right above the dead zone threshold (+0.11% - +0.14%).",
            priority = 705,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m in 0.11..0.14 and val60m > 0.10",
            outputCode = "MOMENTUM_STALL_NEAR_BOUNDARY",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.MODERATE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.12,
                val60m = 0.35,
                expectedMatched = true,
                expectedOutputCode = "MOMENTUM_STALL_NEAR_BOUNDARY",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val5m in 0.11..0.14 && ctx.val60m > 0.10 }
        ),

        // M090: Micro Downward Momentum Stall Near Boundary
        QuantitativeMatrix(
            id = "M090",
            title = "Micro Downward Stall Near Boundary",
            description = "5m has decayed right below the dead zone threshold (-0.14% to -0.11%).",
            priority = 705,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val5m in -0.14..-0.11 and val60m < -0.10",
            outputCode = "DOWNWARD_STALL_NEAR_BOUNDARY",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.MODERATE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.12,
                val60m = -0.35,
                expectedMatched = true,
                expectedOutputCode = "DOWNWARD_STALL_NEAR_BOUNDARY",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val5m in -0.14..-0.11 && ctx.val60m < -0.10 }
        ),

        // M091: Net Sum Flatlining Across Scans
        QuantitativeMatrix(
            id = "M091",
            title = "Net Sum Plateau Warning",
            description = "Net sum changes by less than 0.02% across 3 scans despite active trading volume.",
            priority = 690,
            requiredInputs = setOf(InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.size >= 2 and abs(netSum - history[0].netSum) < 0.02 and abs(history[0].netSum - history[1].netSum) < 0.02",
            outputCode = "NET_SUM_PLATEAU_WARNING",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.MODERATE,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.30,
                val60m = 0.30,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = 0.31, val60m = 0.30, netSum = 0.61),
                    com.example.data.models.MetricSnapshot(val5m = 0.30, val60m = 0.31, netSum = 0.61)
                ),
                expectedMatched = true,
                expectedOutputCode = "NET_SUM_PLATEAU_WARNING",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx ->
                if (ctx.history.size < 2) return@QuantitativeMatrix false
                abs(ctx.netSum - ctx.history[0].netSum) < 0.02 && abs(ctx.history[0].netSum - ctx.history[1].netSum) < 0.02
            }
        ),

        // M092: Acceleration Reversal Early Warning
        QuantitativeMatrix(
            id = "M092",
            title = "Deceleration Turn Warning",
            description = "5m was accelerating previously but has now inverted second derivative.",
            priority = 715,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.SNAPSHOT_HISTORY),
            conditionDescription = "history.size >= 2 and (history[0].val5m - history[1].val5m) > 0.20 and (val5m - history[0].val5m) < -0.15",
            outputCode = "DECELERATION_TURN_WARNING",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = true,
            testVector = MatrixTestVector(
                val5m = 0.40,
                val60m = 0.50,
                history = listOf(
                    com.example.data.models.MetricSnapshot(val5m = 0.60, val60m = 0.50),
                    com.example.data.models.MetricSnapshot(val5m = 0.35, val60m = 0.50)
                ),
                expectedMatched = true,
                expectedOutputCode = "DECELERATION_TURN_WARNING",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx ->
                if (ctx.history.size < 2) return@QuantitativeMatrix false
                val prevDelta = ctx.history[0].val5m - ctx.history[1].val5m
                val currDelta = ctx.val5m - ctx.history[0].val5m
                prevDelta > 0.20 && currDelta < -0.15
            }
        ),

        // M093: Exhausted Bullish Volume Creep
        QuantitativeMatrix(
            id = "M093",
            title = "Exhausted Bullish Creep",
            description = "60m is high (+1.00%+) but 5m cannot exceed +0.15%. Buyers fully exhausted.",
            priority = 708,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m >= 1.00 and val5m in 0.11..0.15",
            outputCode = "EXHAUSTED_BULLISH_CREEP",
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.13,
                val60m = 1.15,
                expectedMatched = true,
                expectedOutputCode = "EXHAUSTED_BULLISH_CREEP",
                expectedDirection = TradeDirection.UP
            ),
            evaluator = { ctx -> ctx.val60m >= 1.00 && ctx.val5m in 0.11..0.15 }
        ),

        // M094: Exhausted Bearish Volume Creep
        QuantitativeMatrix(
            id = "M094",
            title = "Exhausted Bearish Creep",
            description = "60m is low (<= -1.00%) but 5m cannot exceed -0.15%. Sellers fully exhausted.",
            priority = 708,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "val60m <= -1.00 and val5m in -0.15..-0.11",
            outputCode = "EXHAUSTED_BEARISH_CREEP",
            direction = TradeDirection.DOWN,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.13,
                val60m = -1.15,
                expectedMatched = true,
                expectedOutputCode = "EXHAUSTED_BEARISH_CREEP",
                expectedDirection = TradeDirection.DOWN
            ),
            evaluator = { ctx -> ctx.val60m <= -1.00 && ctx.val5m in -0.15..-0.11 }
        )
    )
}
