package com.example

import com.example.data.analyzer.CanonicalDecisionEngine
import com.example.data.analyzer.CanonicalExecutionGate
import com.example.data.analyzer.GateValidationResult
import com.example.data.analyzer.PressureBand
import com.example.data.analyzer.PressureDirection
import com.example.data.analyzer.SignalCooldownGate
import com.example.data.analyzer.SourceQuality
import com.example.data.analyzer.ThreeTimeframePressureCalculator
import com.example.data.models.DataQualityState
import com.example.data.models.DecisionMode
import com.example.data.models.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreeTimeframePressureCalculatorTest {

    @Test
    fun testAllPositiveEmitsUpCallSignal() {
        val result = ThreeTimeframePressureCalculator.calculate(
            p5m = 0.50,
            p60m = 1.20,
            p1d = 2.00,
            timestamp = 1000L,
            sourceQuality = SourceQuality.VERIFIED
        )

        assertFalse(result.isDataIncomplete)
        assertEquals(PressureDirection.UP, result.direction)
        assertTrue(result.netPressurePercent > 0.0)
        assertTrue(result.totalEnergy > 0.0)
        assertEquals(100.0, result.upSharePercent, 0.001)
        assertEquals(0.0, result.downSharePercent, 0.001)
    }

    @Test
    fun testAllNegativeEmitsDownPutSignal() {
        val result = ThreeTimeframePressureCalculator.calculate(
            p5m = -0.50,
            p60m = -1.20,
            p1d = -2.00,
            timestamp = 1000L,
            sourceQuality = SourceQuality.VERIFIED
        )

        assertFalse(result.isDataIncomplete)
        assertEquals(PressureDirection.DOWN, result.direction)
        assertTrue(result.netPressurePercent < 0.0)
        assertTrue(result.totalEnergy > 0.0)
        assertEquals(0.0, result.upSharePercent, 0.001)
        assertEquals(100.0, result.downSharePercent, 0.001)
    }

    @Test
    fun testMissing1dMarksIncompleteData() {
        val result = ThreeTimeframePressureCalculator.calculate(
            p5m = 0.50,
            p60m = 1.20,
            p1d = null,
            timestamp = 1000L,
            sourceQuality = SourceQuality.VERIFIED
        )

        assertTrue(result.isDataIncomplete)
        assertEquals(PressureDirection.NO_SIGNAL, result.direction)
    }

    @Test
    fun testSignalCooldownGateExact30SecondsWindow() {
        val gate = SignalCooldownGate(cooldownMs = 30_000L)
        val t0 = 100_000L

        // Initially can emit
        assertTrue(gate.canEmit(t0))

        // Mark emitted at t0
        gate.markEmitted(
            nowElapsedMs = t0,
            p5m = 0.5,
            p60m = 1.2,
            p1d = 2.0,
            direction = PressureDirection.UP,
            pressureBand = PressureBand.STRONG_UP
        )

        // At t0 + 10s: Cooldown active, cannot emit
        assertFalse(gate.canEmit(t0 + 10_000L))
        assertEquals(20_000L, gate.remainingMs(t0 + 10_000L))

        // At t0 + 29.999s: Still cannot emit
        assertFalse(gate.canEmit(t0 + 29_999L))

        // At t0 + 30.000s: Cooldown expired, can emit!
        assertTrue(gate.canEmit(t0 + 30_000L))
        assertEquals(0L, gate.remainingMs(t0 + 30_000L))
    }

    @Test
    fun testSignalCooldownGateDuplicateDetectionDrop() {
        val gate = SignalCooldownGate(cooldownMs = 30_000L)
        val t0 = 100_000L

        gate.markEmitted(
            nowElapsedMs = t0,
            p5m = 0.5,
            p60m = 1.2,
            p1d = 2.0,
            direction = PressureDirection.UP,
            pressureBand = PressureBand.STRONG_UP
        )

        // During cooldown: gate cannot emit and snapshot captured before expiry is not fresh
        assertFalse(gate.canEmit(t0 + 5_000L))
        assertFalse(gate.isSnapshotFresh(snapshotCapturedElapsedMs = t0 + 5_000L, nowElapsedMs = t0 + 5_000L))

        // Same percentage signature after cooldown is identified as duplicate fingerprint
        assertTrue(gate.canEmit(t0 + 35_000L))
        val isDuplicate = gate.isDuplicateFingerprint(
            p5m = 0.5,
            p60m = 1.2,
            p1d = 2.0,
            direction = PressureDirection.UP,
            pressureBand = PressureBand.STRONG_UP
        )
        assertTrue(isDuplicate)

        // Fresh distinct values after cooldown are NOT duplicate
        val isFreshDifferent = gate.isDuplicateFingerprint(
            p5m = 0.6,
            p60m = 1.2,
            p1d = 2.0,
            direction = PressureDirection.UP,
            pressureBand = PressureBand.STRONG_UP
        )
        assertFalse(isFreshDifferent)
    }

    @Test
    fun testSameTripleAfterCooldownExpiryIsRejectedAsDuplicate() {
        val gate = SignalCooldownGate(cooldownMs = 30_000L, epsilon = 0.005)
        val t0 = 50_000L

        // Initial emission
        gate.markEmitted(
            nowElapsedMs = t0,
            p5m = 0.25,
            p60m = 0.80,
            p1d = 1.50,
            direction = PressureDirection.UP,
            pressureBand = PressureBand.MODERATE_UP
        )

        // After cooldown expires at t0 + 30_001L:
        val tAfter = t0 + 30_001L
        assertTrue(gate.canEmit(tAfter))

        // Same triple tested:
        val isDuplicate = gate.isDuplicateFingerprint(
            p5m = 0.25,
            p60m = 0.80,
            p1d = 1.50,
            direction = PressureDirection.UP,
            pressureBand = PressureBand.MODERATE_UP
        )
        // Must be identified as duplicate
        assertTrue("Identical triple after cooldown expiry must be recognized as duplicate", isDuplicate)
    }

    @Test
    fun testTripleChangesByMoreThanEpsilonAfterCooldownFiresNewSignal() {
        val gate = SignalCooldownGate(cooldownMs = 30_000L, epsilon = 0.005)
        val t0 = 50_000L

        gate.markEmitted(
            nowElapsedMs = t0,
            p5m = 0.250,
            p60m = 0.800,
            p1d = 1.500,
            direction = PressureDirection.UP,
            pressureBand = PressureBand.MODERATE_UP
        )

        val tAfter = t0 + 30_001L
        assertTrue(gate.canEmit(tAfter))

        // Change p5m by 0.01 (which is > epsilon 0.005):
        val isDuplicate = gate.isDuplicateFingerprint(
            p5m = 0.260,
            p60m = 0.800,
            p1d = 1.500,
            direction = PressureDirection.UP,
            pressureBand = PressureBand.MODERATE_UP
        )
        assertFalse("Triple changed by more than epsilon must NOT be duplicate", isDuplicate)
    }

    @Test
    fun testDirectionChangesAfterCooldownFiresNewSignalEvenWithSimilarMagnitude() {
        val gate = SignalCooldownGate(cooldownMs = 30_000L, epsilon = 0.005)
        val t0 = 50_000L

        gate.markEmitted(
            nowElapsedMs = t0,
            p5m = 0.50,
            p60m = 0.50,
            p1d = 0.50,
            direction = PressureDirection.UP,
            pressureBand = PressureBand.MODERATE_UP
        )

        val tAfter = t0 + 30_001L
        assertTrue(gate.canEmit(tAfter))

        // Direction reverses to DOWN:
        val isDuplicate = gate.isDuplicateFingerprint(
            p5m = 0.50,
            p60m = 0.50,
            p1d = 0.50,
            direction = PressureDirection.DOWN,
            pressureBand = PressureBand.MODERATE_DOWN
        )
        assertFalse("Direction change after cooldown must NOT be duplicate", isDuplicate)
    }

    @Test
    fun testCanonicalExecutionGateValidationBeforeMarkEmittedSucceedsForGenuineNewEmission() {
        val gate = SignalCooldownGate(cooldownMs = 30_000L)
        val t0 = 50_000L

        // Prior emission at t0 - 40_000L (cooldown has long expired)
        gate.markEmitted(
            nowElapsedMs = t0 - 40_000L,
            p5m = 0.10,
            p60m = 0.20,
            p1d = 0.30,
            direction = PressureDirection.UP,
            pressureBand = PressureBand.MODERATE_UP
        )

        assertTrue(gate.canEmit(t0))

        // A new genuine snapshot arrives at t0:
        val newPressure = ThreeTimeframePressureCalculator.calculate(
            p5m = 0.80,
            p60m = 1.50,
            p1d = 2.50,
            timestamp = System.currentTimeMillis(),
            sourceQuality = SourceQuality.VERIFIED
        )

        val isDuplicate = gate.isDuplicateFingerprint(
            p5m = newPressure.p5m!!,
            p60m = newPressure.p60m!!,
            p1d = newPressure.p1d!!,
            direction = newPressure.direction,
            pressureBand = newPressure.pressureBand
        )
        assertFalse("Genuine new signal must not be duplicate", isDuplicate)

        // Validate runs BEFORE markEmitted():
        val validationResult = CanonicalExecutionGate.validate(
            pressureResult = newPressure,
            cooldownGate = gate,
            nowElapsedMs = t0,
            snapshotCapturedElapsedMs = t0,
            isKillSwitchActive = false,
            isDestinationConfigured = true,
            isConnectionAvailable = true
        )

        // Must be Eligible (NOT rejected as duplicate fingerprint)
        assertTrue("CanonicalExecutionGate.validate() must be Eligible", validationResult is GateValidationResult.Eligible)

        // Then markEmitted() is called:
        gate.markEmitted(
            nowElapsedMs = t0,
            p5m = newPressure.p5m!!,
            p60m = newPressure.p60m!!,
            p1d = newPressure.p1d!!,
            direction = newPressure.direction,
            pressureBand = newPressure.pressureBand
        )

        // Now cooldown is active for 30s
        assertFalse(gate.canEmit(t0 + 10_000L))
    }

    @Test
    fun testRegression1_AllPositiveInputs_UpShare100_DownShare0() {
        val result = ThreeTimeframePressureCalculator.calculate(
            p5m = 0.50,
            p60m = 1.00,
            p1d = 2.00,
            timestamp = 1000L,
            sourceQuality = SourceQuality.VERIFIED
        )

        assertFalse(result.isDataIncomplete)
        assertEquals(PressureDirection.UP, result.direction)
        assertEquals(100.0, result.upSharePercent, 0.0001)
        assertEquals(0.0, result.downSharePercent, 0.0001)
        assertEquals(100.0, result.netPressurePercent, 0.0001)
        assertTrue(result.upEnergy > 0.0)
        assertEquals(0.0, result.downEnergy, 0.0001)
        assertEquals(result.upEnergy, result.totalEnergy, 0.0001)
    }

    @Test
    fun testRegression2_AllNegativeInputs_UpShare0_DownShare100() {
        val result = ThreeTimeframePressureCalculator.calculate(
            p5m = -0.50,
            p60m = -1.00,
            p1d = -2.00,
            timestamp = 1000L,
            sourceQuality = SourceQuality.VERIFIED
        )

        assertFalse(result.isDataIncomplete)
        assertEquals(PressureDirection.DOWN, result.direction)
        assertEquals(0.0, result.upSharePercent, 0.0001)
        assertEquals(100.0, result.downSharePercent, 0.0001)
        assertEquals(-100.0, result.netPressurePercent, 0.0001)
        assertEquals(0.0, result.upEnergy, 0.0001)
        assertTrue(result.downEnergy > 0.0)
        assertEquals(result.downEnergy, result.totalEnergy, 0.0001)
    }

    @Test
    fun testRegression3_MixedInputs_UpSharePlusDownShareEquals100() {
        val result = ThreeTimeframePressureCalculator.calculate(
            p5m = 0.80,
            p60m = -0.40,
            p1d = 1.20,
            timestamp = 1000L,
            sourceQuality = SourceQuality.VERIFIED
        )

        assertFalse(result.isDataIncomplete)
        assertTrue(result.totalEnergy > 0.0)
        assertTrue(result.upEnergy > 0.0)
        assertTrue(result.downEnergy > 0.0)
        assertEquals(100.0, result.upSharePercent + result.downSharePercent, 0.0001)
        val expectedNet = ((result.upEnergy - result.downEnergy) / result.totalEnergy) * 100.0
        assertEquals(expectedNet, result.netPressurePercent, 0.0001)
    }

    @Test
    fun testRegression4_NullP1d_DataIncompleteAndNoSignalInBothPaths() {
        // Path A: ThreeTimeframePressureCalculator
        val calcResult = ThreeTimeframePressureCalculator.calculate(
            p5m = 0.85,
            p60m = 1.20,
            p1d = null,
            sourceQuality = SourceQuality.VERIFIED
        )
        assertTrue(calcResult.isDataIncomplete)
        assertEquals(PressureDirection.NO_SIGNAL, calcResult.direction)
        assertEquals(0.0, calcResult.upSharePercent, 0.0001)
        assertEquals(0.0, calcResult.downSharePercent, 0.0001)
        assertEquals(0.0, calcResult.totalEnergy, 0.0001)
        assertFalse("Incomplete data must never be marked VERIFIED", calcResult.sourceQuality == SourceQuality.VERIFIED)

        // Path B: CanonicalDecisionEngine
        val decision = CanonicalDecisionEngine.evaluate(
            val5m = 0.85,
            val60m = 1.20,
            val1d = null,
            decisionMode = DecisionMode.THREE_TIMEFRAME_PRESSURE
        )
        assertEquals(TradeDirection.NEUTRAL, decision.direction)
        assertEquals("NONE", decision.side)
        assertTrue(decision.noTrade)
        assertFalse(decision.executionEligibility)
        assertEquals(0.0, decision.upPercentage, 0.0001)
        assertEquals(0.0, decision.downPercentage, 0.0001)
        assertFalse("Incomplete decision data quality must not be VERIFIED", decision.dataQuality == DataQualityState.VERIFIED)
        assertEquals("DATA_INCOMPLETE", decision.forensicPatternCode)
    }

    @Test
    fun testRegression5_AllZeroInputs_TotalEnergyZeroAndNoSignal() {
        val result = ThreeTimeframePressureCalculator.calculate(
            p5m = 0.0,
            p60m = 0.0,
            p1d = 0.0,
            sourceQuality = SourceQuality.VERIFIED
        )

        assertFalse(result.isDataIncomplete)
        assertEquals(0.0, result.totalEnergy, 0.0001)
        assertEquals(0.0, result.upEnergy, 0.0001)
        assertEquals(0.0, result.downEnergy, 0.0001)
        assertEquals(0.0, result.upSharePercent, 0.0001)
        assertEquals(0.0, result.downSharePercent, 0.0001)
        assertEquals(0.0, result.netPressurePercent, 0.0001)
        assertEquals(PressureDirection.NO_SIGNAL, result.direction)
        assertEquals(PressureBand.MIXED_OR_WEAK, result.pressureBand)
    }

    @Test
    fun testRegression6_PressureBandBoundaryValues() {
        // net >= 80: VERY_STRONG_UP
        assertEquals(PressureBand.VERY_STRONG_UP, ThreeTimeframePressureCalculator.classifyPressureBand(100.0))
        assertEquals(PressureBand.VERY_STRONG_UP, ThreeTimeframePressureCalculator.classifyPressureBand(80.0))

        // 60 <= net < 80: STRONG_UP
        assertEquals(PressureBand.STRONG_UP, ThreeTimeframePressureCalculator.classifyPressureBand(79.99))
        assertEquals(PressureBand.STRONG_UP, ThreeTimeframePressureCalculator.classifyPressureBand(60.0))

        // 20 <= net < 60: MODERATE_UP
        assertEquals(PressureBand.MODERATE_UP, ThreeTimeframePressureCalculator.classifyPressureBand(59.99))
        assertEquals(PressureBand.MODERATE_UP, ThreeTimeframePressureCalculator.classifyPressureBand(20.0))

        // -20 < net < 20: MIXED_OR_WEAK
        assertEquals(PressureBand.MIXED_OR_WEAK, ThreeTimeframePressureCalculator.classifyPressureBand(19.99))
        assertEquals(PressureBand.MIXED_OR_WEAK, ThreeTimeframePressureCalculator.classifyPressureBand(0.0))
        assertEquals(PressureBand.MIXED_OR_WEAK, ThreeTimeframePressureCalculator.classifyPressureBand(-19.99))

        // -60 < net <= -20: MODERATE_DOWN
        assertEquals(PressureBand.MODERATE_DOWN, ThreeTimeframePressureCalculator.classifyPressureBand(-20.0))
        assertEquals(PressureBand.MODERATE_DOWN, ThreeTimeframePressureCalculator.classifyPressureBand(-59.99))

        // -80 < net <= -60: STRONG_DOWN
        assertEquals(PressureBand.STRONG_DOWN, ThreeTimeframePressureCalculator.classifyPressureBand(-60.0))
        assertEquals(PressureBand.STRONG_DOWN, ThreeTimeframePressureCalculator.classifyPressureBand(-79.99))

        // net <= -80: VERY_STRONG_DOWN
        assertEquals(PressureBand.VERY_STRONG_DOWN, ThreeTimeframePressureCalculator.classifyPressureBand(-80.0))
        assertEquals(PressureBand.VERY_STRONG_DOWN, ThreeTimeframePressureCalculator.classifyPressureBand(-100.0))
    }
}
