package com.example.data.analyzer

import com.example.data.matrix.Directional206MatrixEngine
import com.example.data.models.TradeDirection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
class DCHMEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var engine: DCHMEngine

    @Before
    fun setup() {
        engine = DCHMEngine(
            matrixValidator = Directional206MatrixValidator(),
            scope = testScope,
            enableAutomaticAudioAlerts = false,
            enableAutomaticWebSocketRelay = false
        )
    }

    @Test
    fun testFirstFrameInitializesBaselineWithoutEmittingSignal() = testScope.runTest {
        // First frame establishes baseline (no previous values exist)
        val signal = engine.processInstantDelta(current5mPercent = 0.50, current60mPercent = 1.00)
        assertNull("First scan should set baseline and return null", signal)
    }

    @Test
    fun testDeadZoneThresholdSuppressesMicroJitter() = testScope.runTest {
        // Baseline
        engine.processInstantDelta(current5mPercent = 0.50, current60mPercent = 1.00)

        // Micro-jitter: 0.01% change (below 0.02% dead zone threshold)
        val suppressedSignal = engine.processInstantDelta(
            current5mPercent = 0.51, // Δ5m = +0.01%
            current60mPercent = 1.01  // Δ60m = +0.01%
        )

        assertNull("Deltas under 0.02% must be suppressed by dead-zone threshold", suppressedSignal)
    }

    @Test
    fun testLayer1EmitsInstantUpPrediction() = testScope.runTest {
        // Baseline
        engine.processInstantDelta(current5mPercent = 0.10, current60mPercent = 0.20)

        // Strong positive movement above dead-zone: Δ5m = +0.15%, Δ60m = +0.10%
        val instantSignal = engine.processInstantDelta(
            current5mPercent = 0.25,
            current60mPercent = 0.30
        )

        assertNotNull("Signal must be emitted for delta >= 0.02%", instantSignal)
        assertEquals(TradeDirection.UP, instantSignal?.prediction)
        assertEquals(0.15, instantSignal!!.delta5m, 0.0001)
        assertEquals(0.10, instantSignal.delta60m, 0.0001)
        assertFalse("Instant Layer 1 signal must initially have isConfirmed = false", instantSignal.isConfirmed)
    }

    @Test
    fun testLayer1EmitsInstantDownPrediction() = testScope.runTest {
        // Baseline
        engine.processInstantDelta(current5mPercent = 0.50, current60mPercent = 0.80)

        // Negative movement: Δ5m = -0.10%, Δ60m = -0.10%
        val instantSignal = engine.processInstantDelta(
            current5mPercent = 0.40,
            current60mPercent = 0.70
        )

        assertNotNull("Signal must be emitted", instantSignal)
        assertEquals(TradeDirection.DOWN, instantSignal?.prediction)
        assertEquals(-0.10, instantSignal!!.delta5m, 0.0001)
        assertEquals(-0.10, instantSignal.delta60m, 0.0001)
        assertFalse(instantSignal.isConfirmed)
    }

    @Test
    fun testLayer2MatrixValidationConfirmsMatchingRule() = testScope.runTest {
        // Find an active rule in Directional206MatrixEngine
        val sampleMatch = Directional206MatrixEngine.evaluate(val5m = 0.15, val60m = 0.45)
        assertNotNull("Directional206MatrixEngine should have a rule for (0.15, 0.45)", sampleMatch)

        // Setup baseline
        engine.processInstantDelta(current5mPercent = 0.05, current60mPercent = 0.35)

        // Generate instant UP movement leading to (0.15, 0.45)
        val instantSignal = engine.processInstantDelta(current5mPercent = 0.15, current60mPercent = 0.45)
        assertNotNull(instantSignal)
        assertEquals(TradeDirection.UP, instantSignal?.prediction)

        // Advance coroutines to run Layer 2 validation
        advanceUntilIdle()

        val confirmedSignal = engine.latestConfirmedSignal.value
        assertNotNull("Confirmed signal must be emitted after Layer 2 validation", confirmedSignal)
        if (sampleMatch!!.direction == TradeDirection.UP) {
            assertTrue("Signal should be confirmed when Layer 1 matches 206 Matrix Rule", confirmedSignal!!.isConfirmed)
            assertTrue("Confidence should be high", confirmedSignal.confidence >= 80.0)
            assertEquals(sampleMatch.id, confirmedSignal.primaryRuleId)
        }
    }

    @Test
    fun testLayer2RejectsSignalWhenNo206RuleMatches() = testScope.runTest {
        // Mock custom validator returning no match
        val customEngine = DCHMEngine(
            matrixValidator = object : MatrixValidator {
                override suspend fun validateAgainstRules(
                    direction: TradeDirection,
                    current5m: Double,
                    current60m: Double,
                    delta5m: Double,
                    delta60m: Double
                ): Pair<Boolean, List<String>> {
                    return false to emptyList() // No match in 206 rules
                }
            },
            scope = testScope,
            enableAutomaticAudioAlerts = false,
            enableAutomaticWebSocketRelay = false
        )

        // Baseline
        customEngine.processInstantDelta(0.10, 0.10)

        // Movement
        val instantSignal = customEngine.processInstantDelta(0.25, 0.25)
        assertNotNull(instantSignal)
        assertEquals(TradeDirection.UP, instantSignal?.prediction)

        // Advance to complete Layer 2 validation
        advanceUntilIdle()

        val finalSignal = customEngine.latestConfirmedSignal.value
        assertNotNull(finalSignal)
        assertFalse("Signal must NOT be confirmed if 206 matrix rules do not match", finalSignal!!.isConfirmed)
        assertEquals(0.0, finalSignal.confidence, 0.001)
        assertTrue(finalSignal.reason.contains("Display UI direction only"))
    }

    @Test
    fun testEngineResetClearsBaselineAndMetrics() = testScope.runTest {
        engine.processInstantDelta(0.10, 0.20)
        engine.processInstantDelta(0.30, 0.40)
        advanceUntilIdle()

        assertTrue(engine.engineMetrics.value.totalSignalsGenerated > 0)

        engine.reset()
        advanceUntilIdle()

        assertNull(engine.latestInstantSignal.value)
        assertNull(engine.latestConfirmedSignal.value)
        assertEquals(0L, engine.engineMetrics.value.totalSignalsGenerated)
    }
}
