package com.example.data.matrix

import com.example.data.models.TradeDirection
import kotlin.math.abs

/**
 * Matrices M001 - M014: Optical, Sensor & Data Quality Integrity Guards.
 */
object DataQualityMatrices {

    val matrices: List<QuantitativeMatrix> = listOf(
        // M001: Data Unavailable / Total Optical Failure
        QuantitativeMatrix(
            id = "M001",
            title = "Data Unavailable (Optical Failure)",
            description = "Frame capture contains no readable numerical metrics or optical scan failed completely.",
            priority = 1000,
            requiredInputs = setOf(InputType.DATA_QUALITY),
            conditionDescription = "dataQuality == UNAVAILABLE or both 5m and 60m are NaN",
            outputCode = "DATA_UNAVAILABLE",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = Double.NaN,
                val60m = Double.NaN,
                dataQuality = "UNAVAILABLE",
                expectedMatched = true,
                expectedOutputCode = "DATA_UNAVAILABLE",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.dataQuality == "UNAVAILABLE" || ctx.val5m.isNaN() || ctx.val60m.isNaN() }
        ),

        // M002: Incomplete 5m Missing
        QuantitativeMatrix(
            id = "M002",
            title = "Incomplete Frame (5m Change Missing)",
            description = "The 5-minute metric was obscured, out of bounds, or unreadable.",
            priority = 990,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.DATA_QUALITY),
            conditionDescription = "val5m is NaN or invalid while val60m is present",
            outputCode = "INCOMPLETE_5M_MISSING",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = Double.NaN,
                val60m = 0.45,
                expectedMatched = true,
                expectedOutputCode = "INCOMPLETE_5M_MISSING",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.val5m.isNaN() && !ctx.val60m.isNaN() }
        ),

        // M003: Incomplete 60m Missing
        QuantitativeMatrix(
            id = "M003",
            title = "Incomplete Frame (60m Change Missing)",
            description = "The 60-minute macro anchor metric was obscured or missing.",
            priority = 990,
            requiredInputs = setOf(InputType.CHANGE_60M, InputType.DATA_QUALITY),
            conditionDescription = "val60m is NaN or invalid while val5m is present",
            outputCode = "INCOMPLETE_60M_MISSING",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.35,
                val60m = Double.NaN,
                expectedMatched = true,
                expectedOutputCode = "INCOMPLETE_60M_MISSING",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val5m.isNaN() && ctx.val60m.isNaN() }
        ),

        // M004: Ambiguous Sign Polarities
        QuantitativeMatrix(
            id = "M004",
            title = "Ambiguous Sign Polarity",
            description = "OCR detected conflicting signs (+ and - in same token) or contradictory glyphs.",
            priority = 980,
            requiredInputs = setOf(InputType.DATA_QUALITY),
            conditionDescription = "dataQuality == AMBIGUOUS",
            outputCode = "AMBIGUOUS_SIGN_POLARITY",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.50,
                val60m = 0.50,
                dataQuality = "AMBIGUOUS",
                expectedMatched = true,
                expectedOutputCode = "AMBIGUOUS_SIGN_POLARITY",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.dataQuality == "AMBIGUOUS" }
        ),

        // M005: Ambiguous Color/Sign Disagreement
        QuantitativeMatrix(
            id = "M005",
            title = "Ambiguous Color-Sign Disagreement",
            description = "Pixel color polarity contradicts numeric sign token (e.g. green text with minus).",
            priority = 975,
            requiredInputs = setOf(InputType.DATA_QUALITY),
            conditionDescription = "dataQuality == COLOR_SIGN_MISMATCH or AMBIGUOUS",
            outputCode = "AMBIGUOUS_COLOR_MISMATCH",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = -0.30,
                val60m = -0.30,
                dataQuality = "COLOR_SIGN_MISMATCH",
                expectedMatched = true,
                expectedOutputCode = "AMBIGUOUS_COLOR_MISMATCH",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.dataQuality == "COLOR_SIGN_MISMATCH" }
        ),

        // M006: Approximate 5m Data Warning
        QuantitativeMatrix(
            id = "M006",
            title = "Approximate 5m Data Flag",
            description = "5m token was preceded by approximate operator (~ / ≈) or marked unverified.",
            priority = 960,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.DATA_QUALITY),
            conditionDescription = "dataQuality == APPROXIMATE",
            outputCode = "APPROXIMATE_5M_WARNING",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.45,
                val60m = 0.30,
                dataQuality = "APPROXIMATE",
                expectedMatched = true,
                expectedOutputCode = "APPROXIMATE_5M_WARNING",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.dataQuality == "APPROXIMATE" }
        ),

        // M007: Approximate 60m Data Warning
        QuantitativeMatrix(
            id = "M007",
            title = "Approximate 60m Data Flag",
            description = "60m macro metric contains approximate symbol; signals must be suppressed.",
            priority = 960,
            requiredInputs = setOf(InputType.CHANGE_60M, InputType.DATA_QUALITY),
            conditionDescription = "dataQuality == APPROXIMATE_60M or (dataQuality == APPROXIMATE and val60m != 0)",
            outputCode = "APPROXIMATE_60M_WARNING",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.HIGH,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.30,
                val60m = 0.45,
                dataQuality = "APPROXIMATE_60M",
                expectedMatched = true,
                expectedOutputCode = "APPROXIMATE_60M_WARNING",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.dataQuality == "APPROXIMATE_60M" }
        ),

        // M008: Approximate Both 5m and 60m Data Warning
        QuantitativeMatrix(
            id = "M008",
            title = "Approximate Dual Horizon Warning",
            description = "Both short and intermediate horizons contain approximate markers.",
            priority = 965,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M, InputType.DATA_QUALITY),
            conditionDescription = "dataQuality == APPROXIMATE_BOTH",
            outputCode = "APPROXIMATE_DUAL_WARNING",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.55,
                val60m = 0.55,
                dataQuality = "APPROXIMATE_BOTH",
                expectedMatched = true,
                expectedOutputCode = "APPROXIMATE_DUAL_WARNING",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.dataQuality == "APPROXIMATE_BOTH" }
        ),

        // M009: Extreme Outlier 5m Beyond Allowed Range (> 500%)
        QuantitativeMatrix(
            id = "M009",
            title = "Extreme Outlier 5m Range Violation",
            description = "5m change exceeds maximum realistic range (> 500.0%).",
            priority = 995,
            requiredInputs = setOf(InputType.CHANGE_5M),
            conditionDescription = "abs(val5m) > 500.0",
            outputCode = "OUTLIER_5M_RANGE_VIOLATION",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 501.0,
                val60m = 0.25,
                expectedMatched = true,
                expectedOutputCode = "OUTLIER_5M_RANGE_VIOLATION",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val5m.isNaN() && abs(ctx.val5m) > 500.0 }
        ),

        // M010: Extreme Outlier 60m Beyond Allowed Range (> 500%)
        QuantitativeMatrix(
            id = "M010",
            title = "Extreme Outlier 60m Range Violation",
            description = "60m change exceeds maximum realistic range (> 500.0%).",
            priority = 995,
            requiredInputs = setOf(InputType.CHANGE_60M),
            conditionDescription = "abs(val60m) > 500.0",
            outputCode = "OUTLIER_60M_RANGE_VIOLATION",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.25,
                val60m = -505.0,
                expectedMatched = true,
                expectedOutputCode = "OUTLIER_60M_RANGE_VIOLATION",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val60m.isNaN() && abs(ctx.val60m) > 500.0 }
        ),

        // M011: Conflicting Sign Multi-Token Contamination
        QuantitativeMatrix(
            id = "M011",
            title = "Multi-Token OCR Contamination",
            description = "Multiple conflicting numerical tokens found in a single bounding box region.",
            priority = 970,
            requiredInputs = setOf(InputType.DATA_QUALITY),
            conditionDescription = "dataQuality == MULTI_TOKEN_CONTAMINATION",
            outputCode = "OCR_TOKEN_CONTAMINATION",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.0,
                val60m = 0.0,
                dataQuality = "MULTI_TOKEN_CONTAMINATION",
                expectedMatched = true,
                expectedOutputCode = "OCR_TOKEN_CONTAMINATION",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.dataQuality == "MULTI_TOKEN_CONTAMINATION" }
        ),

        // M012: Zero Value with Non-Zero Sign Anomaly
        QuantitativeMatrix(
            id = "M012",
            title = "Zero Value With Non-Zero Sign Anomaly",
            description = "A reading of +0.00% or -0.00% without directional magnitude.",
            priority = 940,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M),
            conditionDescription = "abs(val5m) < 0.0001 and abs(val60m) < 0.0001",
            outputCode = "ZERO_SIGN_ANOMALY",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.0000,
                val60m = 0.0000,
                expectedMatched = true,
                expectedOutputCode = "ZERO_SIGN_ANOMALY",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> !ctx.val5m.isNaN() && !ctx.val60m.isNaN() && abs(ctx.val5m) < 0.0001 && abs(ctx.val60m) < 0.0001 }
        ),

        // M013: Corrupted Header / Column Mismatch
        QuantitativeMatrix(
            id = "M013",
            title = "Header Column Mismatch",
            description = "The optical recognition matched values to unexpected table headers.",
            priority = 950,
            requiredInputs = setOf(InputType.DATA_QUALITY),
            conditionDescription = "dataQuality == HEADER_MISMATCH",
            outputCode = "HEADER_COLUMN_MISMATCH",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.NO_TRADE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.15,
                val60m = 0.15,
                dataQuality = "HEADER_MISMATCH",
                expectedMatched = true,
                expectedOutputCode = "HEADER_COLUMN_MISMATCH",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.dataQuality == "HEADER_MISMATCH" }
        ),

        // M014: Approximate 1D Context with Valid Core Values
        QuantitativeMatrix(
            id = "M014",
            title = "Approximate 1D Macro Context",
            description = "Core 5m and 60m are verified, but 1D context is approximate and disregarded.",
            priority = 920,
            requiredInputs = setOf(InputType.CHANGE_5M, InputType.CHANGE_60M, InputType.CHANGE_1D),
            conditionDescription = "dataQuality == APPROXIMATE_1D and val5m and val60m are valid",
            outputCode = "APPROXIMATE_1D_REJECTED",
            direction = TradeDirection.NEUTRAL,
            riskLevel = RiskLevel.MODERATE,
            warningOnly = true,
            requiresHistory = false,
            testVector = MatrixTestVector(
                val5m = 0.25,
                val60m = 0.35,
                val1d = 1.20,
                dataQuality = "APPROXIMATE_1D",
                expectedMatched = true,
                expectedOutputCode = "APPROXIMATE_1D_REJECTED",
                expectedDirection = TradeDirection.NEUTRAL
            ),
            evaluator = { ctx -> ctx.dataQuality == "APPROXIMATE_1D" }
        )
    )
}
