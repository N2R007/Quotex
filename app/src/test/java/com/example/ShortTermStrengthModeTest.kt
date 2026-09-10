package com.example

import com.example.data.analyzer.CanonicalDecisionEngine
import com.example.data.analyzer.MicroKineticVectorEngine
import com.example.data.analyzer.ReactiveMarketPressureEngine
import com.example.data.models.DataQualityState
import com.example.data.models.DecisionMode
import com.example.data.models.TradeDirection
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying [DecisionMode.SHORT_TERM_STRENGTH] in [CanonicalDecisionEngine].
 *
 * Requirements:
 * 1. PRIMARY: ReactiveMarketPressureEngine.calculate() -> upPressure vs downPressure.
 * 2. CONFIRMATION: MicroKineticVectorEngine.calculate() -> kineticUpWeight vs kineticDownWeight.
 * 3. Agreement: Both engines agree -> clean entry (warningOnly = false).
 * 4. Conflict: Engines diverge -> Primary RMP direction is authoritative, but warningOnly = true.
 * 5. Dead-band safety: diff < 2.0 yields NEUTRAL / NO_TRADE.
 * 6. Unavailable data: when both v5 and v60 are null, yields DATA_UNAVAILABLE / NO_TRADE.
 * 7. Seamless switching between LEGACY_MULTILAYER and SHORT_TERM_STRENGTH modes.
 */
class ShortTermStrengthModeTest {

    @Before
    fun setUp() {
        CanonicalDecisionEngine.currentDecisionMode = DecisionMode.SHORT_TERM_STRENGTH
    }

    @After
    fun tearDown() {
        // Reset to default
        CanonicalDecisionEngine.currentDecisionMode = DecisionMode.LEGACY_MULTILAYER
    }

    @Test
    fun testBullishMomentum_dualEngineAgreement_producesBuyEntry() {
        // Strong upward metrics: 5m = +1.20%, 60m = +0.80%, 1d = +2.00%
        val kinetic = MicroKineticVectorEngine.calculate(
            val5m = 1.20,
            val60m = 0.80,
            val1d = 2.00,
            history = emptyList(),
            isApproximate = false
        )

        val rmp = ReactiveMarketPressureEngine.calculate(
            ReactiveMarketPressureEngine.PressureInput(
                current5m = 1.20,
                previous5m = null,
                current60m = 0.80,
                previous60m = null,
                current1d = 2.00,
                previous1d = null,
                elapsedSeconds = 1.0,
                approximate = false
            )
        )

        assertTrue("Expected upPressure > downPressure", rmp.upPressure > rmp.downPressure)
        assertTrue("Expected kineticUpWeight > kineticDownWeight", kinetic.kineticUpWeight > kinetic.kineticDownWeight)

        val decision = CanonicalDecisionEngine.evaluate(
            val5m = 1.20,
            val60m = 0.80,
            val1d = 2.00,
            rmpDecision = rmp,
            kineticResult = kinetic,
            currentTimeMs = System.currentTimeMillis(),
            isApproximate = false,
            decisionMode = DecisionMode.SHORT_TERM_STRENGTH
        )

        assertEquals("Direction must be UP", TradeDirection.UP, decision.direction)
        assertEquals("Side must be BUY", "BUY", decision.side)
        assertTrue("Execution must be eligible", decision.executionEligibility)
        assertFalse("Must not be noTrade", decision.noTrade)
        assertFalse("Engines agree, so warningOnly must be false", decision.warningOnly)
        assertTrue("Explanation should mention short-term pressure", decision.explanation.contains("শর্ট-টার্ম প্রেসার মোড"))
    }

    @Test
    fun testBearishMomentum_dualEngineAgreement_producesSellEntry() {
        // Strong downward metrics: 5m = -1.50%, 60m = -0.90%, 1d = -2.50%
        val kinetic = MicroKineticVectorEngine.calculate(
            val5m = -1.50,
            val60m = -0.90,
            val1d = -2.50,
            history = emptyList(),
            isApproximate = false
        )

        val rmp = ReactiveMarketPressureEngine.calculate(
            ReactiveMarketPressureEngine.PressureInput(
                current5m = -1.50,
                previous5m = null,
                current60m = -0.90,
                previous60m = null,
                current1d = -2.50,
                previous1d = null,
                elapsedSeconds = 1.0,
                approximate = false
            )
        )

        assertTrue("Expected downPressure > upPressure", rmp.downPressure > rmp.upPressure)
        assertTrue("Expected kineticDownWeight > kineticUpWeight", kinetic.kineticDownWeight > kinetic.kineticUpWeight)

        val decision = CanonicalDecisionEngine.evaluate(
            val5m = -1.50,
            val60m = -0.90,
            val1d = -2.50,
            rmpDecision = rmp,
            kineticResult = kinetic,
            currentTimeMs = System.currentTimeMillis(),
            isApproximate = false,
            decisionMode = DecisionMode.SHORT_TERM_STRENGTH
        )

        assertEquals("Direction must be DOWN", TradeDirection.DOWN, decision.direction)
        assertEquals("Side must be SELL", "SELL", decision.side)
        assertTrue("Execution must be eligible", decision.executionEligibility)
        assertFalse("Must not be noTrade", decision.noTrade)
        assertFalse("Engines agree, so warningOnly must be false", decision.warningOnly)
    }

    @Test
    fun testConflictScenario_primaryPressurePrioritized_warningFlagSet() {
        // Create an intentional divergence between RMP pressure and MicroKinetic
        // RMP has UP pressure, but Kinetic has DOWN weight
        val fakeRmp = ReactiveMarketPressureEngine.PressureDecision(
            direction = TradeDirection.UP,
            upPercentage = 75.0,
            downPercentage = 25.0,
            upPressure = 0.85,
            downPressure = 0.15,
            pressureStrength = 80.0,
            movement = 0.70,
            confidencePenalty = 0.0,
            availableTimeframes = setOf(ReactiveMarketPressureEngine.Timeframe.FIVE_MIN),
            fingerprint = "TEST_FP",
            explanation = "Primary UP pressure"
        )

        val fakeKinetic = MicroKineticVectorEngine.KineticVectorResult(
            microVelocity = -0.5,
            kineticBaseEnergy = 1.0,
            netKineticForce = -0.8,
            kineticUpWeight = 30.0,
            kineticDownWeight = 70.0, // Kinetic points DOWN
            vectors = emptyMap(),
            primaryDriverTimeframe = "5M",
            footprint = MicroKineticVectorEngine.ForensicMarketFootprint(
                code = "FP_TEST",
                title = "Test Footprint",
                diagnosis = "Diagnosis",
                rootCause = "Root Cause",
                confidenceScore = 0.90,
                isBrakeInertiaPullback = false
            ),
            mathematicalProof = "Proof"
        )

        val decision = CanonicalDecisionEngine.evaluate(
            val5m = 0.50,
            val60m = 0.20,
            val1d = 0.10,
            rmpDecision = fakeRmp,
            kineticResult = fakeKinetic,
            currentTimeMs = System.currentTimeMillis(),
            isApproximate = false,
            decisionMode = DecisionMode.SHORT_TERM_STRENGTH
        )

        // Primary RMP is authoritative -> UP / BUY
        assertEquals("Primary engine takes precedence for UP", TradeDirection.UP, decision.direction)
        assertEquals("Side must be BUY", "BUY", decision.side)
        assertTrue("Warning flag must be TRUE due to kinetic conflict", decision.warningOnly)
        assertTrue("Conflict flag must be true", decision.conflict)
        assertTrue("Explanation must mention kinetic divergence", decision.explanation.contains("সতর্কতা: কাইনেটিক ডাইভারজেন্স"))
    }

    @Test
    fun testDeadBandSafety_whenDifferenceLessThanTwo_yieldsNoTrade() {
        // Zero metrics: 5m = 0.00%, 60m = 0.00%, 1d = 0.00%
        val kinetic = MicroKineticVectorEngine.calculate(
            val5m = 0.00,
            val60m = 0.00,
            val1d = 0.00,
            history = emptyList(),
            isApproximate = false
        )

        val decision = CanonicalDecisionEngine.evaluate(
            val5m = 0.00,
            val60m = 0.00,
            val1d = 0.00,
            kineticResult = kinetic,
            currentTimeMs = System.currentTimeMillis(),
            isApproximate = false,
            decisionMode = DecisionMode.SHORT_TERM_STRENGTH
        )

        assertEquals("Direction must be NEUTRAL", TradeDirection.NEUTRAL, decision.direction)
        assertEquals("Side must be NONE", "NONE", decision.side)
        assertFalse("Execution must not be eligible", decision.executionEligibility)
        assertTrue("Must be noTrade", decision.noTrade)
        assertEquals(50.0, decision.upPercentage, 0.01)
        assertEquals(50.0, decision.downPercentage, 0.01)
        assertTrue("Explanation must indicate balance / no trade", decision.explanation.contains("ভারসাম্য") || decision.explanation.contains("পার্থক্য < ২.০%"))
    }

    @Test
    fun testDataUnavailable_whenV5AndV60Null_yieldsDataUnavailableNoTrade() {
        val kinetic = MicroKineticVectorEngine.calculate(
            val5m = null,
            val60m = null,
            val1d = null,
            history = emptyList(),
            isApproximate = false
        )

        val decision = CanonicalDecisionEngine.evaluate(
            val5m = null,
            val60m = null,
            val1d = null,
            kineticResult = kinetic,
            currentTimeMs = System.currentTimeMillis(),
            isApproximate = false,
            decisionMode = DecisionMode.SHORT_TERM_STRENGTH
        )

        assertEquals("Direction must be NEUTRAL", TradeDirection.NEUTRAL, decision.direction)
        assertEquals("Side must be NONE", "NONE", decision.side)
        assertFalse("Execution must not be eligible", decision.executionEligibility)
        assertTrue("Must be noTrade", decision.noTrade)
        assertEquals(DataQualityState.UNAVAILABLE, decision.dataQuality)
        assertTrue("Confirmation stage must be DATA_UNAVAILABLE", decision.confirmationStage.contains("DATA_UNAVAILABLE"))
    }

    @Test
    fun testModeSwitching_legacyMultilayerPreservesOriginalLogic() {
        val kinetic = MicroKineticVectorEngine.calculate(
            val5m = 0.85,
            val60m = 0.40,
            val1d = 1.20,
            history = emptyList(),
            isApproximate = false
        )

        val legacyDecision = CanonicalDecisionEngine.evaluate(
            val5m = 0.85,
            val60m = 0.40,
            val1d = 1.20,
            kineticResult = kinetic,
            currentTimeMs = System.currentTimeMillis(),
            isApproximate = false,
            decisionMode = DecisionMode.LEGACY_MULTILAYER
        )

        assertEquals(TradeDirection.UP, legacyDecision.direction)
        assertEquals("BUY", legacyDecision.side)
        assertTrue(legacyDecision.executionEligibility)

        val shortTermDecision = CanonicalDecisionEngine.evaluate(
            val5m = 0.85,
            val60m = 0.40,
            val1d = 1.20,
            kineticResult = kinetic,
            currentTimeMs = System.currentTimeMillis(),
            isApproximate = false,
            decisionMode = DecisionMode.SHORT_TERM_STRENGTH
        )

        assertEquals(TradeDirection.UP, shortTermDecision.direction)
        assertEquals("BUY", shortTermDecision.side)
        assertTrue(shortTermDecision.executionEligibility)
    }

    @Test
    fun testReactiveMarketPressureEngine_integrationWithShortTermMode() {
        CanonicalDecisionEngine.currentDecisionMode = DecisionMode.SHORT_TERM_STRENGTH

        val analysis = ReactiveMarketPressureEngine.buildTradingAnalysis(
            val5m = 0.90,
            val60m = 0.45,
            val1d = 1.50,
            prefix5m = "+0.90%",
            prefix60m = "+0.45%",
            history = emptyList(),
            isApprox = false
        )

        assertEquals(TradeDirection.UP, analysis.direction)
        assertEquals("BUY", analysis.canonicalDecision?.side)
        assertTrue(analysis.canonicalDecision?.executionEligibility == true)
        assertFalse(analysis.isNoTradeZone)
        assertTrue(analysis.canonicalDecision?.explanation?.contains("শর্ট-টার্ম প্রেসার মোড") == true)
    }

    @Test
    fun testFallbackPath_withHistory_computesPreviousMetricsAccurately() {
        CanonicalDecisionEngine.currentDecisionMode = DecisionMode.SHORT_TERM_STRENGTH

        val history = listOf(
            com.example.data.models.MetricSnapshot(
                val5m = 0.30,
                val60m = 0.10,
                val1d = 0.50,
                timestamp = System.currentTimeMillis() - 2000
            )
        )

        // Without passing explicit rmpDecision or kineticResult, triggering internal fallback path
        val decision = CanonicalDecisionEngine.evaluate(
            val5m = 0.95,
            val60m = 0.40,
            val1d = 1.10,
            history = history,
            rmpDecision = null,
            kineticResult = null,
            decisionMode = DecisionMode.SHORT_TERM_STRENGTH
        )

        assertEquals(TradeDirection.UP, decision.direction)
        assertEquals("BUY", decision.side)
        assertTrue(decision.executionEligibility)
        assertFalse(decision.noTrade)
    }
}
