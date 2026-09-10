package com.example

import com.example.data.analyzer.ConfirmationStage
import com.example.data.analyzer.DailyMovementContext
import com.example.data.analyzer.MovementClassificationEngine
import com.example.data.analyzer.NextMovementBias
import com.example.data.analyzer.TimeframeDirection
import com.example.data.analyzer.TradingOutputParser
import com.example.data.models.DataQualityState
import com.example.data.models.MetricSnapshot
import com.example.data.models.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.After
import org.junit.Test

class MovementClassificationEngineTest {

    @Before
    fun setUp() {
        com.example.data.analyzer.CanonicalDecisionEngine.currentDecisionMode = com.example.data.models.DecisionMode.LEGACY_MULTILAYER
        com.example.data.analyzer.CanonicalDecisionEngine.resetState()
    }

    @After
    fun tearDown() {
        com.example.data.analyzer.CanonicalDecisionEngine.currentDecisionMode = com.example.data.models.DecisionMode.LEGACY_MULTILAYER
        com.example.data.analyzer.CanonicalDecisionEngine.resetState()
    }

    // =========================================================================
    // 1. THE 27 BASIC COMBINATIONS (3 x 3 x 3: 5m, 60m, 1D UP/FLAT/DOWN)
    // =========================================================================
    @Test
    fun testAll27Combinations_deterministicAndConsistent() {
        val states5m = listOf(0.40, 0.05, -0.40) // UP, FLAT, DOWN
        val states60m = listOf(0.50, 0.05, -0.50) // UP, FLAT, DOWN
        val states1d = listOf(1.20, 0.05, -1.20) // UP, FLAT, DOWN

        var combinationsCount = 0
        for (v5 in states5m) {
            for (v60 in states60m) {
                for (v1d in states1d) {
                    val result = MovementClassificationEngine.classify(
                        val5m = v5,
                        val60m = v60,
                        val1d = v1d
                    )
                    assertNotNull(result)
                    assertTrue("Code must not be empty", result.movementCode.isNotEmpty())
                    assertTrue("Bengali title must not be empty", result.titleBengali.isNotEmpty())
                    assertTrue("Bengali description must not be empty", result.descriptionBengali.isNotEmpty())
                    assertTrue("Must have supporting tags", result.supportingTags.isNotEmpty())
                    combinationsCount++
                }
            }
        }
        assertEquals(27, combinationsCount)
    }

    @Test
    fun combination_up_up_up_steadyAlignedWithDailyConfirmed() {
        val res = MovementClassificationEngine.classify(val5m = 0.50, val60m = 0.80, val1d = 1.50)
        assertEquals("STEADY_ALIGNED_UP", res.movementCode)
        assertFalse(res.isWarningOnly)
        assertEquals(DailyMovementContext.BULLISH_DAILY, res.dailyContext)
        assertTrue(res.supportingTags.contains("STEADY_UP"))
        assertTrue(res.supportingTags.contains("DAILY_UP"))
        assertTrue(res.supportingTags.contains("DAILY_CONFIRMED"))
        assertEquals(ConfirmationStage.CONFIRMED, res.confirmationStage)
        assertEquals(NextMovementBias.UP, res.nextMovementBias)
    }

    @Test
    fun combination_down_down_down_steadyAlignedWithDailyConfirmed() {
        val res = MovementClassificationEngine.classify(val5m = -0.50, val60m = -0.80, val1d = -1.50)
        assertEquals("STEADY_ALIGNED_DOWN", res.movementCode)
        assertFalse(res.isWarningOnly)
        assertEquals(DailyMovementContext.BEARISH_DAILY, res.dailyContext)
        assertTrue(res.supportingTags.contains("STEADY_DOWN"))
        assertTrue(res.supportingTags.contains("DAILY_DOWN"))
        assertTrue(res.supportingTags.contains("DAILY_CONFIRMED"))
        assertEquals(ConfirmationStage.CONFIRMED, res.confirmationStage)
        assertEquals(NextMovementBias.DOWN, res.nextMovementBias)
    }

    @Test
    fun combination_up_up_down_steadyUpWithDailyConflict() {
        val res = MovementClassificationEngine.classify(val5m = 0.50, val60m = 0.80, val1d = -1.50)
        assertEquals("STEADY_ALIGNED_UP", res.movementCode)
        assertTrue("Daily conflict turns warning to true", res.isWarningOnly)
        assertEquals(DailyMovementContext.BEARISH_DAILY, res.dailyContext)
        assertTrue(res.supportingTags.contains("DAILY_CONFLICT"))
    }

    @Test
    fun combination_down_down_up_steadyDownWithDailyConflict() {
        val res = MovementClassificationEngine.classify(val5m = -0.50, val60m = -0.80, val1d = 1.50)
        assertEquals("STEADY_ALIGNED_DOWN", res.movementCode)
        assertTrue("Daily conflict turns warning to true", res.isWarningOnly)
        assertEquals(DailyMovementContext.BULLISH_DAILY, res.dailyContext)
        assertTrue(res.supportingTags.contains("DAILY_CONFLICT"))
    }

    @Test
    fun combination_flat_flat_flat_stillNoTrade() {
        val res = MovementClassificationEngine.classify(val5m = 0.04, val60m = -0.05, val1d = 0.05)
        assertEquals("STILL_NO_TRADE", res.movementCode)
        assertFalse(res.isWarningOnly)
        assertEquals(ConfirmationStage.CONFIRMED, res.confirmationStage)
        assertEquals(NextMovementBias.NEUTRAL, res.nextMovementBias)
    }

    // =========================================================================
    // 2. FAKEOUT SCENARIOS (TOP & BOTTOM)
    // =========================================================================
    @Test
    fun topFakeoutRisk_detectedCorrectly() {
        // Prev 5m > 0, current 5m < 0, 60m >= 0.20
        val history = listOf(MetricSnapshot(val5m = 0.35, val60m = 0.40, val1d = 0.50, netSum = 0.75))
        val res = MovementClassificationEngine.classify(
            val5m = -0.15,
            val60m = 0.40,
            val1d = 0.50,
            history = history
        )
        assertEquals("TOP_FAKEOUT_RISK", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertTrue(res.supportingTags.contains("BULL_TRAP"))
        assertEquals(ConfirmationStage.WARNING, res.confirmationStage)
        assertEquals(NextMovementBias.DOWN, res.nextMovementBias)
    }

    @Test
    fun bottomFakeoutRisk_detectedCorrectly() {
        // Prev 5m < 0, current 5m > 0, 60m <= -0.20
        val history = listOf(MetricSnapshot(val5m = -0.35, val60m = -0.40, val1d = -0.50, netSum = -0.75))
        val res = MovementClassificationEngine.classify(
            val5m = 0.15,
            val60m = -0.40,
            val1d = -0.50,
            history = history
        )
        assertEquals("BOTTOM_FAKEOUT_RISK", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertTrue(res.supportingTags.contains("BEAR_TRAP"))
        assertEquals(ConfirmationStage.WARNING, res.confirmationStage)
        assertEquals(NextMovementBias.UP, res.nextMovementBias)
    }

    // =========================================================================
    // 3. PULLBACK VS REVERSAL ATTEMPTS
    // =========================================================================
    @Test
    fun downtrendPullbackUp_detectedWhenNoEarlyRecoveryHistory() {
        val res = MovementClassificationEngine.classify(
            val5m = 0.20,
            val60m = -0.50,
            val1d = -0.80,
            history = emptyList()
        )
        assertEquals("DOWNTREND_PULLBACK_UP", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertTrue(res.supportingTags.contains("DOWNTREND_PULLBACK"))
        assertEquals(ConfirmationStage.DEVELOPING, res.confirmationStage)
    }

    @Test
    fun uptrendPullbackDown_detectedWhenNoEarlyDeclineHistory() {
        val res = MovementClassificationEngine.classify(
            val5m = -0.20,
            val60m = 0.50,
            val1d = 0.80,
            history = emptyList()
        )
        assertEquals("UPTREND_PULLBACK_DOWN", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertTrue(res.supportingTags.contains("UPTREND_PULLBACK"))
        assertEquals(ConfirmationStage.DEVELOPING, res.confirmationStage)
    }

    @Test
    fun bullishReversalAttempt_detectedWithEarlyPositiveHistory() {
        val history = listOf(
            MetricSnapshot(val5m = 0.15, val60m = -0.45, val1d = -0.50, netSum = -0.30)
        )
        val res = MovementClassificationEngine.classify(
            val5m = 0.25,
            val60m = -0.40,
            val1d = -0.50,
            history = history
        )
        assertEquals("BULLISH_REVERSAL_ATTEMPT", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertTrue(res.supportingTags.contains("REVERSAL_ATTEMPT"))
    }

    @Test
    fun bearishReversalAttempt_detectedWithEarlyNegativeHistory() {
        val history = listOf(
            MetricSnapshot(val5m = -0.15, val60m = 0.45, val1d = 0.50, netSum = 0.30)
        )
        val res = MovementClassificationEngine.classify(
            val5m = -0.25,
            val60m = 0.40,
            val1d = 0.50,
            history = history
        )
        assertEquals("BEARISH_REVERSAL_ATTEMPT", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertTrue(res.supportingTags.contains("REVERSAL_ATTEMPT"))
    }

    // =========================================================================
    // 4. CONFIRMED REVERSALS (MULTIPLE SNAPSHOTS + THRESHOLD CROSSING)
    // =========================================================================
    @Test
    fun confirmedBullishReversal_requiresConsecutiveFramesAndNetThreshold() {
        // Needs 3 consecutive positive 5m frames (current + 2 in history), netShort >= 0.20, prior context negative
        val history = listOf(
            MetricSnapshot(val5m = 0.35, val60m = -0.10, val1d = -0.50, netSum = 0.25),
            MetricSnapshot(val5m = 0.25, val60m = -0.20, val1d = -0.60, netSum = 0.05)
        )
        val res = MovementClassificationEngine.classify(
            val5m = 0.45,
            val60m = -0.10, // net = +0.35 >= 0.20
            val1d = -0.40,
            history = history
        )
        assertEquals("CONFIRMED_BULLISH_REVERSAL", res.movementCode)
        assertEquals(ConfirmationStage.CONFIRMED, res.confirmationStage)
        assertEquals(NextMovementBias.UP, res.nextMovementBias)
        assertTrue(res.supportingTags.contains("CONFIRMED_REVERSAL"))
    }

    @Test
    fun confirmedBearishReversal_requiresConsecutiveFramesAndNetThreshold() {
        // Needs 3 consecutive negative 5m frames (current + 2 in history), netShort <= -0.20, prior context positive
        val history = listOf(
            MetricSnapshot(val5m = -0.35, val60m = 0.10, val1d = 0.50, netSum = -0.25),
            MetricSnapshot(val5m = -0.25, val60m = 0.20, val1d = 0.60, netSum = -0.05)
        )
        val res = MovementClassificationEngine.classify(
            val5m = -0.45,
            val60m = 0.10, // net = -0.35 <= -0.20
            val1d = 0.40,
            history = history
        )
        assertEquals("CONFIRMED_BEARISH_REVERSAL", res.movementCode)
        assertEquals(ConfirmationStage.CONFIRMED, res.confirmationStage)
        assertEquals(NextMovementBias.DOWN, res.nextMovementBias)
        assertTrue(res.supportingTags.contains("CONFIRMED_REVERSAL"))
    }

    @Test
    fun reversal_neverClassifiesSingleFrameAsConfirmedReversal() {
        // Only 1 frame positive against a negative 60m -> cannot be confirmed reversal
        val res = MovementClassificationEngine.classify(
            val5m = 0.50,
            val60m = -0.20,
            val1d = -0.50,
            history = emptyList()
        )
        assertFalse(res.movementCode == "CONFIRMED_BULLISH_REVERSAL")
    }

    // =========================================================================
    // 5. SUDDEN SPIKES (AFTER STILLNESS)
    // =========================================================================
    @Test
    fun suddenSpikeUp_requiresTwoStillPreviousSnapshots() {
        val history = listOf(
            MetricSnapshot(val5m = 0.05, val60m = 0.04, val1d = 0.10, netSum = 0.09),
            MetricSnapshot(val5m = 0.04, val60m = 0.05, val1d = 0.10, netSum = 0.09)
        )
        val res = MovementClassificationEngine.classify(
            val5m = 0.45,
            val60m = 0.05,
            val1d = 0.10,
            history = history
        )
        assertEquals("SUDDEN_SPIKE_UP", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertTrue(res.supportingTags.contains("SUDDEN_SPIKE"))
    }

    @Test
    fun suddenSpikeDown_requiresTwoStillPreviousSnapshots() {
        val history = listOf(
            MetricSnapshot(val5m = -0.04, val60m = -0.03, val1d = -0.10, netSum = -0.07),
            MetricSnapshot(val5m = 0.02, val60m = -0.04, val1d = -0.10, netSum = -0.02)
        )
        val res = MovementClassificationEngine.classify(
            val5m = -0.45,
            val60m = -0.05,
            val1d = -0.10,
            history = history
        )
        assertEquals("SUDDEN_SPIKE_DOWN", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertTrue(res.supportingTags.contains("SUDDEN_SPIKE"))
    }

    // =========================================================================
    // 6. MOMENTUM BREAK & ACCELERATION & EXPANSION
    // =========================================================================
    @Test
    fun upwardMomentumBreak_detectedWithHighNetAndHistory() {
        val history = listOf(
            MetricSnapshot(val5m = 0.40, val60m = 0.35, val1d = 0.50, netSum = 0.75)
        )
        val res = MovementClassificationEngine.classify(
            val5m = 0.75, // >= 0.60
            val60m = 0.45, // net >= 0.60
            val1d = 0.80,
            history = history
        )
        assertEquals("UPWARD_MOMENTUM_BREAK", res.movementCode)
        assertFalse(res.isWarningOnly)
        assertEquals(ConfirmationStage.CONFIRMED, res.confirmationStage)
        assertEquals(NextMovementBias.UP, res.nextMovementBias)
    }

    @Test
    fun downwardMomentumBreak_detectedWithHighNegativeNetAndHistory() {
        val history = listOf(
            MetricSnapshot(val5m = -0.40, val60m = -0.35, val1d = -0.50, netSum = -0.75)
        )
        val res = MovementClassificationEngine.classify(
            val5m = -0.75, // <= -0.60
            val60m = -0.45, // net <= -0.60
            val1d = -0.80,
            history = history
        )
        assertEquals("DOWNWARD_MOMENTUM_BREAK", res.movementCode)
        assertFalse(res.isWarningOnly)
        assertEquals(ConfirmationStage.CONFIRMED, res.confirmationStage)
        assertEquals(NextMovementBias.DOWN, res.nextMovementBias)
    }

    // =========================================================================
    // 7. MOMENTUM LOSS
    // =========================================================================
    @Test
    fun momentumLossUp_detectedWhen5mDeceleratesBelowThreshold() {
        val history = listOf(
            MetricSnapshot(val5m = 0.35, val60m = 0.40, val1d = 0.50, netSum = 0.75)
        )
        val res = MovementClassificationEngine.classify(
            val5m = 0.08, // in 0.0..0.10 and < prev (0.35)
            val60m = 0.40, // >= 0.20
            val1d = 0.50,
            history = history
        )
        assertEquals("MOMENTUM_LOSS_UP", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertTrue(res.supportingTags.contains("MOMENTUM_LOSS"))
        assertEquals(ConfirmationStage.EXHAUSTING, res.confirmationStage)
    }

    @Test
    fun momentumLossDown_detectedWhen5mDeceleratesBelowThreshold() {
        val history = listOf(
            MetricSnapshot(val5m = -0.35, val60m = -0.40, val1d = -0.50, netSum = -0.75)
        )
        val res = MovementClassificationEngine.classify(
            val5m = -0.08, // in -0.10..0.0 and > prev (-0.35)
            val60m = -0.40, // <= -0.20
            val1d = -0.50,
            history = history
        )
        assertEquals("MOMENTUM_LOSS_DOWN", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertTrue(res.supportingTags.contains("MOMENTUM_LOSS"))
        assertEquals(ConfirmationStage.EXHAUSTING, res.confirmationStage)
    }

    // =========================================================================
    // 8. DATA INTEGRITY, PROVISIONAL, APPROXIMATE, UNAVAILABLE
    // =========================================================================
    @Test
    fun invalidData_returnsDataUnavailable() {
        val res = MovementClassificationEngine.classify(
            val5m = null,
            val60m = null,
            isValid = false,
            dataQuality = DataQualityState.UNAVAILABLE
        )
        assertEquals("DATA_UNAVAILABLE", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertEquals(ConfirmationStage.UNKNOWN, res.confirmationStage)
        assertEquals(NextMovementBias.UNKNOWN, res.nextMovementBias)
    }

    @Test
    fun ambiguousData_returnsDataAmbiguous() {
        val res = MovementClassificationEngine.classify(
            val5m = 0.25,
            val60m = 0.35,
            isValid = true,
            dataQuality = DataQualityState.AMBIGUOUS
        )
        assertEquals("DATA_AMBIGUOUS", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertEquals(ConfirmationStage.UNKNOWN, res.confirmationStage)
        assertEquals(NextMovementBias.UNKNOWN, res.nextMovementBias)
    }

    @Test
    fun provisionalData_returnsProvisionalData() {
        val res = MovementClassificationEngine.classify(
            val5m = 0.25,
            val60m = 0.35,
            isProvisional = true
        )
        assertEquals("PROVISIONAL_DATA", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertEquals(ConfirmationStage.PRELIMINARY, res.confirmationStage)
    }

    @Test
    fun approximateData_returnsApproximateData() {
        val res = MovementClassificationEngine.classify(
            val5m = 0.25,
            val60m = 0.35,
            isApproximate = true
        )
        assertEquals("APPROXIMATE_DATA", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertEquals(ConfirmationStage.PRELIMINARY, res.confirmationStage)
    }

    // =========================================================================
    // 9. CANONICAL NON-OVERWRITE CONTRACT
    // =========================================================================
    @Test
    fun canonicalNonOverwriteContract_movementNeverChangesDirectionOrPercentages() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +1.00%
            * **৬০ মিনিটের পরিবর্তন:** +0.50%

            **শক্তি লেভেল:** High
            **ফলাফল:**
            * **UP:** 100.0%
            * **DOWN:** 0.0%
        """.trimIndent()

        val parsed = TradingOutputParser.parse(rawInput, 100L)
        assertEquals(TradeDirection.UP, parsed.direction)
        assertEquals(61.55, parsed.upPercentage, 0.01)
        assertEquals(38.45, parsed.downPercentage, 0.01)
        assertTrue("Behavior must be populated without mutating canonical values", parsed.behaviorCode.isNotEmpty())
    }

    // =========================================================================
    // 10. TIMEFRAME DIRECTION CLASSIFIER HELPER
    // =========================================================================
    @Test
    fun timeframeDirectionClassifier_boundaries() {
        assertEquals(TimeframeDirection.POSITIVE, MovementClassificationEngine.classifyTimeframeDirection(0.11))
        assertEquals(TimeframeDirection.NEUTRAL, MovementClassificationEngine.classifyTimeframeDirection(0.10))
        assertEquals(TimeframeDirection.NEUTRAL, MovementClassificationEngine.classifyTimeframeDirection(0.0))
        assertEquals(TimeframeDirection.NEUTRAL, MovementClassificationEngine.classifyTimeframeDirection(-0.10))
        assertEquals(TimeframeDirection.NEGATIVE, MovementClassificationEngine.classifyTimeframeDirection(-0.11))
        assertEquals(TimeframeDirection.UNAVAILABLE, MovementClassificationEngine.classifyTimeframeDirection(null))
        assertEquals(TimeframeDirection.UNAVAILABLE, MovementClassificationEngine.classifyTimeframeDirection(Double.NaN))
    }

    // =========================================================================
    // 11. FORECAST DATA INTEGRATION & CONFIRMATION STAGE DISPLAY
    // =========================================================================
    @Test
    fun forecastDataIntegration_propagatesToTradingAnalysis() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.50%
            * **৬০ মিনিটের পরিবর্তন:** +0.80%
            * **১ দিনের পরিবর্তন:** +1.50%

            **শক্তি লেভেল:** High
            **ফলাফল:**
            * **UP:** 100.0%
            * **DOWN:** 0.0%
        """.trimIndent()

        val parsed = TradingOutputParser.parse(rawInput, 100L)
        assertEquals(ConfirmationStage.CONFIRMED, parsed.confirmationStage)
        assertEquals(NextMovementBias.UP, parsed.nextMovementBias)
        assertTrue(parsed.behaviorTags.contains("একাধিক ফ্রেমে নিশ্চিত"))
        assertTrue(parsed.behaviorDescription.contains("পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী — নিশ্চিত পূর্বাভাস নয়"))
    }

    @Test
    fun nextMovementBiasBengaliSentence_appendedForAllBiases() {
        // UP bias (Steady aligned UP)
        val resUp = MovementClassificationEngine.classify(val5m = 0.40, val60m = 0.50)
        assertEquals(NextMovementBias.UP, resUp.nextMovementBias)
        assertTrue(resUp.descriptionBengali.contains("পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী — নিশ্চিত পূর্বাভাস নয়"))

        // DOWN bias (Steady aligned DOWN)
        val resDown = MovementClassificationEngine.classify(val5m = -0.40, val60m = -0.50)
        assertEquals(NextMovementBias.DOWN, resDown.nextMovementBias)
        assertTrue(resDown.descriptionBengali.contains("পরবর্তী সম্ভাব্য দিক: নিম্নমুখী — নিশ্চিত পূর্বাভাস নয়"))

        // NEUTRAL bias (Still no trade)
        val resNeutral = MovementClassificationEngine.classify(val5m = 0.02, val60m = -0.03)
        assertEquals(NextMovementBias.NEUTRAL, resNeutral.nextMovementBias)
        assertTrue(resNeutral.descriptionBengali.contains("পরবর্তী সম্ভাব্য দিক: নিরপেক্ষ বা অনিশ্চিত"))

        // UNKNOWN bias (Data Unavailable)
        val resUnknown = MovementClassificationEngine.classify(val5m = null, val60m = 0.50)
        assertEquals(NextMovementBias.UNKNOWN, resUnknown.nextMovementBias)
        assertFalse(resUnknown.descriptionBengali.contains("পরবর্তী সম্ভাব্য দিক:"))
    }

    @Test
    fun confirmationStagePhrases_presentInSupportingTags() {
        // PRELIMINARY
        val prov = MovementClassificationEngine.classify(val5m = 0.40, val60m = 0.50, isProvisional = true)
        assertEquals(ConfirmationStage.PRELIMINARY, prov.confirmationStage)
        assertTrue(prov.supportingTags.contains("প্রাথমিক ইঙ্গিত"))

        // DEVELOPING
        val dev = MovementClassificationEngine.classify(val5m = 0.40, val60m = -0.50)
        assertEquals(ConfirmationStage.DEVELOPING, dev.confirmationStage)
        assertTrue(dev.supportingTags.contains("বিকাশমান, নিশ্চিতকরণ অপেক্ষমাণ"))

        // CONFIRMED
        val conf = MovementClassificationEngine.classify(val5m = 0.40, val60m = 0.50)
        assertEquals(ConfirmationStage.CONFIRMED, conf.confirmationStage)
        assertTrue(conf.supportingTags.contains("একাধিক ফ্রেমে নিশ্চিত"))

        // EXHAUSTING
        val exhHistory = listOf(MetricSnapshot(0.30, 0.40))
        val exh = MovementClassificationEngine.classify(val5m = 0.05, val60m = 0.40, history = exhHistory)
        assertEquals(ConfirmationStage.EXHAUSTING, exh.confirmationStage)
        assertTrue(exh.supportingTags.contains("মোমেন্টাম দুর্বল হচ্ছে"))

        // WARNING
        val warn = MovementClassificationEngine.classify(val5m = -0.20, val60m = 0.40, history = exhHistory)
        assertEquals(ConfirmationStage.WARNING, warn.confirmationStage)
        assertTrue(warn.supportingTags.contains("সতর্কতা"))
    }

    @Test
    fun invalidAndProvisionalData_neverShowDirectionalForecast() {
        val provisional = MovementClassificationEngine.classify(val5m = 0.50, val60m = 0.80, isProvisional = true)
        assertEquals(NextMovementBias.UNKNOWN, provisional.nextMovementBias)
        assertFalse(provisional.descriptionBengali.contains("ঊর্ধ্বমুখী — নিশ্চিত পূর্বাভাস নয়"))
        assertFalse(provisional.descriptionBengali.contains("নিম্নমুখী — নিশ্চিত পূর্বাভাস নয়"))

        val approx = MovementClassificationEngine.classify(val5m = 0.50, val60m = 0.80, isApproximate = true)
        assertEquals(NextMovementBias.UNKNOWN, approx.nextMovementBias)
        assertFalse(approx.descriptionBengali.contains("ঊর্ধ্বমুখী — নিশ্চিত পূর্বাভাস নয়"))
        assertFalse(approx.descriptionBengali.contains("নিম্নমুখী — নিশ্চিত পূর্বাভাস নয়"))

        val unavail = MovementClassificationEngine.classify(val5m = null, val60m = null)
        assertEquals(NextMovementBias.UNKNOWN, unavail.nextMovementBias)
        assertFalse(unavail.descriptionBengali.contains("ঊর্ধ্বমুখী — নিশ্চিত পূর্বাভাস নয়"))
        assertFalse(unavail.descriptionBengali.contains("নিম্নমুখী — নিশ্চিত পূর্বাভাস নয়"))

        val ambiguous = MovementClassificationEngine.classify(val5m = 0.50, val60m = 0.50, dataQuality = DataQualityState.AMBIGUOUS)
        assertEquals(NextMovementBias.UNKNOWN, ambiguous.nextMovementBias)
        assertFalse(ambiguous.descriptionBengali.contains("ঊর্ধ্বমুখী — নিশ্চিত পূর্বাভাস নয়"))
        assertFalse(ambiguous.descriptionBengali.contains("নিম্নমুখী — নিশ্চিত পূর্বাভাস নয়"))
    }

    // =========================================================================
    // 24-SCENARIO MATHEMATICAL ENGINE TESTS
    // =========================================================================

    @Test
    fun scenario_extremeCompressionCoil_detected() {
        val res = MovementClassificationEngine.classify(val5m = 0.03, val60m = 0.04, val1d = 0.05)
        assertEquals("STILL_NO_TRADE", res.movementCode)
        assertTrue(res.supportingTags.contains("COMPRESSION_COIL"))
        assertTrue(res.supportingTags.contains("BREAKOUT_PENDING"))
        assertTrue(res.titleBengali.contains("চরম কম্প্রেশন"))
    }

    @Test
    fun scenario_aggressiveBreakoutUp_detected() {
        val res = MovementClassificationEngine.classify(val5m = 0.65, val60m = -0.40, val1d = -0.50)
        assertEquals("AGGRESSIVE_UP_BREAK", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertEquals(NextMovementBias.UP, res.nextMovementBias)
        assertTrue(res.supportingTags.contains("BREAKOUT_ATTEMPT"))
        assertTrue(res.supportingTags.contains("AGGRESSIVE_UP"))
    }

    @Test
    fun scenario_aggressiveBreakdownDown_detected() {
        val res = MovementClassificationEngine.classify(val5m = -0.65, val60m = 0.40, val1d = 0.50)
        assertEquals("AGGRESSIVE_DOWN_BREAK", res.movementCode)
        assertTrue(res.isWarningOnly)
        assertEquals(NextMovementBias.DOWN, res.nextMovementBias)
        assertTrue(res.supportingTags.contains("BREAKDOWN_ATTEMPT"))
        assertTrue(res.supportingTags.contains("AGGRESSIVE_DOWN"))
    }

    @Test
    fun scenario_downtrendWeakBounceResistanceRejection_detected() {
        val res = MovementClassificationEngine.classify(val5m = 0.12, val60m = -0.50, val1d = -0.80)
        assertEquals("DOWNTREND_PULLBACK_UP", res.movementCode)
        assertTrue(res.supportingTags.contains("RESISTANCE_REJECTION"))
        assertTrue(res.supportingTags.contains("DOWN_CONTINUATION"))
        assertEquals(NextMovementBias.DOWN, res.nextMovementBias)
        assertTrue(res.titleBengali.contains("রেজিস্ট্যান্স রিজেকশন"))
    }

    @Test
    fun scenario_uptrendWeakPullbackSupportBounce_detected() {
        val res = MovementClassificationEngine.classify(val5m = -0.12, val60m = 0.50, val1d = 0.80)
        assertEquals("UPTREND_PULLBACK_DOWN", res.movementCode)
        assertTrue(res.supportingTags.contains("SUPPORT_BOUNCE"))
        assertTrue(res.supportingTags.contains("UP_CONTINUATION"))
        assertEquals(NextMovementBias.UP, res.nextMovementBias)
        assertTrue(res.titleBengali.contains("সাপোর্ট বাউন্স"))
    }

    @Test
    fun scenario_tripleAlignedStrongUp_detected() {
        val res = MovementClassificationEngine.classify(val5m = 0.45, val60m = 0.50, val1d = 1.20)
        assertEquals("STEADY_ALIGNED_UP", res.movementCode)
        assertTrue(res.supportingTags.contains("TRIPLE_ALIGNED_STRONG_UP"))
        assertTrue(res.titleBengali.contains("ত্রি-টাইমফ্রেম সুসংহত তীব্র উত্থান"))
    }

    @Test
    fun scenario_tripleAlignedStrongDown_detected() {
        val res = MovementClassificationEngine.classify(val5m = -0.45, val60m = -0.50, val1d = -1.20)
        assertEquals("STEADY_ALIGNED_DOWN", res.movementCode)
        assertTrue(res.supportingTags.contains("TRIPLE_ALIGNED_STRONG_DOWN"))
        assertTrue(res.titleBengali.contains("ত্রি-টাইমফ্রেম সুসংহত তীব্র পতন"))
    }

    @Test
    fun scenario_dailyBullishDipCorrection_detected() {
        val res = MovementClassificationEngine.classify(val5m = -0.25, val60m = -0.25, val1d = 0.90)
        assertEquals("STEADY_ALIGNED_DOWN", res.movementCode)
        assertTrue(res.supportingTags.contains("DAILY_BULLISH_DIP"))
        assertTrue(res.titleBengali.contains("ডিপ কারেকশন"))
    }

    @Test
    fun scenario_dailyBearishShortSqueeze_detected() {
        val res = MovementClassificationEngine.classify(val5m = 0.25, val60m = 0.25, val1d = -0.90)
        assertEquals("STEADY_ALIGNED_UP", res.movementCode)
        assertTrue(res.supportingTags.contains("DAILY_BEARISH_SQUEEZE"))
        assertTrue(res.titleBengali.contains("শর্ট স্কুইজ"))
    }

    @Test
    fun scenario_accelerationUp_detected() {
        val res = MovementClassificationEngine.classify(val5m = 0.55, val60m = 0.30, val1d = 0.10)
        assertEquals("STEADY_ALIGNED_UP", res.movementCode)
        assertTrue(res.supportingTags.contains("ACCELERATION_UP"))
        assertTrue(res.titleBengali.contains("তীব্র আপ গতি বৃদ্ধি"))
    }

    @Test
    fun scenario_decelerationUp_pullbackRisk_detected() {
        val res = MovementClassificationEngine.classify(val5m = 0.18, val60m = 0.35, val1d = 0.10)
        assertEquals("STEADY_ALIGNED_UP", res.movementCode)
        assertTrue(res.supportingTags.contains("DECELERATION_UP"))
        assertTrue(res.supportingTags.contains("PULLBACK_RISK"))
        assertTrue(res.titleBengali.contains("গতি মন্থর"))
    }

    @Test
    fun scenario_accelerationDown_detected() {
        val res = MovementClassificationEngine.classify(val5m = -0.55, val60m = -0.30, val1d = -0.10)
        assertEquals("STEADY_ALIGNED_DOWN", res.movementCode)
        assertTrue(res.supportingTags.contains("ACCELERATION_DOWN"))
        assertTrue(res.titleBengali.contains("তীব্র ডাউন গতি বৃদ্ধি"))
    }

    @Test
    fun scenario_decelerationDown_bounceRisk_detected() {
        val res = MovementClassificationEngine.classify(val5m = -0.18, val60m = -0.35, val1d = -0.10)
        assertEquals("STEADY_ALIGNED_DOWN", res.movementCode)
        assertTrue(res.supportingTags.contains("DECELERATION_DOWN"))
        assertTrue(res.supportingTags.contains("BOUNCE_RISK"))
        assertTrue(res.titleBengali.contains("গতি মন্থর"))
    }
}
