package com.example.network

import com.example.data.models.CanonicalDecision
import com.example.data.models.TradeDirection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TradeExecutionDedupTest {

    @Before
    fun setUp() {
        TradeExecutionDispatcher.reset()
    }

    private fun createDecision(
        fingerprint: String,
        direction: TradeDirection,
        executionEligibility: Boolean = true
    ): CanonicalDecision {
        return CanonicalDecision(
            direction = direction,
            upPercentage = if (direction == TradeDirection.UP) 80.0 else 20.0,
            downPercentage = if (direction == TradeDirection.DOWN) 80.0 else 20.0,
            strength = 80.0,
            dataQuality = "VERIFIED",
            fingerprint = fingerprint,
            explanation = "Test decision for $fingerprint",
            executionEligibility = executionEligibility
        )
    }

    @Test
    fun exactSameFingerprint_isBlockedImmediatelyOnSecondCall() {
        val decision = createDecision(
            fingerprint = "M01-UP-5m:1.20-60m:0.80",
            direction = TradeDirection.UP
        )

        val first = TradeExecutionDispatcher.dispatchDecision(decision, 10.0)
        assertTrue("First dispatch with valid new fingerprint must succeed", first)

        val second = TradeExecutionDispatcher.dispatchDecision(decision, 10.0)
        assertFalse("Duplicate dispatch with identical fingerprint must be blocked", second)
    }

    @Test
    fun exactSameFingerprint_isBlockedEvenAfterTimeElapsed() {
        val decision = createDecision(
            fingerprint = "M01-UP-5m:1.20-60m:0.80",
            direction = TradeDirection.UP
        )

        val first = TradeExecutionDispatcher.dispatchDecision(decision, 10.0)
        assertTrue("First dispatch must succeed", first)

        // Simulate 1200ms passing (which previously caused duplicates due to 1000ms window)
        Thread.sleep(1200)

        val second = TradeExecutionDispatcher.dispatchDecision(decision, 10.0)
        assertFalse(
            "CRITICAL: Identical fingerprint must NEVER be dispatched again even after 1000ms+ has elapsed",
            second
        )
    }

    @Test
    fun directionChange_dispatchesImmediatelyWithoutCooldown() {
        val decisionUp = createDecision(
            fingerprint = "M01-UP-5m:1.20-60m:0.80",
            direction = TradeDirection.UP
        )
        val first = TradeExecutionDispatcher.dispatchDecision(decisionUp, 10.0)
        assertTrue("UP trade must succeed", first)

        val decisionDown = createDecision(
            fingerprint = "M02-DOWN-5m:-1.20-60m:-0.80",
            direction = TradeDirection.DOWN
        )
        val second = TradeExecutionDispatcher.dispatchDecision(decisionDown, 10.0)
        assertTrue("Reversal DOWN trade must dispatch immediately on direction change", second)
    }

    @Test
    fun sameDirection_newFingerprint_withinCooldown_isBlockedToPreventOpticalJitter() {
        val decision1 = createDecision(
            fingerprint = "M01-UP-5m:1.20-60m:0.80",
            direction = TradeDirection.UP
        )
        val first = TradeExecutionDispatcher.dispatchDecision(decision1, 10.0)
        assertTrue("First trade must succeed", first)

        // Micro-jitter: new fingerprint (e.g. +1.21% instead of +1.20%) in SAME direction 50ms later
        val decision2 = createDecision(
            fingerprint = "M01-UP-5m:1.21-60m:0.80",
            direction = TradeDirection.UP
        )
        val second = TradeExecutionDispatcher.dispatchDecision(decision2, 10.0)
        assertFalse("Same-direction rapid multi-click from optical jitter must be suppressed", second)
    }

    @Test
    fun resetAutoTradeState_allowsNewDispatchOnSameFingerprintAfterReset() {
        val decision = createDecision(
            fingerprint = "M01-UP-5m:1.20-60m:0.80",
            direction = TradeDirection.UP
        )
        val first = TradeExecutionDispatcher.dispatchDecision(decision, 10.0)
        assertTrue(first)

        val duplicate = TradeExecutionDispatcher.dispatchDecision(decision, 10.0)
        assertFalse(duplicate)

        // State reset (e.g. market went to neutral or scanning stopped)
        TradeExecutionDispatcher.resetAutoTradeState()

        val afterReset = TradeExecutionDispatcher.dispatchDecision(decision, 10.0)
        assertTrue("After state reset, trade can be dispatched again if re-triggered", afterReset)
    }

    @Test
    fun nonEligibleDecision_isRejected() {
        val decision = createDecision(
            fingerprint = "M01-UP-5m:1.20-60m:0.80",
            direction = TradeDirection.UP,
            executionEligibility = false
        )
        val result = TradeExecutionDispatcher.dispatchDecision(decision, 10.0)
        assertFalse("Non-eligible decision must be rejected", result)
    }

    @Test
    fun neutralDirection_isRejected() {
        val decision = createDecision(
            fingerprint = "M01-NEUTRAL-5m:0.0-60m:0.0",
            direction = TradeDirection.NEUTRAL
        )
        val result = TradeExecutionDispatcher.dispatchDecision(decision, 10.0)
        assertFalse("Neutral direction must never be dispatched", result)
    }

    @Test
    fun cancelledMatrix_isStrictlyRejected() {
        val decision = CanonicalDecision(
            direction = TradeDirection.UP,
            upPercentage = 80.0,
            downPercentage = 20.0,
            strength = 80.0,
            dataQuality = "VERIFIED",
            fingerprint = "M011-UP-5m:1.20-60m:0.80",
            explanation = "Cancelled matrix test",
            executionEligibility = true,
            primaryMatrixId = "M011",
            cancelledMatrixIds = listOf("M011")
        )
        val result = TradeExecutionDispatcher.dispatchDecision(decision, 10.0)
        assertFalse("Primary matrix with CANCEL (❌) mark must be rejected", result)
    }
}
