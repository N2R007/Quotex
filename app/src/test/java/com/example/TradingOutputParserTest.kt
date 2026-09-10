package com.example

import com.example.audio.AudioSignalEngine
import com.example.data.analyzer.TradingOutputParser
import com.example.data.models.StrengthLevel
import com.example.data.models.TradeDirection
import com.example.data.models.TradingAnalysis
import com.example.ui.components.formatActionCardContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TradingOutputParserTest {

    @Before
    fun setUp() {
        com.example.data.analyzer.CanonicalDecisionEngine.currentDecisionMode = com.example.data.models.DecisionMode.LEGACY_MULTILAYER
        com.example.data.analyzer.CanonicalDecisionEngine.resetState()
    }

    @Test
    fun parse_validBullishInput_computesCanonicalMathAndAudioAlert() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +1.25%
            * **৬০ মিনিটের পরিবর্তন:** +0.75%

            **শক্তি লেভেল:** High

            **ফলাফল:**
            * **UP:** 100.0%
            * **DOWN:** 0.0%
            * **শব্দ সংকেত (Audio Event):** SOUND_UP_ALERT
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 150L)

        assertTrue(result.isSuccess)
        assertEquals("+1.25%", result.change5m)
        assertEquals("+0.75%", result.change60m)
        assertEquals(TradeDirection.UP, result.direction)
        assertEquals(StrengthLevel.HIGH, result.strengthLevel)
        assertEquals(64.1, result.upPercentage, 0.05)
        assertEquals(35.9, result.downPercentage, 0.05)
        assertEquals(AudioSignalEngine.SOUND_UP_ALERT, result.audioEvent)
    }

    @Test
    fun parse_validBearishInput_computesCanonicalDownMathAndAudioAlert() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** -0.25%
            * **৬০ মিনিটের পরিবর্তন:** +0.02%

            **শক্তি লেভেল:** Normal

            **ফলাফল:**
            * **UP:** 7.4%
            * **DOWN:** 92.6%
            * **শব্দ সংকেত (Audio Event):** SOUND_DOWN_ALERT
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 120L)

        assertTrue(result.isSuccess)
        assertEquals("-0.25%", result.change5m)
        assertEquals("+0.02%", result.change60m)
        assertEquals(TradeDirection.DOWN, result.direction)
        assertEquals(StrengthLevel.NORMAL, result.strengthLevel) // |-0.23| < 0.50 -> Normal
        assertEquals(AudioSignalEngine.SOUND_DOWN_ALERT, result.audioEvent)
    }

    @Test
    fun parse_neutralInput_computesFiftyFiftyAndSoundNone() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.50%
            * **৬০ মিনিটের পরিবর্তন:** -0.50%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)

        assertTrue(result.isSuccess)
        assertEquals(TradeDirection.NEUTRAL, result.direction)
        assertEquals(50.0, result.upPercentage, 0.01)
        assertEquals(50.0, result.downPercentage, 0.01)
        assertEquals(AudioSignalEngine.SOUND_NONE, result.audioEvent)
    }

    @Test
    fun parse_ignoresForbiddenRegions() {
        val rawInput = """
            Traders' Sentiment: 85% UP
            Profit: +150.00%
            1 day change: +12.5%
            1d change: -5.40%
            24h change: +2.10%
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** -0.85%
            * **৬০ মিনিটের পরিবর্তন:** -0.45%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)

        assertTrue(result.isSuccess)
        assertEquals("-0.85%", result.change5m)
        assertEquals("-0.45%", result.change60m)
        assertEquals(TradeDirection.DOWN, result.direction)
    }

    @Test
    fun parse_failsIfOnly1DayChangePresent() {
        val rawInput = """
            1 day change: +15.5%
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +1.20%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 50L)
        assertFalse(result.isSuccess)
        assertEquals(AudioSignalEngine.SOUND_NONE, result.audioEvent)
    }

    @Test
    fun parse_hardFailure_whenValuesMissing() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +1.20%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 50L)
        assertFalse(result.isSuccess)
        assertEquals(AudioSignalEngine.SOUND_NONE, result.audioEvent)
    }

    @Test
    fun parse_emptyInput_returnsFailureGracefully() {
        val result = TradingOutputParser.parse("", 50L)
        assertFalse(result.isSuccess)
        assertEquals(AudioSignalEngine.SOUND_NONE, result.audioEvent)
    }

    @Test
    fun formatWithSign_omitsTrailingZeros() {
        assertEquals("+1.1%", TradingOutputParser.formatWithSign(1.1))
        assertEquals("-0.85%", TradingOutputParser.formatWithSign(-0.85))
        assertEquals("+2%", TradingOutputParser.formatWithSign(2.0))
        assertEquals("0%", TradingOutputParser.formatWithSign(0.0))
        assertEquals("-1.1%", TradingOutputParser.formatWithSign(-1.1))
    }

    @Test
    fun parse_noTradeZone_whenBothUnderPointOne_forcesNoTradeAndNeutral() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.03%
            * **৬০ মিনিটের পরিবর্তন:** -0.05%
            * **১ দিনের পরিবর্তন:** +1.10%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)

        assertTrue(result.isSuccess)
        assertTrue(result.isNoTradeZone)
        assertEquals(TradeDirection.NEUTRAL, result.direction)
        assertEquals(50.0, result.upPercentage, 0.01)
        assertEquals(50.0, result.downPercentage, 0.01)
        assertEquals(50.0, result.calculatedPercentage, 0.01)
        assertEquals(AudioSignalEngine.SOUND_NONE, result.audioEvent)
        assertEquals(com.example.data.models.DataQualityState.VERIFIED, result.dataQuality)
    }

    @Test
    fun calculateCanonicalAnalysis_noTradeZone_exactBoundaries() {
        // Exact boundary at 0.10
        val res1 = TradingOutputParser.calculateCanonicalAnalysis(0.10, 0.10)
        assertTrue(res1.isNoTradeZone)
        assertEquals(TradeDirection.NEUTRAL, res1.direction)
        assertEquals(50.0, res1.upPercentage, 0.01)
        assertEquals(50.0, res1.downPercentage, 0.01)
        assertEquals(AudioSignalEngine.SOUND_NONE, res1.audioEvent)

        // Exact boundary at -0.10
        val res2 = TradingOutputParser.calculateCanonicalAnalysis(-0.10, -0.10)
        assertTrue(res2.isNoTradeZone)
        assertEquals(TradeDirection.NEUTRAL, res2.direction)
        assertEquals(50.0, res2.upPercentage, 0.01)
        assertEquals(50.0, res2.downPercentage, 0.01)

        // Zero magnitude
        val res3 = TradingOutputParser.calculateCanonicalAnalysis(0.0, 0.0)
        assertTrue(res3.isNoTradeZone)
        assertEquals(TradeDirection.NEUTRAL, res3.direction)

        // Just outside boundary: 0.11 and 0.05 -> NOT dead market
        val res4 = TradingOutputParser.calculateCanonicalAnalysis(0.11, 0.05)
        assertFalse(res4.isNoTradeZone)
        assertEquals(TradeDirection.UP, res4.direction)
        assertEquals(100.0, res4.upPercentage, 0.01)
        assertEquals(0.0, res4.downPercentage, 0.01)
        assertEquals(AudioSignalEngine.SOUND_UP_ALERT, res4.audioEvent)
    }

    @Test
    fun calculateCanonicalAnalysis_strengthBoundaries() {
        // < 0.50 -> NORMAL
        val normal = TradingOutputParser.calculateCanonicalAnalysis(0.20, 0.25) // netSum = 0.45
        assertEquals(StrengthLevel.NORMAL, normal.strengthLevel)

        // 0.50 <= netSum < 1.00 -> MEDIUM
        val medium1 = TradingOutputParser.calculateCanonicalAnalysis(0.25, 0.25) // netSum = 0.50
        assertEquals(StrengthLevel.MEDIUM, medium1.strengthLevel)

        val medium2 = TradingOutputParser.calculateCanonicalAnalysis(0.50, 0.49) // netSum = 0.99
        assertEquals(StrengthLevel.MEDIUM, medium2.strengthLevel)

        // >= 1.00 -> HIGH
        val high = TradingOutputParser.calculateCanonicalAnalysis(0.50, 0.50) // netSum = 1.00
        assertEquals(StrengthLevel.HIGH, high.strengthLevel)
    }

    @Test
    fun calculateCanonicalAnalysis_nanAndInfinity_safelyHandled() {
        val nanResult = TradingOutputParser.calculateCanonicalAnalysis(Double.NaN, 0.5)
        assertFalse(nanResult.isValid)
        assertFalse(nanResult.isNoTradeZone)
        assertEquals(TradeDirection.NEUTRAL, nanResult.direction)

        val infResult = TradingOutputParser.calculateCanonicalAnalysis(Double.POSITIVE_INFINITY, 0.5)
        assertFalse(infResult.isValid)
        assertFalse(infResult.isNoTradeZone)
        assertEquals(TradeDirection.NEUTRAL, infResult.direction)
    }

    @Test
    fun parse_dataQuality_independentOf1dPresence() {
        val with1d = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +1.20%
            * **৬০ মিনিটের পরিবর্তন:** +0.50%
            * **১ দিনের পরিবর্তন:** +3.50%
        """.trimIndent()
        val res1 = TradingOutputParser.parse(with1d, 100L)
        assertTrue(res1.isSuccess)
        assertEquals(com.example.data.models.DataQualityState.VERIFIED, res1.dataQuality)

        val without1d = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +1.20%
            * **৬০ মিনিটের পরিবর্তন:** +0.50%
        """.trimIndent()
        val res2 = TradingOutputParser.parse(without1d, 100L)
        assertTrue(res2.isSuccess)
        // Data quality MUST NOT depend on 1D presence
        assertEquals(com.example.data.models.DataQualityState.VERIFIED, res2.dataQuality)

        val empty = ""
        val res3 = TradingOutputParser.parse(empty, 100L)
        assertFalse(res3.isSuccess)
        assertEquals(com.example.data.models.DataQualityState.UNAVAILABLE, res3.dataQuality)
    }

    @Test
    fun canonicalMathFormulas_calculateAccurately() {
        // A = -0.25, B = +0.02
        // netSum = -0.23
        // totalMagnitude = 0.27
        // sensitivityRatio = (| -0.23 | / 0.27) * 100 = 85.185185...%
        // baseScore = 50 + (85.185185 / 2) = 92.59259...% -> rounded to 1 decimal: 92.6%
        // down = 92.6%, up = 7.4%
        val res = TradingOutputParser.calculateCanonicalAnalysis(-0.25, 0.02)
        assertEquals(-0.23, res.netSum, 0.001)
        assertEquals(0.27, res.totalMagnitude, 0.001)
        assertEquals(85.185, res.sensitivityRatio, 0.01)
        assertEquals(92.6, res.downPercentage, 0.01)
        assertEquals(7.4, res.upPercentage, 0.01)
        assertEquals(TradeDirection.DOWN, res.direction)
        assertEquals(StrengthLevel.NORMAL, res.strengthLevel)
        assertEquals(AudioSignalEngine.SOUND_DOWN_ALERT, res.audioEvent)
    }

    @Test
    fun calculateCanonicalAnalysis_equalMagnitudeOppositeSigns_returnsNeutral5050() {
        val res = TradingOutputParser.calculateCanonicalAnalysis(0.50, -0.50)
        assertEquals(0.0, res.netSum, 0.001)
        assertEquals(1.0, res.totalMagnitude, 0.001)
        assertEquals(0.0, res.sensitivityRatio, 0.001)
        assertEquals(50.0, res.upPercentage, 0.01)
        assertEquals(50.0, res.downPercentage, 0.01)
        assertEquals(TradeDirection.NEUTRAL, res.direction)
        assertEquals(StrengthLevel.NORMAL, res.strengthLevel)
        assertEquals(AudioSignalEngine.SOUND_NONE, res.audioEvent)
    }

    @Test
    fun calculateCanonicalAnalysis_bothPositive_sensitivity100() {
        val res = TradingOutputParser.calculateCanonicalAnalysis(0.30, 0.70)
        assertEquals(1.00, res.netSum, 0.001)
        assertEquals(1.00, res.totalMagnitude, 0.001)
        assertEquals(100.0, res.sensitivityRatio, 0.001)
        assertEquals(100.0, res.upPercentage, 0.01)
        assertEquals(0.0, res.downPercentage, 0.01)
        assertEquals(TradeDirection.UP, res.direction)
        assertEquals(StrengthLevel.HIGH, res.strengthLevel)
        assertEquals(AudioSignalEngine.SOUND_UP_ALERT, res.audioEvent)
    }

    @Test
    fun calculateCanonicalAnalysis_bothNegative_sensitivity100() {
        val res = TradingOutputParser.calculateCanonicalAnalysis(-0.40, -0.60)
        assertEquals(-1.00, res.netSum, 0.001)
        assertEquals(1.00, res.totalMagnitude, 0.001)
        assertEquals(100.0, res.sensitivityRatio, 0.001)
        assertEquals(0.0, res.upPercentage, 0.01)
        assertEquals(100.0, res.downPercentage, 0.01)
        assertEquals(TradeDirection.DOWN, res.direction)
        assertEquals(StrengthLevel.HIGH, res.strengthLevel)
        assertEquals(AudioSignalEngine.SOUND_DOWN_ALERT, res.audioEvent)
    }

    @Test
    fun calculateCanonicalAnalysis_oneZeroOnePositive() {
        val res = TradingOutputParser.calculateCanonicalAnalysis(0.0, 0.50)
        assertEquals(0.50, res.netSum, 0.001)
        assertEquals(0.50, res.totalMagnitude, 0.001)
        assertEquals(100.0, res.sensitivityRatio, 0.001)
        assertEquals(100.0, res.upPercentage, 0.01)
        assertEquals(0.0, res.downPercentage, 0.01)
        assertEquals(TradeDirection.UP, res.direction)
        assertEquals(StrengthLevel.MEDIUM, res.strengthLevel)
    }

    @Test
    fun calculateCanonicalAnalysis_oneZeroOneNegative() {
        val res = TradingOutputParser.calculateCanonicalAnalysis(0.0, -0.50)
        assertEquals(-0.50, res.netSum, 0.001)
        assertEquals(0.50, res.totalMagnitude, 0.001)
        assertEquals(100.0, res.sensitivityRatio, 0.001)
        assertEquals(0.0, res.upPercentage, 0.01)
        assertEquals(100.0, res.downPercentage, 0.01)
        assertEquals(TradeDirection.DOWN, res.direction)
        assertEquals(StrengthLevel.MEDIUM, res.strengthLevel)
    }

    @Test
    fun parse_1dChangeIgnoredForCalculationDirection() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** -0.25%
            * **৬০ মিনিটের পরিবর্তন:** +0.02%
            * **১ দিনের পরিবর্তন:** +50.00%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertTrue(result.isSuccess)
        // 1d change must NOT affect direction or up/down calculations
        assertEquals(TradeDirection.DOWN, result.direction)
        assertEquals(52.0, result.downPercentage, 0.01)
        assertEquals("+50%", result.change1d)
    }

    @Test
    fun parse_deadMarketBothNegativeBelowPointTen() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** -0.08%
            * **৬০ মিনিটের পরিবর্তন:** -0.09%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertTrue(result.isSuccess)
        assertTrue(result.isNoTradeZone)
        assertTrue(result.isDeadMarket)
        assertEquals(TradeDirection.NEUTRAL, result.direction)
        assertEquals(50.0, result.upPercentage, 0.01)
        assertEquals(50.0, result.downPercentage, 0.01)
        assertEquals(AudioSignalEngine.SOUND_NONE, result.audioEvent)
    }

    @Test
    fun parse_deadMarketMixedBelowPointTen() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.05%
            * **৬০ মিনিটের পরিবর্তন:** -0.08%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertTrue(result.isSuccess)
        assertTrue(result.isNoTradeZone)
        assertTrue(result.isDeadMarket)
        assertEquals(TradeDirection.NEUTRAL, result.direction)
        assertEquals(50.0, result.upPercentage, 0.01)
        assertEquals(50.0, result.downPercentage, 0.01)
    }

    @Test
    fun parse_withApproximationSymbol_stripsAndParsesCorrectly() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** ~+1.25%
            * **৬০ মিনিটের পরিবর্তন:** ~+0.75%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertTrue(result.isSuccess)
        assertTrue(result.isApproximate)
        assertEquals("~+1.25%", result.change5m)
        assertEquals("~+0.75%", result.change60m)
        assertEquals(TradeDirection.UP, result.direction)
        assertEquals(61.41, result.upPercentage, 0.01)
        assertEquals(AudioSignalEngine.SOUND_NONE, result.audioEvent) // Muted for approximate data
    }

    @Test
    fun parse_withUnicodeApproxSymbol_preservesApproximationFlag() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** ≈-0.80%
            * **৬০ মিনিটের পরিবর্তন:** ≈-0.40%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertTrue(result.isSuccess)
        assertTrue(result.isApproximate)
        assertEquals("≈-0.8%", result.change5m)
        assertEquals("≈-0.4%", result.change60m)
        assertEquals(TradeDirection.DOWN, result.direction)
        assertEquals(AudioSignalEngine.SOUND_NONE, result.audioEvent) // Muted for approximate data
    }

    @Test
    fun parse_withUnsignedValues_failsSignAmbiguityProtection() {
        // Without explicit sign (+/-), value is ambiguous and must be rejected
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** 1.25%
            * **৬০ মিনিটের পরিবর্তন:** 0.75%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertFalse(result.isSuccess)
        assertFalse(result.isValid)
        assertFalse(result.isNoTradeZone)
        assertEquals(TradeDirection.NEUTRAL, result.direction)
    }

    @Test
    fun parse_invalidData_neverMarkedAsNoTradeZone() {
        val rawInput = """
            Garbage OCR text without proper headers or values
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertFalse(result.isSuccess)
        assertFalse(result.isValid)
        assertFalse(result.isNoTradeZone)
        assertEquals(TradeDirection.NEUTRAL, result.direction)
    }

    @Test
    fun parse_withCommaDecimal_parsesCorrectly() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +1,25%
            * **৬০ মিনিটের পরিবর্তন:** +0,75%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertTrue(result.isSuccess)
        assertEquals("+1.25%", result.change5m)
        assertEquals("+0.75%", result.change60m)
        assertEquals(TradeDirection.UP, result.direction)
    }

    @Test
    fun formatWithSign_invalidInputs_returnDoubleDashNeverZero() {
        assertEquals("--", TradingOutputParser.formatWithSign(Double.NaN))
        assertEquals("--", TradingOutputParser.formatWithSign(Double.POSITIVE_INFINITY))
        assertEquals("--", TradingOutputParser.formatWithSign(Double.NEGATIVE_INFINITY))
        val nullVal: Double? = null
        assertEquals("--", TradingOutputParser.formatWithSign(nullVal))
        assertFalse(TradingOutputParser.formatWithSign(Double.NaN) == "0.0%")
        assertFalse(TradingOutputParser.formatWithSign(Double.POSITIVE_INFINITY) == "0.0%")
    }

    @Test
    fun resolveActionCardTexts_fakeoutWarningDoesNotContradictDirection() {
        // UP direction with TOP_FAKEOUT_SELL signal: Must warn about pump then sudden dump
        val textUp = TradingOutputParser.resolveActionCardTexts(
            direction = TradeDirection.UP,
            signalType = com.example.data.models.SignalType.TOP_FAKEOUT_SELL,
            isNoTrade = false,
            isApproximate = false,
            isValid = true,
            v5m = 0.50,
            v60m = 0.30
        )
        assertTrue("Action title must reflect top fakeout risk", textUp.title.contains("উপরে") && textUp.title.contains("ঝুঁকি"))
        assertTrue("Must be marked as warning only", textUp.isWarningOnly)

        // DOWN direction with BOTTOM_FAKEOUT_BUY signal: Must warn about dip then sudden bounce
        val textDown = TradingOutputParser.resolveActionCardTexts(
            direction = TradeDirection.DOWN,
            signalType = com.example.data.models.SignalType.BOTTOM_FAKEOUT_BUY,
            isNoTrade = false,
            isApproximate = false,
            isValid = true,
            v5m = -0.50,
            v60m = -0.30
        )
        assertTrue("Action title must reflect bottom fakeout risk", textDown.title.contains("নিচে") && textDown.title.contains("বাউন্স"))
        assertTrue("Must be marked as warning only", textDown.isWarningOnly)
    }

    @Test
    fun resolveActionCardTexts_noTradeZoneTakesPriorityOverFakeout() {
        // Even if signalType is TOP_FAKEOUT_SELL, if isNoTrade is true, NO TRADE must take priority!
        val text = TradingOutputParser.resolveActionCardTexts(
            direction = TradeDirection.NEUTRAL,
            signalType = com.example.data.models.SignalType.TOP_FAKEOUT_SELL,
            isNoTrade = true,
            isApproximate = false,
            isValid = true
        )
        assertTrue("No trade must take priority", text.title.contains("স্থির") || text.title.contains("গতিহীন"))
        assertFalse("Must not show fakeout as title", text.title.contains("ফেক"))
    }

    @Test
    fun resolveActionCardTexts_approximateDataShowsUncertainty() {
        val text = TradingOutputParser.resolveActionCardTexts(
            direction = TradeDirection.UP,
            signalType = com.example.data.models.SignalType.NONE,
            isNoTrade = false,
            isApproximate = true,
            isValid = true
        )
        assertTrue("Must indicate approximate data", text.title.contains("আনুমানিক"))
        assertTrue("Must state score is uncertain", text.subtitle.contains("অনিশ্চিত") || text.subtitle.contains("স্থগিত"))
    }

    @Test
    fun resolveActionCardTexts_invalidDataShowsDataUnavailable() {
        val text = TradingOutputParser.resolveActionCardTexts(
            direction = TradeDirection.NEUTRAL,
            signalType = com.example.data.models.SignalType.NONE,
            isNoTrade = false,
            isApproximate = false,
            isValid = false
        )
        assertTrue("Must show unavailable or ambiguous", text.title.contains("অনুপলব্ধ") || text.title.contains("অস্পষ্ট"))
    }

    @Test
    fun parse_approximateWithoutExplicitSign_isRejectedAsAmbiguous() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** ~0.50%
            * **৬০ মিনিটের পরিবর্তন:** +0.30%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertFalse("Must not accept approximate input without explicit sign", result.isSuccess)
        assertFalse(result.isValid)
        assertFalse(result.isNoTradeZone)
        assertEquals(com.example.data.models.DataQualityState.AMBIGUOUS, result.dataQuality)
    }

    @Test
    fun classifyMovementBehavior_stillNoTrade_whenMotionless() {
        val behavior = TradingOutputParser.classifyMovementBehavior(
            val5m = 0.0,
            val60m = 0.0,
            val1d = 0.0,
            history = emptyList()
        )
        assertEquals("STILL_NO_TRADE", behavior.code)
        assertTrue(behavior.title.contains("স্থির"))
        assertFalse(behavior.isWarningOnly)
    }

    @Test
    fun classifyMovementBehavior_suddenSpikeUp_detected() {
        val history = listOf(
            com.example.data.models.MetricSnapshot(0.05, 0.05, 0.0, 0.10),
            com.example.data.models.MetricSnapshot(0.05, 0.05, 0.0, 0.10)
        )
        val behavior = TradingOutputParser.classifyMovementBehavior(
            val5m = 0.60,
            val60m = 0.15,
            val1d = 0.50,
            history = history
        )
        assertEquals("SUDDEN_SPIKE_UP", behavior.code)
        assertTrue(behavior.title.contains("হঠাৎ উপরের ধাক্কা"))
        assertTrue(behavior.isWarningOnly)
    }

    @Test
    fun classifyMovementBehavior_suddenSpikeDown_detected() {
        val history = listOf(
            com.example.data.models.MetricSnapshot(-0.05, -0.05, 0.0, -0.10),
            com.example.data.models.MetricSnapshot(-0.05, -0.05, 0.0, -0.10)
        )
        val behavior = TradingOutputParser.classifyMovementBehavior(
            val5m = -0.65,
            val60m = -0.15,
            val1d = -0.50,
            history = history
        )
        assertEquals("SUDDEN_SPIKE_DOWN", behavior.code)
        assertTrue(behavior.title.contains("হঠাৎ নিচের ধাক্কা"))
        assertTrue(behavior.isWarningOnly)
    }

    @Test
    fun classifyMovementBehavior_repeatedAlternation_detected() {
        val history = listOf(
            com.example.data.models.MetricSnapshot(0.15, 0.05, 0.0, 0.20),
            com.example.data.models.MetricSnapshot(-0.15, 0.05, 0.0, -0.10),
            com.example.data.models.MetricSnapshot(0.15, 0.05, 0.0, 0.20)
        )
        val behavior = TradingOutputParser.classifyMovementBehavior(
            val5m = -0.15,
            val60m = 0.05,
            val1d = 0.0,
            history = history
        )
        assertEquals("REPEATED_ALTERNATION", behavior.code)
        assertTrue(behavior.title.contains("উপর-নিচ-উপর-নিচ"))
        assertTrue(behavior.isWarningOnly)
    }

    @Test
    fun classifyMovementBehavior_steadyDownwardTrend_detected() {
        val behavior = TradingOutputParser.classifyMovementBehavior(
            val5m = -0.35,
            val60m = -0.40,
            val1d = -0.05,
            history = emptyList()
        )
        assertEquals("STEADY_ALIGNED_DOWN", behavior.code)
        assertTrue(behavior.title.contains("স্থিতিশীল গতিতে নিচে নামার ট্রেন্ড"))
        assertFalse(behavior.isWarningOnly)
    }

    @Test
    fun classifyMovementBehavior_steadyUpwardTrend_detected() {
        val behavior = TradingOutputParser.classifyMovementBehavior(
            val5m = 0.35,
            val60m = 0.40,
            val1d = 0.05,
            history = emptyList()
        )
        assertEquals("STEADY_ALIGNED_UP", behavior.code)
        assertTrue(behavior.title.contains("স্থিতিশীল গতিতে উপরে ওঠার ট্রেন্ড"))
        assertFalse(behavior.isWarningOnly)
    }

    @Test
    fun classifyMovementBehavior_topFakeoutRisk_detected() {
        val history = listOf(
            com.example.data.models.MetricSnapshot(0.20, 0.50, 1.0, 0.70)
        )
        val behavior = TradingOutputParser.classifyMovementBehavior(
            val5m = -0.08,
            val60m = 0.50,
            val1d = 1.0,
            history = history
        )
        assertEquals("TOP_FAKEOUT_RISK", behavior.code)
        assertTrue(behavior.title.contains("সামান্য উপরে গিয়ে দ্রুত পতনের ঝুঁকি"))
        assertTrue(behavior.isWarningOnly)
    }

    @Test
    fun classifyMovementBehavior_bottomFakeoutRisk_detected() {
        val history = listOf(
            com.example.data.models.MetricSnapshot(-0.20, -0.50, -1.0, -0.70)
        )
        val behavior = TradingOutputParser.classifyMovementBehavior(
            val5m = 0.08,
            val60m = -0.50,
            val1d = -1.0,
            history = history
        )
        assertEquals("BOTTOM_FAKEOUT_RISK", behavior.code)
        assertTrue(behavior.title.contains("সামান্য নিচে নেমে দ্রুত বাউন্সের সম্ভাবনা"))
        assertTrue(behavior.isWarningOnly)
    }

    @Test
    fun classifyMovementBehavior_momentumLossUp_detected() {
        val history = listOf(
            com.example.data.models.MetricSnapshot(0.25, 0.20, 0.5, 0.45)
        )
        val behavior = TradingOutputParser.classifyMovementBehavior(
            val5m = 0.02,
            val60m = 0.20,
            val1d = 0.5,
            history = history
        )
        assertEquals("MOMENTUM_LOSS_UP", behavior.code)
        assertTrue(behavior.title.contains("উপরে ওঠার গতি কমছে"))
        assertTrue(behavior.isWarningOnly)
    }

    @Test
    fun calculatePnl_standardContract_computesAccurateNetAndWinRate() {
        val pnl = TradingOutputParser.calculatePnl(
            investmentAmount = 100.0,
            payoutPercentage = 85.0,
            profitCount = 2,
            lossCount = 1
        )
        assertEquals(170.0, pnl.grossProfit, 0.001)
        assertEquals(100.0, pnl.grossLoss, 0.001)
        assertEquals(70.0, pnl.netPnl, 0.001)
        assertEquals(66.7, pnl.winRate, 0.05)
    }

    @Test
    fun calculatePnl_zeroTrades_returnsZeroSafeDefaults() {
        val pnl = TradingOutputParser.calculatePnl(
            investmentAmount = 50.0,
            payoutPercentage = 80.0,
            profitCount = 0,
            lossCount = 0
        )
        assertEquals(0.0, pnl.grossProfit, 0.001)
        assertEquals(0.0, pnl.grossLoss, 0.001)
        assertEquals(0.0, pnl.netPnl, 0.001)
        assertEquals(0.0, pnl.winRate, 0.001)
    }

    @Test
    fun resolveBehaviorCode_mapsKnownCodesDirectly() {
        val topHistory = listOf(com.example.data.models.MetricSnapshot(0.10, 0.50, 1.0, 0.60))
        assertEquals("TOP_FAKEOUT_RISK", TradingOutputParser.resolveBehaviorCode(val5m = -0.08, val60m = 0.50, history = topHistory))
        val bottomHistory = listOf(com.example.data.models.MetricSnapshot(-0.10, -0.50, -1.0, -0.60))
        assertEquals("BOTTOM_FAKEOUT_RISK", TradingOutputParser.resolveBehaviorCode(val5m = 0.08, val60m = -0.50, history = bottomHistory))
        // STEADY_ALIGNED_UP: 5m > 0, 60m > 0
        assertEquals("STEADY_ALIGNED_UP", TradingOutputParser.resolveBehaviorCode(val5m = 0.40, val60m = 0.35))
        // STEADY_ALIGNED_DOWN: 5m < 0, 60m < 0
        assertEquals("STEADY_ALIGNED_DOWN", TradingOutputParser.resolveBehaviorCode(val5m = -0.40, val60m = -0.35))
        // STILL_NO_TRADE: 5m = 0, 60m = 0
        assertEquals("STILL_NO_TRADE", TradingOutputParser.resolveBehaviorCode(val5m = 0.0, val60m = 0.0))
    }

    @Test
    fun fakeout_rejectedWhenNoPreviousHistory() {
        // Without previous history (prev5m == null), fakeout MUST NOT trigger
        val behavior = TradingOutputParser.classifyMovementBehavior(
            val5m = -0.08,
            val60m = 0.50,
            val1d = 1.0,
            history = emptyList()
        )
        assertFalse("Fakeout must not trigger on initial frame", behavior.code.contains("FAKEOUT"))
    }

    @Test
    fun parse_withBullTrapInputs_detectsFakeoutCorrectly() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** -0.06%
            * **৬০ মিনিটের পরিবর্তন:** +0.65%
            * **১ দিনের পরিবর্তন:** +1.20%
        """.trimIndent()
        val history = listOf(com.example.data.models.MetricSnapshot(0.20, 0.65, 1.20, 0.85))
        val result = TradingOutputParser.parse(rawInput, 90L, history = history)
        assertTrue(result.isSuccess)
        assertEquals(com.example.data.models.SignalType.TOP_FAKEOUT_SELL, result.signalType)
        assertTrue(result.isWarningOnly)
        assertTrue(result.behaviorTitle.contains("ঝুঁকি") || result.behaviorTitle.contains("Bull Trap"))
    }

    @Test
    fun dailyContext_classifiedCorrectly() {
        val bullish = TradingOutputParser.evaluateDailyContext(0.45)
        assertEquals(TradingOutputParser.DailyContext.BULLISH_DAILY, bullish)

        val bearish = TradingOutputParser.evaluateDailyContext(-0.35)
        assertEquals(TradingOutputParser.DailyContext.BEARISH_DAILY, bearish)

        val neutral = TradingOutputParser.evaluateDailyContext(0.15)
        assertEquals(TradingOutputParser.DailyContext.NEUTRAL, neutral)
    }

    @Test
    fun dailyContextMatrix_allSevenStatesCoveredCorrectly() {
        // 1. STRONG_BULLISH: 1D >= 0.30, netShort > 0.10
        val strongBull = TradingOutputParser.evaluateDailyContextMatrix(val1d = 0.50, val5m = 0.30, val60m = 0.20)
        assertEquals(TradingOutputParser.DailyContextMatrix.STRONG_BULLISH, strongBull)

        // 2. PULLBACK_BULLISH: 1D >= 0.30, netShort < -0.10
        val pullbackBull = TradingOutputParser.evaluateDailyContextMatrix(val1d = 0.50, val5m = -0.20, val60m = -0.15)
        assertEquals(TradingOutputParser.DailyContextMatrix.PULLBACK_BULLISH, pullbackBull)

        // 3. CHOPPY_BULLISH: 1D >= 0.30, but short-term dead/choppy (|5m|<=0.10, |60m|<=0.10) or netShort between -0.10 and +0.10
        val choppyBull1 = TradingOutputParser.evaluateDailyContextMatrix(val1d = 0.50, val5m = 0.05, val60m = 0.02)
        assertEquals(TradingOutputParser.DailyContextMatrix.CHOPPY_BULLISH, choppyBull1)
        val choppyBull2 = TradingOutputParser.evaluateDailyContextMatrix(val1d = 0.50, val5m = 0.15, val60m = -0.10)
        assertEquals(TradingOutputParser.DailyContextMatrix.CHOPPY_BULLISH, choppyBull2)

        // 4. STRONG_BEARISH: 1D <= -0.30, netShort < -0.10
        val strongBear = TradingOutputParser.evaluateDailyContextMatrix(val1d = -0.50, val5m = -0.25, val60m = -0.20)
        assertEquals(TradingOutputParser.DailyContextMatrix.STRONG_BEARISH, strongBear)

        // 5. PULLBACK_BEARISH: 1D <= -0.30, netShort > 0.10
        val pullbackBear = TradingOutputParser.evaluateDailyContextMatrix(val1d = -0.50, val5m = 0.20, val60m = 0.15)
        assertEquals(TradingOutputParser.DailyContextMatrix.PULLBACK_BEARISH, pullbackBear)

        // 6. CHOPPY_BEARISH: 1D <= -0.30, but short-term dead/choppy
        val choppyBear = TradingOutputParser.evaluateDailyContextMatrix(val1d = -0.50, val5m = -0.02, val60m = 0.01)
        assertEquals(TradingOutputParser.DailyContextMatrix.CHOPPY_BEARISH, choppyBear)

        // 7. NEUTRAL: |1D| < 0.30
        val neutral1 = TradingOutputParser.evaluateDailyContextMatrix(val1d = 0.15, val5m = 0.50, val60m = 0.50)
        assertEquals(TradingOutputParser.DailyContextMatrix.NEUTRAL, neutral1)

        // Outliers and invalid: null, NaN, >500.0 must evaluate to NEUTRAL
        assertEquals(TradingOutputParser.DailyContextMatrix.NEUTRAL, TradingOutputParser.evaluateDailyContextMatrix(null, 0.5, 0.5))
        assertEquals(TradingOutputParser.DailyContextMatrix.NEUTRAL, TradingOutputParser.evaluateDailyContextMatrix(Double.NaN, 0.5, 0.5))
        assertEquals(TradingOutputParser.DailyContextMatrix.NEUTRAL, TradingOutputParser.evaluateDailyContextMatrix(501.0, 0.5, 0.5))
    }

    @Test
    fun roundTo1Decimal_bankersRoundingBoundaryChecks() {
        assertEquals(1.2, TradingOutputParser.roundTo1Decimal(1.25), 0.0001) // Even 2
        assertEquals(1.4, TradingOutputParser.roundTo1Decimal(1.35), 0.0001) // Even 4
        assertEquals(0.0, TradingOutputParser.roundTo1Decimal(0.05), 0.0001) // Even 0
        assertEquals(-1.2, TradingOutputParser.roundTo1Decimal(-1.25), 0.0001)
        assertEquals(10.0, TradingOutputParser.roundTo1Decimal(9.95), 0.0001)
    }

    @Test
    fun canonicalMath_strictAuditedPairs() {
        // Outside dead market: +0.25 / -0.08 -> Net +0.17 -> UP
        val res1 = TradingOutputParser.calculateStrengthsAudited(0.25, -0.08)
        assertEquals(TradeDirection.UP, res1.direction)
        assertTrue(res1.isValid)

        // Outside dead market: -0.25 / +0.08 -> Net -0.17 -> DOWN
        val res2 = TradingOutputParser.calculateStrengthsAudited(-0.25, 0.08)
        assertEquals(TradeDirection.DOWN, res2.direction)
        assertTrue(res2.isValid)

        // Dead market: +0.05 / -0.08 -> |5m|<=0.10 & |60m|<=0.10 -> NO TRADE / NEUTRAL
        val resDead = TradingOutputParser.calculateStrengthsAudited(0.05, -0.08)
        assertEquals(TradeDirection.NEUTRAL, resDead.direction)
        assertTrue(resDead.isNoTradeZone)

        // +0.02 / +0.02 -> |5m|<=0.10 & |60m|<=0.10 -> NO TRADE / NEUTRAL
        val res3 = TradingOutputParser.calculateStrengthsAudited(0.02, 0.02)
        assertEquals(TradeDirection.NEUTRAL, res3.direction)
        assertTrue(res3.isNoTradeZone)

        // 0.00 / 0.00 -> NO TRADE / NEUTRAL
        val res4 = TradingOutputParser.calculateStrengthsAudited(0.0, 0.0)
        assertEquals(TradeDirection.NEUTRAL, res4.direction)
        assertTrue(res4.isNoTradeZone)

        // +501.0 / +0.50 -> Outlier rejected
        val res5 = TradingOutputParser.calculateStrengthsAudited(501.0, 0.50)
        assertFalse(res5.isValid)

        // NaN / Infinite -> Rejected
        assertFalse(TradingOutputParser.calculateStrengthsAudited(Double.NaN, 0.50).isValid)
        assertFalse(TradingOutputParser.calculateStrengthsAudited(0.50, Double.POSITIVE_INFINITY).isValid)
    }

    @Test
    fun historyContract_alternationRequiresFourSnapshots() {
        // 2 snapshots in history + current = 3 snapshots total -> not enough for alternation
        val twoHist = listOf(
            com.example.data.models.MetricSnapshot(0.30, 0.20, 0.50, 0.50),
            com.example.data.models.MetricSnapshot(-0.30, 0.20, 0.50, -0.10)
        )
        val behavior2 = TradingOutputParser.classifyMovementBehavior(
            val5m = 0.30,
            val60m = 0.20,
            val1d = 0.50,
            history = twoHist
        )
        assertFalse("Alternation requires at least 4 confirmed snapshots", behavior2.code == "REPEATED_ALTERNATION")

        // 3 snapshots in history + current = 4 alternating snapshots -> triggers REPEATED_ALTERNATION
        val threeHist = listOf(
            com.example.data.models.MetricSnapshot(0.30, 0.20, 0.50, 0.50),
            com.example.data.models.MetricSnapshot(-0.30, 0.20, 0.50, -0.10),
            com.example.data.models.MetricSnapshot(0.30, 0.20, 0.50, 0.50)
        )
        val behavior4 = TradingOutputParser.classifyMovementBehavior(
            val5m = -0.30,
            val60m = 0.20,
            val1d = 0.50,
            history = threeHist
        )
        assertEquals("REPEATED_ALTERNATION", behavior4.code)
    }

    @Test
    fun historyContract_spikeRequiresTwoStillSnapshots() {
        // Only 1 still snapshot -> not a sudden spike
        val oneStill = listOf(
            com.example.data.models.MetricSnapshot(0.02, 0.05, 0.0, 0.07)
        )
        val res1 = TradingOutputParser.classifyMovementBehavior(
            val5m = 0.40,
            val60m = 0.05,
            val1d = 0.0,
            history = oneStill
        )
        assertFalse(res1.code == "SUDDEN_SPIKE_UP")

        // 2 still snapshots -> triggers SUDDEN_SPIKE_UP
        val twoStill = listOf(
            com.example.data.models.MetricSnapshot(0.02, 0.05, 0.0, 0.07),
            com.example.data.models.MetricSnapshot(0.01, 0.02, 0.0, 0.03)
        )
        val res2 = TradingOutputParser.classifyMovementBehavior(
            val5m = 0.40,
            val60m = 0.05,
            val1d = 0.0,
            history = twoStill
        )
        assertEquals("SUDDEN_SPIKE_UP", res2.code)
    }

    @Test
    fun historyContract_approximateSnapshotsIgnoredInBehaviorClassification() {
        // Approximate snapshots mixed in must be filtered out
        val mixedHistory = listOf(
            com.example.data.models.MetricSnapshot(0.02, 0.05, 0.0, 0.07, isValid = true, isApproximate = true),
            com.example.data.models.MetricSnapshot(0.01, 0.02, 0.0, 0.03, isValid = true, isApproximate = false)
        )
        // With 1 approximate and 1 confirmed, there is only 1 confirmed still snapshot -> breakout must NOT trigger
        val res = TradingOutputParser.classifyMovementBehavior(
            val5m = 0.40,
            val60m = 0.05,
            val1d = 0.0,
            history = mixedHistory
        )
        assertFalse(res.code == "SUDDEN_SPIKE_UP")
    }

    @Test
    fun parse_approximateFrame_marksAppropriateDataQuality() {
        val approxRaw = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** ~+0.25%
            * **৬০ মিনিটের পরিবর্তন:** +0.30%
        """.trimIndent()
        val result = TradingOutputParser.parse(approxRaw, 100L)
        assertTrue(result.isSuccess)
        assertTrue(result.isApproximate)
        assertEquals(com.example.data.models.DataQualityState.APPROXIMATE, result.dataQuality)
        assertEquals(AudioSignalEngine.SOUND_NONE, result.audioEvent)
    }

    @Test
    fun canonicalAnalysis_nanAndInfinityRejection_acrossAllSurfaces() {
        // Canonical analysis rejects NaN and Infinities
        val nan1 = TradingOutputParser.calculateCanonicalAnalysis(Double.NaN, 0.50)
        assertFalse(nan1.isValid)
        val nan2 = TradingOutputParser.calculateCanonicalAnalysis(0.50, Double.POSITIVE_INFINITY)
        assertFalse(nan2.isValid)
        val nan3 = TradingOutputParser.calculateCanonicalAnalysis(Double.NEGATIVE_INFINITY, 0.50)
        assertFalse(nan3.isValid)

        // calculateStrengthsAudited rejects NaN and Infinities
        val auditedNan = TradingOutputParser.calculateStrengthsAudited(Double.NaN, 0.20)
        assertFalse(auditedNan.isValid)
        val auditedInf = TradingOutputParser.calculateStrengthsAudited(0.20, Double.POSITIVE_INFINITY)
        assertFalse(auditedInf.isValid)

        // Daily context rejects NaN and Infinities
        assertEquals(TradingOutputParser.DailyContext.NEUTRAL, TradingOutputParser.evaluateDailyContext(Double.NaN))
        assertEquals(TradingOutputParser.DailyContext.NEUTRAL, TradingOutputParser.evaluateDailyContext(Double.POSITIVE_INFINITY))
        assertEquals(TradingOutputParser.DailyContext.NEUTRAL, TradingOutputParser.evaluateDailyContext(Double.NEGATIVE_INFINITY))

        // Daily matrix rejects NaN and Infinities
        assertEquals(TradingOutputParser.DailyContextMatrix.NEUTRAL, TradingOutputParser.evaluateDailyContextMatrix(Double.NaN, 0.50, 0.50))
        assertEquals(TradingOutputParser.DailyContextMatrix.NEUTRAL, TradingOutputParser.evaluateDailyContextMatrix(0.50, Double.NaN, 0.50))
        assertEquals(TradingOutputParser.DailyContextMatrix.NEUTRAL, TradingOutputParser.evaluateDailyContextMatrix(0.50, 0.50, Double.POSITIVE_INFINITY))
    }

    @Test
    fun dailyContextMatrix_invalidShortTermMetrics_returnsNeutral() {
        // Outlier val5m (> 500%) must cause matrix to yield NEUTRAL safely
        val resOutlier5m = TradingOutputParser.evaluateDailyContextMatrix(
            val1d = 0.50,
            val5m = 501.0,
            val60m = 0.20
        )
        assertEquals(TradingOutputParser.DailyContextMatrix.NEUTRAL, resOutlier5m)

        // NaN short term metric must yield NEUTRAL safely
        val resNan60m = TradingOutputParser.evaluateDailyContextMatrix(
            val1d = 0.50,
            val5m = 0.20,
            val60m = Double.NaN
        )
        assertEquals(TradingOutputParser.DailyContextMatrix.NEUTRAL, resNan60m)
    }

    @Test
    fun historyAlternation_ignoresMismatchedNetSum_recomputesFromRawValues() {
        // History snapshot with valid val5m & val60m but intentionally corrupted/mismatched netSum
        // e.g. val5m = 0.30, val60m = 0.20 -> actual net is +0.50, but cached netSum is -999.0
        val historyWithMismatchedNetSum = listOf(
            com.example.data.models.MetricSnapshot(0.30, 0.20, 0.0, -999.0),
            com.example.data.models.MetricSnapshot(0.30, 0.20, 0.0, -999.0),
            com.example.data.models.MetricSnapshot(0.30, 0.20, 0.0, -999.0)
        )
        val behavior = TradingOutputParser.classifyMovementBehavior(
            val5m = 0.30,
            val60m = 0.20,
            val1d = 0.0,
            history = historyWithMismatchedNetSum
        )
        // Since actual values are all positive (+0.50), there are NO sign flips.
        // If the corrupted netSum (-999.0) was trusted, it would falsely trigger alternation.
        assertFalse("Alternation must not trigger from corrupted cached netSum", behavior.code == "REPEATED_ALTERNATION")
    }

    @Test
    fun historyAlternation_thresholdBoundaryBehavior() {
        // Neutral noise values (|netSum| <= 0.05) must NOT be counted as sign flips
        val neutralNoiseHistory = listOf(
            com.example.data.models.MetricSnapshot(0.02, 0.01, 0.0, 0.03),  // net = +0.03
            com.example.data.models.MetricSnapshot(-0.02, -0.01, 0.0, -0.03), // net = -0.03
            com.example.data.models.MetricSnapshot(0.02, 0.01, 0.0, 0.03)   // net = +0.03
        )
        val behaviorNoise = TradingOutputParser.classifyMovementBehavior(
            val5m = -0.02,
            val60m = -0.01, // net = -0.03
            val1d = 0.0,
            history = neutralNoiseHistory
        )
        assertFalse("Noise within ±0.05 boundary must not trigger alternation", behaviorNoise.code == "REPEATED_ALTERNATION")

        // Significant values (|netSum| > 0.10) alternating must trigger REPEATED_ALTERNATION
        val significantAlternatingHistory = listOf(
            com.example.data.models.MetricSnapshot(0.20, 0.10, 0.0, 0.30),  // net = +0.30
            com.example.data.models.MetricSnapshot(-0.20, -0.10, 0.0, -0.30), // net = -0.30
            com.example.data.models.MetricSnapshot(0.20, 0.10, 0.0, 0.30)   // net = +0.30
        )
        val behaviorAlternation = TradingOutputParser.classifyMovementBehavior(
            val5m = -0.20,
            val60m = -0.10, // net = -0.30
            val1d = 0.0,
            history = significantAlternatingHistory
        )
        assertEquals("REPEATED_ALTERNATION", behaviorAlternation.code)
    }

    @Test
    fun canonicalAnalysis_floatingPointCancellation_neutralResolution() {
        // Equal and opposite magnitudes: netSum == 0.0
        val resExactOpposite = TradingOutputParser.calculateCanonicalAnalysis(0.50, -0.50)
        assertTrue(resExactOpposite.isValid)
        assertEquals(TradeDirection.NEUTRAL, resExactOpposite.direction)
        assertEquals(50.0, resExactOpposite.upPercentage, 1e-6)
        assertEquals(50.0, resExactOpposite.downPercentage, 1e-6)
        assertEquals(100.0, resExactOpposite.upPercentage + resExactOpposite.downPercentage, 1e-6)
        assertEquals(AudioSignalEngine.SOUND_NONE, resExactOpposite.audioEvent)

        // Near-cancellation within EPSILON
        val resNearCancel = TradingOutputParser.calculateCanonicalAnalysis(0.1234567891, -0.1234567890)
        assertTrue(resNearCancel.isValid)
        // net is 1e-10 <= EPSILON, so it's resolved as NEUTRAL 50/50
        assertEquals(TradeDirection.NEUTRAL, resNearCancel.direction)
        assertEquals(50.0, resNearCancel.upPercentage, 1e-6)
        assertEquals(50.0, resNearCancel.downPercentage, 1e-6)
    }

    @Test
    fun canonicalAnalysis_sensitivityClampingAndExact100SumGuarantee() {
        // Case 1: (-0.25, +0.02) => Net -0.23, Sensitivity approx 85.185, DOWN 92.6, UP 7.4
        val res1 = TradingOutputParser.calculateCanonicalAnalysis(-0.25, 0.02)
        assertTrue(res1.isValid)
        assertEquals(-0.23, res1.netSum, 1e-6)
        assertEquals(85.185, res1.sensitivityRatio, 0.01)
        assertEquals(TradeDirection.DOWN, res1.direction)
        assertEquals(92.6, res1.downPercentage, 1e-6)
        assertEquals(7.4, res1.upPercentage, 1e-6)
        assertEquals(100.0, res1.upPercentage + res1.downPercentage, 1e-6)

        // Case 2: (+0.30, +0.70) => UP 100.0, DOWN 0.0
        val res2 = TradingOutputParser.calculateCanonicalAnalysis(0.30, 0.70)
        assertTrue(res2.isValid)
        assertEquals(TradeDirection.UP, res2.direction)
        assertEquals(100.0, res2.upPercentage, 1e-6)
        assertEquals(0.0, res2.downPercentage, 1e-6)
        assertEquals(100.0, res2.upPercentage + res2.downPercentage, 1e-6)

        // Case 3: (+0.50, -0.50) => NEUTRAL 50/50
        val res3 = TradingOutputParser.calculateCanonicalAnalysis(0.50, -0.50)
        assertTrue(res3.isValid)
        assertEquals(TradeDirection.NEUTRAL, res3.direction)
        assertEquals(50.0, res3.upPercentage, 1e-6)
        assertEquals(50.0, res3.downPercentage, 1e-6)
        assertEquals(100.0, res3.upPercentage + res3.downPercentage, 1e-6)

        // Check a range of values to ensure UP + DOWN is always strictly 100.0
        val testValues = listOf(
            Pair(0.15, 0.25),
            Pair(-0.40, -0.10),
            Pair(0.80, -0.20),
            Pair(-0.75, 0.15),
            Pair(0.33, 0.67),
            Pair(-0.11, -0.22)
        )
        for ((v5, v60) in testValues) {
            val res = TradingOutputParser.calculateCanonicalAnalysis(v5, v60)
            assertTrue(res.isValid)
            assertTrue(res.upPercentage in 0.0..100.0)
            assertTrue(res.downPercentage in 0.0..100.0)
            assertEquals(100.0, res.upPercentage + res.downPercentage, 1e-6)
        }
    }

    @Test
    fun calculatePnl_invalidInputsAndOverflowLimits() {
        // Negative inputs clamped to 0
        val negPnl = TradingOutputParser.calculatePnl(
            investmentAmount = -50.0,
            payoutPercentage = -85.0,
            profitCount = -5,
            lossCount = -2
        )
        assertEquals(0.0, negPnl.grossProfit, 1e-6)
        assertEquals(0.0, negPnl.grossLoss, 1e-6)
        assertEquals(0.0, negPnl.netPnl, 1e-6)
        assertEquals(0.0, negPnl.winRate, 1e-6)

        // NaN and Infinity inputs produce safe 0.0
        val nanPnl = TradingOutputParser.calculatePnl(
            investmentAmount = Double.NaN,
            payoutPercentage = Double.POSITIVE_INFINITY,
            profitCount = 10,
            lossCount = 5
        )
        assertTrue(!nanPnl.grossProfit.isNaN() && !nanPnl.grossProfit.isInfinite())
        assertTrue(!nanPnl.netPnl.isNaN() && !nanPnl.netPnl.isInfinite())
        assertEquals(0.0, nanPnl.grossProfit, 1e-6)

        // Valid scenario with standard 85% payout
        val normalPnl = TradingOutputParser.calculatePnl(
            investmentAmount = 100.0,
            payoutPercentage = 85.0,
            profitCount = 7,
            lossCount = 3
        )
        assertEquals(595.0, normalPnl.grossProfit, 1e-6) // 7 * 100 * 0.85 = 595.0
        assertEquals(300.0, normalPnl.grossLoss, 1e-6)   // 3 * 100 = 300.0
        assertEquals(295.0, normalPnl.netPnl, 1e-6)     // 595 - 300 = 295.0
        assertEquals(70.0, normalPnl.winRate, 1e-6)

        // Extreme overflow bounds clamped safely
        val hugePnl = TradingOutputParser.calculatePnl(
            investmentAmount = 1e12, // exceeds MAX_INVESTMENT_AMOUNT (1e9)
            payoutPercentage = 1000.0, // exceeds MAX_PAYOUT_PERCENTAGE (500)
            profitCount = 1,
            lossCount = 0
        )
        assertTrue(hugePnl.grossProfit <= 1e9 * 5.0)
        assertTrue(!hugePnl.grossProfit.isInfinite())
    }

    @Test
    fun parse1d_nonZeroWithoutSign_treatedAsAmbiguousOrNull() {
        // 1D non-zero without explicit sign (+/-) in text must not silently become positive
        val rawNoSign1d = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.25%
            * **৬০ মিনিটের পরিবর্তন:** +0.30%
            * **১ দিনের পরিবর্তন:** 1.50%
        """.trimIndent()
        val result = TradingOutputParser.parse(rawNoSign1d, 50L)
        assertTrue(result.isSuccess)
        assertTrue(result.isValid)
        // 1D must be null because non-zero without sign is ambiguous
        assertEquals(null, result.change1dValue)
        // Direction and scores are unaffected by 1D
        assertEquals(TradeDirection.UP, result.direction)

        // Explicit signed 1D (+1.50%) is preserved
        val rawSigned1d = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.25%
            * **৬০ মিনিটের পরিবর্তন:** +0.30%
            * **১ দিনের পরিবর্তন:** +1.50%
        """.trimIndent()
        val resultSigned = TradingOutputParser.parse(rawSigned1d, 50L)
        assertTrue(resultSigned.isSuccess)
        assertEquals(1.50, resultSigned.change1dValue)
        assertEquals(TradeDirection.UP, resultSigned.direction)
    }

    @Test
    fun parse1d_approximateValues_safelyDiscardedAsUnavailable() {
        // ~+1.50% should have approximate 1D safely discarded and displayed as unavailable
        val rawApproxTilde = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.35%
            * **৬০ মিনিটের পরিবর্তন:** +0.45%
            * **১ দিনের পরিবর্তন:** ~+1.50%
        """.trimIndent()
        val resTilde = TradingOutputParser.parse(rawApproxTilde, 45L)
        assertTrue(resTilde.isSuccess)
        assertTrue(resTilde.isValid)
        assertEquals("--", resTilde.change1d)
        assertEquals(null, resTilde.change1dValue)
        assertEquals(TradeDirection.UP, resTilde.direction)
        assertEquals("NEUTRAL", resTilde.dailyContext)

        // ≈-1.50% should also have approximate 1D safely discarded
        val rawApproxAlmost = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.35%
            * **৬০ মিনিটের পরিবর্তন:** +0.45%
            * **১ দিনের পরিবর্তন:** ≈-1.50%
        """.trimIndent()
        val resAlmost = TradingOutputParser.parse(rawApproxAlmost, 45L)
        assertTrue(resAlmost.isSuccess)
        assertTrue(resAlmost.isValid)
        assertEquals("--", resAlmost.change1d)
        assertEquals(null, resAlmost.change1dValue)
        assertEquals(TradeDirection.UP, resAlmost.direction)
        assertEquals("NEUTRAL", resAlmost.dailyContext)
    }

    @Test
    fun calculatePnl_intMaxTradeCounts_handlesWithoutOverflowAndFiniteOutput() {
        // Both profitCount and lossCount equal Int.MAX_VALUE
        val maxTradesPnl = TradingOutputParser.calculatePnl(
            investmentAmount = 100.0,
            payoutPercentage = 85.0,
            profitCount = Int.MAX_VALUE,
            lossCount = Int.MAX_VALUE
        )
        // 2 * Int.MAX_VALUE total trades -> exactly 50.0% win rate
        assertEquals(50.0, maxTradesPnl.winRate, 1e-6)
        assertTrue("Gross profit must be finite", maxTradesPnl.grossProfit.isFinite())
        assertTrue("Gross loss must be finite", maxTradesPnl.grossLoss.isFinite())
        assertTrue("Net PnL must be finite", maxTradesPnl.netPnl.isFinite())
        assertTrue(maxTradesPnl.grossProfit > 0.0)
        assertTrue(maxTradesPnl.grossLoss > 0.0)
        // At 85% payout on 50/50 trades, net PnL is negative
        assertTrue("Net PnL must be negative for 85% payout on 50/50 trades", maxTradesPnl.netPnl < 0.0)

        // All wins with Int.MAX_VALUE
        val allWinsPnl = TradingOutputParser.calculatePnl(
            investmentAmount = 100.0,
            payoutPercentage = 85.0,
            profitCount = Int.MAX_VALUE,
            lossCount = 0
        )
        assertEquals(100.0, allWinsPnl.winRate, 1e-6)
        assertEquals(0.0, allWinsPnl.grossLoss, 1e-6)
        assertEquals(allWinsPnl.grossProfit, allWinsPnl.netPnl, 1e-6)

        // Normal 85% payout results remain exactly unchanged
        val normalPnl = TradingOutputParser.calculatePnl(
            investmentAmount = 10.0,
            payoutPercentage = 85.0,
            profitCount = 10,
            lossCount = 5
        )
        assertEquals(85.0, normalPnl.grossProfit, 1e-6)
        assertEquals(50.0, normalPnl.grossLoss, 1e-6)
        assertEquals(35.0, normalPnl.netPnl, 1e-6)
        assertEquals(66.7, normalPnl.winRate, 1e-6)

        // Overflow policy: extreme bound clamps deterministically to MAX_FINITE_PNL instead of silent 0.0
        val extremePnl = TradingOutputParser.calculatePnl(
            investmentAmount = Double.POSITIVE_INFINITY,
            payoutPercentage = 85.0,
            profitCount = 10,
            lossCount = 5
        )
        assertEquals(0.0, extremePnl.grossProfit, 1e-6) // invalid investment becomes safe 0.0
    }

    @Test
    fun calculatePnl_extremeUnequalCountsExceedingMaxFinite_retainsCorrectSignAndFinite() {
        // Profit dominates: both gross values reach/exceed MAX_FINITE_PNL (1e15)
        // rawGrossProfit = 2e9 * 1e6 * 1.0 = 2e15
        // rawGrossLoss = 1e9 * 1e6 = 1e15
        // rawNetPnl = 2e15 - 1e15 = 1e15 > 0
        val profitDominant = TradingOutputParser.calculatePnl(
            investmentAmount = 1_000_000.0,
            payoutPercentage = 100.0,
            profitCount = 2_000_000_000,
            lossCount = 1_000_000_000
        )
        assertTrue("Gross profit must be finite", profitDominant.grossProfit.isFinite())
        assertTrue("Gross loss must be finite", profitDominant.grossLoss.isFinite())
        assertTrue("Net PnL must be finite", profitDominant.netPnl.isFinite())
        assertEquals(TradingOutputParser.MAX_FINITE_PNL, profitDominant.grossProfit, 1e-6)
        assertEquals(0.5 * TradingOutputParser.MAX_FINITE_PNL, profitDominant.grossLoss, 1e-6)
        assertTrue("Net PnL must retain positive sign", profitDominant.netPnl > 0.0)
        assertEquals(0.5 * TradingOutputParser.MAX_FINITE_PNL, profitDominant.netPnl, 1e-6)
        // Verify exact displayed arithmetic relationship: grossProfit - grossLoss == netPnl
        assertEquals(profitDominant.grossProfit - profitDominant.grossLoss, profitDominant.netPnl, 1e-6)

        // Loss dominates: both gross values reach/exceed MAX_FINITE_PNL
        // rawGrossProfit = 1e9 * 1e6 = 1e15
        // rawGrossLoss = 2e9 * 1e6 = 2e15
        // rawNetPnl = 1e15 - 2e15 = -1e15 < 0
        val lossDominant = TradingOutputParser.calculatePnl(
            investmentAmount = 1_000_000.0,
            payoutPercentage = 100.0,
            profitCount = 1_000_000_000,
            lossCount = 2_000_000_000
        )
        assertTrue("Gross profit must be finite", lossDominant.grossProfit.isFinite())
        assertTrue("Gross loss must be finite", lossDominant.grossLoss.isFinite())
        assertTrue("Net PnL must be finite", lossDominant.netPnl.isFinite())
        assertEquals(0.5 * TradingOutputParser.MAX_FINITE_PNL, lossDominant.grossProfit, 1e-6)
        assertEquals(TradingOutputParser.MAX_FINITE_PNL, lossDominant.grossLoss, 1e-6)
        assertTrue("Net PnL must retain negative sign", lossDominant.netPnl < 0.0)
        assertEquals(-0.5 * TradingOutputParser.MAX_FINITE_PNL, lossDominant.netPnl, 1e-6)
        // Verify exact displayed arithmetic relationship: grossProfit - grossLoss == netPnl
        assertEquals(lossDominant.grossProfit - lossDominant.grossLoss, lossDominant.netPnl, 1e-6)
    }

    @Test
    fun sampleChartGenerator_canonicalFieldParity() {
        val scenario = com.example.data.sample.SampleChartGenerator.scenarios[0] // Bullish Surge +1.25%, +0.75%
        val sampleAnalysis = com.example.data.sample.SampleChartGenerator.generateAnalysisForScenario(scenario)
        val canonicalResult = TradingOutputParser.calculateStrengthsAudited(1.25, 0.75)

        // SampleChartGenerator must populate all canonical mathematical fields identically
        assertEquals(canonicalResult.netSum, sampleAnalysis.netSumValue!!, 1e-6)
        assertEquals(canonicalResult.totalMagnitude, sampleAnalysis.totalMagnitude, 1e-6)
        assertEquals(canonicalResult.sensitivityRatio, sampleAnalysis.sensitivityRatio, 1e-6)
        assertEquals(canonicalResult.sensitivityRatio, sampleAnalysis.alignmentScore, 1e-6)
        assertEquals(canonicalResult.magnitudeScore, sampleAnalysis.magnitudeScore, 1e-6)
        assertEquals(canonicalResult.evidenceScore, sampleAnalysis.evidenceScore, 1e-6)
        assertEquals(canonicalResult.strengthLevel, sampleAnalysis.strengthLevel)
        assertEquals(canonicalResult.direction, sampleAnalysis.direction)
        assertEquals(canonicalResult.upPercentage, sampleAnalysis.upPercentage, 1e-6)
        assertEquals(canonicalResult.downPercentage, sampleAnalysis.downPercentage, 1e-6)
        assertEquals(canonicalResult.upPercentage, sampleAnalysis.calculatedPercentage, 1e-6)
        assertEquals(100.0, sampleAnalysis.upPercentage + sampleAnalysis.downPercentage, 1e-6)
    }

    @Test
    fun localQuantVisionEngine_canonicalFieldParity_successfulPathPopulatesAllFields() {
        val testVectors = listOf(
            Pair(1.25, 0.75),   // Bullish
            Pair(-1.25, -0.75), // Bearish
            Pair(0.05, -0.05)   // No Trade Zone
        )

        for ((v5m, v60m) in testVectors) {
            val localAnalysis = com.example.data.analyzer.LocalQuantVisionEngine.createAnalysisFromMetrics(
                est5m = v5m,
                est60m = v60m
            )
            val canonicalResult = TradingOutputParser.calculateStrengthsAudited(v5m, v60m)

            assertTrue("LocalQuantVisionEngine must succeed for ($v5m, $v60m)", localAnalysis.isSuccess)
            assertTrue("LocalQuantVisionEngine must be valid for ($v5m, $v60m)", localAnalysis.isValid)

            // Verify that every successful path populates all 11 canonical fields:
            assertNotNull(localAnalysis.netSumValue)
            assertNotNull(localAnalysis.totalMagnitude)
            assertNotNull(localAnalysis.sensitivityRatio)
            assertNotNull(localAnalysis.alignmentScore)
            assertNotNull(localAnalysis.magnitudeScore)
            assertNotNull(localAnalysis.evidenceScore)
            assertNotNull(localAnalysis.strengthLevel)
            assertNotNull(localAnalysis.direction)
            assertNotNull(localAnalysis.calculatedPercentage)
            assertNotNull(localAnalysis.upPercentage)
            assertNotNull(localAnalysis.downPercentage)

            // Verify parity against canonical calculateStrengthsAudited
            assertEquals(canonicalResult.netSum, localAnalysis.netSumValue!!, 1e-6)
            assertEquals(canonicalResult.totalMagnitude, localAnalysis.totalMagnitude, 1e-6)
            assertEquals(canonicalResult.sensitivityRatio, localAnalysis.sensitivityRatio, 1e-6)
            assertEquals(canonicalResult.sensitivityRatio, localAnalysis.alignmentScore, 1e-6)
            assertEquals(canonicalResult.magnitudeScore, localAnalysis.magnitudeScore, 1e-6)
            assertEquals(canonicalResult.evidenceScore, localAnalysis.evidenceScore, 1e-6)
            assertEquals(canonicalResult.strengthLevel, localAnalysis.strengthLevel)
            assertEquals(canonicalResult.direction, localAnalysis.direction)
            assertEquals(100.0, localAnalysis.upPercentage + localAnalysis.downPercentage, 1e-4)
        }
    }

    @Test
    fun placeholderApiKeys_bothMyGeminiApiKeyAndDefaultKey_rejected() {
        assertTrue(com.example.MainViewModel.isPlaceholderApiKey("MY_GEMINI_API_KEY"))
        assertTrue(com.example.MainViewModel.isPlaceholderApiKey("DEFAULT_KEY"))
        assertTrue(com.example.MainViewModel.isPlaceholderApiKey(""))
        assertTrue(com.example.MainViewModel.isPlaceholderApiKey("   "))
        assertTrue(com.example.MainViewModel.isPlaceholderApiKey(null))

        // Legitimate keys are not placeholders
        assertFalse(com.example.MainViewModel.isPlaceholderApiKey("AIzaSyDummyValidLookingKeyForTesting123"))
    }

    @Test
    fun alternation_boundaryThresholdsAndEpsilonConsistency() {
        // Below ALTERNATION_THRESHOLD: +0.049 and -0.049 are inside noise band [-0.05, 0.05]
        val historyBelow = listOf(
            com.example.data.models.MetricSnapshot(0.149, -0.10, 0.0, 0.049),   // net = +0.049
            com.example.data.models.MetricSnapshot(-0.149, 0.10, 0.0, -0.049),  // net = -0.049
            com.example.data.models.MetricSnapshot(0.149, -0.10, 0.0, 0.049)    // net = +0.049
        )
        val resBelow = TradingOutputParser.classifyMovementBehavior(
            val5m = -0.149,
            val60m = 0.10, // net = -0.049
            val1d = 0.0,
            history = historyBelow
        )
        assertFalse("Values at +/- 0.049 are inside noise band and must not trigger alternation", resBelow.code == "REPEATED_ALTERNATION")

        // Equal to ALTERNATION_THRESHOLD: +0.050 and -0.050 are boundary of noise band
        val historyEqual = listOf(
            com.example.data.models.MetricSnapshot(0.15, -0.10, 0.0, 0.05),   // net = +0.05
            com.example.data.models.MetricSnapshot(-0.15, 0.10, 0.0, -0.05),  // net = -0.05
            com.example.data.models.MetricSnapshot(0.15, -0.10, 0.0, 0.05)    // net = +0.05
        )
        val resEqual = TradingOutputParser.classifyMovementBehavior(
            val5m = -0.15,
            val60m = 0.10, // net = -0.05
            val1d = 0.0,
            history = historyEqual
        )
        assertFalse("Values at exactly +/- 0.05 are not strictly > 0.05 and must not trigger alternation", resEqual.code == "REPEATED_ALTERNATION")

        // Just above ALTERNATION_THRESHOLD: +0.051 and -0.051 cross boundary
        val historyAbove = listOf(
            com.example.data.models.MetricSnapshot(0.151, -0.10, 0.0, 0.051),   // net = +0.051
            com.example.data.models.MetricSnapshot(-0.151, 0.10, 0.0, -0.051),  // net = -0.051
            com.example.data.models.MetricSnapshot(0.151, -0.10, 0.0, 0.051)    // net = +0.051
        )
        val resAbove = TradingOutputParser.classifyMovementBehavior(
            val5m = -0.151,
            val60m = 0.10, // net = -0.051
            val1d = 0.0,
            history = historyAbove
        )
        assertEquals("REPEATED_ALTERNATION", resAbove.code)

        // Intended 0.10 boundary transitions
        val historyAt10 = listOf(
            com.example.data.models.MetricSnapshot(0.20, -0.10, 0.0, 0.10),   // net = +0.10
            com.example.data.models.MetricSnapshot(-0.20, 0.10, 0.0, -0.10),  // net = -0.10
            com.example.data.models.MetricSnapshot(0.20, -0.10, 0.0, 0.10)    // net = +0.10
        )
        val resAt10 = TradingOutputParser.classifyMovementBehavior(
            val5m = -0.20,
            val60m = 0.10, // net = -0.10
            val1d = 0.0,
            history = historyAt10
        )
        assertEquals("REPEATED_ALTERNATION", resAt10.code)

        // EPSILON boundary in stillness check
        val behaviorZeroMagnitude = TradingOutputParser.classifyMovementBehavior(
            val5m = 0.0,
            val60m = 0.0,
            val1d = 0.0
        )
        assertEquals("STILL_NO_TRADE", behaviorZeroMagnitude.code)
    }

    @Test
    fun probabilities_upPlusDownAlwaysFiniteRange0To100AndExactly100AfterRounding() {
        val testPairs = listOf(
            Pair(0.0, 0.0),
            Pair(0.01, -0.01),
            Pair(0.10, 0.10),
            Pair(0.10, -0.10),
            Pair(0.25, 0.35),
            Pair(-0.45, -0.15),
            Pair(1.25, -0.75),
            Pair(2.40, -0.60),
            Pair(0.333, 0.667),
            Pair(499.0, -200.0),
            Pair(-499.0, 100.0),
            Pair(0.05, 0.05)
        )
        for ((v5m, v60m) in testPairs) {
            val res = TradingOutputParser.calculateStrengthsAudited(v5m, v60m)
            assertTrue("UP must be finite for ($v5m, $v60m)", res.upPercentage.isFinite())
            assertTrue("DOWN must be finite for ($v5m, $v60m)", res.downPercentage.isFinite())
            assertTrue("UP must be >= 0 for ($v5m, $v60m)", res.upPercentage >= 0.0)
            assertTrue("UP must be <= 100 for ($v5m, $v60m)", res.upPercentage <= 100.0)
            assertTrue("DOWN must be >= 0 for ($v5m, $v60m)", res.downPercentage >= 0.0)
            assertTrue("DOWN must be <= 100 for ($v5m, $v60m)", res.downPercentage <= 100.0)
            assertEquals("UP + DOWN must equal 100.0 for ($v5m, $v60m)", 100.0, res.upPercentage + res.downPercentage, 1e-6)
        }
    }

    @Test
    fun parse_1dMissing_isNotRejected_allowsPreservingPreviousContext() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.80%
            * **৬০ মিনিটের পরিবর্তন:** +0.40%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertTrue(result.isSuccess)
        assertEquals(null, result.change1dValue)
        assertEquals("--", result.change1d)
        assertFalse("Missing 1D must NOT be flagged as explicitly rejected", result.is1dExplicitlyRejected)
    }

    @Test
    fun parse_1dApproximateWithTilde_isExplicitlyRejected() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.80%
            * **৬০ মিনিটের পরিবর্তন:** +0.40%
            * **১ দিন:** ~+1.50%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertTrue(result.isSuccess)
        assertEquals(null, result.change1dValue)
        assertEquals("--", result.change1d)
        assertTrue("Approximate 1D with ~ must be flagged as explicitly rejected", result.is1dExplicitlyRejected)
    }

    @Test
    fun parse_1dApproximateWithAlmostEqual_isExplicitlyRejected() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.80%
            * **৬০ মিনিটের পরিবর্তন:** +0.40%
            * **1 day:** ≈-2.10%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertTrue(result.isSuccess)
        assertEquals(null, result.change1dValue)
        assertEquals("--", result.change1d)
        assertTrue("Approximate 1D with ≈ must be flagged as explicitly rejected", result.is1dExplicitlyRejected)
    }

    @Test
    fun parse_1dUnsignedNonZero_isExplicitlyRejected() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.80%
            * **৬০ মিনিটের পরিবর্তন:** +0.40%
            * **1d change:** 1.50%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertTrue(result.isSuccess)
        assertEquals(null, result.change1dValue)
        assertEquals("--", result.change1d)
        assertTrue("Unsigned non-zero 1D must be flagged as explicitly rejected", result.is1dExplicitlyRejected)
    }

    @Test
    fun parse_1dValidExplicitSign_isAcceptedAndNotRejected() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.80%
            * **৬০ মিনিটের পরিবর্তন:** +0.40%
            * **1 day change:** +1.50%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertTrue(result.isSuccess)
        assertEquals(1.50, result.change1dValue!!, 1e-6)
        assertEquals("+1.5%", result.change1d)
        assertFalse("Valid 1D must NOT be flagged as explicitly rejected", result.is1dExplicitlyRejected)
    }

    @Test
    fun calculatePnl_intMaxCounts_normal85Payout_satisfiesArithmeticConsistency() {
        val pnl = TradingOutputParser.calculatePnl(
            investmentAmount = 10.0,
            payoutPercentage = 85.0,
            profitCount = Int.MAX_VALUE,
            lossCount = Int.MAX_VALUE
        )
        assertTrue("Gross profit must be finite", pnl.grossProfit.isFinite())
        assertTrue("Gross loss must be finite", pnl.grossLoss.isFinite())
        assertTrue("Net PnL must be finite", pnl.netPnl.isFinite())
        assertTrue("Net PnL must be negative for 85% payout with equal wins and losses", pnl.netPnl < 0.0)
        assertEquals("Win rate must be exactly 50%", 50.0, pnl.winRate, 1e-6)
        // Verify displayedNetPnl == displayedGrossProfit - displayedGrossLoss
        assertEquals("displayedNetPnl must equal displayedGrossProfit - displayedGrossLoss",
            pnl.grossProfit - pnl.grossLoss, pnl.netPnl, 1e-6)
    }

    @Test
    fun calculatePnl_commonScalingPolicy_alwaysPreservesArithmeticRelationship() {
        val testCases = listOf(
            Triple(2_000_000_000, 1_000_000_000, 1_000_000.0),
            Triple(1_000_000_000, 2_000_000_000, 1_000_000.0),
            Triple(Int.MAX_VALUE, Int.MAX_VALUE / 2, 100_000.0),
            Triple(Int.MAX_VALUE / 2, Int.MAX_VALUE, 100_000.0)
        )
        for ((wins, losses, investment) in testCases) {
            val pnl = TradingOutputParser.calculatePnl(
                investmentAmount = investment,
                payoutPercentage = 90.0,
                profitCount = wins,
                lossCount = losses
            )
            assertTrue("Gross profit must be finite", pnl.grossProfit.isFinite())
            assertTrue("Gross loss must be finite", pnl.grossLoss.isFinite())
            assertTrue("Net PnL must be finite", pnl.netPnl.isFinite())
            assertTrue("Gross profit within MAX_FINITE_PNL", pnl.grossProfit <= TradingOutputParser.MAX_FINITE_PNL)
            assertTrue("Gross loss within MAX_FINITE_PNL", pnl.grossLoss <= TradingOutputParser.MAX_FINITE_PNL)
            assertTrue("Net PnL magnitude within MAX_FINITE_PNL", kotlin.math.abs(pnl.netPnl) <= TradingOutputParser.MAX_FINITE_PNL)
            assertEquals("displayedNetPnl must equal displayedGrossProfit - displayedGrossLoss",
                pnl.grossProfit - pnl.grossLoss, pnl.netPnl, 1e-6)
        }
    }

    @Test
    fun resolveCandidatePolarity_tildeAndAlmostEqualAndUnsignedHandledStrictly() {
        val tildeRes = com.example.data.analyzer.LocalQuantVisionEngine.resolveCandidatePolarity("~+1.50%", 1, 1.50)
        assertTrue("Tilde must be approximate", tildeRes.isApproximate)
        assertEquals("~", tildeRes.prefix)
        assertEquals(1.50, tildeRes.resolvedValue, 1e-6)

        val almostRes = com.example.data.analyzer.LocalQuantVisionEngine.resolveCandidatePolarity("≈-1.50%", -1, 1.50)
        assertTrue("Almost equal must be approximate", almostRes.isApproximate)
        assertEquals("≈", almostRes.prefix)
        assertEquals(-1.50, almostRes.resolvedValue, 1e-6)

        val unsignedNeutral = com.example.data.analyzer.LocalQuantVisionEngine.resolveCandidatePolarity("1.50%", 0, 1.50)
        assertTrue("Unsigned neutral color must be ambiguous", unsignedNeutral.isAmbiguous)
        assertFalse("Unsigned neutral color is not approximate", unsignedNeutral.isApproximate)

        val signedGreen = com.example.data.analyzer.LocalQuantVisionEngine.resolveCandidatePolarity("+1.50%", 1, 1.50)
        assertFalse("Signed green is not approximate", signedGreen.isApproximate)
        assertFalse("Signed green is not ambiguous", signedGreen.isAmbiguous)
        assertEquals(1.50, signedGreen.resolvedValue, 1e-6)

        val signedRed = com.example.data.analyzer.LocalQuantVisionEngine.resolveCandidatePolarity("-1.50%", -1, 1.50)
        assertFalse("Signed red is not approximate", signedRed.isApproximate)
        assertFalse("Signed red is not ambiguous", signedRed.isAmbiguous)
        assertEquals(-1.50, signedRed.resolvedValue, 1e-6)
    }

    @Test
    fun isValidMetricValue_boundsAndSpecialValuesChecked() {
        assertTrue(TradingOutputParser.isValidMetricValue(0.0))
        assertTrue(TradingOutputParser.isValidMetricValue(500.0))
        assertTrue(TradingOutputParser.isValidMetricValue(-500.0))
        assertTrue(TradingOutputParser.isValidMetricValue(0.05))
        assertTrue(TradingOutputParser.isValidMetricValue(-0.08))

        assertFalse(TradingOutputParser.isValidMetricValue(500.001))
        assertFalse(TradingOutputParser.isValidMetricValue(-500.001))
        assertFalse(TradingOutputParser.isValidMetricValue(9999.0))
        assertFalse(TradingOutputParser.isValidMetricValue(Double.NaN))
        assertFalse(TradingOutputParser.isValidMetricValue(Double.POSITIVE_INFINITY))
        assertFalse(TradingOutputParser.isValidMetricValue(Double.NEGATIVE_INFINITY))
        assertFalse(TradingOutputParser.isValidMetricValue(null))
    }

    @Test
    fun calculatePnl_overflowingInfiniteTrades_producesDeterministicFiniteScaledResult() {
        // High trades with high investment producing large magnitudes
        val pnl = TradingOutputParser.calculatePnl(
            investmentAmount = 1_000_000_000.0,
            payoutPercentage = 85.0,
            profitCount = 2_000_000_000,
            lossCount = 1_000_000_000
        )
        assertTrue("Gross profit must be finite", pnl.grossProfit.isFinite())
        assertTrue("Gross loss must be finite", pnl.grossLoss.isFinite())
        assertTrue("Net PnL must be finite", pnl.netPnl.isFinite())
        assertTrue("Net PnL must be positive when wins dominate", pnl.netPnl > 0.0)
        assertFalse("Must not silently be all zero", pnl.grossProfit == 0.0 && pnl.netPnl == 0.0)
        assertEquals("displayedNetPnl must equal displayedGrossProfit - displayedGrossLoss",
            pnl.grossProfit - pnl.grossLoss, pnl.netPnl, 1e-6)
    }

    @Test
    fun parse_1dOutOfRange_isExplicitlyRejected() {
        val rawInput = """
            **গাণিতিক হিসাব:**
            * **৫ মিনিটের পরিবর্তন:** +0.80%
            * **৬০ মিনিটের পরিবর্তন:** +0.40%
            * **1 day change:** +999.0%
        """.trimIndent()

        val result = TradingOutputParser.parse(rawInput, 100L)
        assertTrue(result.isSuccess)
        assertEquals(null, result.change1dValue)
        assertEquals("--", result.change1d)
        assertTrue("Out-of-range 1D (> 500%) must be flagged as explicitly rejected", result.is1dExplicitlyRejected)
    }

    @Test
    fun formatActionCardContent_momentumLossDown_usesSpecificBounceWarningEvenWithNegative5m60m() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "MOMENTUM_LOSS_DOWN",
            change5mValue = -0.35,
            change60mValue = -1.20
        )
        val (title, subtitle) = formatActionCardContent(
            rawTitle = "নিচে নামার গতি কমছে — সাময়িক বিরতি বা বাউন্সের সম্ভাবনা",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = -0.35,
            v60m = -1.20
        )
        assertTrue("Title should contain specific UP bounce possibility", title.contains("UP বাউন্স সম্ভাবনা]"))
        assertFalse("Title should NOT contain generic bounce alert", title.contains("UP বাউন্স সতর্কতা]"))
        assertTrue("Subtitle should contain specific bounce warning", subtitle.contains("বাউন্স সতর্কতা: DOWN নামার গতি কমছে; DOWN চাপ কমে গাড়ির ব্রেকের মতো"))
        assertTrue("Subtitle should contain UP bounce or reversal risk", subtitle.contains("সাময়িক UP বাউন্স বা রিভার্সাল ঝুঁকি রয়েছে"))
        assertFalse("Subtitle should NOT contain wait words", subtitle.contains("স্থগিত") || subtitle.contains("অপেক্ষা"))
        assertFalse("Subtitle should NOT contain generic strong down pullback note", subtitle.contains("তীব্র পতনের কারণে যেকোনো মুহূর্তে গাড়ির ব্রেকের মতো"))
    }

    @Test
    fun formatActionCardContent_momentumLossUp_usesSpecificRetracementWarningEvenWithPositive5m60m() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "MOMENTUM_LOSS_UP",
            change5mValue = 0.35,
            change60mValue = 1.20
        )
        val (title, subtitle) = formatActionCardContent(
            rawTitle = "উপরে ওঠার গতি কমছে — সাময়িক বিরতি বা পতনের সম্ভাবনা",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.35,
            v60m = 1.20
        )
        assertTrue("Title should contain specific DOWN retracement possibility", title.contains("DOWN রিট্রেসমেন্ট সম্ভাবনা]"))
        assertTrue("Subtitle should contain specific momentum loss up pullback warning", subtitle.contains("UP যাওয়ার গতি কমছে; UP চাপ কমে যেকোনো সময়"))
        assertTrue("Subtitle should contain DOWN pullback or reversal risk", subtitle.contains("সাময়িক DOWN পুলব্যাক বা রিভার্সাল ঝুঁকি রয়েছে"))
        assertFalse("Subtitle should NOT contain wait words", subtitle.contains("স্থগিত") || subtitle.contains("অপেক্ষা"))
        assertFalse("Subtitle should NOT contain generic strong up pullback note", subtitle.contains("তীব্র বৃদ্ধির কারণে যেকোনো মুহূর্তে গাড়ির ব্রেকের মতো"))
    }

    @Test
    fun formatActionCardContent_downtrendPullbackUp_usesSpecificPullbackWarningEvenIfNegativeAligned() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "DOWNTREND_PULLBACK_UP",
            change5mValue = -0.20,
            change60mValue = -0.80
        )
        val (title, subtitle) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী টান",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = -0.20,
            v60m = -0.80
        )
        assertTrue("Title should contain specific resume down risk", title.contains("• [পুনরায় DOWN পতন শঙ্কা]"))
        assertTrue("Subtitle should contain specific pullback advice", subtitle.contains("💡 পুলব্যাক পরামর্শ: এটি মূল পতনের মাঝে সাময়িক UP বাউন্স; বাউন্স শেষ হওয়া পর্যন্ত অপেক্ষা করে পুনরায় ডাউন এন্ট্রি নিন।"))
    }

    @Test
    fun formatActionCardContent_uptrendPullbackDown_usesSpecificPullbackWarningEvenIfPositiveAligned() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "UPTREND_PULLBACK_DOWN",
            change5mValue = 0.20,
            change60mValue = 0.80
        )
        val (title, subtitle) = formatActionCardContent(
            rawTitle = "আপট্রেন্ডে নিম্নমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "আপট্রেন্ডের মাঝে সাময়িক সংশোধন",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.20,
            v60m = 0.80
        )
        assertTrue("Title should contain specific resume up risk", title.contains("• [পুনরায় UP উত্থান শঙ্কা]"))
        assertTrue("Subtitle should contain specific pullback advice", subtitle.contains("💡 পুলব্যাক পরামর্শ: এটি মূল ঊর্ধ্বগতির মাঝে সাময়িক DOWN সংশোধন; সংশোধন শেষ হওয়া পর্যন্ত অপেক্ষা করে পুনরায় আপ এন্ট্রি নিন।"))
    }

    @Test
    fun formatActionCardContent_steadyAlignedDown_usesGenericWarningWhenNoSpecificBehaviorApplies() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "STEADY_ALIGNED_DOWN",
            change5mValue = -0.80,
            change60mValue = -1.50
        )
        val (title, subtitle) = formatActionCardContent(
            rawTitle = "স্থিতিশীল গতিতে নিচে নামার ট্রেন্ড",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = -0.80,
            v60m = -1.50
        )
        assertTrue("Title should have generic bounce alert", title.contains("UP বাউন্স সতর্কতা]"))
        assertTrue("Subtitle should have generic down pullback note", subtitle.contains("⚠️ পুলব্যাক সতর্কতা: তীব্র পতনের কারণে যেকোনো মুহূর্তে গাড়ির ব্রেকের মতো"))
    }

    @Test
    fun formatActionCardContent_fakeoutAndReversals_remainUnchanged() {
        val trapAnalysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "TOP_FAKEOUT_RISK",
            change5mValue = 0.80,
            change60mValue = 1.20
        )
        val (trapTitle, trapSubtitle) = formatActionCardContent(
            rawTitle = "সামান্য উপরে গিয়ে দ্রুত পতনের ঝুঁকি (Bull Trap)",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = trapAnalysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.80,
            v60m = 1.20
        )
        assertTrue("Trap title should have trap tag", trapTitle.contains("UP ট্র্যাপ / Bull Trap • [হঠাৎ তীব্র DOWN পতনের ঝুঁকি]"))
        assertFalse("Trap subtitle should not have generic pullback note appended", trapSubtitle.contains("পুলব্যাক সতর্কতা"))

        val revAnalysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "CONFIRMED_BULLISH_REVERSAL",
            change5mValue = 0.60,
            change60mValue = 0.30
        )
        val (revTitle, revSubtitle) = formatActionCardContent(
            rawTitle = "নিশ্চিত ঊর্ধ্বমুখী রিভার্সাল — ট্রেন্ড বদল",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী",
            analysis = revAnalysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.60,
            v60m = 0.30
        )
        assertTrue("Reversal title should have BUY signal tag", revTitle.contains("নিশ্চিত UP রিভার্সাল • [BUY সিগন্যাল]"))
        assertFalse("Reversal subtitle should not have generic pullback note appended", revSubtitle.contains("পুলব্যাক সতর্কতা"))
    }

    @Test
    fun formatActionCardContent_confirmedBearishReversal_withDeceleratingMomentum_showsBounceWarning() {
        val revAnalysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "CONFIRMED_BEARISH_REVERSAL",
            change5mValue = -0.12,
            change60mValue = -0.16
        )
        val (revTitle, revSubtitle) = formatActionCardContent(
            rawTitle = "নিশ্চিত নিম্নমুখী রিভার্সাল — ট্রেন্ড বদল",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = revAnalysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = -0.12,
            v60m = -0.16
        )
        assertTrue("Reversal title should include UP bounce risk tag when 5m is slower than 60m",
            revTitle.contains("নিশ্চিত DOWN রিভার্সাল • [SELL সিগন্যাল • সাময়িক UP বাউন্স ঝুঁকি]"))
        assertTrue("Reversal subtitle should warn about DOWN slowing and UP bounce risk",
            revSubtitle.contains("বাউন্স সতর্কতা: DOWN নামার গতি কমছে; DOWN চাপ কমে গাড়ির ব্রেকের মতো সাময়িক UP বাউন্স বা রিভার্সাল ঝুঁকি রয়েছে।"))
        assertFalse("Must not forbid entry or say wait", revSubtitle.contains("স্থগিত") || revSubtitle.contains("অপেক্ষা"))
    }

    @Test
    fun formatActionCardContent_confirmedBullishReversal_withDeceleratingMomentum_showsPullbackWarning() {
        val revAnalysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "CONFIRMED_BULLISH_REVERSAL",
            change5mValue = 0.12,
            change60mValue = 0.16
        )
        val (revTitle, revSubtitle) = formatActionCardContent(
            rawTitle = "নিশ্চিত ঊর্ধ্বমুখী রিভার্সাল — ট্রেন্ড বদল",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী",
            analysis = revAnalysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.12,
            v60m = 0.16
        )
        assertTrue("Reversal title should include DOWN pullback risk tag when 5m is slower than 60m",
            revTitle.contains("নিশ্চিত UP রিভার্সাল • [BUY সিগন্যাল • সাময়িক DOWN পুলব্যাক ঝুঁকি]"))
        assertTrue("Reversal subtitle should warn about UP slowing and DOWN pullback risk",
            revSubtitle.contains("মোমেন্টাম সতর্কতা: UP যাওয়ার গতি কমছে; UP চাপ কমে যেকোনো সময় সাময়িক DOWN পুলব্যাক বা রিভার্সাল ঝুঁকি রয়েছে।"))
        assertFalse("Must not forbid entry or say wait", revSubtitle.contains("স্থগিত") || revSubtitle.contains("অপেক্ষা"))
    }
}

