package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.audio.AudioSignalEngine
import com.example.data.analyzer.PressureBand
import com.example.data.analyzer.PressureDirection
import com.example.data.analyzer.SourceQuality
import com.example.data.analyzer.ThreeTimeframePressureCalculator
import com.example.data.analyzer.ThreeTimeframePressureResult
import com.example.data.models.TradeDirection
import com.example.data.models.TradingAnalysis
import com.example.ui.components.ThreeTimeframePressureDashboardCard
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThreeTimeframePressureUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testUpTextContainsExactEmojiAndFiveRightSideMetricsArePresent() {
        val upResult = ThreeTimeframePressureCalculator.calculate(
            p5m = 0.50,
            p60m = 1.00,
            p1d = 2.00,
            timestamp = 1000L,
            sourceQuality = SourceQuality.VERIFIED
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(pressureResult = upResult)
            }
        }

        // 1. Exact directional text with 🔺 emoji
        composeTestRule.onNodeWithText("UP  🔺").assertIsDisplayed()

        // 2. Five right-side metrics remain present
        composeTestRule.onNodeWithText("UP Share").assertIsDisplayed()
        composeTestRule.onNodeWithText("DOWN Share").assertIsDisplayed()
        composeTestRule.onNodeWithText("UP Energy").assertIsDisplayed()
        composeTestRule.onNodeWithText("DOWN Energy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pressure Band").assertIsDisplayed()
    }

    @Test
    fun testDownTextContainsExactEmojiAndFiveRightSideMetricsArePresent() {
        val downResult = ThreeTimeframePressureCalculator.calculate(
            p5m = -0.50,
            p60m = -1.00,
            p1d = -2.00,
            timestamp = 1000L,
            sourceQuality = SourceQuality.VERIFIED
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(pressureResult = downResult)
            }
        }

        // 1. Exact directional text with 🔻 emoji
        composeTestRule.onNodeWithText("DOWN  🔻").assertIsDisplayed()

        // 2. Five right-side metrics remain present
        composeTestRule.onNodeWithText("UP Share").assertIsDisplayed()
        composeTestRule.onNodeWithText("DOWN Share").assertIsDisplayed()
        composeTestRule.onNodeWithText("UP Energy").assertIsDisplayed()
        composeTestRule.onNodeWithText("DOWN Energy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pressure Band").assertIsDisplayed()
    }

    @Test
    fun testChangingPressureResultUpdatesDisplayedMetrics() {
        val upResult = ThreeTimeframePressureCalculator.calculate(
            p5m = 1.0,
            p60m = 1.0,
            p1d = 1.0,
            timestamp = 1000L,
            sourceQuality = SourceQuality.VERIFIED
        )
        val downResult = ThreeTimeframePressureCalculator.calculate(
            p5m = -1.0,
            p60m = -1.0,
            p1d = -1.0,
            timestamp = 2000L,
            sourceQuality = SourceQuality.VERIFIED
        )

        val resultState = mutableStateOf<ThreeTimeframePressureResult?>(upResult)

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(pressureResult = resultState.value)
            }
        }

        // Initially UP
        composeTestRule.onNodeWithText("UP  🔺").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net/P: +100.00%").assertIsDisplayed()

        // Update to DOWN
        resultState.value = downResult

        // Recomposed with DOWN
        composeTestRule.onNodeWithText("DOWN  🔻").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net/P: -100.00%").assertIsDisplayed()
    }

    @Test
    fun testChangingAnalysisUpdatesDisplayedMetrics() {
        val upAnalysis = TradingAnalysis(
            change5mValue = 1.5,
            change60mValue = 1.0,
            change1dValue = 0.8
        )
        val downAnalysis = TradingAnalysis(
            change5mValue = -1.5,
            change60mValue = -1.0,
            change1dValue = -0.8
        )

        val analysisState = mutableStateOf<TradingAnalysis?>(upAnalysis)

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(analysis = analysisState.value)
            }
        }

        // Initially UP
        composeTestRule.onNodeWithText("UP  🔺").assertIsDisplayed()

        // Update to DOWN
        analysisState.value = downAnalysis

        // Recomposed with DOWN
        composeTestRule.onNodeWithText("DOWN  🔻").assertIsDisplayed()
    }

    @Test
    fun testCooldownRemainingGreaterThanZero_Changing5mUpdatesDisplayedResultImmediately() {
        val initialAnalysis = TradingAnalysis(
            change5mValue = 1.0,
            change60mValue = 0.5,
            change1dValue = 0.5
        )
        val analysisState = mutableStateOf(initialAnalysis)
        val cooldownState = mutableStateOf(25) // Active cooldown

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(
                    analysis = analysisState.value,
                    cooldownRemainingSeconds = cooldownState.value
                )
            }
        }

        composeTestRule.onNodeWithText("UP  🔺").assertIsDisplayed()

        // Change 5m to strongly negative while cooldown is active
        analysisState.value = TradingAnalysis(
            change5mValue = -2.5,
            change60mValue = 0.5,
            change1dValue = 0.5
        )

        // Result updates immediately to DOWN despite active cooldown
        composeTestRule.onNodeWithText("DOWN  🔻").assertIsDisplayed()
    }

    @Test
    fun testChanging60mUpdatesDisplayedResult() {
        val initialAnalysis = TradingAnalysis(
            change5mValue = 0.5,
            change60mValue = 0.5,
            change1dValue = 0.5
        )
        val analysisState = mutableStateOf(initialAnalysis)

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(analysis = analysisState.value)
            }
        }

        composeTestRule.onNodeWithText("UP  🔺").assertIsDisplayed()

        // Change 60m to strongly negative
        analysisState.value = TradingAnalysis(
            change5mValue = 0.5,
            change60mValue = -3.0,
            change1dValue = 0.5
        )

        composeTestRule.onNodeWithText("DOWN  🔻").assertIsDisplayed()
    }

    @Test
    fun testChanging1dUpdatesDisplayedResult() {
        val initialAnalysis = TradingAnalysis(
            change5mValue = 0.5,
            change60mValue = 0.5,
            change1dValue = 0.5
        )
        val analysisState = mutableStateOf(initialAnalysis)

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(analysis = analysisState.value)
            }
        }

        composeTestRule.onNodeWithText("UP  🔺").assertIsDisplayed()

        // Change 1D to strongly negative
        analysisState.value = TradingAnalysis(
            change5mValue = 0.5,
            change60mValue = 0.5,
            change1dValue = -3.0
        )

        composeTestRule.onNodeWithText("DOWN  🔻").assertIsDisplayed()
    }

    @Test
    fun testIncompleteFrameDoesNotDestroyPreviousValidResult() {
        val initialValidAnalysis = TradingAnalysis(
            change5mValue = 1.0,
            change60mValue = 1.0,
            change1dValue = 1.0
        )
        val analysisState = mutableStateOf<TradingAnalysis?>(initialValidAnalysis)

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(analysis = analysisState.value)
            }
        }

        composeTestRule.onNodeWithText("UP  🔺").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net/P: +100.00%").assertIsDisplayed()

        // Transient incomplete frame (e.g. 1D missing)
        analysisState.value = TradingAnalysis(
            change5mValue = 1.0,
            change60mValue = 1.0,
            change1dValue = null
        )

        // Previous valid result is retained and not destroyed
        composeTestRule.onNodeWithText("UP  🔺").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net/P: +100.00%").assertIsDisplayed()
    }

    @Test
    fun testNextValidTripleImmediatelyReplacesOldResult() {
        val initialValidAnalysis = TradingAnalysis(
            change5mValue = 1.0,
            change60mValue = 1.0,
            change1dValue = 1.0
        )
        val analysisState = mutableStateOf<TradingAnalysis?>(initialValidAnalysis)

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(analysis = analysisState.value)
            }
        }

        composeTestRule.onNodeWithText("UP  🔺").assertIsDisplayed()

        // Transient incomplete frame
        analysisState.value = TradingAnalysis(
            change5mValue = 1.0,
            change60mValue = 1.0,
            change1dValue = null
        )
        composeTestRule.onNodeWithText("UP  🔺").assertIsDisplayed()

        // Next valid complete triple arrives (DOWN)
        analysisState.value = TradingAnalysis(
            change5mValue = -1.0,
            change60mValue = -1.0,
            change1dValue = -1.0
        )

        // Immediately replaced with new valid triple
        composeTestRule.onNodeWithText("DOWN  🔻").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net/P: -100.00%").assertIsDisplayed()
    }

    @Test
    fun testSameVisibleValuesRepeated_ZeroAudio_ZeroResultChange() {
        AudioSignalEngine.clearRecordedEventsForTesting()
        val initialAnalysis = TradingAnalysis(
            change5mValue = 0.50,
            change60mValue = 1.00,
            change1dValue = 2.00
        )
        val analysisState = mutableStateOf<TradingAnalysis?>(initialAnalysis)

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(
                    analysis = analysisState.value,
                    isAudioAlertEnabled = true
                )
            }
        }
        composeTestRule.waitForIdle()
        org.junit.Assert.assertEquals(1, AudioSignalEngine.recordedEventsForTesting.size)
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_UP_ALERT, AudioSignalEngine.recordedEventsForTesting.last())

        // Repeated identical frame arrives multiple times
        repeat(5) {
            analysisState.value = TradingAnalysis(
                change5mValue = 0.50,
                change60mValue = 1.00,
                change1dValue = 2.00
            )
            composeTestRule.waitForIdle()
        }

        // Result never emits again, zero extra audio triggered
        org.junit.Assert.assertEquals(1, AudioSignalEngine.recordedEventsForTesting.size)
    }

    @Test
    fun testHiddenSubDisplayJitter_ZeroAudio_ZeroResultChange() {
        AudioSignalEngine.clearRecordedEventsForTesting()
        // Canonical: 5m: 0.01%, 60m: 0.03%, 1D: 0.12%
        val initialAnalysis = TradingAnalysis(
            change5mValue = 0.01,
            change60mValue = 0.03,
            change1dValue = 0.12
        )
        val analysisState = mutableStateOf<TradingAnalysis?>(initialAnalysis)

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(
                    analysis = analysisState.value,
                    isAudioAlertEnabled = true
                )
            }
        }
        composeTestRule.waitForIdle()
        org.junit.Assert.assertEquals(1, AudioSignalEngine.recordedEventsForTesting.size)
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_UP_ALERT, AudioSignalEngine.recordedEventsForTesting.last())

        // Micro-jitter in raw double (e.g. 0.011 vs 0.014) maps to exact same canonical 2-decimal display value
        analysisState.value = TradingAnalysis(
            change5mValue = 0.011,
            change60mValue = 0.032,
            change1dValue = 0.121
        )
        composeTestRule.waitForIdle()
        // No extra sound triggered
        org.junit.Assert.assertEquals(1, AudioSignalEngine.recordedEventsForTesting.size)

        analysisState.value = TradingAnalysis(
            change5mValue = 0.014,
            change60mValue = 0.029,
            change1dValue = 0.119
        )
        composeTestRule.waitForIdle()
        // Still zero extra sound triggered
        org.junit.Assert.assertEquals(1, AudioSignalEngine.recordedEventsForTesting.size)
    }

    @Test
    fun testCanonicalPercentageChangesTriggerExactAudioAlerts() {
        AudioSignalEngine.clearRecordedEventsForTesting()
        val initialAnalysis = TradingAnalysis(
            change5mValue = 0.50,
            change60mValue = 1.00,
            change1dValue = 2.00
        )
        val analysisState = mutableStateOf<TradingAnalysis?>(initialAnalysis)

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(
                    analysis = analysisState.value,
                    isAudioAlertEnabled = true
                )
            }
        }
        composeTestRule.waitForIdle()
        org.junit.Assert.assertEquals(1, AudioSignalEngine.recordedEventsForTesting.size)
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_UP_ALERT, AudioSignalEngine.recordedEventsForTesting.last())

        // 5m canonical change: 0.50 -> 0.51 (direction remains UP, duplicate prevented)
        analysisState.value = TradingAnalysis(
            change5mValue = 0.51,
            change60mValue = 1.00,
            change1dValue = 2.00
        )
        composeTestRule.waitForIdle()
        org.junit.Assert.assertEquals(1, AudioSignalEngine.recordedEventsForTesting.size)
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_UP_ALERT, AudioSignalEngine.recordedEventsForTesting.last())

        // 60m canonical change: 1.00 -> 1.01 (direction remains UP, duplicate prevented)
        analysisState.value = TradingAnalysis(
            change5mValue = 0.51,
            change60mValue = 1.01,
            change1dValue = 2.00
        )
        composeTestRule.waitForIdle()
        org.junit.Assert.assertEquals(1, AudioSignalEngine.recordedEventsForTesting.size)
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_UP_ALERT, AudioSignalEngine.recordedEventsForTesting.last())

        // 1D canonical change: 2.00 -> 2.01 (direction remains UP, duplicate prevented)
        analysisState.value = TradingAnalysis(
            change5mValue = 0.51,
            change60mValue = 1.01,
            change1dValue = 2.01
        )
        composeTestRule.waitForIdle()
        org.junit.Assert.assertEquals(1, AudioSignalEngine.recordedEventsForTesting.size)
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_UP_ALERT, AudioSignalEngine.recordedEventsForTesting.last())
    }

    @Test
    fun testDirectionChangeAudioSignals() {
        AudioSignalEngine.clearRecordedEventsForTesting()
        val initialAnalysis = TradingAnalysis(
            change5mValue = 1.0,
            change60mValue = 1.0,
            change1dValue = 1.0
        )
        val analysisState = mutableStateOf<TradingAnalysis?>(initialAnalysis)

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(
                    analysis = analysisState.value,
                    isAudioAlertEnabled = true
                )
            }
        }
        composeTestRule.waitForIdle()
        org.junit.Assert.assertEquals(1, AudioSignalEngine.recordedEventsForTesting.size)
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_UP_ALERT, AudioSignalEngine.recordedEventsForTesting.last())

        // Change direction to DOWN
        analysisState.value = TradingAnalysis(
            change5mValue = -1.0,
            change60mValue = -1.0,
            change1dValue = -1.0
        )
        composeTestRule.waitForIdle()
        org.junit.Assert.assertEquals(2, AudioSignalEngine.recordedEventsForTesting.size)
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_DOWN_ALERT, AudioSignalEngine.recordedEventsForTesting.last())

        // Change direction back to UP
        analysisState.value = TradingAnalysis(
            change5mValue = 2.0,
            change60mValue = 2.0,
            change1dValue = 2.0
        )
        composeTestRule.waitForIdle()
        org.junit.Assert.assertEquals(3, AudioSignalEngine.recordedEventsForTesting.size)
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_UP_ALERT, AudioSignalEngine.recordedEventsForTesting.last())
    }

    @Test
    fun testIncompleteNoSignalProducesNoSoundAndKeepsLastValidResultVisible() {
        AudioSignalEngine.clearRecordedEventsForTesting()
        val initialValid = TradingAnalysis(
            change5mValue = 1.0,
            change60mValue = 1.0,
            change1dValue = 1.0
        )
        val analysisState = mutableStateOf<TradingAnalysis?>(initialValid)

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(
                    analysis = analysisState.value,
                    isAudioAlertEnabled = true
                )
            }
        }
        composeTestRule.waitForIdle()
        org.junit.Assert.assertEquals(1, AudioSignalEngine.recordedEventsForTesting.size)
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_UP_ALERT, AudioSignalEngine.recordedEventsForTesting.last())

        // Incomplete frame arrives (missing 1D)
        analysisState.value = TradingAnalysis(
            change5mValue = 1.0,
            change60mValue = 1.0,
            change1dValue = null
        )
        composeTestRule.waitForIdle()
        // No sound produced for incomplete
        org.junit.Assert.assertEquals(1, AudioSignalEngine.recordedEventsForTesting.size)
        // Last valid result remains visible
        composeTestRule.onNodeWithText("UP  🔺").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net/P: +100.00%").assertIsDisplayed()
    }

    @Test
    fun testCanonicalPressureValuesAndNormalization() {
        // 5m = -0.01%, 60m = +0.03%, 1D = +0.12%
        val res = ThreeTimeframePressureCalculator.calculate(
            p5m = -0.01,
            p60m = 0.03,
            p1d = 0.12,
            timestamp = 1000L,
            sourceQuality = SourceQuality.VERIFIED
        )

        // UP Share + DOWN Share equals 100.00% within floating-point tolerance
        org.junit.Assert.assertEquals(100.0, res.upSharePercent + res.downSharePercent, 1e-6)

        // Verify values
        org.junit.Assert.assertEquals(99.35, res.upSharePercent, 0.01)
        org.junit.Assert.assertEquals(0.65, res.downSharePercent, 0.01)
        org.junit.Assert.assertEquals(0.005100, res.upEnergy, 0.000001)
        org.junit.Assert.assertEquals(0.000033, res.downEnergy, 0.000001)
        org.junit.Assert.assertEquals(98.70, res.netPressurePercent, 0.01)
        org.junit.Assert.assertEquals(PressureBand.VERY_STRONG_UP, res.pressureBand)
        org.junit.Assert.assertEquals(PressureDirection.UP, res.direction)
    }

    @Test
    fun testDistinctSoundSignatures() {
        org.junit.Assert.assertNotEquals(AudioSignalEngine.SOUND_UP_ALERT, AudioSignalEngine.SOUND_DOWN_ALERT)
        org.junit.Assert.assertEquals("SOUND_UP_ALERT", AudioSignalEngine.SOUND_UP_ALERT)
        org.junit.Assert.assertEquals("SOUND_DOWN_ALERT", AudioSignalEngine.SOUND_DOWN_ALERT)
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_UP_ALERT, ThreeTimeframePressureCalculator.getDeterministicAudioEvent(PressureDirection.UP))
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_DOWN_ALERT, ThreeTimeframePressureCalculator.getDeterministicAudioEvent(PressureDirection.DOWN))
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_NONE, ThreeTimeframePressureCalculator.getDeterministicAudioEvent(PressureDirection.NO_SIGNAL))
        org.junit.Assert.assertEquals(AudioSignalEngine.SOUND_NONE, ThreeTimeframePressureCalculator.getDeterministicAudioEvent(PressureDirection.UP, isDataIncomplete = true))
    }

    @Test
    fun testInstantAutoTradeZeroCooldownDefault() {
        val defaultGate = com.example.data.analyzer.SignalCooldownGate()
        val t0 = 100_000L
        org.junit.Assert.assertTrue(defaultGate.canEmit(t0))
        defaultGate.markEmitted(
            nowElapsedMs = t0,
            p5m = 1.0,
            p60m = 1.0,
            p1d = 1.0,
            direction = PressureDirection.UP,
            pressureBand = PressureBand.VERY_STRONG_UP
        )
        // With 30-second cooldown removed, immediately allows new detection trade dispatch
        org.junit.Assert.assertTrue(defaultGate.canEmit(t0 + 50L))
        org.junit.Assert.assertEquals(0L, defaultGate.remainingMs(t0 + 50L))
    }

    @Test
    fun testParameterizedCooldownWindow() {
        val gate30s = com.example.data.analyzer.SignalCooldownGate(cooldownMs = 30_000L)
        val t0 = 100_000L
        org.junit.Assert.assertTrue(gate30s.canEmit(t0))
        gate30s.markEmitted(
            nowElapsedMs = t0,
            p5m = 1.0,
            p60m = 1.0,
            p1d = 1.0,
            direction = PressureDirection.UP,
            pressureBand = PressureBand.VERY_STRONG_UP
        )
        org.junit.Assert.assertFalse(gate30s.canEmit(t0 + 10_000L))
        org.junit.Assert.assertFalse(gate30s.canEmit(t0 + 29_999L))
        org.junit.Assert.assertTrue(gate30s.canEmit(t0 + 30_000L))
    }

    @Test
    fun testPrimaryTriggeredMatrixDisplayedInBigSquare() {
        val m016Analysis = TradingAnalysis(
            primaryMatrixId = "M016",
            direction = TradeDirection.UP,
            change5mValue = 0.5,
            change60mValue = 0.8,
            change1dValue = 1.2
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ThreeTimeframePressureDashboardCard(analysis = m016Analysis)
            }
        }

        // The big square displays M016 and UP ↗
        composeTestRule.onNodeWithText("M016", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("UP ↗").assertIsDisplayed()
    }
}
