package com.example

import com.example.data.models.MetricSnapshot
import com.example.data.models.TradeDirection
import com.example.data.models.TradingAnalysis
import com.example.ui.components.HEURISTIC_WINDOW_LABEL
import com.example.ui.components.INSUFFICIENT_HISTORY_MSG
import com.example.ui.components.PullbackHistoryPoint
import com.example.ui.components.calculateHeuristicPullbackWindowLabel
import com.example.ui.components.calculatePullbackDurationLabel
import com.example.ui.components.calculateQuickPrediction
import com.example.ui.components.evaluateCanHaveDuration
import com.example.ui.components.formatActionCardContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PullbackDurationTest {

    @Test
    fun insufficientHistory_returnsNeutralBengaliPhrase_andOmitDuration() {
        val result = calculatePullbackDurationLabel(v5m = 0.35, v60m = -0.75, history = emptyList<MetricSnapshot>())
        assertEquals("Should return neutral Bengali phrase when history is empty", INSUFFICIENT_HISTORY_MSG, result)
        assertNotEquals("Must never return ~15s", "~15s", result)
        assertNotEquals("Must never return ~20s", "~20s", result)

        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "DOWNTREND_PULLBACK_UP",
            change5mValue = 0.35,
            change60mValue = -0.75
        )
        val (title, subtitle) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.35,
            v60m = -0.75,
            history = emptyList()
        )
        assertTrue("Title has warning without fabricated seconds", title.contains("• [পুনরায় DOWN পতন শঙ্কা]"))
        assertFalse("Title must not have ~15s", title.contains("~15s"))
        assertFalse("Title must not have ~20s", title.contains("~20s"))
        assertFalse("Subtitle must not have fabricated duration", subtitle.contains("~15s"))
    }

    @Test
    fun nullValues_returnNeutralBengaliPhrase_andNeverDefaultTo15s() {
        assertEquals(INSUFFICIENT_HISTORY_MSG, calculatePullbackDurationLabel(null, null))
        assertEquals(INSUFFICIENT_HISTORY_MSG, calculatePullbackDurationLabel(0.20, null))
        assertEquals(INSUFFICIENT_HISTORY_MSG, calculatePullbackDurationLabel(null, -0.40))

        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "DOWNTREND_PULLBACK_UP",
            change5mValue = null,
            change60mValue = null
        )
        val (title, subtitle) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = null,
            v60m = null,
            history = emptyList()
        )
        assertFalse("Null metrics must never fabricate duration", title.contains("~15s"))
        assertFalse("Null metrics must never fabricate duration", subtitle.contains("~15s"))
    }

    @Test
    fun nanAndInfiniteValues_returnNeutralBengaliPhrase_andNeverDefaultTo15s() {
        assertEquals(INSUFFICIENT_HISTORY_MSG, calculatePullbackDurationLabel(Double.NaN, 0.40))
        assertEquals(INSUFFICIENT_HISTORY_MSG, calculatePullbackDurationLabel(0.40, Double.POSITIVE_INFINITY))
        assertEquals(INSUFFICIENT_HISTORY_MSG, calculatePullbackDurationLabel(Double.NEGATIVE_INFINITY, Double.NaN))

        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "UPTREND_PULLBACK_DOWN",
            change5mValue = Double.NaN,
            change60mValue = 0.50
        )
        val (title, subtitle) = formatActionCardContent(
            rawTitle = "আপট্রেন্ডে নিম্নমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = Double.NaN,
            v60m = 0.50,
            history = emptyList()
        )
        assertFalse("NaN metrics must never fabricate duration", title.contains("~15s"))
        assertFalse("NaN metrics must never fabricate duration", subtitle.contains("~15s"))
    }

    @Test
    fun actualDowntrendPullbackUp_withoutHistory_omitsSeconds_withHistory_showsExactSeconds() {
        val now = 100_000L
        val history = listOf(
            TradingAnalysis(
                isValid = true,
                isApproximate = false,
                change5mValue = 0.30,
                change60mValue = -0.60,
                timestamp = now - 20_000L,
                behaviorCode = "DOWNTREND_PULLBACK_UP"
            )
        )
        val currentAnalysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "DOWNTREND_PULLBACK_UP",
            change5mValue = 0.25,
            change60mValue = -0.55,
            timestamp = now
        )

        // With confirmed history:
        val (titleWithHist, subtitleWithHist) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = currentAnalysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.25,
            v60m = -0.55,
            history = history
        )
        assertTrue("Title with history shows exact seconds", titleWithHist.contains("20s পুনরায় DOWN পতন শঙ্কা") || titleWithHist.contains("20s"))
        assertTrue("Subtitle with history shows exact elapsed duration", subtitleWithHist.contains("20s চলমান"))

        // Without history:
        val (titleNoHist, subtitleNoHist) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = currentAnalysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.25,
            v60m = -0.55,
            history = emptyList()
        )
        assertTrue("Title without history shows generic tag", titleNoHist.contains("• [পুনরায় DOWN পতন শঙ্কা]"))
        assertFalse("Title without history has no seconds", titleNoHist.contains("20s"))
        assertTrue("Subtitle without history gives advice without fabricated duration", subtitleNoHist.contains("💡 পুলব্যাক পরামর্শ: এটি মূল পতনের মাঝে সাময়িক UP বাউন্স; বাউন্স শেষ হওয়া পর্যন্ত অপেক্ষা করে পুনরায় ডাউন এন্ট্রি নিন।"))
    }

    @Test
    fun actualUptrendPullbackDown_withoutHistory_omitsSeconds_withHistory_showsExactSeconds() {
        val now = 120_000L
        val history = listOf(
            TradingAnalysis(
                isValid = true,
                isApproximate = false,
                change5mValue = -0.20,
                change60mValue = 0.80,
                timestamp = now - 25_000L,
                behaviorCode = "UPTREND_PULLBACK_DOWN"
            )
        )
        val currentAnalysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "UPTREND_PULLBACK_DOWN",
            change5mValue = -0.15,
            change60mValue = 0.75,
            timestamp = now
        )

        // With confirmed history:
        val (titleWithHist, subtitleWithHist) = formatActionCardContent(
            rawTitle = "আপট্রেন্ডে নিম্নমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী",
            analysis = currentAnalysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = -0.15,
            v60m = 0.75,
            history = history
        )
        assertTrue("Title with history shows exact seconds", titleWithHist.contains("25s পুনরায় UP উত্থান শঙ্কা") || titleWithHist.contains("25s"))
        assertTrue("Subtitle with history shows exact elapsed duration", subtitleWithHist.contains("25s চলমান"))

        // Without history:
        val (titleNoHist, subtitleNoHist) = formatActionCardContent(
            rawTitle = "আপট্রেন্ডে নিম্নমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী",
            analysis = currentAnalysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = -0.15,
            v60m = 0.75,
            history = emptyList()
        )
        assertTrue("Title without history shows generic tag", titleNoHist.contains("• [পুনরায় UP উত্থান শঙ্কা]"))
        assertFalse("Title without history has no seconds", titleNoHist.contains("25s"))
        assertTrue("Subtitle without history gives advice without fabricated duration", subtitleNoHist.contains("💡 পুলব্যাক পরামর্শ: এটি মূল ঊর্ধ্বগতির মাঝে সাময়িক DOWN সংশোধন; সংশোধন শেষ হওয়া পর্যন্ত অপেক্ষা করে পুনরায় আপ এন্ট্রি নিন।"))
    }

    @Test
    fun confirmedHistoricalTimestamps_calculateAccurateSeconds_andDoNotUseMaxAbsPercentages() {
        val now = 200_000L
        // 60m is -3.50% (under previous heuristic it would have output ~60s+), but history shows 12 seconds
        val history1 = listOf(
            MetricSnapshot(
                val5m = 0.15,
                val60m = -3.50,
                timestamp = now - 12_000L,
                isValid = true,
                isApproximate = false
            )
        )
        val label1 = calculatePullbackDurationLabel(
            v5m = 0.20,
            v60m = -3.50,
            history = history1,
            currentTimestamp = now,
            behaviorCode = "DOWNTREND_PULLBACK_UP"
        )
        assertEquals("Should be accurately measured 12s, NOT ~60s+", "12s", label1)

        // 5m is 0.05% (under previous heuristic it would have output ~15s), but history shows 42 seconds
        val history2 = listOf(
            MetricSnapshot(
                val5m = 0.05,
                val60m = -0.15,
                timestamp = now - 42_000L,
                isValid = true,
                isApproximate = false
            )
        )
        val label2 = calculatePullbackDurationLabel(
            v5m = 0.05,
            v60m = -0.15,
            history = history2,
            currentTimestamp = now,
            behaviorCode = "DOWNTREND_PULLBACK_UP"
        )
        assertEquals("Should be accurately measured 42s, NOT ~15s", "42s", label2)
    }

    @Test
    fun noAccidentalDurationOnInvalidNoTradeOrApproximateAnalysis() {
        val now = 300_000L
        val history = listOf(
            TradingAnalysis(
                isValid = true,
                isApproximate = false,
                change5mValue = 0.30,
                change60mValue = -0.50,
                timestamp = now - 15_000L
            )
        )
        val analysis = TradingAnalysis(
            isValid = false,
            isApproximate = true,
            isNoTradeZone = true,
            change5mValue = 0.30,
            change60mValue = -0.50,
            timestamp = now
        )

        // Test invalid
        val (invalidTitle, invalidSubtitle) = formatActionCardContent(
            rawTitle = "অনিশ্চিত ডাটা",
            rawSubtitle = "বিশ্লেষণ অসম্ভব",
            analysis = analysis,
            isValid = false,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.30,
            v60m = -0.50,
            history = history
        )
        assertFalse("Invalid card has no duration tag", invalidTitle.contains("• ["))
        assertFalse("Invalid card has no duration note", invalidSubtitle.contains("বাউন্স সতর্কতা"))

        // Test approximate
        val (approxTitle, approxSubtitle) = formatActionCardContent(
            rawTitle = "আনুমানিক হিসাব",
            rawSubtitle = "সতর্কতা",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = true,
            v5m = 0.30,
            v60m = -0.50,
            history = history
        )
        assertFalse("Approximate card has no duration tag", approxTitle.contains("• ["))
        assertFalse("Approximate card has no duration note", approxSubtitle.contains("বাউন্স সতর্কতা"))

        // Test no-trade
        val (noTradeTitle, noTradeSubtitle) = formatActionCardContent(
            rawTitle = "নো-ট্রেড জোন",
            rawSubtitle = "মার্কেট ফ্ল্যাট",
            analysis = analysis,
            isValid = true,
            isNoTrade = true,
            isApprox = false,
            v5m = 0.30,
            v60m = -0.50,
            history = history
        )
        assertFalse("No-trade card has no duration tag", noTradeTitle.contains("• ["))
        assertFalse("No-trade card has no duration note", noTradeSubtitle.contains("বাউন্স সতর্কতা"))
    }

    @Test
    fun existingFakeoutAndReversalTextRemainUnchanged() {
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
        assertTrue("Trap title unchanged", trapTitle.contains("UP ট্র্যাপ / Bull Trap • [হঠাৎ তীব্র DOWN পতনের ঝুঁকি]"))
        assertFalse("Trap subtitle has no generic pullback note", trapSubtitle.contains("পুলব্যাক সতর্কতা"))

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
        assertTrue("Reversal title unchanged", revTitle.contains("নিশ্চিত UP রিভার্সাল • [BUY সিগন্যাল]"))
        assertFalse("Reversal subtitle has no generic pullback note", revSubtitle.contains("পুলব্যাক সতর্কতা"))
    }

    @Test
    fun durationIncreasing_whenCurrentWallClockTimeAdvances() {
        val baseTimestamp = 500_000L
        val history = listOf(
            TradingAnalysis(
                isValid = true,
                isApproximate = false,
                change5mValue = 0.40,
                change60mValue = -0.80,
                timestamp = baseTimestamp,
                behaviorCode = "DOWNTREND_PULLBACK_UP"
            )
        )
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "DOWNTREND_PULLBACK_UP",
            change5mValue = 0.40,
            change60mValue = -0.80,
            timestamp = baseTimestamp
        )

        // At baseTimestamp + 20s
        val (title1, subtitle1) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.40,
            v60m = -0.80,
            history = history,
            wallClockTimestamp = baseTimestamp + 20_000L
        )
        assertTrue("At +20s shows 20s in title", title1.contains("20s"))
        assertTrue("At +20s shows 20s চলমান in subtitle", subtitle1.contains("20s চলমান"))

        // At baseTimestamp + 21s
        val (title2, subtitle2) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.40,
            v60m = -0.80,
            history = history,
            wallClockTimestamp = baseTimestamp + 21_000L
        )
        assertTrue("At +21s shows 21s in title", title2.contains("21s"))
        assertTrue("At +21s shows 21s চলমান in subtitle", subtitle2.contains("21s চলমান"))

        // At baseTimestamp + 25s
        val (title3, subtitle3) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.40,
            v60m = -0.80,
            history = history,
            wallClockTimestamp = baseTimestamp + 25_000L
        )
        assertTrue("At +25s shows 25s in title", title3.contains("25s"))
        assertTrue("At +25s shows 25s চলমান in subtitle", subtitle3.contains("25s চলমান"))
    }

    @Test
    fun stableUnchangedOcrValues_stillAllowDisplayedElapsedDurationToUpdate() {
        val ocrTimestamp = 1_000_000L
        // The analysis and history entry were captured at ocrTimestamp
        val reusedAnalysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "UPTREND_PULLBACK_DOWN",
            change5mValue = -0.30,
            change60mValue = 0.90,
            timestamp = ocrTimestamp
        )
        val reusedHistory = listOf(reusedAnalysis)

        // Wall clock 15 seconds after OCR capture:
        val (title15, subtitle15) = formatActionCardContent(
            rawTitle = "আপট্রেন্ডে নিম্নমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী",
            analysis = reusedAnalysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = -0.30,
            v60m = 0.90,
            history = reusedHistory,
            wallClockTimestamp = ocrTimestamp + 15_000L
        )
        assertTrue("Should show 15s even with frozen OCR analysis timestamp", title15.contains("15s"))
        assertTrue("Should show 15s চলমান in subtitle", subtitle15.contains("15s চলমান"))

        // Wall clock 16 seconds after OCR capture:
        val (title16, subtitle16) = formatActionCardContent(
            rawTitle = "আপট্রেন্ডে নিম্নমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী",
            analysis = reusedAnalysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = -0.30,
            v60m = 0.90,
            history = reusedHistory,
            wallClockTimestamp = ocrTimestamp + 16_000L
        )
        assertTrue("Should live update to 16s with identical frozen OCR analysis", title16.contains("16s"))
        assertTrue("Should live update to 16s চলমান in subtitle", subtitle16.contains("16s চলমান"))

        // Wall clock 30 seconds after OCR capture:
        val (title30, subtitle30) = formatActionCardContent(
            rawTitle = "আপট্রেন্ডে নিম্নমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী",
            analysis = reusedAnalysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = -0.30,
            v60m = 0.90,
            history = reusedHistory,
            wallClockTimestamp = ocrTimestamp + 30_000L
        )
        assertTrue("Should live update to 30s with identical frozen OCR analysis", title30.contains("30s"))
        assertTrue("Should live update to 30s চলমান in subtitle", subtitle30.contains("30s চলমান"))
    }

    @Test
    fun existingUiActionCardText_remainsUnchangedApartFromLiveSecondsUpdate() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "DOWNTREND_PULLBACK_UP",
            change5mValue = 0.35,
            change60mValue = -0.75,
            timestamp = 100_000L
        )

        // Without history (live seconds hidden)
        val (titleNoHist, subtitleNoHist) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.35,
            v60m = -0.75,
            history = emptyList(),
            wallClockTimestamp = 120_000L
        )
        assertEquals("DOWN ট্রেন্ডে সাময়িক UP পুলব্যাক • [পুনরায় DOWN পতন শঙ্কা]", titleNoHist)
        assertEquals("পরবর্তী সম্ভাব্য দিক: DOWN • 💡 পুলব্যাক পরামর্শ: এটি মূল পতনের মাঝে সাময়িক UP বাউন্স; বাউন্স শেষ হওয়া পর্যন্ত অপেক্ষা করে পুনরায় ডাউন এন্ট্রি নিন।", subtitleNoHist)

        // With history at 20s elapsed
        val (titleHist, subtitleHist) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.35,
            v60m = -0.75,
            history = listOf(analysis),
            wallClockTimestamp = 120_000L
        )
        assertEquals("DOWN ট্রেন্ডে সাময়িক UP পুলব্যাক • [20s পুনরায় DOWN পতন শঙ্কা]", titleHist)
        assertEquals("পরবর্তী সম্ভাব্য দিক: DOWN • 💡 পুলব্যাক পরামর্শ: এটি মূল পতনের মাঝে সাময়িক UP বাউন্স (20s চলমান); বাউন্স শেষ হওয়া পর্যন্ত অপেক্ষা করে পুনরায় ডাউন এন্ট্রি নিন।", subtitleHist)
    }

    @Test
    fun heuristicPullbackWindow_explicitlyLabeledWithoutFabricatedSeconds() {
        assertEquals(HEURISTIC_WINDOW_LABEL, calculateHeuristicPullbackWindowLabel(0.40, 0.80))
        assertEquals(INSUFFICIENT_HISTORY_MSG, calculateHeuristicPullbackWindowLabel(null, 0.80))
        assertEquals(INSUFFICIENT_HISTORY_MSG, calculateHeuristicPullbackWindowLabel(Double.NaN, 0.80))
    }

    @Test
    fun canHaveDuration_returnsFalse_whenNullableValuesAreNullWithoutFabricatedFallbacks() {
        val matchingHistory = listOf(
            PullbackHistoryPoint(
                val5m = 0.40,
                val60m = -0.80,
                timestamp = 100_000L,
                isValid = true,
                isApproximate = false,
                behaviorCode = "DOWNTREND_PULLBACK_UP"
            )
        )

        // v5m is null
        val resNull5 = evaluateCanHaveDuration(
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            hasValidMetrics = false,
            historyPoints = matchingHistory,
            v5m = null,
            v60m = -0.80,
            behaviorCode = "DOWNTREND_PULLBACK_UP"
        )
        assertFalse("canHaveDuration must be false when v5m is null", resNull5)

        // v60m is null
        val resNull60 = evaluateCanHaveDuration(
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            hasValidMetrics = false,
            historyPoints = matchingHistory,
            v5m = 0.40,
            v60m = null,
            behaviorCode = "DOWNTREND_PULLBACK_UP"
        )
        assertFalse("canHaveDuration must be false when v60m is null", resNull60)

        // Both null
        val resBothNull = evaluateCanHaveDuration(
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            hasValidMetrics = false,
            historyPoints = matchingHistory,
            v5m = null,
            v60m = null,
            behaviorCode = "DOWNTREND_PULLBACK_UP"
        )
        assertFalse("canHaveDuration must be false when both metrics are null", resBothNull)
    }

    @Test
    fun canHaveDuration_returnsFalse_forInvalidApproxNoTradeOrEmptyHistory() {
        val historyPoint = PullbackHistoryPoint(
            val5m = 0.40,
            val60m = -0.80,
            timestamp = 100_000L,
            isValid = true,
            isApproximate = false,
            behaviorCode = "DOWNTREND_PULLBACK_UP"
        )

        // isValid = false
        assertFalse(
            evaluateCanHaveDuration(
                isValid = false,
                isNoTrade = false,
                isApprox = false,
                hasValidMetrics = true,
                historyPoints = listOf(historyPoint),
                v5m = 0.40,
                v60m = -0.80,
                behaviorCode = "DOWNTREND_PULLBACK_UP"
            )
        )

        // isNoTrade = true
        assertFalse(
            evaluateCanHaveDuration(
                isValid = true,
                isNoTrade = true,
                isApprox = false,
                hasValidMetrics = true,
                historyPoints = listOf(historyPoint),
                v5m = 0.40,
                v60m = -0.80,
                behaviorCode = "DOWNTREND_PULLBACK_UP"
            )
        )

        // isApprox = true
        assertFalse(
            evaluateCanHaveDuration(
                isValid = true,
                isNoTrade = false,
                isApprox = true,
                hasValidMetrics = true,
                historyPoints = listOf(historyPoint),
                v5m = 0.40,
                v60m = -0.80,
                behaviorCode = "DOWNTREND_PULLBACK_UP"
            )
        )

        // hasValidMetrics = false
        assertFalse(
            evaluateCanHaveDuration(
                isValid = true,
                isNoTrade = false,
                isApprox = false,
                hasValidMetrics = false,
                historyPoints = listOf(historyPoint),
                v5m = 0.40,
                v60m = -0.80,
                behaviorCode = "DOWNTREND_PULLBACK_UP"
            )
        )

        // historyPoints empty
        assertFalse(
            evaluateCanHaveDuration(
                isValid = true,
                isNoTrade = false,
                isApprox = false,
                hasValidMetrics = true,
                historyPoints = emptyList(),
                v5m = 0.40,
                v60m = -0.80,
                behaviorCode = "DOWNTREND_PULLBACK_UP"
            )
        )

        // Fully valid and matching
        assertTrue(
            evaluateCanHaveDuration(
                isValid = true,
                isNoTrade = false,
                isApprox = false,
                hasValidMetrics = true,
                historyPoints = listOf(historyPoint),
                v5m = 0.40,
                v60m = -0.80,
                behaviorCode = "DOWNTREND_PULLBACK_UP"
            )
        )
    }

    @Test
    fun wallClockProgression_20s_21s_22s_advancesContinuously() {
        val baseTime = 200_000L
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "DOWNTREND_PULLBACK_UP",
            change5mValue = 0.35,
            change60mValue = -0.75,
            timestamp = baseTime
        )
        val history = listOf(analysis)

        val (t20, s20) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.35,
            v60m = -0.75,
            history = history,
            wallClockTimestamp = baseTime + 20_000L
        )
        assertTrue("Shows 20s in title", t20.contains("20s"))
        assertTrue("Shows 20s চলমান in subtitle", s20.contains("20s চলমান"))

        val (t21, s21) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.35,
            v60m = -0.75,
            history = history,
            wallClockTimestamp = baseTime + 21_000L
        )
        assertTrue("Shows 21s in title", t21.contains("21s"))
        assertTrue("Shows 21s চলমান in subtitle", s21.contains("21s চলমান"))

        val (t22, s22) = formatActionCardContent(
            rawTitle = "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন",
            rawSubtitle = "পরবর্তী সম্ভাব্য দিক: নিম্নমুখী",
            analysis = analysis,
            isValid = true,
            isNoTrade = false,
            isApprox = false,
            v5m = 0.35,
            v60m = -0.75,
            history = history,
            wallClockTimestamp = baseTime + 22_000L
        )
        assertTrue("Shows 22s in title", t22.contains("22s"))
        assertTrue("Shows 22s চলমান in subtitle", s22.contains("22s চলমান"))
    }

    @Test
    fun testQuickPrediction_weakPullbackInUptrend_givesUpSignalAndBuyerPower() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "UPTREND_PULLBACK_DOWN",
            change5mValue = -0.02,
            change60mValue = 0.44,
            change1dValue = 1.20,
            direction = TradeDirection.UP
        )
        val pred = calculateQuickPrediction(analysis)
        assertTrue(pred.isValid)
        assertTrue(pred.isUp)
        assertFalse(pred.isDown)
        assertEquals("UP ↗", pred.label)
        // base = 95.45%, dailyBoost = min(5, 1.2 * 2) = 2.4, final = min(99.9, 95.45 + 0.96) = 96.4%
        assertEquals("96.4%", pred.powerPercentageStr)
    }

    @Test
    fun testQuickPrediction_weakBounceInDowntrend_givesDownSignalAndSellerPower() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "DOWNTREND_PULLBACK_UP",
            change5mValue = 0.05,
            change60mValue = -0.50,
            change1dValue = -0.80,
            direction = TradeDirection.DOWN
        )
        val pred = calculateQuickPrediction(analysis)
        assertTrue(pred.isValid)
        assertFalse(pred.isUp)
        assertTrue(pred.isDown)
        assertEquals("DOWN ↘", pred.label)
        // base = 90.0%, dailyBoost = min(5, 0.8 * 2) = 1.6, final = 90.0 + 0.64 = 90.6%
        assertEquals("90.6%", pred.powerPercentageStr)
    }

    @Test
    fun testQuickPrediction_momentumLossDown_givesUpBounceSignal() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "MOMENTUM_LOSS_DOWN",
            change5mValue = -0.04,
            change60mValue = -0.65,
            direction = TradeDirection.DOWN,
            upPercentage = 72.0
        )
        val pred = calculateQuickPrediction(analysis)
        assertTrue(pred.isValid)
        assertTrue(pred.isUp)
        assertEquals("UP ↗", pred.label)
        assertEquals("72.0%", pred.powerPercentageStr)
    }

    @Test
    fun testQuickPrediction_steadyUptrend_givesUpSignalAndPercentage() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "STEADY_ALIGNED_UP",
            change5mValue = 0.40,
            change60mValue = 0.80,
            direction = TradeDirection.UP,
            upPercentage = 84.5
        )
        val pred = calculateQuickPrediction(analysis)
        assertTrue(pred.isValid)
        assertTrue(pred.isUp)
        assertEquals("UP ↗", pred.label)
        // 84.5 + (0.4*4*0.6 + 0.8*2*0.4) = 84.5 + (0.96 + 0.64) = 86.1%
        assertEquals("86.1%", pred.powerPercentageStr)
    }

    @Test
    fun testQuickPrediction_steadyDowntrend_givesDownSignalAndPercentage() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "STEADY_ALIGNED_DOWN",
            change5mValue = -0.50,
            change60mValue = -1.10,
            direction = TradeDirection.DOWN,
            downPercentage = 87.2
        )
        val pred = calculateQuickPrediction(analysis)
        assertTrue(pred.isValid)
        assertTrue(pred.isDown)
        assertEquals("DOWN ↘", pred.label)
        // 87.2 + (0.5*4*0.6 + 1.1*2*0.4) = 87.2 + (1.2 + 0.88) = 89.3%
        assertEquals("89.3%", pred.powerPercentageStr)
    }

    @Test
    fun testQuickPrediction_deadMarketOrNoTrade_givesHold50Percent() {
        val analysis = TradingAnalysis(
            isValid = true,
            isNoTradeZone = true,
            direction = TradeDirection.NEUTRAL
        )
        val pred = calculateQuickPrediction(analysis)
        assertTrue(pred.isValid)
        assertFalse(pred.isUp)
        assertFalse(pred.isDown)
        assertEquals("HOLD", pred.label)
        assertEquals("50.0%", pred.powerPercentageStr)
    }

    @Test
    fun testQuickPrediction_invalid_returnsDashes() {
        val pred = calculateQuickPrediction(null)
        assertFalse(pred.isValid)
        assertEquals("সিগন্যাল", pred.label)
        assertEquals("--", pred.powerPercentageStr)
    }

    @Test
    fun testQuickPrediction_suddenSpikeDown_givesDownSignalWithPatternSubtitle() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "SUDDEN_SPIKE_DOWN",
            change5mValue = -0.65,
            change60mValue = -0.10,
            direction = TradeDirection.DOWN,
            downPercentage = 89.0
        )
        val pred = calculateQuickPrediction(analysis)
        assertTrue(pred.isValid)
        assertTrue(pred.isDown)
        assertFalse(pred.isUp)
        assertEquals("DOWN ↘", pred.label)
        assertEquals("89.0%", pred.powerPercentageStr)
        assertEquals("তীব্র পতন", pred.patternSubtitle)
    }

    @Test
    fun testQuickPrediction_bullTrap_givesDownSignalWithPatternSubtitle() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "BULL_TRAP",
            change5mValue = 0.15,
            change60mValue = -0.30,
            direction = TradeDirection.DOWN,
            downPercentage = 84.0
        )
        val pred = calculateQuickPrediction(analysis)
        assertTrue(pred.isValid)
        assertTrue(pred.isDown)
        assertEquals("DOWN ↘", pred.label)
        assertEquals("84.0%", pred.powerPercentageStr)
        assertEquals("বুল ট্র্যাপ", pred.patternSubtitle)
    }

    @Test
    fun testQuickPrediction_bearTrap_givesUpSignalWithPatternSubtitle() {
        val analysis = TradingAnalysis(
            isValid = true,
            behaviorCode = "BEAR_TRAP",
            change5mValue = -0.20,
            change60mValue = 0.40,
            direction = TradeDirection.UP,
            upPercentage = 83.0
        )
        val pred = calculateQuickPrediction(analysis)
        assertTrue(pred.isValid)
        assertTrue(pred.isUp)
        assertEquals("UP ↗", pred.label)
        assertEquals("83.0%", pred.powerPercentageStr)
        assertEquals("বিয়ার ট্র্যাপ", pred.patternSubtitle)
    }
}
