package com.example.data.analyzer

import com.example.data.models.MetricSnapshot
import com.example.data.models.TradeDirection
import com.example.data.models.TradingAnalysis
import com.example.ui.components.calculateQuickPrediction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortEntryDecisionEngineTest {

    @Test
    fun nullAnalysis_returnsInvalidData() {
        val decision = ShortEntryDecisionEngine.evaluateShortEntry(null)
        assertEquals(EntryState.INVALID_DATA, decision.state)
        assertNull(decision.strengthScore)
        assertTrue(decision.riskFlags.contains(ShortRiskFlag.INVALID_DATA.name))
    }

    @Test
    fun invalidAnalysis_returnsInvalidData() {
        val analysis = TradingAnalysis(isValid = false)
        val decision = ShortEntryDecisionEngine.evaluateShortEntry(analysis)
        assertEquals(EntryState.INVALID_DATA, decision.state)
        assertTrue(decision.riskFlags.contains(ShortRiskFlag.INVALID_DATA.name))
    }

    @Test
    fun missingChangeValues_returnsInvalidData() {
        val analysis = TradingAnalysis(
            isValid = true,
            change5mValue = null,
            change60mValue = -0.80
        )
        val decision = ShortEntryDecisionEngine.evaluateShortEntry(analysis)
        assertEquals(EntryState.INVALID_DATA, decision.state)
        assertTrue(decision.riskFlags.contains(ShortRiskFlag.INVALID_DATA.name))
    }

    @Test
    fun deadMarket_returnsNoTrade() {
        val now = 1_000_000L
        val analysis = TradingAnalysis(
            isValid = true,
            isNoTradeZone = false,
            change5mValue = -0.01,
            change60mValue = -0.02,
            timestamp = now
        )
        val decision = ShortEntryDecisionEngine.evaluateShortEntry(analysis, currentTimeMs = now)
        assertEquals(EntryState.NO_TRADE, decision.state)
        assertTrue(decision.riskFlags.contains(ShortRiskFlag.NO_TRADE_ZONE.name))
    }

    @Test
    fun noTradeZone_returnsNoTrade() {
        val now = 1_000_000L
        val analysis = TradingAnalysis(
            isValid = true,
            isNoTradeZone = true,
            change5mValue = -0.30,
            change60mValue = -0.70,
            timestamp = now
        )
        val decision = ShortEntryDecisionEngine.evaluateShortEntry(analysis, currentTimeMs = now)
        assertEquals(EntryState.NO_TRADE, decision.state)
        assertTrue(decision.riskFlags.contains(ShortRiskFlag.NO_TRADE_ZONE.name))
    }

    @Test
    fun staleSignal_returnsSignalExpired() {
        val now = 100_000L
        val analysis = TradingAnalysis(
            isValid = true,
            change5mValue = -0.35,
            change60mValue = -0.85,
            timestamp = now - 20_000L // 20s old (TTL is 15s)
        )
        val decision = ShortEntryDecisionEngine.evaluateShortEntry(analysis, currentTimeMs = now)
        assertEquals(EntryState.SIGNAL_EXPIRED, decision.state)
        assertTrue(decision.riskFlags.contains(ShortRiskFlag.STALE_SIGNAL.name))
    }

    @Test
    fun strongMacroUp_withShortAttempt_returnsHoldOrNoTrade() {
        val now = 1_000_000L
        val analysis = TradingAnalysis(
            isValid = true,
            direction = TradeDirection.UP,
            change5mValue = 0.50,
            change60mValue = 1.20,
            timestamp = now
        )
        val decision = ShortEntryDecisionEngine.evaluateShortEntry(analysis, currentTimeMs = now)
        assertEquals(EntryState.HOLD, decision.state)
        assertTrue(decision.riskFlags.contains(ShortRiskFlag.DIRECTION_CONFLICT.name))
    }

    @Test
    fun confirmedBearishBreakdown_withConsecutiveDownHistory_returnsShortEntryReady() {
        val now = 1_000_000L
        val analysis = TradingAnalysis(
            isValid = true,
            direction = TradeDirection.DOWN,
            change5mValue = -0.55,
            change60mValue = -1.30,
            change1dValue = -2.10,
            behaviorCode = "STRONG_MOMENTUM_BREAK_DOWN",
            timestamp = now
        )

        val history = listOf(
            MetricSnapshot(val5m = -0.35, val60m = -1.15, val1d = -2.0, timestamp = now - 500L),
            MetricSnapshot(val5m = -0.45, val60m = -1.22, val1d = -2.05, timestamp = now - 250L),
            MetricSnapshot(val5m = -0.55, val60m = -1.30, val1d = -2.10, timestamp = now)
        )

        val decision = ShortEntryDecisionEngine.evaluateShortEntry(analysis, history = history, currentTimeMs = now)
        assertEquals(EntryState.SHORT_ENTRY_READY, decision.state)
        assertNotNull(decision.strengthScore)
        assertTrue("Strength score should be strong (> 75.0)", decision.strengthScore!! >= 75.0)
        assertTrue("Strength score bounded <= 99.9", decision.strengthScore!! <= 99.9)
        assertFalse(decision.riskFlags.contains(ShortRiskFlag.SINGLE_CONFIRMATION.name))
    }

    @Test
    fun bearishPullbackExhaustion_returnsShortSetup() {
        val now = 1_000_000L
        val analysis = TradingAnalysis(
            isValid = true,
            direction = TradeDirection.DOWN,
            change5mValue = 0.20, // small bounce
            change60mValue = -1.10, // strong macro downtrend
            behaviorCode = "DOWNTREND_PULLBACK_UP",
            timestamp = now
        )

        val decision = ShortEntryDecisionEngine.evaluateShortEntry(analysis, currentTimeMs = now)
        assertEquals(EntryState.SHORT_SETUP, decision.state)
        assertNotNull(decision.strengthScore)
        assertTrue(decision.strengthScore!! in 50.0..99.9)
        assertTrue(decision.riskFlags.contains(ShortRiskFlag.PULLBACK_NOT_FINISHED.name))
    }

    @Test
    fun bullTrap_withHistory_returnsShortEntryReady() {
        val now = 1_000_000L
        val analysis = TradingAnalysis(
            isValid = true,
            direction = TradeDirection.DOWN,
            change5mValue = -0.40,
            change60mValue = -0.80,
            behaviorCode = "BULL_TRAP",
            timestamp = now
        )

        val history = listOf(
            MetricSnapshot(val5m = -0.20, val60m = -0.75, timestamp = now - 250L),
            MetricSnapshot(val5m = -0.40, val60m = -0.80, timestamp = now)
        )

        val decision = ShortEntryDecisionEngine.evaluateShortEntry(analysis, history = history, currentTimeMs = now)
        assertEquals(EntryState.SHORT_ENTRY_READY, decision.state)
    }

    @Test
    fun unconfirmedSingleFrame_includesSingleConfirmationRiskFlag() {
        val now = 1_000_000L
        val analysis = TradingAnalysis(
            isValid = true,
            direction = TradeDirection.DOWN,
            change5mValue = -0.30,
            change60mValue = -0.70,
            timestamp = now
        )

        val decision = ShortEntryDecisionEngine.evaluateShortEntry(analysis, history = emptyList<MetricSnapshot>(), currentTimeMs = now)
        assertTrue(decision.riskFlags.contains(ShortRiskFlag.SINGLE_CONFIRMATION.name))
        assertEquals(EntryState.WAIT_CONFIRMATION, decision.state)
    }

    @Test
    fun approximateOcr_includesApproximateOcrRiskFlag() {
        val now = 1_000_000L
        val analysis = TradingAnalysis(
            isValid = true,
            isApproximate = true,
            direction = TradeDirection.DOWN,
            change5mValue = -0.40,
            change60mValue = -0.90,
            timestamp = now
        )

        val decision = ShortEntryDecisionEngine.evaluateShortEntry(analysis, currentTimeMs = now)
        assertTrue(decision.riskFlags.contains(ShortRiskFlag.APPROXIMATE_OCR.name))
        assertEquals(EntryState.WAIT_CONFIRMATION, decision.state)
    }

    @Test
    fun calculateQuickPrediction_downDirection_attachesShortDecision() {
        val now = 1_000_000L
        val analysis = TradingAnalysis(
            isValid = true,
            direction = TradeDirection.DOWN,
            change5mValue = -0.45,
            change60mValue = -1.20,
            downPercentage = 84.5,
            timestamp = now
        )

        val pred = calculateQuickPrediction(analysis, currentTimeMs = now)
        assertEquals("DOWN ↘", pred.label)
        assertTrue(pred.isDown)
        assertFalse(pred.isUp)
        assertTrue(pred.isValid)
        assertNotNull(pred.shortDecision)
        assertTrue(pred.powerPercentageStr.contains("%"))
    }

    @Test
    fun calculateQuickPrediction_upDirection_shortDecisionIsNull() {
        val now = 1_000_000L
        val analysis = TradingAnalysis(
            isValid = true,
            direction = TradeDirection.UP,
            change5mValue = 0.45,
            change60mValue = 1.20,
            upPercentage = 82.0,
            timestamp = now
        )

        val pred = calculateQuickPrediction(analysis, currentTimeMs = now)
        assertEquals("UP ↗", pred.label)
        assertTrue(pred.isUp)
        assertFalse(pred.isDown)
        assertNull(pred.shortDecision)
    }

    @Test
    fun calculateQuickPrediction_formulaScoresVaryContinuously_noFixedPercentages() {
        val now = 1_000_000L
        val analysis1 = TradingAnalysis(
            isValid = true,
            direction = TradeDirection.DOWN,
            change5mValue = -0.20,
            change60mValue = -0.30,
            behaviorCode = "BULL_TRAP",
            downPercentage = 0.0, // test fallback formula
            timestamp = now
        )

        val analysis2 = TradingAnalysis(
            isValid = true,
            direction = TradeDirection.DOWN,
            change5mValue = -0.70,
            change60mValue = -1.20,
            behaviorCode = "BULL_TRAP",
            downPercentage = 0.0, // test fallback formula
            timestamp = now
        )

        val pred1 = calculateQuickPrediction(analysis1, currentTimeMs = now)
        val pred2 = calculateQuickPrediction(analysis2, currentTimeMs = now)

        val score1 = pred1.powerPercentageStr.replace("%", "").toDouble()
        val score2 = pred2.powerPercentageStr.replace("%", "").toDouble()

        assertTrue("Higher magnitude must yield higher score", score2 > score1)
        assertFalse("Must not be fixed 82.0", score1 == 82.0 && score2 == 82.0)
    }
}
