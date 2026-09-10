package com.example.data.matrix

import com.example.data.models.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rigorous deterministic validation test suite for the 165 Quantitative Decision Matrices.
 *
 * Enforces:
 * 1. Exact catalog count (165 matrices, zero placeholder or duplicate entries)
 * 2. Strict ID format and uniqueness (M001 to M165)
 * 3. Complete field specifications (no empty condition descriptions or output codes)
 * 4. Exact test vector compliance across all 165 matrices
 * 5. Safety rules: Dead Zone triggering, Conflict resolution, Directional stability
 */
class MatrixCatalogTest {

    @Test
    fun catalog_containsExactCountOf165Matrices() {
        val total = MatrixCatalog.count
        assertEquals("Matrix Catalog must contain exactly 165 deterministic decision matrices", 165, total)
        assertEquals("allMatrices size must match count property", 165, MatrixCatalog.allMatrices.size)
    }

    @Test
    fun catalog_idsAreUniqueAndFollowCanonicalPattern() {
        val ids = MatrixCatalog.allMatrices.map { it.id }
        val uniqueIds = ids.toSet()

        assertEquals("Every matrix must have a strictly unique ID", ids.size, uniqueIds.size)

        // Check sequential numbering M001 through M165
        for (i in 1..165) {
            val expectedId = String.format("M%03d", i)
            assertTrue("Catalog must contain matrix with ID $expectedId", uniqueIds.contains(expectedId))
            val matrix = MatrixCatalog.getById(expectedId)
            assertNotNull("Matrix lookup for $expectedId must not be null", matrix)
            assertEquals("Matrix ID property must match lookup ID", expectedId, matrix?.id)
        }
    }

    @Test
    fun catalog_allMatricesHaveCompleteSpecifications() {
        for (matrix in MatrixCatalog.allMatrices) {
            assertTrue("Matrix ${matrix.id} title must not be blank", matrix.title.isNotBlank())
            assertTrue("Matrix ${matrix.id} description must not be blank", matrix.description.isNotBlank())
            assertTrue("Matrix ${matrix.id} condition description must not be blank", matrix.conditionDescription.isNotBlank())
            assertTrue("Matrix ${matrix.id} output code must not be blank", matrix.outputCode.isNotBlank())
            assertTrue("Matrix ${matrix.id} priority must be positive", matrix.priority > 0)
            assertTrue("Matrix ${matrix.id} must declare required input types", matrix.requiredInputs.isNotEmpty())
            assertNotNull("Matrix ${matrix.id} direction must not be null", matrix.direction)
            assertNotNull("Matrix ${matrix.id} riskLevel must not be null", matrix.riskLevel)
            assertNotNull("Matrix ${matrix.id} test vector must not be null", matrix.testVector)
        }
    }

    @Test
    fun catalog_all165TestVectorsPassDeterministically() {
        var passedCount = 0
        for (matrix in MatrixCatalog.allMatrices) {
            val tv = matrix.testVector
            val ctx = MatrixEvaluationContext(
                val5m = tv.val5m,
                val60m = tv.val60m,
                val1d = tv.val1d,
                dataQuality = tv.dataQuality,
                history = tv.history
            )
            val matched = matrix.evaluator(ctx)
            assertEquals(
                "Matrix ${matrix.id} (${matrix.title}) test vector match failure. Expected: ${tv.expectedMatched}, Actual: $matched",
                tv.expectedMatched,
                matched
            )
            passedCount++
        }
        assertEquals("All 165 test vectors must pass", 165, passedCount)
    }

    @Test
    fun mtfCalculation_threeTimeframeConfluence_calculatesAccurately() {
        // Bullish confluence: 5m = +1.00%, 60m = +1.00%, 1D = +1.00%
        val res = com.example.data.analyzer.TradingOutputParser.calculate5_60_1dPercentage(
            val5m = 1.0,
            val60m = 1.0,
            val1d = 1.0
        )
        assertEquals(TradeDirection.UP, res.direction)
        assertEquals(100.0, res.upPercentage, 0.1)
        assertEquals(0.0, res.downPercentage, 0.1)
        assertTrue(res.isAligned)

        // Bearish confluence: 5m = -1.00%, 60m = -1.00%, 1D = -1.00%
        val bearRes = com.example.data.analyzer.TradingOutputParser.calculate5_60_1dPercentage(
            val5m = -1.0,
            val60m = -1.0,
            val1d = -1.0
        )
        assertEquals(TradeDirection.DOWN, bearRes.direction)
        assertEquals(100.0, bearRes.downPercentage, 0.1)
        assertEquals(0.0, bearRes.upPercentage, 0.1)
        assertTrue(bearRes.isAligned)

        // Conflicted or dead market
        val deadRes = com.example.data.analyzer.TradingOutputParser.calculate5_60_1dPercentage(
            val5m = 0.0,
            val60m = 0.0,
            val1d = 0.0
        )
        assertEquals(TradeDirection.NEUTRAL, deadRes.direction)
        assertEquals(50.0, deadRes.mtfPercentage, 0.1)
    }

    @Test
    fun evaluationEngine_bullishAlignment_producesUpDirection() {
        // Clear bullish momentum: 5m = +1.50%, 60m = +2.20%
        val result = MatrixEvaluationEngine.evaluate(
            val5m = 1.50,
            val60m = 2.20,
            val1d = 3.10
        )

        assertFalse("Strong bullish alignment should not flag conflict", result.isConflicting)
        assertNotNull("Primary matrix must be selected for strong trend", result.primaryMatrix)
        assertEquals(
            "Aligned bullish conditions must produce UP direction",
            TradeDirection.UP,
            result.primaryMatrix?.direction
        )
        assertTrue("Evaluated count must be positive", result.evaluatedMatricesCount > 0)
        assertTrue("Decision trace must be generated", result.decisionTrace.isNotEmpty())
    }

    @Test
    fun evaluationEngine_bearishAlignment_producesDownDirection() {
        // Clear bearish momentum: 5m = -1.80%, 60m = -2.50%
        val result = MatrixEvaluationEngine.evaluate(
            val5m = -1.80,
            val60m = -2.50,
            val1d = -3.20
        )

        assertFalse("Strong bearish alignment should not flag conflict", result.isConflicting)
        assertNotNull("Primary matrix must be selected for strong trend", result.primaryMatrix)
        assertEquals(
            "Aligned bearish conditions must produce DOWN direction",
            TradeDirection.DOWN,
            result.primaryMatrix?.direction
        )
        assertTrue("Evaluated count must be positive", result.evaluatedMatricesCount > 0)
        assertTrue("Decision trace must be generated", result.decisionTrace.isNotEmpty())
    }

    @Test
    fun evaluationEngine_deadZone_triggersNoTradeSafetyRule() {
        // Minimal price movement in chop zone: 5m = +0.02%, 60m = -0.01%
        val result = MatrixEvaluationEngine.evaluate(
            val5m = 0.02,
            val60m = -0.01,
            val1d = 0.00
        )

        val primary = result.primaryMatrix
        assertNotNull("Dead zone must trigger safety matrix", primary)
        assertTrue(
            "Dead zone matrix must enforce NEUTRAL direction or NO_TRADE risk level",
            primary?.direction == TradeDirection.NEUTRAL || primary?.riskLevel == RiskLevel.NO_TRADE || primary?.riskLevel == RiskLevel.EXTREME
        )
    }

    @Test
    fun evaluationEngine_conflictingSignals_flagsConflictSafely() {
        // Sharp divergence: 5m is strongly positive (+2.50%), 60m is strongly negative (-2.80%)
        val result = MatrixEvaluationEngine.evaluate(
            val5m = 2.50,
            val60m = -2.80,
            val1d = null
        )

        val primary = result.primaryMatrix
        assertTrue(
            "Severe multi-timeframe divergence must either flag conflict or select a pullback/trap matrix",
            result.isConflicting || primary?.direction == TradeDirection.NEUTRAL || primary?.riskLevel == RiskLevel.HIGH || primary?.riskLevel == RiskLevel.EXTREME
        )
    }

    @Test
    fun honestyRule_zeroQuantumClaims_deterministicVerification() {
        for (matrix in MatrixCatalog.allMatrices) {
            val combinedText = "${matrix.title} ${matrix.description} ${matrix.conditionDescription}"
            assertFalse(
                "Matrix ${matrix.id} contains misleading 'quantum' claim in text: $combinedText",
                combinedText.contains("quantum computing", ignoreCase = true) ||
                        combinedText.contains("quantum circuit", ignoreCase = true) ||
                        combinedText.contains("qubit", ignoreCase = true)
            )
        }
    }

    @Test
    fun evaluationEngine_whenInputsMissing_reportsActualEvaluatedAndSkippedCounts() {
        // Evaluate with 5m and 60m present, but 1D missing (null)
        val result = MatrixEvaluationEngine.evaluate(
            val5m = 1.20,
            val60m = 1.80,
            val1d = null
        )

        assertEquals("Total count must be 165", 165, result.totalMatricesCount)
        assertTrue("Skipped count must be > 0 when 1D is missing", result.skippedMatricesCount > 0)
        assertTrue("Evaluated count must be less than total count", result.evaluatedMatricesCount < result.totalMatricesCount)
        assertEquals(
            "Sum of evaluated and skipped matrices must equal total count",
            result.totalMatricesCount,
            result.evaluatedMatricesCount + result.skippedMatricesCount
        )
        assertEquals("Failed count must be 0 for valid catalog", 0, result.failedMatricesCount)
        assertTrue("Failure traces must be empty", result.failureTraces.isEmpty())
    }

    @Test
    fun evaluationEngine_failureTraceVisibility_cannotCreateLiveSignal() {
        // Create an intentional faulty matrix that throws an unexpected exception during evaluation
        val faultyMatrix = QuantitativeMatrix(
            id = "M999",
            title = "Faulty Test Matrix",
            description = "Simulates evaluator runtime exception",
            conditionDescription = "Always throws",
            outputCode = "FAULTY_EXCEPTION_SIGNAL",
            priority = 9999, // Artificially high priority to prove it cannot be selected if it fails
            direction = TradeDirection.UP,
            riskLevel = RiskLevel.HIGH,
            requiredInputs = setOf(InputType.CHANGE_5M),
            testVector = MatrixTestVector(0.5, 0.5, null, expectedMatched = true),
            evaluator = { throw IllegalStateException("Deterministic simulated crash in evaluator") }
        )

        val context = MatrixEvaluationContext(
            val5m = 1.0,
            val60m = 1.0,
            val1d = null,
            is5mPresent = true,
            is60mPresent = true,
            is1dPresent = false
        )

        val result = MatrixEvaluationEngine.evaluateMatrices(listOf(faultyMatrix), context)

        assertEquals("Evaluated count must be 1", 1, result.evaluatedMatricesCount)
        assertEquals("Failed count must be 1", 1, result.failedMatricesCount)
        assertEquals("Faulty matrix must NOT be added to matched matrices", 0, result.matchedMatrices.size)
        assertTrue("Failure traces must record the exception", result.failureTraces.isNotEmpty())
        assertTrue("Failure trace must contain matrix ID", result.failureTraces.first().contains("M999"))
        assertTrue("Failure trace must contain error message", result.failureTraces.first().contains("Deterministic simulated crash in evaluator"))
        assertTrue("Decision trace must display failure warning", result.decisionTrace.any { it.contains("Evaluator failures detected") })

        // Most critical: A failed matrix MUST NEVER become the primary matrix or generate a live signal
        assertFalse("Primary matrix output code must not be from failed matrix", result.primaryMatrix?.outputCode == "FAULTY_EXCEPTION_SIGNAL")
    }
}
