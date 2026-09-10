package com.example

import com.example.data.analyzer.CanonicalDecisionEngine
import com.example.data.models.DecisionMode
import com.example.data.models.TradeDirection
import com.example.network.TradeExecutionDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ThreeTimeframePressureModeTest {

    @Before
    fun setUp() {
        CanonicalDecisionEngine.currentDecisionMode = DecisionMode.THREE_TIMEFRAME_PRESSURE
        CanonicalDecisionEngine.resetState()
        TradeExecutionDispatcher.reset()
    }

    @After
    fun tearDown() {
        CanonicalDecisionEngine.currentDecisionMode = DecisionMode.LEGACY_MULTILAYER
        CanonicalDecisionEngine.resetState()
        TradeExecutionDispatcher.reset()
    }

    @Test
    fun testBullishThreeTimeframeGeneratesUpAndBuy() {
        val decision = CanonicalDecisionEngine.evaluate(
            val5m = 0.85,
            val60m = 1.20,
            val1d = 0.50,
            decisionMode = DecisionMode.THREE_TIMEFRAME_PRESSURE
        )

        assertEquals(TradeDirection.UP, decision.direction)
        assertEquals("BUY", decision.side)
        assertTrue(decision.executionEligibility)
        assertFalse(decision.noTrade)
        assertTrue(decision.upPercentage > 50.0)
        assertTrue(decision.downPercentage < 50.0)
        assertEquals("3TF_MATH", decision.primaryMatrixId)
    }

    @Test
    fun testBearishThreeTimeframeGeneratesDownAndSell() {
        val decision = CanonicalDecisionEngine.evaluate(
            val5m = -0.85,
            val60m = -1.20,
            val1d = -0.50,
            decisionMode = DecisionMode.THREE_TIMEFRAME_PRESSURE
        )

        assertEquals(TradeDirection.DOWN, decision.direction)
        assertEquals("SELL", decision.side)
        assertTrue(decision.executionEligibility)
        assertFalse(decision.noTrade)
        assertTrue(decision.downPercentage > 50.0)
        assertTrue(decision.upPercentage < 50.0)
        assertEquals("3TF_MATH", decision.primaryMatrixId)
    }

    @Test
    fun testZeroInputsProduceNeutralNoTrade() {
        val decision = CanonicalDecisionEngine.evaluate(
            val5m = 0.0,
            val60m = 0.0,
            val1d = 0.0,
            decisionMode = DecisionMode.THREE_TIMEFRAME_PRESSURE
        )

        assertEquals(TradeDirection.NEUTRAL, decision.direction)
        assertEquals("NONE", decision.side)
        assertFalse(decision.executionEligibility)
        assertTrue(decision.noTrade)
    }

    @Test
    fun testMissingMetricsProduceUnavailableNoTrade() {
        val decision = CanonicalDecisionEngine.evaluate(
            val5m = null,
            val60m = null,
            val1d = null,
            decisionMode = DecisionMode.THREE_TIMEFRAME_PRESSURE
        )

        assertEquals(TradeDirection.NEUTRAL, decision.direction)
        assertEquals("NONE", decision.side)
        assertFalse(decision.executionEligibility)
        assertTrue(decision.noTrade)
    }

    @Test
    fun testTradeDispatcherAcceptsThreeTimeframeDecision() {
        val decision = CanonicalDecisionEngine.evaluate(
            val5m = 0.60,
            val60m = 0.90,
            val1d = 1.10,
            decisionMode = DecisionMode.THREE_TIMEFRAME_PRESSURE
        )

        assertTrue(decision.executionEligibility)
        val dispatched = TradeExecutionDispatcher.dispatchDecision(
            decision = decision,
            investmentAmount = 100.0,
            availableTimeframes = setOf("5m", "60m", "1d")
        )
        assertTrue(dispatched)
    }
}
